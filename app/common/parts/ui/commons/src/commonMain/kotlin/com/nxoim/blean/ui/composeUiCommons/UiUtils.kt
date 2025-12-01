package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RowScope.Spacer(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun ColumnScope.Spacer(height: Dp) = Spacer(Modifier.height(height))

@Composable
@ReadOnlyComposable
fun PaddingValues.copy(
    start: Dp = this.calculateStartPadding(LocalLayoutDirection.current),
    top: Dp = this.calculateTopPadding(),
    end: Dp = this.calculateEndPadding(LocalLayoutDirection.current),
    bottom: Dp = this.calculateBottomPadding()
) = PaddingValues(start, top, end, bottom)

@Composable
@ReadOnlyComposable
inline fun PaddingValues.copy(
    start:  (Dp) -> Dp = { it },
    top: (Dp) -> Dp = { it },
    end: (Dp) -> Dp = { it },
    bottom: (Dp) -> Dp = { it }
) = PaddingValues(
    start = start(calculateStartPadding(LocalLayoutDirection.current)),
    top = top(calculateTopPadding()),
    end = end(calculateEndPadding(LocalLayoutDirection.current)),
    bottom = bottom(calculateBottomPadding())
)

@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = this.calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        top = this.calculateTopPadding() + other.calculateTopPadding(),
        end = this.calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        bottom = this.calculateBottomPadding() + other.calculateBottomPadding()
    )
}

@Composable
@ReadOnlyComposable
operator fun PaddingValues.minus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = (this.calculateStartPadding(layoutDirection) - other.calculateStartPadding(layoutDirection)),
        top = (this.calculateTopPadding() - other.calculateTopPadding()),
        end = (this.calculateEndPadding(layoutDirection) - other.calculateEndPadding(layoutDirection)),
        bottom = (this.calculateBottomPadding() - other.calculateBottomPadding())
    )
}

operator fun PaddingValues.plus(value: Dp): PaddingValues =
    PaddingValues(
        start = calculateStartPadding(LayoutDirection.Ltr) + value,
        top = calculateTopPadding() + value,
        end = calculateEndPadding(LayoutDirection.Ltr) + value,
        bottom = calculateBottomPadding() + value
    )

operator fun PaddingValues.minus(value: Dp): PaddingValues =
    PaddingValues(start = (calculateStartPadding(LayoutDirection.Ltr) - value).coerceAtLeast(0.dp), top = (calculateTopPadding() - value).coerceAtLeast(0.dp), end = (calculateEndPadding(LayoutDirection.Ltr) - value).coerceAtLeast(0.dp), bottom = (calculateBottomPadding() - value).coerceAtLeast(0.dp))

