package com.nxoim.blean.platformCredentialsManagement

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.stringWithString

class IOSCredentialsRepository  private constructor(
    roomCredentialsRepository: RoomCredentialsRepository,
) : PlatformCredentialsRepository by roomCredentialsRepository {
    constructor(
        encryptionKey: ByteArray,
        storagePath: String,
        logger: Logger
    ) : this(
        RoomCredentialsRepository(
            buildRoomDatabase<PlatformCredentialsDatabase>(
                basePathUri = storagePath,
                name = "c",
                encryptionKey = encryptionKey,
                logger
            ).dao(),
            logger = logger
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
private inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?,
    logger: Logger
): T {
    makeSurePathExists(basePathUri)

    return Room.databaseBuilder<T>("$basePathUri/$name.db")
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)

                    logger.v(tag = _logTag) { "Created db for $name" }
                }

                override fun onOpen(connection: SQLiteConnection) {
                    super.onOpen(connection)

                    logger.v(tag = _logTag) { "Opened db for $name" }
                }

                override fun onDestructiveMigration(connection: SQLiteConnection) {
                    super.onDestructiveMigration(connection)

                    logger.v(tag = _logTag) { "Destructive migration for $name" }
                }
            }
        )
        .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun makeSurePathExists(basePathUri: String) {
    val nsFileManager = NSFileManager.defaultManager
    val dirPath = NSString.stringWithString(basePathUri)
    nsFileManager.createDirectoryAtPath(
        dirPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
}

private const val _logTag = "IOSCredentialsRepository"
