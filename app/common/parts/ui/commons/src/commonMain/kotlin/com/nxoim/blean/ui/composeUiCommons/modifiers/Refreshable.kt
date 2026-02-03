package com.nxoim.blean.ui.composeUiCommons.modifiers

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class BasicPullToRefreshState(
    initialIsRefreshing: Boolean,
    private val scope: CoroutineScope,
    private val onRefresh: () -> Unit,
    private val triggerThresholdPx: () -> Float,
    private val animatedPullTargetOffsetPx: () -> Float,
    private val animationSpec: () -> AnimationSpec<Float>,
) {
    private var _pullOffsetY by mutableFloatStateOf(0f)
    private var _pullProgressTrigger by mutableFloatStateOf(0f)

    private var _pullOffsetYAnimationJob: Job? = null
    private var _pullProgressTriggerAnimationJob: Job? = null

    val pullOffsetY get() = _pullOffsetY
    val progress get() = _pullProgressTrigger / triggerThresholdPx()

    var isRefreshing by mutableStateOf(initialIsRefreshing)
        private set

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // When the user pulls up (before refresh)
            // Consume the upward scroll and reduce the pull offset

            if (source == NestedScrollSource.UserInput && available.y < 0 && _pullOffsetY > 0 && !isRefreshing) {
                _pullOffsetYAnimationJob?.cancel()
                _pullProgressTriggerAnimationJob?.cancel()
                val consumed = consume(available.y)
                _pullProgressTrigger = _pullOffsetY
                return consumed
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // When the user pulls down
            // Capture the downward scroll and update pull offset and progress

            if (source == NestedScrollSource.UserInput && available.y > 0 && !isRefreshing) {
                _pullOffsetYAnimationJob?.cancel()
                _pullProgressTriggerAnimationJob?.cancel()
                val consumedScroll = consume(available.y)
                _pullProgressTrigger = _pullOffsetY
                return consumedScroll
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // When the user releases or flings
            // If pull progress exceeds threshold, trigger refresh
            // Animate pull offset back to 0 or to the animated target if refreshing

            if (_pullProgressTrigger > triggerThresholdPx() && !isRefreshing) {
                onRefresh()
            }

            if (_pullProgressTrigger != 0f) {
                _pullOffsetYAnimationJob?.cancel()
                _pullProgressTriggerAnimationJob?.cancel()

                _pullOffsetYAnimationJob = scope.launch {
                    animate(
                        initialValue = _pullOffsetY,
                        targetValue =  if (isRefreshing) animatedPullTargetOffsetPx() else 0f,
                        animationSpec = animationSpec(),
                        initialVelocity = if (isRefreshing) 0f else available.y
                    ) { value, _ -> _pullOffsetY = value }
                }
                _pullProgressTriggerAnimationJob = scope.launch {
                    animate(
                        initialValue = _pullProgressTrigger,
                        targetValue = if (isRefreshing) triggerThresholdPx() else 0f,
                        animationSpec = animationSpec(),
                        initialVelocity = if (isRefreshing) 0f else available.y
                    ) { value, _ -> _pullProgressTrigger = value }
                }
            }

            // Don't consume any velocity, so the list can fling freely
            return Velocity.Zero
        }

        // Helper function to consume scroll delta and update pull offset
        private fun consume(delta: Float): Offset {
            val newOffset = (_pullOffsetY + delta).coerceAtLeast(0f)
            val consumed = newOffset - _pullOffsetY
            _pullOffsetY = newOffset
            return Offset(0f, consumed)
        }
    }

    internal fun update(isRefreshing: Boolean) {
        // When the refresh state changes (e.g., refresh operation completes)
        // Animate pull offset back to 0, hiding the indicator

        if (this.isRefreshing != isRefreshing) {
            _pullOffsetYAnimationJob?.cancel()
            _pullProgressTriggerAnimationJob?.cancel()
            this.isRefreshing = isRefreshing
            _pullOffsetYAnimationJob = scope.launch {
                animate(
                    initialValue = _pullOffsetY,
                    targetValue = if (isRefreshing) animatedPullTargetOffsetPx() else 0f,
                    animationSpec = animationSpec()
                ) { value, _ -> _pullOffsetY = value }
            }
            _pullProgressTriggerAnimationJob = scope.launch {
                animate(
                    initialValue = _pullProgressTrigger,
                    targetValue = if (isRefreshing) triggerThresholdPx() else 0f,
                    animationSpec = animationSpec()
                ) { value, _ -> _pullProgressTrigger = value }
            }
            scope.launch {
                joinAll(
                    _pullOffsetYAnimationJob ?: Job(),
                    _pullProgressTriggerAnimationJob ?: Job()
                )
            }
        }
    }
}