@OptIn(ExperimentalSharedTransitionApi::class)
class CombinedSharedTransitionScope(
    private val sharedTransition: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope
) : AnimatedVisibilityScope by visibility, SharedTransitionScope by sharedTransition {
    fun Modifier.sharedBounds(
        sharedContentState: SharedContentState,
        enter: EnterTransition = fadeIn(),
        exit: ExitTransition = fadeOut(),
        boundsTransform: BoundsTransform = bunds,
        resizeMode: ResizeMode = scaleToBounds(ContentScale.FillWidth, Center),
        placeHolderSize: PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip = object : OverlayClip {
            override fun getClipPath(
                state: SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density
            ): Path? {
                return state.parentSharedContentState?.clipPathInOverlay
            }
        }
    ) = this.sharedBounds(
        sharedContentState,
        visibility,
        enter,
        exit,
        boundsTransform,
        resizeMode,
        placeHolderSize,
        renderInOverlayDuringTransition,
        zIndexInOverlay,
        clipInOverlayDuringTransition
    )

    fun Modifier.sharedElement(
        state: SharedContentState,
        boundsTransform: BoundsTransform = bunds,
        placeHolderSize: PlaceholderSize = ContentSize,
        renderInOverlayDuringTransition: Boolean = true,
        zIndexInOverlay: Float = 0f,
        clipInOverlayDuringTransition: OverlayClip =  object : OverlayClip {
            override fun getClipPath(
                state: SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density
            ): Path? {
                return state.parentSharedContentState?.clipPathInOverlay
            }
        }
    ) = this.sharedElement(
        state,
        visibility,
        boundsTransform,
        placeHolderSize,
        renderInOverlayDuringTransition,
        zIndexInOverlay,
        clipInOverlayDuringTransition
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CombineSharedTransitionAndAnimatedVisibility(animatedVisibilityScope: AnimatedVisibilityScope, content: @Composable CombinedSharedTransitionScope.() -> Unit) {
    val scope = CombinedSharedTransitionScope(this@CombineSharedTransitionAndAnimatedVisibility, animatedVisibilityScope)
    CompositionLocalProvider(
        LocalCombinedSharedTransitionScope provides scope,
        content = { scope.content() }
    )
}

val LocalCombinedSharedTransitionScope = staticCompositionLocalOf<CombinedSharedTransitionScope?> {
    null
}

@OptIn(ExperimentalSharedTransitionApi::class)
val bunds = BoundsTransform { _, _ ->
    spring(
        dampingRatio = 1.5f,
        stiffness = 2500f,
        visibilityThreshold = Rect(0.000001f, 0.000001f, 0.000001f, 0.000001f)
    )
}

// making an expect actual for this is not worth it imo
@Composable
fun isSoftwareKeyboardOpen(): State<Boolean> {
    val keyboardPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    var previousKeyboardPadding by remember { mutableStateOf(keyboardPadding) }

    return produceState(keyboardPadding != 0.dp, keyboardPadding) {
        value = keyboardPadding < previousKeyboardPadding
        previousKeyboardPadding = keyboardPadding
    }
}

// https://canopas.com/autosizing-textfield-in-jetpack-compose-7a80f0270853
@Composable
fun AutoSizableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    maxLines: Int = Int.MAX_VALUE,
    minFontSize: TextUnit = 1.sp,
    scaleFactor: Float = 0.9f,
    textStyle: TextStyle = LocalTextStyle.current
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        var nFontSize = fontSize

        val calculateParagraph = @Composable {
            Paragraph(
                text = value,
                style = TextStyle(fontSize = nFontSize),
                density = LocalDensity.current,
                resourceLoader = LocalFontLoader.current,
                maxLines = maxLines,
                width = with(LocalDensity.current) { maxWidth.toPx() }
            )
        }

        var intrinsics = calculateParagraph()
        with(LocalDensity.current) {
            while ((intrinsics.height.toDp() > maxHeight || intrinsics.didExceedMaxLines) && nFontSize >= minFontSize) {
                nFontSize *= scaleFactor
                intrinsics = calculateParagraph()
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            maxLines = maxLines,
            textStyle = textStyle.plus(TextStyle(fontSize = nFontSize)),
        )
    }
}

val LocalFocusRequestingIsAllowed = compositionLocalOf<Boolean> { true }

/**
 * is like data class .copy() except you can only specify the
 * visibility threshold
 */
fun <T> SpringSpec<*>.derive(visibilityThreshold: T? = null) =
    SpringSpec(this.dampingRatio, this.stiffness, visibilityThreshold)

val TransformOrigin.Companion.BottomCenter get() = TransformOriginExtensions.BottomCenter
val TransformOrigin.Companion.TopCenter get() = TransformOriginExtensions.TopCenter
val TransformOrigin.Companion.CenterLeft get() = TransformOriginExtensions.CenterLeft
val TransformOrigin.Companion.CenterRight get() = TransformOriginExtensions.CenterRight
val TransformOrigin.Companion.TopLeft get() = TransformOriginExtensions.TopLeft
val TransformOrigin.Companion.TopRight get() = TransformOriginExtensions.TopRight
val TransformOrigin.Companion.BottomLeft get() = TransformOriginExtensions.BottomLeft
val TransformOrigin.Companion.BottomRight get() = TransformOriginExtensions.BottomRight

object TransformOriginExtensions {
    val BottomCenter = TransformOrigin(0.5f, 1f)
    val TopCenter = TransformOrigin(0.5f, 0f)
    val CenterLeft = TransformOrigin(0f, 0.5f)
    val CenterRight = TransformOrigin(1f, 0.5f)
    val TopLeft = TransformOrigin(0f, 0f)
    val TopRight = TransformOrigin(1f, 0f)
    val BottomLeft = TransformOrigin(0f, 1f)
    val BottomRight = TransformOrigin(1f, 1f)
}