package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun <T, V : AnimationVector> animateValueAsState(
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: AnimationSpec<T> = spring(),
    initialVelocity: T.(currentVelocity: T) -> T = { it },
    visibilityThreshold: T? = null,
    label: String = "customAnimateValueAsState",
): State<T> {
    val animatable = remember { Animatable(targetValue, typeConverter, visibilityThreshold, label) }

    val channel = remember { Channel<T>(Channel.CONFLATED) }

    SideEffect { channel.trySend(targetValue) }

    LaunchedEffect(channel) {
        for (target in channel) {
            val newTarget = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    animatable.animateTo(
                        newTarget,
                        animationSpec,
                        initialVelocity = initialVelocity(newTarget, animatable.velocity)
                    )
                }
            }
        }
    }

    return animatable.asState()
}

@Composable
fun animateDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = spring(),
    initialVelocity: Dp.(currentVelocity: Dp) -> Dp = { it },
    label: String = "animateValueAsState"
): State<Dp> = animateValueAsState(
    targetValue,
    typeConverter = Dp.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateColorAsState(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = spring(),
    initialVelocity: Color.(currentVelocity: Color) -> Color = { it },
    label: String = "animateValueAsState"
): State<Color> = animateValueAsState(
    targetValue,
    typeConverter = remember(targetValue) { Color.VectorConverter(targetValue.colorSpace) },
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = spring(),
    initialVelocity: Float.(currentVelocity: Float) -> Float = { it },
    label: String = "animateValueAsState"
): State<Float> = animateValueAsState(
    targetValue,
    typeConverter = Float.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateIntAsState(
    targetValue: Int,
    animationSpec: AnimationSpec<Int> = spring(),
    initialVelocity: Int.(currentVelocity: Int) -> Int = { it },
    label: String = "animateValueAsState"
): State<Int> = animateValueAsState(
    targetValue,
    typeConverter = Int.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateIntOffsetAsState(
    targetValue: IntOffset,
    animationSpec: AnimationSpec<IntOffset> = spring(),
    initialVelocity: IntOffset.(currentVelocity: IntOffset) -> IntOffset = { it },
    label: String = "animateValueAsState"
): State<IntOffset> = animateValueAsState(
    targetValue,
    typeConverter = IntOffset.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateIntSizeAsState(
    targetValue: IntSize,
    animationSpec: AnimationSpec<IntSize> = spring(),
    initialVelocity: IntSize.(currentVelocity: IntSize) -> IntSize = { it },
    label: String = "animateValueAsState"
): State<IntSize> = animateValueAsState(
    targetValue,
    typeConverter = IntSize.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateOffsetAsState(
    targetValue: Offset,
    animationSpec: AnimationSpec<Offset> = spring(),
    initialVelocity: Offset.(currentVelocity: Offset) -> Offset = { it },
    label: String = "animateValueAsState"
): State<Offset> = animateValueAsState(
    targetValue,
    typeConverter = Offset.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateSizeAsState(
    targetValue: Size,
    animationSpec: AnimationSpec<Size> = spring(),
    initialVelocity: Size.(currentVelocity: Size) -> Size = { it },
    label: String = "animateValueAsState"
): State<Size> = animateValueAsState(
    targetValue,
    typeConverter = Size.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)

@Composable
fun animateRectAsState(
    targetValue: Rect,
    animationSpec: AnimationSpec<Rect> = spring(),
    initialVelocity: Rect.(currentVelocity: Rect) -> Rect = { it },
    label: String = "animateValueAsState"
): State<Rect> = animateValueAsState(
    targetValue,
    typeConverter = Rect.VectorConverter,
    animationSpec = animationSpec,
    initialVelocity = { initialVelocity(it) },
    label = label
)