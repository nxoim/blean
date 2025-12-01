package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import com.nxoim.blean.ui.composeUiCommons.BaseButton
import com.nxoim.blean.ui.composeUiCommons.Layout
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.composeUiCommons.animateDpAsState
import com.nxoim.blean.ui.composeUiCommons.animateFloatAsState
import kotlinx.coroutines.flow.conflate
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediumInteractionButton(
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: MediumInteractionButtonColors = MediumInteractionButtonDefaults.colors,
    animationSpecs: MediumInteractionButtonAnimationSpecs = MediumInteractionButtonDefaults.animationSpecs,
    interactionSpecs: MediumInteractionButtonInteractionSpecs = MediumInteractionButtonDefaults.interactionSpecs,
    shapes: MediumInteractionButtonMorphShapes = MediumInteractionButtonDefaults.Shapes.middle,
    label: (@Composable () -> Unit)? = null,
    icon: @Composable () -> Unit,
    scope: SpaceFabricScope? = LocalSpaceFabricScope.current,
) {
    val spaceFabricItemState = scope?.registerAndRememberItemState()

    BaseButton(
        onClick = onClick,
        enabled = enabled,
    ) { clickableModifier ->
        val isPressed by if (spaceFabricItemState == null) {
            produceState(false) {
                interactions.conflate().collect {
                    value = (it is PressInteraction.Press)
                }
            }
        } else {
            LaunchedEffect(spaceFabricItemState) {
                interactions.conflate().collect {
                    if (it is PressInteraction.Press) {
                        spaceFabricItemState.press()
                    } else if (it is PressInteraction.Release || it is PressInteraction.Cancel) {
                        spaceFabricItemState.release()
                    }
                }
            }

            remember(spaceFabricItemState) { derivedStateOf { spaceFabricItemState.isEpicentre } }
        }

        val impactStrength by remember {
            derivedStateOf {
                when {
                    isPressed -> 1f
                    !isPressed && scope != null && scope.epicentreExists -> {
                        val distanceFromEpicentre =
                            abs(spaceFabricItemState!!.distanceFromEpicentre)

                        (1f / distanceFromEpicentre) / interactionSpecs.neighborImpactFalloff
                    }

                    else -> 1f
                }
            }
        }

        val backgroundColor by animateColorAsState(
            targetValue = when {
                !enabled -> colors.disabledBackground
                isPressed -> if (selected)
                    colors.pressedSelectedBackground
                else
                    colors.pressedUnselectedBackground

                else -> if (selected)
                    colors.idleSelectedBackground
                else
                    colors.idleUnselectedBackground
            },
            animationSpec = animationSpecs.colorAnimationSpec()
        )

        val contentColor by animateColorAsState(
            targetValue = when {
                !enabled -> colors.disabledContent
                isPressed -> if (selected) colors.pressedSelectedContent
                else colors.pressedUnselectedContent

                else -> if (selected) colors.idleSelectedContent
                else colors.idleUnselectedContent
            },
            animationSpec = animationSpecs.colorAnimationSpec()
        )

        val scale by animateFloatAsState(
            if (isPressed) animationSpecs.pressedScale else animationSpecs.idleScale,
            animationSpec = animationSpecs.motionSpec(),
            initialVelocity = {
                if (isPressed)
                    interactionSpecs.scaleInitialVelocity
                else
                    interactionSpecs.idleInitialVelocity + it
            }
        )

        val pressPadding by animateDpAsState(
            targetValue = when {
                isPressed -> interactionSpecs.pressedAdditionalPadding
                scope != null && scope.epicentreExists -> -interactionSpecs.pressedAdditionalPadding * impactStrength
                else -> 0.dp
            },
            animationSpec = animationSpecs.motionSpec(),
            initialVelocity = {
                if (isPressed) interactionSpecs.pressedPaddingInitialVelocity
                else if (scope != null && scope.epicentreExists) -interactionSpecs.pressedPaddingInitialVelocity * impactStrength
                else it
            }
        )

        val morphProgress by animateFloatAsState(
            targetValue = when {
                isPressed -> 0.5f
                selected -> 1f
                else -> 0f
            },
            animationSpec = animationSpecs.motionSpec(),
            initialVelocity = {
                if (isPressed)
                    interactionSpecs.shapePressedInitialVelocity
                else
                    interactionSpecs.shapeIdleInitialVelocity + it
            }
        )

        val clipShape = remember {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val bounds: Rect

                    val path = when {
                        morphProgress < 0.5f -> {
                            val localProgress = morphProgress / 0.5f
                            Morph(shapes.idle(density, size), shapes.pressed(density, size))
                                .also { bounds = it.calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) } }
                                .toPath(localProgress)
                        }
                        else -> {
                            val localProgress = (morphProgress - 0.5f) / 0.5f
                            Morph(shapes.pressed(density, size), shapes.selected(density, size))
                                .also { bounds = it.calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) } }
                                .toPath(localProgress)
                        }
                    }

                    val matrix = Matrix()

                    matrix.translate(-bounds.left, -bounds.top)

                    path.transform(matrix)
                    return Outline.Generic(path)
                }
            }
        }

        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            Row(
                modifier
                    .graphicsLayer {
                        clip = true
                        shape = clipShape
                    }
                    .drawBehind { drawRect(SolidColor(backgroundColor)) }
                    .then(clickableModifier)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .widthResizing { pressPadding },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Layout(Modifier.size(18.dp)) { icon() }

                label?.let {
                    Spacer(2.dp)
                    it.invoke()
                }
            }
        }
    }
}


