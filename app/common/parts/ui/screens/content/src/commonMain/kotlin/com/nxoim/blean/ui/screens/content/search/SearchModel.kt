package com.nxoim.blean.ui.screens.content.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.evolpagink.core.pageable
import com.nxoim.evolpagink.core.prefetchMinimumItemAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchModel(
    private val source: SearchSource,
    val navigation: SearchNavigation,
    private val coroutineScope: CoroutineScope
) {
    var searchState by mutableStateOf<SearchState>(SearchState.Idle)
        private set
    var query by mutableStateOf("")
        private set

    val cursors = mutableMapOf<Int, ChunkCursor>(0 to ChunkCursor.Next(null))

    val pageable = pageable(
        coroutineScope,
        context = snapshotFlow { query }
            .debounce() { if (it.isNotEmpty()) 2.seconds else 0.seconds }
            .onEach {
                if (it.isEmpty()) {
                    searchState = SearchState.Idle
                    source.clearAllSearch()
                } else {
                    searchState = SearchState.Loading
                    source.clearAndPerformFirstSearch(query)
                        .onFailure {
                            searchState = SearchState.Failed(it.toString())
                        }.onSuccess {
                            searchState = SearchState.Done
                        }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), query),
        onPage = { index ->
            val query = this

            source.get(query, index.toLong())
                .distinctUntilChanged()
                .onEach { feedChunk ->
                    if (feedChunk != null) {
                        cursors[index + 1] = feedChunk.first
                    }

                    val currentCursor = cursors[index]

                    if (query.isNotEmpty() && currentCursor != null) {
                        if (feedChunk == null) {
                            if (index == 0) {

                            } else {
                                when (currentCursor) {
                                    is ChunkCursor.Next -> {
                                        source.loadAndCache(query, currentCursor.value)
                                    }

                                    ChunkCursor.None -> {}
                                }
                            }
                        }
                    }
                }
                .filterNotNull()
                .map { it.second }
        },
        strategy = prefetchMinimumItemAmount()
    )

    fun search(query: String) { this.query = query }
}

interface SearchSource {
    fun get(
        query: String,
        index: Long
    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Search>>>?>

    suspend fun clearAndPerformFirstSearch(query: String): Result<Unit, Unit>

    suspend fun loadAndCache(
        query: String,
        cursor: String?
    ): Result<Unit, Unit>

    suspend fun clearAllSearch()
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Failed(val message: String) : SearchState
    data object Done : SearchState
}

interface SearchNavigation {
    fun openPost(post: AtUri)
    fun openPost(post: PostContainer<PostType>)
}