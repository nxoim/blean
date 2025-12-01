package com.nxoim.blean.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
actual fun AppTheme(
    overscrollFactory: OverscrollFactory,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        if (isDark) darkColorScheme() else lightColorScheme(),
    ) {
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory
        ) {
            Surface(content = content)
        }
    }
}