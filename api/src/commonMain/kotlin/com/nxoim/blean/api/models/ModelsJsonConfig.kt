package com.nxoim.blean.api.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val modelsJsonConfig = Json {
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "\$type"
    decodeEnumsCaseInsensitive = true
}