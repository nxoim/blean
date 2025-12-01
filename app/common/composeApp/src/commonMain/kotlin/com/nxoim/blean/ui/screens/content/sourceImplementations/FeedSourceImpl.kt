package com.nxoim.blean.ui.screens.content.sourceImplementations

import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.models.account.FeedType
import com.nxoim.blean.models.account.SavedFeed
import com.nxoim.blean.ui.screens.content.feed.FeedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedSourceImpl(private val client: BleanClient.LoggedIn) : FeedSource {

    private fun SavedFeed.toUiSavedFeed() =
        com.nxoim.blean.ui.screens.content.feed.models.SavedFeed(
            id = this.id,
            name = this.name,
            isPinned = this.isPinned,
            avatarUrl = this.avatarUrl,
            description = this.description,
            isOnline = this.isOnline,
            isValid = this.isValid,
            did = this.did,
            cid = this.cid,
            authorDid = this.authorDid,
            uri = this.uri,
            type = when (this.type) {
                FeedType.NormalPosts -> com.nxoim.blean.ui.screens.content.feed.models.FeedType.NormalPosts
                FeedType.Unsupported -> com.nxoim.blean.ui.screens.content.feed.models.FeedType.Unsupported
                FeedType.Video -> com.nxoim.blean.ui.screens.content.feed.models.FeedType.Video
            }
        )

    override fun getFeeds(): Flow<List<com.nxoim.blean.ui.screens.content.feed.models.SavedFeed>?> {
        return client.account.feeds.map { list -> list?.map { it.toUiSavedFeed() } }
    }

    override fun getSavedFeed(uri: String): Flow<com.nxoim.blean.ui.screens.content.feed.models.SavedFeed?> =
        client.account.feeds
            .map { list -> list?.find { it.uri == uri }?.toUiSavedFeed() }
}