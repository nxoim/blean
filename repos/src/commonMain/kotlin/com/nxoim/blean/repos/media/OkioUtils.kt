package com.nxoim.blean.repos.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer
import okio.use
import kotlin.coroutines.CoroutineContext

suspend fun Flow<ByteArray>.writeTo(
    sink: BufferedSink,
    coroutineContext: CoroutineContext = Dispatchers.IO
) = withContext(coroutineContext) { sink.use { collect(it::write) } }

suspend fun BufferedSink.write(
    content: Flow<ByteArray>,
    coroutineContext: CoroutineContext = Dispatchers.IO
) = content.writeTo(this, coroutineContext)

fun byteArrayFlowFromSource(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    sourceFactory: suspend () -> Source
) = flow {
    val source = sourceFactory().buffer()
    source.use {
        while (!source.exhausted()) {
            emit(source.readByteArray(BYTE_ARRAY_FLOW_CHUNK_SIZE.coerceAtMost(source.buffer.size)))
        }
    }
}.flowOn(coroutineContext)

suspend fun FileSystem.write(
    path: Path,
    content: Flow<ByteArray>,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): Unit = withContext(coroutineContext) {
    content.writeTo(sink(path).buffer(), coroutineContext)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun FileSystem.readByteArrayFlow(
    path: Path,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): Flow<ByteArray?> = flow {
    emit(
        if (exists(path))
        byteArrayFlowFromSource(coroutineContext) { source(path) }
    else
        flowOf(null)
    )
}.flatMapLatest { it }

suspend fun Flow<ByteArray>.collectAllIntoByteArray(): ByteArray {
    val allByteArrays = this.toList()
    val concatByteArray = ByteArray(allByteArrays.sumOf { it.size })
    var byteArrayPosition = 0
    allByteArrays.forEach { byteArray ->
        if (byteArray.isNotEmpty()) {
            byteArray.copyInto(concatByteArray, byteArrayPosition)
            byteArrayPosition += byteArray.size
        }
    }
    return concatByteArray
}

fun Flow<ByteArray>.takeBytes(size: Int): Flow<ByteArray> = flow {
    var currentSize = 0
    takeWhile { next ->
        currentSize += next.size
        currentSize <= size
    }.collect(this)
}

const val BYTE_ARRAY_FLOW_CHUNK_SIZE: Long = 1_024 * 1_024 // 1 MiB