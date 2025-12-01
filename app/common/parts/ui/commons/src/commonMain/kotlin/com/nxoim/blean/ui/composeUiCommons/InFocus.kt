package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned

fun Modifier.detectFocusByPosition(onFocusChanged: (Boolean) -> Unit) = onGloballyPositioned {
    it.parentLayoutCoordinates?.let { parentCoordinates ->
        val focusThresholdRect = Rect(
            Offset(
                x = 0f,
                y = parentCoordinates.boundsInParent().height / 2
            ),
            Size(
                width = parentCoordinates.boundsInParent().width,
                height = 1f
            )
        )

        onFocusChanged(it.boundsInParent().overlaps(focusThresholdRect))
    }
}


/**
 * null = not provided
 */
val LocalIsInFocus = compositionLocalOf<Boolean?> { null }