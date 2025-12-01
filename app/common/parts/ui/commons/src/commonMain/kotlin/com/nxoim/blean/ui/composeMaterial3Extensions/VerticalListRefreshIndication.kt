package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.nxoim.blean.ui.composeUiCommons.LocalScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.LocalScrollVisualFactor
import com.nxoim.blean.ui.composeUiCommons.modifiers.BasicPullToRefreshState

@Composable
fun VerticalListRefreshIndication(
    refreshState: BasicPullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        top = 6.dp + defaultVerticalListRefreshTopPadding()
    )
) {
    RefreshIndicator(
        isRefreshing = refreshState.isRefreshing,
        progress = { refreshState.progress },
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.surface.copy(refreshState.progress),
                    Color.Transparent
                )
            )
        )
            .padding(contentPadding)
            .height((refreshState.pullOffsetY / LocalDensity.current.density).dp)
    )
}

@Composable
@Suppress("NOTHING_TO_INLINE")
inline fun defaultVerticalListRefreshTopPadding(
    scrollFactor: Float = LocalScrollVisualFactor.current.fraction.value,
    minimumTopPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
) = lerp(
    LocalScaffoldPadding.current.calculateTopPadding().coerceAtLeast(minimumTopPadding),
    minimumTopPadding,
    scrollFactor
)

