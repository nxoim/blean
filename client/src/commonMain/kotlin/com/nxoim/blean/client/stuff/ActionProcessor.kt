package com.nxoim.blean.client.stuff

import com.nxoim.blean.client.stuff.utils.ProcessingDefaults
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface ActionProcessor {
    suspend fun process(paused: StateFlow<Boolean> = ProcessingDefaults.NeverPaused)
    suspend fun recover()
    suspend fun autoCleanOutbox(deleteItemsOlderThan: Duration)
}