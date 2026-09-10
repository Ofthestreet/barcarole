package io.github.ofthestreet.barcarole.data.stats

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One row per listen. Storing the events rather than running totals is what lets any window be
 * answered exactly — this month, this year, all time — without a counter to reset on a date change.
 */
@Entity(tableName = "play_events")
data class PlayEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
)

@Entity(tableName = "favourites")
data class Favourite(
    @PrimaryKey val songId: Long,
    val addedAt: Long,
)

data class PlayCount(val songId: Long, val plays: Int, val lastPlayedAt: Long)

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun recordPlay(event: PlayEvent)

    @Query(
        """
        SELECT songId, COUNT(*) AS plays, MAX(playedAt) AS lastPlayedAt
        FROM play_events
        WHERE playedAt >= :since
        GROUP BY songId
        ORDER BY plays DESC, lastPlayedAt DESC
        """,
    )
    fun countsSince(since: Long): Flow<List<PlayCount>>

    @Query("SELECT DISTINCT songId FROM play_events")
    fun playedSongIds(): Flow<List<Long>>

    @Query("DELETE FROM play_events WHERE songId = :songId")
    suspend fun forget(songId: Long)
}

@Dao
interface FavouriteDao {

    @Query("SELECT songId FROM favourites ORDER BY addedAt DESC")
    fun favouriteIds(): Flow<List<Long>>

    @Insert
    suspend fun add(favourite: Favourite)

    @Query("DELETE FROM favourites WHERE songId = :songId")
    suspend fun remove(songId: Long)

    @Query("SELECT COUNT(*) FROM favourites WHERE songId = :songId")
    suspend fun count(songId: Long): Int
}

@Database(entities = [PlayEvent::class, Favourite::class], version = 1, exportSchema = false)
abstract class LocalMusicDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun favouriteDao(): FavouriteDao
}
