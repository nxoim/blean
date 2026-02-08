package com.nxoim.blean.repos

import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger

inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T = with(Logger) {
    buildRoomDatabase<T>(basePathUri, name, encryptionKey)
 }

context(logger: Logger)
expect inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T

expect inline fun <reified T : RoomDatabase> inMemoryDatabaseBuilder(): RoomDatabase.Builder<T>

@Suppress("NO_ACTUAL_FOR_EXPECT")
annotation class SuppressNoActualForExpect

