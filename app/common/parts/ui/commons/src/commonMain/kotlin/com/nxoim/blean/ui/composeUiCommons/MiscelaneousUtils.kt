package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.runtime.Composable
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun coilImageRequest(url: String, placeholder: String? = null) = ImageRequest.Builder(LocalPlatformContext.current)
    .data(url)
    .placeholderMemoryCacheKey(placeholder)
    .memoryCacheKey(url)
    .crossfade(true)
    .build()