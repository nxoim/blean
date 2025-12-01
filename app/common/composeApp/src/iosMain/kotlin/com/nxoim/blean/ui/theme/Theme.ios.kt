package com.nxoim.blean.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun AppTheme(
    overscrollFactory: OverscrollFactory,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        if (isDark) darkColorScheme() else lightColorScheme(),
        motionScheme = MotionScheme.expressive()
    ) {
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory
        ) {
            Surface(content = content)
        }
    }
}