package com.nxoim.blean.repos.media

import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// TODO remove try methods
// https://github.com/benkuly/trixnity/blob/main/trixnity-utils/src/commonMain/kotlin/net/folivo/trixnity/utils/KeyedMutex.kt
open class KeyedMutex<K : Any> {
    private data class ClaimedMutex(
        val claimCount: Int = 0,
        val mutex: Mutex = Mutex(),
    )

    private val mutexByKeyMutex = Mutex()
    private val mutexByKey = ConcurrentMutableMap<K, ClaimedMutex>()

    suspend fun <T : Any?> withLock(key: K, block: suspend () -> T): T {
        return try {
            val mutex = claimMutex(key)
            mutex.withLock { block() }
        } finally {
            withContext(NonCancellable) { releaseMutex(key) }
        }
    }

    suspend fun isLocked(key: K): Boolean = mutexByKeyMutex.withLock {
        mutexByKey[key]?.mutex?.isLocked ?: false
    }

    fun <T : Any?> tryWithLock(key: K, block: () -> T): T {
        return try {
            val mutex = tryClaimMutex(key)
            mutex.tryWithLock { block() }
        } finally {
            tryReleaseMutex(key)
        }
    }

    private suspend fun claimMutex(key: K): Mutex = mutexByKeyMutex.withLock {
        val claimedMutex = mutexByKey[key]
            ?.run { copy(claimCount = claimCount + 1) }
            ?: ClaimedMutex(1)
        mutexByKey[key] = claimedMutex
        claimedMutex.mutex
    }

    private fun tryClaimMutex(key: K): Mutex = mutexByKeyMutex.tryWithLock {
        val claimedMutex = mutexByKey[key]
            ?.run { copy(claimCount = claimCount + 1) }
            ?: ClaimedMutex(1)
        mutexByKey[key] = claimedMutex
        claimedMutex.mutex
    }

    private suspend fun releaseMutex(key: K): Unit = mutexByKeyMutex.withLock {
        val claimedMutex = mutexByKey[key] ?: return@withLock
        if (claimedMutex.claimCount == 1) mutexByKey.remove(key)
        else mutexByKey[key] = claimedMutex.copy(claimCount = claimedMutex.claimCount - 1)
    }

    private fun tryReleaseMutex(key: K): Unit = mutexByKeyMutex.tryWithLock {
        val claimedMutex = mutexByKey[key] ?: return@tryWithLock
        if (claimedMutex.claimCount == 1) mutexByKey.remove(key)
        else mutexByKey[key] = claimedMutex.copy(claimCount = claimedMutex.claimCount - 1)
    }
}

@OptIn(ExperimentalContracts::class)
private inline fun <T> Mutex.tryWithLock(owner: Any? = null, action: () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    tryLock(owner)
    return try {
        action()
    } finally {
        unlock(owner)
    }
}