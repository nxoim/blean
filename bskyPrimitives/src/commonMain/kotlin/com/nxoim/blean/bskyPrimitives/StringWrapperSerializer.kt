package com.nxoim.blean.bskyPrimitives

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Makes it easy to create wrappers for strings for various id's and stuff
 * that still need to be represented as regular strings in json
 *
 * Example:
 * ```kotlin
 * @Serializable(with = StringHolder.Serializer::class)
 * data class StringHolder(val value: String) {
 *     init { validate(value) } // validate if needed
 *
 *     object Serializer : StringWrapperSerializer<StringHolder>("SerialName", ::StringHolder)
 * }
 * ```
 */
abstract class StringWrapperSerializer<T : Any>(
    serialName: String,
    private val factory: (String) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.toString())
    }
    override fun deserialize(decoder: Decoder) = factory(decoder.decodeString())
}