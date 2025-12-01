package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaseButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable @UiComposable InteractionSource.(clickableModifier: Modifier) -> Unit
) = content(
    interactionSource,
    Modifier
        .hoverable(interactionSource, enabled)
        .focusable(enabled, interactionSource)
        .pointerInput(currentCompositeKeyHashCode) {
            this.awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial).let {
                    if (it.pressed)
                        interactionSource.tryEmit(PressInteraction.Press(Offset.Zero))

                    this.awaitDragOrCancellation(it.id)?.let {
                        interactionSource.tryEmit(
                            PressInteraction.Release(
                                PressInteraction.Press(
                                    Offset.Zero
                                )
                            )
                        )
                    }

                }

            }
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            role = Role.Button,
            enabled = enabled,
        )
)

