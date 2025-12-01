package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Put this everywhere so the feel is consistent
 * @param alternate is a condition. Use it to alternate between faster and
 * slower transitions, for example, in a container expansion/collapse
 * animation
 * @param alternateSpeedMultiplier defines how much faster or slower the
 * alternative animation is
 */
fun <T> softSpring(
    alternate: Boolean = false,
    alternateSpeedMultiplier: Float = 2f
): SpringSpec<T> = spring(
    1.8f,
    if (alternate) (2500f * alternateSpeedMultiplier) else 2500f
)

/**
 * Used for coordination of animations in the context of one of the targets
 * not being rendered in overlay for the cross ui layer fade effect
 * that does not look flickery
 */
object MediaSharedBoundsTransition {
    /**
     * For [androidx.compose.animation.AnimatedContent]
     */
    val containerTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        // not adding scale here due to measurement issues
        // when animating sharedBounds with render in
        // overlay set to false which we need for cross ui
        // layer fade animation. If that ever gets fixed the
        // initial/target scale must be 0.98f and spec softSpring()
        fadeIn(tween(200)) togetherWith fadeOut(tween(300, easing = EaseInCubic))
    }

    object NoOverlay {
        val enter = EnterTransition.None
        val exit = fadeOut(tween(400, easing = EaseInCubic))
    }

    object Overlay {
        val enter = fadeIn(tween(250, easing = EaseOutExpo))
        val exit = fadeOut(tween(250, easing = EaseInCirc))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun scaleInWithFade(
    transformOrigin: TransformOrigin = TransformOrigin.Center
) = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
            scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), material3AnimScale, transformOrigin)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun scaleOutWithFade(
    transformOrigin: TransformOrigin = TransformOrigin.Center
) = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
        scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec(), material3AnimScale, transformOrigin)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ExpressiveBoundsTransform: BoundsTransform
    @Composable
    get() {
        val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

        return BoundsTransform { _, _ -> spec }
    }

private const val material3AnimScale = 0.92f