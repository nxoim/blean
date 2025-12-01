package com.nxoim.blean.ui.screens.content.sourceImplementations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.subscribe
import com.nxoim.blean.ui.fullscreenMedia.FullScreenMedia
import com.nxoim.blean.ui.screens.content.MediaAwareThingState
import com.nxoim.blean.ui.screens.content.MediaSelectionHandler

class MediaAwareThingStateImpl(private val context: ComponentContext) : MediaAwareThingState {
    val slotNavigator = SlotNavigation<MediaAwareThingState.State>()
    val slot = context.childSlot(
        slotNavigator,
        serializer = null,
        initialConfiguration = { MediaAwareThingState.State.None },
        childFactory = { configuration, context ->
            configuration
        },
        handleBackButton = true,
        key = "MediaAwareThingStateSlot"
    )

    override val state by mutableStateOf(slot.value.child!!.instance).apply {
        slot.subscribe(context.lifecycle) {
            value = it.child?.instance ?: MediaAwareThingState.State.None
        }
    }

    override val handler = object : MediaSelectionHandler {
        override fun selectMedia(
            allMedia: List<FullScreenMedia>,
            focusOn: FullScreenMedia
        ) {
            slotNavigator.activate(
                MediaAwareThingState.State.Selected(
                    focusOn,
                    allMedia
                )
            )
        }

        override fun dismiss() {
            slotNavigator.dismiss()
        }
    }
}