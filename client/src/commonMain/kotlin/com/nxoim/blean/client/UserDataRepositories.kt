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
import com.nxoim.blean.repos.media.OkioFileStore
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class UserDataRepositories(
    private val feedsDb: SavedFeedsRoomDatabase,
    private val mutedWordsDb: MutedWordsRoomDatabase,
    private val draftsDb: DraftsRoomDatabase,
    private val postInteractionsOutboxDb: OutboxRoomDatabase,
    private val baseContentSpecificDbUri: String,
    private val mediaFileSystem: FileSystem = FileSystem.SYSTEM
) {
    constructor(
        baseContentSpecificDbUri: String,
        encryptionKey: ByteArray?,
        mediaFileSystem: FileSystem = FileSystem.SYSTEM
    ) : this(
        buildRoomDatabase<SavedFeedsRoomDatabase>(
            basePathUri = baseContentSpecificDbUri,
            name = "feeds",
            encryptionKey = encryptionKey
        ),
        buildRoomDatabase<MutedWordsRoomDatabase>(
            basePathUri = baseContentSpecificDbUri,
            name = "mutedWords",
            encryptionKey = encryptionKey
        ),
        buildRoomDatabase<DraftsRoomDatabase>(
            basePathUri = baseContentSpecificDbUri,
            name = "drafts",
            encryptionKey = encryptionKey
        ),
        buildRoomDatabase<OutboxRoomDatabase>(
            basePathUri = baseContentSpecificDbUri,
            name = "outbox",
            encryptionKey = encryptionKey
        ),
        baseContentSpecificDbUri = baseContentSpecificDbUri,
        mediaFileSystem = mediaFileSystem
    )

    val feeds = FeedsSettingsRepository(feedsDb.dao())
    val mutedWords = MutedWordsRepository(mutedWordsDb.dao())
    val drafts = DraftsRepository(draftsDb.dao())
    val draftMediaStorage = FileManager(
        OkioFileStore(
            "$baseContentSpecificDbUri/draftMedia",
            mediaFileSystem
        )
    )

    val postInteractionsOutbox = OutboxRepository(postInteractionsOutboxDb.dao())

    suspend fun initialize() {
//        feeds.open()
//        mutedWords.open()
//        drafts.open()
        mediaFileSystem.createDirectories(baseContentSpecificDbUri.toPath())
        draftMediaStorage.createFolderIfMissing()
    }

    suspend fun deinitialize() {
        feedsDb.close()
        mutedWordsDb.close()
        draftsDb.close()
        postInteractionsOutboxDb.close()
    }
}
