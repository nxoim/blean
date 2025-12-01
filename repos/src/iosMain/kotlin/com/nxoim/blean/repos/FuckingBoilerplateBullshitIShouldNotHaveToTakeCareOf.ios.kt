package com.nxoim.blean.repos

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.stringWithString

context(logger: Logger)
actual inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T {
    makeSurePathExists(basePathUri)

    return Room.databaseBuilder<T>(
        "$basePathUri/$name.db",
    )
        .setDriver(BundledSQLiteDriver())
        .build()
}

@OptIn(ExperimentalForeignApi::class)
@PublishedApi
internal fun makeSurePathExists(basePathUri: String) {
    val nsFileManager = NSFileManager.defaultManager
    val dirPath = NSString.stringWithString(basePathUri)
    nsFileManager.createDirectoryAtPath(
        dirPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
}
