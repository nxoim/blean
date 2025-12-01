package com.nxoim.blean.ui.composeUiCommons.scroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun rememberCupertinoOverscrollFactory(
    animationSpec: AnimationSpec<Float> = CupertinoOverscrollEffectDefaults.overscrollSpring()
): CupertinoOverscrollEffectFactory {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    return remember {
        CupertinoOverscrollEffectFactory(
            density = density,
            layoutDirection = layoutDirection,
            animationSpec = animationSpec
        )
    }
}

data class CupertinoOverscrollEffectFactory(
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    private val animationSpec: AnimationSpec<Float>
) : OverscrollFactory {
    @OptIn(ExperimentalFoundationApi::class)
    override fun createOverscrollEffect() =
        CupertinoOverscrollEffect(
            density.density,
            applyClip = false,
            animationSpec = animationSpec
        )
}