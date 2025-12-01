package com.nxoim.blean.ui.composeUiCommons.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

inline fun Modifier.offsetWithMotionFrameOfReference(
    crossinline offset: Density.() -> IntOffset
) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
        withMotionFrameOfReferencePlacement {
            placeable.placeRelative(offset())
        }
    }
}