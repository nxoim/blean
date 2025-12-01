package com.nxoim.blean.bskyPrimitives

import com.github.michaelbull.result.getOrThrow
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * at://
 */
@Serializable(AtUri.Serializer::class)
@JvmInline
value class AtUri(private val value: String) {
    init {
        require(value.startsWith("at://")) {
            "Can't parse string to AtUri because it's not an at://. was $value"
        }
    }

    override fun toString() = value
//    override fun equals(other: Any?) = value == other
//    override fun hashCode() = value.hashCode()

    object Serializer : StringWrapperSerializer<AtUri>("AtUri", ::AtUri)
}

sealed interface ParsedAtUri {
    /**
     * app.bsky.feed.post
     */
    data class Post(
        val creatorDid: Did, // should be AccountIdentificator??
        val recordKey: String,
    ) : ParsedAtUri

    /**
     * app.bsky.feed.like
     */
    data class Like(
        val repo: Did,
        val recordKey: String, // todo should record key be in primitives?
    ) : ParsedAtUri
}

fun AtUri.parse(): ParsedAtUri {
    val atUriString = this.toString()
    val postRegex = Regex(postRegexString)
    val likeRegex = Regex(likeRegexString)

    return when {
        postRegex.matches(atUriString) -> {
            val matchResult = postRegex.find(atUriString)!!
            val (didString, recordKey) = matchResult.destructured
            ParsedAtUri.Post(
                creatorDid = Did.parse(didString).getOrThrow(),
                recordKey = recordKey
            )
        }

        likeRegex.matches(atUriString) -> {
            val matchResult = likeRegex.find(atUriString)!!
            val (didString, recordKey) = matchResult.destructured
            ParsedAtUri.Like(
                repo = Did.parse(didString).getOrThrow(),
                recordKey = recordKey
            )
        }

        else -> error("failed to parse $this")
    }
}

private const val postRegexString = """^at://([^/]+)/app\.bsky\.feed\.post/([^/]+)$"""
private const val likeRegexString = """^at://([^/]+)/app\.bsky\.feed\.like/([^/]+)$"""