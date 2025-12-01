package com.nxoim.blean.ui.feedViewing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.RefreshState
import com.nxoim.blean.ui.composeMaterial3Extensions.RefreshIndicatorUtils
import com.nxoim.blean.ui.composeUiCommons.LocalScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.copy
import com.nxoim.blean.ui.composeUiCommons.modifiers.BasicPullToRefreshState
import com.nxoim.blean.ui.composeUiCommons.modifiers.rememberBasicPullToRefreshState
import kotlinx.coroutines.flow.drop

@Composable
fun AnimateScrollToTopOnRefreshEffect(
    refreshState: RefreshState,
    lazyListState: LazyListState
) =
    LaunchedEffect(Unit) {
    snapshotFlow { refreshState }
        .drop(1)
        .collect {
            if (it is RefreshState.None) {
                lazyListState.animateScrollToItem(0)
            }
        }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberPullToRefreshStateForM3Indicator(
    refreshState: RefreshState,
    onRefresh: () -> Unit
): BasicPullToRefreshState = rememberBasicPullToRefreshState(
    isRefreshing = refreshState == RefreshState.InProgress,
    onRefresh = onRefresh,
    triggerThreshold = RefreshIndicatorUtils.triggerThreshold,
    animatedPullTargetOffset = 64.dp,
    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
)

@Composable
fun localScaffoldPaddingPlusRefreshPullAndMore(pullToRefreshState: BasicPullToRefreshState): PaddingValues =
    LocalScaffoldPadding.current.copy(
        top = { (it + 16.dp + (pullToRefreshState.pullOffsetY / LocalDensity.current.density).dp).coerceAtLeast(Dp.Hairline) }
    )

val feedHorizontalContentPadding = PaddingValues(horizontal = 16.dp)
val feedVerticalContentPadding = PaddingValues(vertical = 16.dp)
val feedWholeContentPadding = feedHorizontalContentPadding + feedVerticalContentPadding
