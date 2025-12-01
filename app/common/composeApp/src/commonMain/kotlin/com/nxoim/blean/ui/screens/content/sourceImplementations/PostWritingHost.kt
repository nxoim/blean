package com.nxoim.blean.ui.screens.content.sourceImplementations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingModel
import kotlinx.serialization.serializer

class PostWritingHost(
    private val context: ComponentContext,
    private val postWritingModel: PostWritingModel
) {
    private val slotNavigation = SlotNavigation<Boolean>()
    private val slot = context.childSlot(
        slotNavigation,
        childFactory = { open, context ->
            if (open)
                InstanceWrapper.Model(postWritingModel)
            else
                InstanceWrapper.None
        },
        serializer = serializer(),
        handleBackButton = true
    )
    val model by mutableStateOf(slot.value.child?.instance?.model).apply {
        slot.subscribe { value = it.child?.instance?.model }
    }

    fun open() { slotNavigation.activate(true) }

    fun hide() {
        val model = slot.value.child?.instance?.model
        slotNavigation.dismiss()
        model?.saveCurrentToDraftAndReset()
    }

    // because we cant have null in child slot
    // and we want to use child slot for automatic back handling
    // and state restoration
    private sealed interface InstanceWrapper {
        data object None : InstanceWrapper
        data class Model(val value: PostWritingModel) : InstanceWrapper
    }

    private val InstanceWrapper.model
        get() = if (this is InstanceWrapper.Model) value else null
}