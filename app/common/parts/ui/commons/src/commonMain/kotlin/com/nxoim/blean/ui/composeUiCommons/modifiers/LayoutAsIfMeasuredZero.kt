package com.nxoim.blean.ui.composeUiCommons.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

fun Modifier.layoutAsIfMeasuredZero() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(0, 0) {
        placeable.placeRelative(0, 0)
    }
}