package com.nxoim.blean.commonThingsDumpster

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.jvm.JvmInline

fun CoroutineScope.childCoroutineScope(): CoroutineScope = this + Job(coroutineContext[Job])
fun CoroutineScope.cancelChildren() = this.coroutineContext.cancelChildren()

/**
 * Example
 *
 * ```kotlin
 * val refreshJob = JobContainer()
 *
 * fun refresh() {
 *     refreshJob.getOrCreate {
 *         coroutineScope.launch {
 *             // will be executed and the next calls of `refresh`
 *             // will have no effect until this job is cancelled either
 *             // by successful completion or error
 *         }
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAtomicApi::class)
@JvmInline
value class JobContainer(private val job: AtomicReference<Job?> = AtomicReference(null)) {
    fun getOrCreate(new: () -> Job): Job = job.updateAndFetch {
        if (it == null) {
            new().apply {
                invokeOnCompletion { job.store(null) }
            }
        } else {
            it
        }
    }!!

    fun CoroutineScope.getOrCreate(new: () -> Unit): Job = job.updateAndFetch {
        if (it == null) {
            launch { new() }.apply {
                invokeOnCompletion { job.store(null) }
            }
        } else {
            it
        }
    }!!

    fun cancelAndCreate(new: () -> Job): Job = job.updateAndFetch {
        it?.cancel()

        new().apply {
            invokeOnCompletion { job.store(null) }
        }
    }!!

    fun CoroutineScope.cancelAndCreate(new: () -> Unit): Job = job.updateAndFetch {
        it?.cancel()

        launch { new() }.apply {
            invokeOnCompletion { job.store(null) }
        }
    }!!
}