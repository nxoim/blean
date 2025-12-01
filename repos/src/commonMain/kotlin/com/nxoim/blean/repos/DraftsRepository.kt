@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.draft

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.nxoim.blean.api.models.modelsJsonConfig
import com.nxoim.blean.models.draft.Draft
import com.nxoim.blean.models.draft.LocalMedia
import com.nxoim.blean.repos.SuppressNoActualForExpect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class DraftsRepository(
    private val dao: DraftsDao,
    private val json: Json = modelsJsonConfig
) {
    suspend fun saveOrUpdate(draft: Draft): Result<Unit, Throwable> =
        runCatching {
            dao.saveOrUpdate(draft.toRoom(json))
        }

    fun get(id: String): Flow<Draft?> {
        return dao.getById(id).map { roomDraft ->
            roomDraft?.toDomain(json)
        }
    }

    fun getDrafts(amount: IntRange): Flow<List<Draft>> {
        return dao.getDrafts(amount.first, amount.last - amount.first + 1).map { list ->
            list.map { it.toDomain(json) }
        }
    }

    fun count(): Flow<Int> = dao.count()

    suspend fun delete(id: String): Result<Unit, Throwable> = runCatching {
        dao.deleteById(id)
    }

    suspend fun deleteAll(): Result<Unit, Throwable> = runCatching {
        dao.deleteAll()
    }
}

@Database(entities = [RoomDraft::class], version = 1, exportSchema = false)
@ConstructedBy(DraftsRoomDatabaseConstructor::class)
abstract class DraftsRoomDatabase : RoomDatabase() {
    abstract fun dao(): DraftsDao
}

@SuppressNoActualForExpect
expect object DraftsRoomDatabaseConstructor : RoomDatabaseConstructor<DraftsRoomDatabase> {
    override fun initialize(): DraftsRoomDatabase
}

@Dao
interface DraftsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdate(draft: RoomDraft)

    @Query("SELECT * FROM drafts WHERE id = :id LIMIT 1")
    fun getById(id: String): Flow<RoomDraft?>

    @Query("SELECT * FROM drafts ORDER BY creationDateIso8601 DESC LIMIT :limit OFFSET :offset")
    fun getDrafts(offset: Int, limit: Int): Flow<List<RoomDraft>>

    @Query("SELECT COUNT(*) FROM drafts")
    fun count(): Flow<Int>

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM drafts")
    suspend fun deleteAll()
}

@Entity(tableName = "drafts")
data class RoomDraft(
    @PrimaryKey val id: String,
    val creationDateIso8601: String,
    val text: String,
    val mediaListJson: String
)

fun Draft.toRoom(json: Json): RoomDraft {
    return RoomDraft(
        id = this.id,
        creationDateIso8601 = this.creationDate.toString(),
        text =this.text,
        mediaListJson = json.encodeToString(this.media.map { it.toRoom() })
    )
}

fun RoomDraft.toDomain(json: Json): Draft {
    return Draft(
        id = this.id,
        creationDate = Instant.parse(this.creationDateIso8601),
        text = this.text,
        media = json.decodeFromString<List<RoomLocalMedia>>(this.mediaListJson).map { it.toDomain() }.toSet()
    )
}

@Serializable
private sealed class RoomLocalMedia(val key: String) {
    @Serializable

    data class Image(private val _key: String) : RoomLocalMedia(_key)
    @Serializable
    data class Video(private val _key: String) : RoomLocalMedia(_key)
}

private fun LocalMedia.toRoom() = when (this) {
    is LocalMedia.Image -> RoomLocalMedia.Image(this.key)
    is LocalMedia.Video -> RoomLocalMedia.Video(this.key)
}

private fun RoomLocalMedia.toDomain() = when (this) {
    is RoomLocalMedia.Image -> LocalMedia.Image(key)
    is RoomLocalMedia.Video -> LocalMedia.Video(key)
}

