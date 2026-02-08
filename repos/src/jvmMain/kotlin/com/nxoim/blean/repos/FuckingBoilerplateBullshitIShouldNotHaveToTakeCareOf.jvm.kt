package com.nxoim.blean.repos

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger

context(logger: Logger)
actual inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T = Room.databaseBuilder<T>("$basePathUri/$name.db").build()


actual inline fun <reified T : RoomDatabase> inMemoryDatabaseBuilder() =
    Room.inMemoryDatabaseBuilder<T>()
        .setDriver(BundledSQLiteDriver())