@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.blean.ui.theme

import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.nxoim.blean.ui.composeUiCommons.scroll.rememberCupertinoOverscrollFactory

@Composable
expect fun AppTheme(
    overscrollFactory: OverscrollFactory = rememberCupertinoOverscrollFactory(
        MaterialTheme.motionScheme.slowSpatialSpec()
    ),
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)