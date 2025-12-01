package com.nxoim.blean.shared

import BuildConfig
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.JvmInline

// wrapper needed because this way each instance will be unique
class DeeplinkInstance(uri: String) {
    val value = Deeplink.parseFrom(uri)
}

/**
 * A [StateFlow] of [DeeplinkInstance]s. Is a stateflow because this must have an initial
 * value upon app start, must be observable, and platform independent.
 *
 * Each deeplink emission, even repeated emissions of the same deeplinks,
 * will be reflected in this state flow because the state flow is not able
 * to tell the difference in contents of [DeeplinkInstance] since it's a regular class and each
 * instance of a regular class is unique. This is intentional
 */
typealias DeeplinkStateFlow = StateFlow<DeeplinkInstance?>

// aligned with the parameters the api wants
sealed interface Deeplink {
    @JvmInline
    value class OAuthContinuation(val rawUri: String) : Deeplink

    // dont forget to also add these to the android manifest
    sealed interface BlueskyDeeplink : Deeplink {
        @JvmInline
        value class Post(val uri: String) : BlueskyDeeplink

        @JvmInline
        value class Account(val identificator: AccountIdentificator) : BlueskyDeeplink

        @JvmInline
        value class StarterPack(val uri: String) : BlueskyDeeplink

        @JvmInline
        value class List(val uri: String) : BlueskyDeeplink

        @JvmInline
        value class Feed(val uri: String) : BlueskyDeeplink

        data object Unknown : BlueskyDeeplink

        companion object {
            fun parseFrom(rawUrl: String) = when {
                rawUrl.startsWith(profileUrl) -> {
                    when {
                        rawUrl.contains(postSegment) -> Post(rawUrl)
                        rawUrl.contains(feedSegment) -> Feed(rawUrl)
                        else -> Account(
                            AccountIdentificator.Handle(
                                rawUrl.removePrefix(profileUrl)
                            )
                        )
                    }
                }
                rawUrl.startsWith(starterPackUrl) -> StarterPack(rawUrl)
                rawUrl.startsWith(listsUrl) -> List(rawUrl)
                rawUrl.startsWith(postUrl) -> Post(rawUrl)
                else -> Unknown
            }
        }
    }

    data object Unknown : Deeplink

    companion object {
        fun parseFrom(rawUri: String) = when {
            rawUri.startsWith(oauthContinuationPrefix) -> OAuthContinuation(rawUri)
            rawUri.startsWith(baseUrl) -> BlueskyDeeplink.parseFrom(rawUri)
            else -> Unknown
        }
    }
}

private const val oauthContinuationPrefix = "${BuildConfig.Environment.defaultOauthEndpoint}/oauth2/callback?"

private const val baseUrl = "https://bsky.app"
private const val profileUrl = "$baseUrl/profile/"
private const val postUrl = "$baseUrl/post/"
private const val feedSegment = "/feed/"
private const val postSegment = "/post/"
private const val starterPackUrl = "$baseUrl/starter-pack/"
private const val listsUrl = "$baseUrl/lists/"