package com.nxoim.blean.ui.composeMaterial3Extensions

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.launch

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

object RefreshIndicatorUtils {
    val triggerThreshold = 48.dp
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun RefreshIndicatorPreview() {
    MaterialExpressiveTheme {
        Surface {
            var isRefreshing by remember { mutableStateOf(false) }
            val progress = remember { androidx.compose.animation.core.Animatable(0.01f) }

            LaunchedEffect(Unit) {
                while (true) {
                    progress.animateTo(1f, initialVelocity = 20f) {
                        if (value >= 1f) isRefreshing = true
                    }
                    kotlinx.coroutines.delay(2000)
                    isRefreshing = false
                    progress.animateTo(0.01f)
                }
            }

            Box(modifier = Modifier.size(400.dp), contentAlignment = Alignment.Center) {
                RefreshIndicator(
                    isRefreshing = isRefreshing,
                    progress = { progress.value },
                    modifier = Modifier.size(64.dp * progress.value)
                )
            }
        }
    }
}