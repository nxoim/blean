package com.nxoim.blean.repos

import androidx.room.Room
import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger

context(logger: Logger)
actual inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T = Room.databaseBuilder<T>("$basePathUri/$name.db").build()