package com.nxoim.blean.ui.postUi.parts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// NOTE: could be turned into a more adaptive hyper customizable grid
/**
 * @param mainItemMaxWeight The maximum amount
 * of horizontal space that could be taken by the first item
 *
 * @param unlockMainItemsAspectRatio Attempt to keep
 * the first item's aspect ratio as measured
 */
@Composable
fun <T> ItemFocusGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    gutter: Dp = 0.dp,
    unlockMainItemsAspectRatio: Boolean = true,
    mainItemMaxWeight: Float = 0.7f,
    content: @Composable (Int, T) -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->
                key(item) { content(index, item) }
            }
        }
    ) { measurables, constraints ->
        val gutterPx = if (items.size <= 1) 0 else gutter.roundToPx()
        val mainConstraints = if (unlockMainItemsAspectRatio)
        // TODO figure out what causes it to measure to the max
            Constraints(
                minWidth = 0,
                maxWidth = ((constraints.maxWidth * mainItemMaxWeight).toInt() - gutterPx).coerceAtMost(
                    (1000 * density).roundToInt()
                )
            )
        else
            Constraints.fixedWidth(
                ((constraints.maxWidth * mainItemMaxWeight).toInt() - gutterPx).coerceAtMost((1000 * density).roundToInt())
            )

        val mainPlaceable = measurables.getOrNull(0)?.measure(mainConstraints)

        if (mainPlaceable == null) {
            layout(0, 0) { }
        } else {
            val stackPlaceables = (measurables - measurables[0])
                .map {
                    it.measure(
                        Constraints.fixed(
                            width = (constraints.maxWidth - mainPlaceable.width - gutterPx).coerceAtMost(
                                (1000 * density).roundToInt()
                            ),
                            height = if (measurables.size <= 1) {
                                // If no stack items - main item height or 0
                                mainPlaceable.height
                            } else {
                                maxOf(
                                    0,
                                    (mainPlaceable.height - gutterPx * (measurables.size - 2).coerceAtLeast(
                                        0
                                    )) / (measurables.size - 1).coerceAtLeast(1)
                                )
                            }
                        )
                    )
                }

            layout(
                width = constraints.maxWidth.coerceAtMost((1000 * density).roundToInt()),
                height = mainPlaceable.height
            ) {
                mainPlaceable.placeRelative(0, 0)

                stackPlaceables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(
                        mainPlaceable.width + gutterPx,
                        (placeable.height + gutterPx) * index
                    )
                }
            }
        }
    }
}