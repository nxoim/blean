package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.models.account.MutedWord
import com.nxoim.blean.api.models.account.UserPreference
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.client.UserDataRepositories
import com.nxoim.blean.models.account.FeedType
import com.nxoim.blean.models.account.MuteAccountTarget
import com.nxoim.blean.models.account.MuteContentTarget
import com.nxoim.blean.models.account.SavedFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

private const val logTag = "BleanAccount"

class BleanAccount(
    private val accountApi: AccountApi,
    private val feedApi: FeedApi,
    private val userDataRepository: UserDataRepositories,
    private val onAuthenticationContextRequest: suspend () -> AuthenticationContext,
    private val clientCoroutineScope: CoroutineScope,
    private val logger: Logger
) {
    private val scopeContext = clientCoroutineScope.coroutineContext
    /**
     * note: should be pageable? last i remember is the api returns all of them as a list
     */
    val feeds = userDataRepository.feeds
        .get()
        .stateIn(
            clientCoroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    suspend fun refreshAccountDetails() {
        withContext(scopeContext) {
            logger.v(tag = logTag) { "refreshing feeds. getting preferences" }

            accountApi.getPreferences(onAuthenticationContextRequest())
                .onSuccess { preferencesResponse ->
                    logger.v(tag = logTag) { "got preferences" }

                    val savedFeedsButNotReally = preferencesResponse.preferences
                        .firstOrNull { it is UserPreference.SavedFeedsV2 }
                            as? UserPreference.SavedFeedsV2

                    val mutedWords = preferencesResponse.preferences
                        .firstOrNull { it is UserPreference.MutedWords }
                            as? UserPreference.MutedWords

                    savedFeedsButNotReally?.let { getAndReplaceCachedFeeds(it) }
                    mutedWords?.let { replaceCachedMutedWords(it.items) }
                }
                .onFailure {
                    logger.e(tag = logTag) {"failed to get preferences $it with $it" }
                }
        }
    }

    private suspend fun getAndReplaceCachedFeeds(savedFeedsV2: UserPreference.SavedFeedsV2) {
        withContext(scopeContext) {
            logger.v(tag = logTag) { "got ${savedFeedsV2.items.size} feeds. processing and saving to repository" }

            val feedGeneratorDataToLoad = savedFeedsV2.items.mapNotNull {
                if (it.isFeed) it else null
            }

            val outdatedFeeds = userDataRepository.feeds
                .get()
                .first()
                .asSequence()
                .filter { !feedGeneratorDataToLoad.map { it.value }.contains(it.uri.toString()) }
                .toList()

            userDataRepository.feeds.delete(outdatedFeeds)

            val savedFeeds = feedGeneratorDataToLoad.mapNotNull {
                feedApi
                    .getFeedGenerator(onAuthenticationContextRequest(), it.value)
                    .get()
                    ?.let { feedGenerator ->
                        SavedFeed(
                            id = it.id,
                            name = feedGenerator.view.displayName,
                            isPinned = it.pinned,
                            avatarUrl = feedGenerator.view.avatar,
                            description = feedGenerator.view.description,
                            isOnline = feedGenerator.isOnline,
                            isValid = feedGenerator.isValid,
                            did = feedGenerator.view.did,
                            cid = feedGenerator.view.cid.toString(),
                            authorDid = feedGenerator.view.creator.did,
                            uri = it.value,
                            type = if (feedGenerator.view.isVideoFeed)
                                FeedType.Video
                            else if (feedGenerator.view.isDefaultKindOfFeed)
                                FeedType.NormalPosts
                            else
                                FeedType.Unsupported
                        )
                    }
            }

            userDataRepository.feeds.saveOrUpdate(savedFeeds)
        }
    }

    private suspend fun replaceCachedMutedWords(mutedWords: List<MutedWord>) {
        withContext(scopeContext) {
            val wordsInUsableFormat = mutedWords
                .asSequence()
                .map {
                    com.nxoim.blean.models.account.MutedWord(
                        id = it.id,
                        word = it.value,
                        accountTarget = when {
                            it.isTargetingAllActors -> MuteAccountTarget.All
                            it.isTargetingExcludeFollowing -> MuteAccountTarget.ExcludeFollowedAccounts
                            else -> error("unknown target ${it.actorTarget}")
                        },
                        contentTarget = when {
                            it.isTargetingContent && it.isTargetingTags -> MuteContentTarget.All
                            it.isTargetingTags -> MuteContentTarget.Tag
                            it.isTargetingContent -> MuteContentTarget.Content
                            else -> error("how did this happen jerry")
                        },
                        expiresOnISO8601 = it.expiresAt
                    )
                }

            userDataRepository.mutedWords.saveOrUpdate(wordsInUsableFormat)
        }
    }
}