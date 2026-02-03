package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.nxoim.blean.ui.composeUiCommons.RotatingMorphingShape
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimatedFlowerShapeFrame(
	flowerShapeState: RotatingFlowerShapeState = rememberRotatingFlowerShapeState(),
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.(shapeState: RotatingFlowerShapeState) -> Unit
) {
	Box(
		modifier.graphicsLayer {
			clip = true
			shape = flowerShapeState.shape
		},
		content = { content(flowerShapeState) }
	)
}

@Composable
fun rememberRotatingFlowerShapeState(
	initialFlowerShapePrevalence: Float = 1f
): RotatingFlowerShapeState {
	val infiniteTransition = if (initialFlowerShapePrevalence > 0f)
		rememberInfiniteTransition()
	else
		null

	return remember(infiniteTransition) {
		RotatingFlowerShapeState(infiniteTransition, initialFlowerShapePrevalence)
	}
}

class RotatingFlowerShapeState(
	private val infiniteTransition: InfiniteTransition?,
	initialFlowerShapePrevalence: Float = 1f
) {
	private val flowerShape = Morph(
		RoundedPolygon.circle(),
		RoundedPolygon.star(
			numVerticesPerRadius = 8,
			rounding = CornerRounding(32f),
			innerRounding = CornerRounding(32f),
			radius = 1.15f,
			innerRadius = 0.85f
		)
	)
	var prevalence by mutableStateOf(initialFlowerShapePrevalence)
		private set

	var rotationDegrees by mutableStateOf(0f)
		private set

	val shape get() = RotatingMorphingShape(
		morph = flowerShape,
		progress = prevalence,
		rotation = rotationDegrees ?: 0f
	)

	@Composable
    @NonRestartableComposable
	fun RotateInfinitelyEffect(cycleDuration: Duration = defaultCycleDuration) {
		val rotation = infiniteTransition?.animateFloat(
			0f,
			360f,
			infiniteRepeatable(tween(cycleDuration.inWholeMilliseconds.toInt(), easing = LinearEasing))
		)

		LaunchedEffect(rotation?.value) {
			if (rotation?.value != null) rotationDegrees = rotation!!.value
		}
	}

	suspend fun animateRotationTo(
		degrees: Float,
		initialVelocity: Float = 0f,
		animationSpec: AnimationSpec<Float> = spring()
	) {
		animate(rotationDegrees, degrees, initialVelocity, animationSpec) { value, _ ->
			rotationDegrees = value
		}
	}

	suspend fun animatePrevalenceTo(
		prevalence: Float,
		initialVelocity: Float = 0f,
		animationSpec: AnimationSpec<Float> = spring()
	) {
		animate(this.prevalence, prevalence, initialVelocity, animationSpec) { value, _ ->
			this.prevalence = value
		}
	}

	fun snapPrevalenceTo(prevalence: Float) {
		this.prevalence = prevalence
	}
}

@Preview
@Composable
private fun Preview() {
    val state = rememberRotatingFlowerShapeState()
    state.RotateInfinitelyEffect()
    AnimatedFlowerShapeFrame(state) {
        Box(Modifier.size(128.dp).background(Color.Red))
    }
}

private val defaultCycleDuration = 100.seconds