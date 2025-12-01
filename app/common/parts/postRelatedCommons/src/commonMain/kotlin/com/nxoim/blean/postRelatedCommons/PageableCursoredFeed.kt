package com.nxoim.blean.postRelatedCommons

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.commonThingsDumpster.JobContainer
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class PageableCursoredFeed<T>(
    private val scope: CoroutineScope,
    private val fetchChunk: (index: Long) -> Flow<Pair<ChunkCursor, List<T>>?>,
    private val onFirstPageEmpty: suspend () -> Result<*, *>,
    private val onNextPageEmpty: suspend (ChunkCursor) -> Result<*, *>
) {
    @OptIn(ExperimentalAtomicApi::class)
    private var refreshJob = JobContainer()
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.None)
    val refreshState = _refreshState.asStateFlow()

    private val cursors = mutableMapOf<Int, ChunkCursor>(0 to ChunkCursor.Next(null))

    val pageable = pageable(
        scope,
        strategy = prefetchMinimumItemAmount(initialPage = 0),
        onPage = { index ->
            fetchChunk(index.toLong())
                .distinctUntilChanged { _, new -> new == null }
                .onEach { chunk ->
                    if (chunk != null) cursors[index + 1] = chunk.first

                    val currentCursor = cursors[index]

                    if (currentCursor != null && chunk == null) {
                        if (index == 0) {
                            refresh()
                        } else if (currentCursor is ChunkCursor.Next) {
                            onNextPageEmpty(currentCursor)
                        }
                    }
                }
                .filterNotNull()
                .map { it.second }
        }
    )

    fun refresh() {
        refreshJob.getOrCreate {
            scope.launch {
                _refreshState.value = RefreshState.InProgress
                onFirstPageEmpty()
                    .onFailure {
                        _refreshState.value = RefreshState.Error(it.toString())
                    }
                    .onSuccess {
                        delay(200)
                        _refreshState.value = RefreshState.None
                    }
            }
        }
    }
}

sealed interface RefreshState {
    data object None : RefreshState

    data object InProgress : RefreshState

    data class Error(val message: String) : RefreshState
}

class PageableFeed<T>(
    private val scope: CoroutineScope,
    private val fetchChunk: (index: Int) -> Flow<List<T>?>,
    private val onRefresh: suspend () -> Result<*, *>,
    private val initialItems: List<T> = emptyList()
) {
    @OptIn(ExperimentalAtomicApi::class)
    private var refreshJob = JobContainer()
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.None)
    val refreshState = _refreshState.asStateFlow()

    val pageable = pageable(
        scope,
        strategy = prefetchMinimumItemAmount(initialPage = 0),
        onPage = { index ->
            fetchChunk(index)
                .distinctUntilChanged { _, new -> new == null }
                .onEach { chunk ->
                    if (chunk == null) {
                        if (index == 0) {
                            refresh()
                        }
                    }
                }
        },
        initialItems = initialItems
    )

    fun refresh() {
        refreshJob.getOrCreate {
            scope.launch {
                _refreshState.value = RefreshState.InProgress
                onRefresh()
                    .onFailure {
                        _refreshState.value = RefreshState.Error(it.toString())
                    }
                    .onSuccess {
                        delay(200)
                        _refreshState.value = RefreshState.None
                    }
            }
        }
    }
}
