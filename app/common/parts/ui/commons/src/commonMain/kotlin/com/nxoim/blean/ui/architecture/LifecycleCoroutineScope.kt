package com.nxoim.blean.ui.architecture

import com.arkivanov.decompose.GenericComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreateSimple
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

fun CoroutineScope.bindToLifecycleOf(context: GenericComponentContext<*>) = this.apply {
    context.lifecycle.doOnDestroy(coroutineContext::cancel)
}

fun GenericComponentContext<*>.lifecycledCoroutineScope(
    coroutineContext: CoroutineContext
) = CoroutineScope(coroutineContext).bindToLifecycleOf(this)

/**
 * Retained lifecycle aware coroutine scope with the default dispatcher
 */
val GenericComponentContext<*>.coroutineScope
    get() = this.instanceKeeper.getOrCreateSimple("defaultLifecycledCoroutineScope") {
        lifecycledCoroutineScope(Dispatchers.Default + SupervisorJob())
    }
