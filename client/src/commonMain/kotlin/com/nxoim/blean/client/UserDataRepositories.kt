package com.nxoim.blean.client

import com.nxoim.blean.draft.DraftsRepository
import com.nxoim.blean.draft.DraftsRoomDatabase
import com.nxoim.blean.outbox.OutboxRepository
import com.nxoim.blean.outbox.OutboxRoomDatabase
import com.nxoim.blean.repos.FeedsSettingsRepository
import com.nxoim.blean.repos.MutedWordsRepository
import com.nxoim.blean.repos.MutedWordsRoomDatabase
import com.nxoim.blean.repos.SavedFeedsRoomDatabase
import com.nxoim.blean.repos.buildRoomDatabase
import com.nxoim.blean.repos.media.FileManager
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class UserDataRepositories(
    private val baseContentSpecificDbUri: String,
    encryptionKey: ByteArray?
) {
    private val feedsDb = buildRoomDatabase<SavedFeedsRoomDatabase>(
        basePathUri = baseContentSpecificDbUri,
        name = "feeds",
        encryptionKey = encryptionKey
    )
    private val mutedWordsDb = buildRoomDatabase<MutedWordsRoomDatabase>(
        basePathUri = baseContentSpecificDbUri,
        name = "mutedWords",
        encryptionKey = encryptionKey
    )
    private val draftsDb = buildRoomDatabase<DraftsRoomDatabase>(
        basePathUri = baseContentSpecificDbUri,
        name = "drafts",
        encryptionKey = encryptionKey
    )
    private val postInteractionsOutboxDb = buildRoomDatabase<OutboxRoomDatabase>(
        basePathUri = baseContentSpecificDbUri,
        name = "outbox",
        encryptionKey = encryptionKey
    )

    val feeds = FeedsSettingsRepository(feedsDb.dao())
    val mutedWords = MutedWordsRepository(mutedWordsDb.dao())
    val drafts = DraftsRepository(draftsDb.dao())
    val draftMediaStorage = FileManager(baseContentSpecificDbUri, "draft")
    val postInteractionsOutbox = OutboxRepository(postInteractionsOutboxDb.dao())

    suspend fun initialize() {
//        feeds.open()
//        mutedWords.open()
//        drafts.open()
        draftMediaStorage.createFolderIfMissing()
    }

    suspend fun deinitialize() {
//        feeds.close()
//        mutedWords.close()
//        drafts.close()
        feedsDb.close()
        mutedWordsDb.close()
        draftsDb.close()
        postInteractionsOutboxDb.close()
    }

    suspend fun deinitializeAndNuke() {
        deinitialize()
        FileSystem.SYSTEM.delete(baseContentSpecificDbUri.toPath())
    }
}
