package com.nxoim.blean.repos

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.runCatching
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.models.account.FeedType
import com.nxoim.blean.models.account.SavedFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedsSettingsRepository(
    private val dao: SavedFeedsDao
) {
    suspend fun saveOrUpdate(feeds: Iterable<SavedFeed>): Result<Unit, Throwable> = runCatching {
        val roomFeeds = feeds.mapIndexed { index, feed -> feed.toRoom(index) }
        dao.saveOrUpdateAll(roomFeeds)
    }

    fun get(): Flow<List<SavedFeed>> {
        return dao.getAllOrderedByIndex().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun delete(feeds: Iterable<SavedFeed>): Result<Unit, Throwable> = runCatching {
        val uuidsToDelete = feeds.map { it.id }
        if (uuidsToDelete.isNotEmpty()) {
            dao.deleteByUuids(uuidsToDelete)
        }
    }
}

@Database(entities = [RoomSavedFeed::class], version = 1, exportSchema = false)
@ConstructedBy(SavedFeedsRoomDatabaseConstructor::class)
abstract class SavedFeedsRoomDatabase : RoomDatabase() {
    abstract fun dao(): SavedFeedsDao
}

@SuppressNoActualForExpect
expect object SavedFeedsRoomDatabaseConstructor : RoomDatabaseConstructor<SavedFeedsRoomDatabase> {
    override fun initialize(): SavedFeedsRoomDatabase
}

@Dao
interface SavedFeedsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdateAll(feeds: List<RoomSavedFeed>)

    @Query("SELECT * FROM saved_feeds ORDER BY `index` ASC")
    fun getAllOrderedByIndex(): Flow<List<RoomSavedFeed>>

    @Query("DELETE FROM saved_feeds WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Query("DELETE FROM saved_feeds WHERE uuid IN (:uuids)")
    suspend fun deleteByUuids(uuids: List<String>)

    // @Query("DELETE FROM saved_feeds WHERE did = :did OR cid = :cid")
    // suspend fun deleteByDidOrCid(did: String, cid: String)

    @Query("DELETE FROM saved_feeds")
    suspend fun deleteAll()
}

@Entity(
    tableName = "saved_feeds",
    indices = [Index(value = ["did"]), Index(value = ["cid"])]
)
data class RoomSavedFeed(
    @PrimaryKey val uuid: String,
    val isPinned: Boolean,
    val feedName: String,
    val avatarUrl: String?,
    val description: String?,
    val did: String,
    val cid: String,
    val isOnline: Boolean,
    val isValid: Boolean,
    val authorDid: String,
    val uri: String,
    val index: Int,
    val typeEnumString: String
)

fun SavedFeed.toRoom(index: Int): RoomSavedFeed {
    return RoomSavedFeed(
        uuid = this.id,
        isPinned = this.isPinned,
        feedName = this.name,
        avatarUrl = this.avatarUrl,
        description = this.description,
        did = this.did.toString(),
        cid = this.cid,
        isOnline = this.isOnline,
        isValid = this.isValid,
        authorDid = this.authorDid.toString(),
        uri = this.uri,
        index = index,
        typeEnumString = this.type.toDbString()
    )
}

fun RoomSavedFeed.toDomain(): SavedFeed {
    return SavedFeed(
        id = this.uuid,
        isPinned = this.isPinned,
        name = this.feedName,
        avatarUrl = this.avatarUrl,
        description = this.description,
        did =  Did.parse(this.did).getOrThrow(),
        cid = this.cid,
        isOnline = this.isOnline,
        isValid = this.isValid,
        authorDid = AccountIdentificator.Did(Did.parse(this.authorDid).getOrThrow()),
        uri = this.uri,
        type = feedTypeFromDbString(this.typeEnumString)
    )
}

private fun FeedType.toDbString(): String = when (this) {
    FeedType.Video -> "Video"
    FeedType.NormalPosts -> "NormalPosts"
    FeedType.Unsupported -> "Unsupported"
}

private fun feedTypeFromDbString(dbString: String): FeedType = try {
    when(dbString) {
        "Video" -> FeedType.Video
        "NormalPosts" -> FeedType.NormalPosts
        else -> FeedType.Unsupported
    }
} catch (e: IllegalArgumentException) {
    FeedType.Unsupported
}