@Preview
@Composable
private fun MediumInteractionButtonPreview() {
    data class ButtonData(val label: String, val selected: Boolean, val icon: ImageVector, val shapes: MediumInteractionButtonMorphShapes)

    val buttons = remember {
        mutableStateListOf(
            ButtonData("1", false, Icons.Default.Add, MediumInteractionButtonDefaults.Shapes.start),
            ButtonData("2", true, Icons.Default.Add, MediumInteractionButtonDefaults.Shapes.middle),
            ButtonData("3", false, Icons.Default.Add, MediumInteractionButtonDefaults.Shapes.end),
        )
    }

    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    WIthSpaceFabricScope {
                        buttons.forEach { buttonData ->
                            key(buttonData) {
                                MediumInteractionButton(
                                    onClick = {
                                        val index = buttons.indexOf(buttonData)
                                        buttons[index] = buttonData.copy(selected = !buttonData.selected)
                                    },
                                    selected = buttonData.selected,
                                    label = { Text(buttonData.label) },
                                    icon = { Icon(buttonData.icon, contentDescription = null) },
                                    shapes = buttonData.shapes,
                                )
                            }
                        }
                    }
                }
                BaseButton(
                    onClick = { buttons.shuffle() }
                ) {
                    Text("Shuffle", modifier = it.padding(8.dp))
                }

            }
        }
    }
}


object MediumInteractionButtonDefaults {
//    private val fallbackMotionSpec = spring<Any>(dampingRatio = 0.95f, stiffness = 350f)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    val animationSpecs = object : MediumInteractionButtonAnimationSpecs {
        override val pressedScale = 0.90f
        override val idleScale = 1f

        @Composable
        @ReadOnlyComposable
        override fun <T> colorAnimationSpec(): AnimationSpec<T> = tween(100)

