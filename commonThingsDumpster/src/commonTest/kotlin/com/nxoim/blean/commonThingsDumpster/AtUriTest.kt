package com.nxoim.blean.commonThingsDumpster

import com.github.michaelbull.result.getOrThrow
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.ParsedAtUri
import com.nxoim.blean.bskyPrimitives.parse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtUriTest {
    @Test
    fun `parse valid post AtUri`() {
        val atUriString = "at://did:plc:xyz123/app.bsky.feed.post/3k4j2l5m6n"
        val atUri = AtUri(atUriString)
        val expected = ParsedAtUri.Post(
            creatorDid = Did.parse("did:plc:xyz123").getOrThrow(),
            recordKey = "3k4j2l5m6n"
        )
        assertEquals(expected, atUri.parse())
    }

    @Test
    fun `parse invalid AtUri scheme`() {
        val atUriString = "http://example.com"
        assertFailsWith<IllegalArgumentException> {
            AtUri(atUriString)
        }
    }

    @Test
    fun `parse AtUri with unsupported type`() {
        val atUriString = "at://did:plc:xyz123/app.bsky.actor.profile/self"
        val atUri = AtUri(atUriString)
        assertFailsWith<IllegalStateException> {
            atUri.parse()
        }
    }

    @Test
    fun `parse AtUri with missing parts`() {
        val atUriString = "at://did:plc:xyz123/app.bsky.feed.post/"
        val atUri = AtUri(atUriString)
        // This will fail because split will result in less than 5 parts
        // and Did.parse might fail or recordKey will be empty depending on split behavior
        assertFailsWith<Exception> { // Could be IndexOutOfBoundsException or specific from Did.parse
            atUri.parse()
        }
    }

    @Test
    fun `AtUri toString returns original value`() {
        val atUriString = "at://did:plc:abc/app.bsky.feed.post/123"
        val atUri = AtUri(atUriString)
        assertEquals(atUriString, atUri.toString())
    }
}
