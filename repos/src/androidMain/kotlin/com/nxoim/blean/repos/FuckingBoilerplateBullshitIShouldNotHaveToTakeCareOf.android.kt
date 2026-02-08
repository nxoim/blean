package com.nxoim.blean.repos

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.startup.Initializer
import co.touchlab.kermit.Logger

context(logger: Logger)
actual inline fun <reified T : RoomDatabase> buildRoomDatabase(
    basePathUri: String,
    name: String,
    encryptionKey: ByteArray?
): T = try {
    Room.databaseBuilder<T>(
        context = RoomProvider.appContext,
        name = "$basePathUri/$name.db".also {
            logger.v(tag = _logTag) { "Database path: $it" }
        }
    )
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
        .build()
} catch (e: kotlin.Exception) {
    logger.e(e, tag = _logTag) { "Room.databaseBuilder failed" }
    throw e
}

class RoomProvider private constructor(/* so we surely dont intialize it manually*/): Initializer<Context>  {
    companion object {
        @SuppressLint("StaticFieldLeak") // stfu
        lateinit var appContext: Context
    }

    override fun create(p0: Context): Context {
        appContext = p0.applicationContext
        Logger.v("RoomProvider") { "RoomProvider created, context set" }
        return appContext
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}

actual inline fun <reified T : RoomDatabase> inMemoryDatabaseBuilder() =
    Room.inMemoryDatabaseBuilder<T>(RoomProvider.appContext)

const val _logTag = "RoomProvider"
