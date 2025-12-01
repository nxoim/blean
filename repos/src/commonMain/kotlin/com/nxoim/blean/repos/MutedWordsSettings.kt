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
import com.github.michaelbull.result.runCatching
import com.nxoim.blean.models.account.MuteAccountTarget
import com.nxoim.blean.models.account.MuteContentTarget
import com.nxoim.blean.models.account.MutedWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MutedWordsRepository(
    private val dao: MutedWordsDao
) {
    suspend fun saveOrUpdate(mutedWords: Sequence<MutedWord>): Result<Unit, Throwable> =
        runCatching {
            dao.saveOrUpdateAll(mutedWords.map { it.toRoom() }.toList())
        }

    fun getAll(): Flow<List<MutedWord>> = dao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun delete(mutedWords: Iterable<MutedWord>): Result<Unit, Throwable> = runCatching {
        val idsToDelete = mutedWords.map { it.id }
        if (idsToDelete.isNotEmpty()) {
            dao.deleteByIds(idsToDelete)
        }
    }

    suspend fun delete(mutedWord: MutedWord): Result<Unit, Throwable> = runCatching {
        dao.deleteById(mutedWord.id)
    }

    suspend fun delete(word: String): Result<Unit, Throwable> = runCatching {
        dao.deleteByWord(word)
    }
}

@Database(entities = [RoomMutedWord::class], version = 1, exportSchema = false)
@ConstructedBy(MutedWordsRoomDatabaseConstructor::class)
abstract class MutedWordsRoomDatabase : RoomDatabase() {
    abstract fun dao(): MutedWordsDao
}

@SuppressNoActualForExpect
expect object MutedWordsRoomDatabaseConstructor : RoomDatabaseConstructor<MutedWordsRoomDatabase> {
    override fun initialize(): MutedWordsRoomDatabase
}

@Dao
interface MutedWordsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdateAll(mutedWords: List<RoomMutedWord>)

    @Query("SELECT * FROM muted_words")
    fun getAll(): Flow<List<RoomMutedWord>>

    @Query("DELETE FROM muted_words WHERE id = :id")
    suspend fun deleteById(id: String)

     @Query("DELETE FROM muted_words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM muted_words WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("DELETE FROM muted_words")
    suspend fun deleteAll()
}

@Entity(
    tableName = "muted_words",
    indices = [Index(value = ["word"])]
)
data class RoomMutedWord(
    @PrimaryKey val id: String,
    val word: String,
    val accountTarget: String,
    val contentTarget: String,
    val expiresOn: String?
)

fun MutedWord.toRoom(): RoomMutedWord {
    return RoomMutedWord(
        id = this.id,
        word = this.word,
        accountTarget = this.accountTarget.name,
        contentTarget = this.contentTarget.name,
        expiresOn = this.expiresOnISO8601
    )
}

fun RoomMutedWord.toDomain(): MutedWord {
    return MutedWord(
        id = this.id,
        word = this.word,
        accountTarget = MuteAccountTarget.valueOf(this.accountTarget),
        contentTarget = MuteContentTarget.valueOf(this.contentTarget),
        expiresOnISO8601 = this.expiresOn
    )
}