package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserPreference {
    @Serializable
    @SerialName("app.bsky.actor.defs#adultContentPref")
    data class AdultContent(
        val enabled: Boolean
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#contentLabelPref")
    data class ContentLabel(
        val labelerId: Did? = null,
        val label: String,
        val visibility: String
    ) : UserPreference {
        val ignore = visibility == "ignore"
        val show = visibility == "show"
        val warn = visibility == "warn"
        val hide = visibility == "hide"
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#savedFeedsPref")
    data class SavedFeedsV1(
        val pinned: AtUri,
        val saved: AtUri,
        val timelineIndex: Int? = null
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#savedFeedsPrefV2")
    data class SavedFeedsV2(
        val items: List<SavedFeedItem>
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#personalDetailsPref")
    data class PersonalDetails(
        val birthDate: TimestampISO8601
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#feedViewPref")
    data class FeedView(
        /**
         * is either uri or AT identifier
         */
        val feed: String,
        val hideReplies: Boolean? = null,
        val hideRepliesByUnfollowed: Boolean? = true,
        val hideRepliesByLikeCount: Int? = null,
        val hideReposts: Boolean? = null,
        val hideQuotePosts: Boolean? = null
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#threadViewPref")
    data class ThreadView(
        val sort: String? = null,
        val prioritizeFollowedUsers: Boolean? = null
    ) : UserPreference {
        val isSortingByOldest = sort == "oldest"
        val isSortingByNewest = sort == "newest"
        val isSortingByMostLikes = sort == "most-likes"
        val isSortingByRandom = sort == "random"
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#interestsPref")
    data class Interests(
        val tags: List<String>
    ) : UserPreference {
        init {
            require(tags.size <= 100) {
                "tags must be less than or equal to 100"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#mutedWordsPref")
    data class MutedWords(
        val items: List<MutedWord>
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#hiddenPostsPref")
    data class HiddenPosts(
        val items: List<AtUri>
    ) : UserPreference

    @Serializable
    @SerialName("app.bsky.actor.defs#bskyAppStatePref")
    data class BskyAppState(
        val activeProgressGuide: ActiveProgressGuide? = null,
        val queuedNudges: List<String>? = null,
        val nuxs: List<Nux>? = null // possible value &lt; 100
    ) : UserPreference {
        init {
            if (queuedNudges != null) require(queuedNudges.size <= 1000) {
                "queuedNudges must be less than or equal to 1000"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#labelersPref")
    data class Labelers(
        val labelers: List<Labeler>? = null
    ) : UserPreference
}