package com.nxoim.blean.ui.screens.content.feed

import com.nxoim.blean.ui.screens.content.feed.models.SavedFeed
import kotlinx.coroutines.flow.Flow

interface FeedSource {
    fun getFeeds(): Flow<List<SavedFeed>?>
    fun getSavedFeed(uri: String): Flow<SavedFeed?>

}