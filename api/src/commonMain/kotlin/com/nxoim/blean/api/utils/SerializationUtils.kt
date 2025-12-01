package com.nxoim.blean.api.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

//internal inline fun <T : Any> throwPolymorphicSerializerError(targetClass: KClass<T>): Nothing =
//    error("Unable to serialize/deserialize $targetClass in polymorphic serializer")

internal inline fun <reified T : Any> KSerializer<T>.throwPolymorphicSerializerError(
    json: JsonObject
): Nothing = error(
    """Unable to serialize/deserialize ${T::class.qualifiedName ?: T::class.toString()} in polymorphic serializer. 
        |Check the type discriminator of the declared polymorphic serializer
        |. json ${json.toString()}""".trimMargin()
)