/**
 * @param triggerThreshold Refresh gesture confirmation threshold.
 * When gesture is released and gesture offset equals, or is more
 * than this value, then [onRefresh] will be called.
 *
 * @param animatedPullTargetOffset Separate value from trigger threshold that
 * represents the offset when [isRefreshing] is true. It is helpful to preserve
 * natural\scrolling feeling when your layout moves with the refresh
 * gesture and [triggerThreshold] does not match your
 * refresh indicator's height. Set this value to your expected
 * refresh indicator height when [isRefreshing] is true.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberBasicPullToRefreshState(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    triggerThreshold: Dp = 32.dp,
    animatedPullTargetOffset: Dp = triggerThreshold * 2,
    animationSpec: AnimationSpec<Float> = spring()
): BasicPullToRefreshState {
    val scope = rememberCoroutineScope()
    val refreshThresholdPx = with(LocalDensity.current) { triggerThreshold.toPx() }
    val animatedPullTargetOffsetPxResolved = with(LocalDensity.current) { animatedPullTargetOffset.toPx() }

    return remember {
        BasicPullToRefreshState(
            initialIsRefreshing = isRefreshing,
            scope = scope,
            onRefresh = onRefresh,
            triggerThresholdPx = { refreshThresholdPx },
            animatedPullTargetOffsetPx = { animatedPullTargetOffsetPxResolved },
            animationSpec = { animationSpec },
        )
    }.apply {
        this.update(isRefreshing)
    }
}

fun Modifier.pullToRefresh(
    state: BasicPullToRefreshState,
    enabled: Boolean = true,
) = if (enabled) nestedScroll(state.nestedScrollConnection) else this


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val triggerThreshold = 48.dp
    val animatedPullTargetOffset = 64.dp

    val state = rememberBasicPullToRefreshState(
        isRefreshing = isRefreshing,
        triggerThreshold = triggerThreshold,
        animatedPullTargetOffset = animatedPullTargetOffset,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                delay(2000)
                isRefreshing = false
            }
        }
    )

    MaterialExpressiveTheme {
        Surface {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(state)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    item {
                        RefreshIndicator(
                            isRefreshing = state.isRefreshing,
                            progress = { state.progress },
                            modifier = Modifier.height((state.pullOffsetY / LocalDensity.current.density).dp)
                        )
                    }
                    items(50) { index ->
                        Text(text = "Item ${index + 1}", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    rotationCycles: Int = 1,
    rotationAnimationSpec: AnimationSpec<Float> = spring(stiffness = 50f),
) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    var additionalRotationDegrees by remember { mutableStateOf(0f) }.apply {
        if (!isRefreshing) value = (progress() * (-36f /* not an error , its 36*/ * rotationCycles))
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            coroutineScope.launch {
                rotation.animateTo(
                    targetValue = 1f,
                    animationSpec = rotationAnimationSpec,
                    initialVelocity = 0.5f
                )
                rotation.snapTo(0f)
            }
        }
    }

    Box(
        modifier
            .graphicsLayer() { alpha = progress() }
            .graphicsLayer() {
                val coercedProgress = progress().fastCoerceIn(0f, 1f)
                // scaling like this for the line not to get changed due to
                // changing measurements during resizing
                scaleX = coercedProgress
                scaleY = coercedProgress
            }
            .graphicsLayer() {
                rotationZ = (rotation.value * (-360 * rotationCycles)) + additionalRotationDegrees
            }
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = {
                // so the progress spring does not affect the circle
                // when refreshing
                if (isRefreshing) 1f else progress()
            },
            amplitude = { 1f },
            trackColor = Color.Transparent,
            waveSpeed = if (isRefreshing)
                WavyProgressIndicatorDefaults.CircularWavelength
            else
                0.dp,
            modifier = Modifier
                .size(/* CircularProgressIndicatorTokens.WaveSize */ 48.dp)
                // make sure it gets squished properly if it does get squished
                .aspectRatio(1f)
        )
    }
}