        @Composable
        @ReadOnlyComposable
        override fun <T> motionSpec(): AnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()
    }

    val interactionSpecs = MediumInteractionButtonInteractionSpecs(
        neighborImpactFalloff = 2f,
        pressedAdditionalPadding = 4.dp,
        scaleInitialVelocity = -5f,
        idleInitialVelocity = 2f,
        pressedPaddingInitialVelocity = 550.dp,
        shapePressedInitialVelocity = 5f,
        shapeIdleInitialVelocity = -5f
    )

    val colors
        @Composable
        @ReadOnlyComposable
        get() = MediumInteractionButtonColors(
            idleSelectedBackground = MaterialTheme.colorScheme.secondary,
            idleSelectedContent = MaterialTheme.colorScheme.onSecondary,
            idleUnselectedBackground = MaterialTheme.colorScheme.surfaceContainer,
            idleUnselectedContent = MaterialTheme.colorScheme.onSurface,
            pressedUnselectedBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
            pressedUnselectedContent = MaterialTheme.colorScheme.onSurfaceVariant,
            pressedSelectedBackground = MaterialTheme.colorScheme.secondary,
            pressedSelectedContent = MaterialTheme.colorScheme.onSecondary,
            disabledBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContent = MaterialTheme.colorScheme.onSurfaceVariant
        )

    object Shapes {
        private fun polygonFromCorners(
            size: Size, br: Float, bl: Float, tl: Float, tr: Float
        ): RoundedPolygon = RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            perVertexRounding = listOf(
                CornerRounding(tl),
                CornerRounding(tr),
                CornerRounding(br),
                CornerRounding(bl)
            )
        )

        private val fullRounded: Density.(Size) -> RoundedPolygon = { size: Size ->
            RoundedPolygon.rectangle(
                width = size.width,
                height = size.height,
                rounding = CornerRounding(16.dp.toPx())
            )
        }
        private val fullSquarish: Density.(Size) -> RoundedPolygon = { size: Size ->
            RoundedPolygon.rectangle(
                width = size.width,
                height = size.height,
                rounding = CornerRounding(4.dp.toPx())
            )
        }

        val start = MediumInteractionButtonMorphShapes(
            idle = {
                polygonFromCorners(
                    it,
                    16.dp.toPx(),
                    4.dp.toPx(),
                    4.dp.toPx(),
                    16.dp.toPx()
                )
            },
            selected = fullRounded,
            pressed = fullSquarish
        )

        val middle = MediumInteractionButtonMorphShapes(
            idle = fullSquarish,
            selected = fullRounded,
            pressed = fullSquarish
        )

        val end = MediumInteractionButtonMorphShapes(
            idle = {
                polygonFromCorners(
                    it,
                    4.dp.toPx(),
                    16.dp.toPx(),
                    16.dp.toPx(),
                    4.dp.toPx()
                )
            },
            selected = fullRounded,
            pressed = fullSquarish
        )

        val single = MediumInteractionButtonMorphShapes(
            idle = fullRounded,
            selected = fullRounded,
            pressed = fullSquarish
        )
    }
}

data class MediumInteractionButtonMorphShapes(
    val idle: Density.(Size) -> RoundedPolygon,
    val selected: Density.(Size) -> RoundedPolygon,
    val pressed: Density.(Size) -> RoundedPolygon
)

data class MediumInteractionButtonColors(
    val idleSelectedBackground: Color,
    val idleSelectedContent: Color,
    val idleUnselectedBackground: Color,
    val idleUnselectedContent: Color,
    val pressedUnselectedBackground: Color,
    val pressedUnselectedContent: Color,
    val pressedSelectedBackground: Color,
    val pressedSelectedContent: Color,
    val disabledBackground: Color,
    val disabledContent: Color
)

interface MediumInteractionButtonAnimationSpecs {
    val pressedScale: Float
    val idleScale: Float

    @Composable
    @ReadOnlyComposable
    fun <T> colorAnimationSpec(): AnimationSpec<T>

    @Composable
    @ReadOnlyComposable
    fun <T> motionSpec(): AnimationSpec<T>
}

data class MediumInteractionButtonInteractionSpecs(
    val neighborImpactFalloff: Float,
    val pressedAdditionalPadding: Dp,
    val scaleInitialVelocity: Float,
    val idleInitialVelocity: Float,
    val pressedPaddingInitialVelocity: Dp,
    val shapePressedInitialVelocity: Float,
    val shapeIdleInitialVelocity: Float
)

private fun Modifier.widthResizing(amount: () -> Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val newWidth = (placeable.width + amount().roundToPx() * 2)
        .coerceIn(constraints.minWidth, constraints.maxWidth)

    layout(newWidth, placeable.height) {
        placeable.placeRelative(x = (newWidth - placeable.width) / 2, y = 0)
    }
}
