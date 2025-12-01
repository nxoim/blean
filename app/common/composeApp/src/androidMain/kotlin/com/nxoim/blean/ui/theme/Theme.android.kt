package com.nxoim.blean.ui.theme

import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun AppTheme(
    overscrollFactory: OverscrollFactory,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark)
            dynamicDarkColorScheme(LocalContext.current)
        else
            dynamicLightColorScheme(LocalContext.current)
    } else {
        if (isDark) darkColorScheme() else lightColorScheme()
    }
    MaterialExpressiveTheme(colorScheme, motionScheme = MotionScheme.expressive()) {
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory
        ) {
            Surface(content = content)
        }
    }
}