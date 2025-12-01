package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.nxoim.blean.composeVideoPlayer.PlaybackState
import com.nxoim.blean.ui.composeUiCommons.Layout
import com.nxoim.blean.ui.composeUiCommons.modifiers.offsetWithMotionFrameOfReference
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class VideoControlsState(
    optionsVisible: Boolean = false
) {
    var optionsVisible by mutableStateOf(optionsVisible)
}

@Composable
fun rememberVideoControlsState() = remember { VideoControlsState() }

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun VideoControls(
    playbackState: PlaybackState,
    onPlayingChange: (Boolean) -> Unit,
    duration: Duration,
    elapsed: Duration,
    onNewDurationRequested: (Duration) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    options: @Composable (contentPadding: PaddingValues) -> Unit,
    state: VideoControlsState = rememberVideoControlsState(),
    modifier: Modifier = Modifier,
    optionsTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = run {
        val motionScheme = MaterialTheme.motionScheme

        {
            (fadeIn(animationSpec = motionScheme.slowEffectsSpec()) +
                    scaleIn(
                        initialScale = 0.92f,
                        animationSpec = motionScheme.slowSpatialSpec()
                    ))
                .togetherWith(fadeOut(animationSpec = motionScheme.fastEffectsSpec()))
        }
    },
    overshootAnimationSpec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.slowSpatialSpec()
) {
    val currentElapsedMillis = elapsed.inWholeMilliseconds.toFloat()
    val maxDurationMillis =
        if (duration.inWholeMilliseconds > 0) duration.inWholeMilliseconds.toFloat() else 1f
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragging by interactionSource.collectIsDraggedAsState()
    var expectedUserChosenDuration by remember { mutableStateOf(elapsed) }

    val elapsedForDisplay = if (isPressed || isDragging)
        expectedUserChosenDuration.inWholeMilliseconds.toFloat()
    else
        currentElapsedMillis

    var overshootOffsetX by remember { mutableFloatStateOf(0f) }


    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 4.dp,
        modifier = modifier
            .offsetWithMotionFrameOfReference() {
                IntOffset(overshootOffsetX.fastRoundToInt(), 0)
            }
    ) {
        AnimatedContent(
            targetState = state.optionsVisible,
            transitionSpec = optionsTransitionSpec,
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) { isOptionsVisible ->
            if (isOptionsVisible) {
                Column(Modifier.fillMaxWidth()) {
                    val containerPadding = 12.dp

                    Layout(Modifier.weight(1f, fill = false)) {
                        options(
                            PaddingValues(
                                start = containerPadding + 4.dp,
                                end = containerPadding,
                                top = containerPadding,
                                bottom = containerPadding
                            )
                        )
                    }

                    Column(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        TextButton(
                            onClick = { state.optionsVisible = false },
                            modifier = Modifier
                        ) {
                            Text("Done")
                        }
                    }
                }
            } else {
                val coroutineScope = rememberCoroutineScope()
                val assumedLayoutWidth = with(LocalDensity.current) { 300.dp.roundToPx() }
                var layoutWidthPx by remember { mutableStateOf(assumedLayoutWidth) }
                val dragSensitivity = layoutWidthPx.toFloat()

                val preciseSlideModifier = Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {
                        val addedFraction = it / dragSensitivity
                        val addedFractionInMillis = maxDurationMillis * addedFraction
                        expectedUserChosenDuration += addedFractionInMillis.toLong().milliseconds
                        if (expectedUserChosenDuration <= Duration.ZERO || expectedUserChosenDuration >= duration) {
                            val overshootRatio = when {
                                expectedUserChosenDuration < Duration.ZERO -> {
                                    expectedUserChosenDuration.inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds
                                }

                                else -> {
                                    (expectedUserChosenDuration - duration).inWholeMilliseconds.toFloat() / duration.inWholeMilliseconds
                                }
                            }

                            overshootOffsetX = (overshootRatio * layoutWidthPx) * 0.3f
                        }
                    },
                    interactionSource = interactionSource,
                    onDragStarted = {
                        coroutineScope.coroutineContext.cancelChildren()
                        expectedUserChosenDuration = elapsed
                    },
                    onDragStopped = {
                        onNewDurationRequested(
                            expectedUserChosenDuration
                                .coerceIn(Duration.ZERO, duration)
                        )
                        if (overshootOffsetX != 0f) coroutineScope.launch {
                            animate(
                                initialValue = overshootOffsetX,
                                targetValue = 0f,
                                animationSpec = overshootAnimationSpec,
                                initialVelocity = it
                            ) { value, velocity ->
                                overshootOffsetX = value
                            }
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .then(preciseSlideModifier)
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconToggleButton(
                            checked = volume == 1f,
                            onCheckedChange = { onVolumeChange(if (it) 1f else 0f) },
                        ) {
                            AnimatedContent(
                                volume == 1f,
                            ) { isVolumeUp ->
                                if (isVolumeUp) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = null
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.VolumeOff,
                                        contentDescription = null
                                    )
                                }
                            }
                        }

                        PlayButton(
                            playbackState,
                            onPlayingChange
                        )
                    }

                    Slider(
                        value = elapsedForDisplay,
                        onValueChange = { newValue ->
                            expectedUserChosenDuration = newValue.toLong().milliseconds
                        },
                        onValueChangeFinished = {
                            onNewDurationRequested(
                                expectedUserChosenDuration
                                    .coerceIn(Duration.ZERO, duration)
                            )
                        },
                        valueRange = 0f..maxDurationMillis,
                        modifier = Modifier
                            .onSizeChanged() { layoutWidthPx = it.width }
                            .weight(1f)
                            .fillMaxWidth(),
                        interactionSource = interactionSource,
                        track = {
                            LinearWavyProgressIndicator(
                                progress = { elapsedForDisplay / it.valueRange.endInclusive },
                                waveSpeed = 24.dp,
                                amplitude = {
                                    if (playbackState is PlaybackState.Stopped || (isPressed || isDragging)) 0f else 1f
                                },
                                modifier = preciseSlideModifier
                            )
                        }
                    )

                    IconButton(
                        onClick = { state.optionsVisible = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    MaterialExpressiveTheme {
        Surface {
            VideoControls(
                playbackState = PlaybackState.Playing,
                onPlayingChange = {},
                duration = Duration.parse("PT1M30S"),
                elapsed = Duration.parse("PT1M30S"),
                onNewDurationRequested = { },
                volume = 1f,
                onVolumeChange = { },
                options = { contentPadding ->
                    VideoOptionsContainer(
                        containerColor = Color.Transparent,
                        contentPadding = contentPadding,
                    ) {
                        SpeedSelector(
                            currentSpeedMultiplier = 1f,
                            onNewSpeedMultiplier = { },
                        )

//                                            HorizontalDivider()

                        Column(
                            Modifier.fillMaxWidth().width(IntrinsicSize.Max),
                            verticalArrangement = spacedBy(4.dp)
                        ) {
                            Setting(
                                title = { Text("Subtitles") },
                                subtitle = { Text("English") },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ShortText,
                                        contentDescription = null,
                                    )
                                },
                                onClick = { },
                                modifier = Modifier
                            )
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun BreathingDot() {
    val breathingEffectAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Spacer(
        Modifier
            .alpha(breathingEffectAlpha)
            .clip(CircleShape)
            .size(8.dp)
            .background(LocalContentColor.current)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayButton(
    playbackState: PlaybackState,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconToggleButton(
        checked = playbackState is PlaybackState.Playing,
        enabled = playbackState !is PlaybackState.Stopped.ByError && playbackState != PlaybackState.Stopped.Buffering,
        onCheckedChange = { onPlayingChange(playbackState !is PlaybackState.Playing) },
        modifier = modifier.width(48.dp)
    ) {
        AnimatedContent(playbackState) { state ->
            when (state) {
                PlaybackState.Playing -> Icon(
                    Icons.Outlined.Pause,
                    contentDescription = null
                )

                PlaybackState.Stopped.Buffering -> BreathingDot()

                PlaybackState.Stopped.ByCompletion -> Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null
                )

                is PlaybackState.Stopped.ByError -> {
                    // nothing
                }

                PlaybackState.Stopped.ByUser -> Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}