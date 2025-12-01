package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalScaffoldPadding = compositionLocalOf { PaddingValues() }

@Composable
fun ProvideScaffoldPadding(
    value: PaddingValues,
    content: @Composable () -> Unit
) = CompositionLocalProvider(
    LocalScaffoldPadding provides value,
    content = content
)