package io.github.ofthestreet.barcarole.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class QueueSnapshot(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
)

private val Context.queueDataStore: DataStore<Preferences> by preferencesDataStore(name = "queue")

/**
 * Remembers what was playing so the app comes back where it was left. Only song ids are stored;
 * the tracks themselves are looked up in the library at restore time, so a file that disappeared
 * simply drops out of the queue.
 */
@Singleton
class QueueStore @Inject constructor(private val context: Context) {

    val snapshot: Flow<QueueSnapshot?> = context.queueDataStore.data.map { prefs ->
        val ids = prefs[KeySongIds]
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) {
            null
        } else {
            QueueSnapshot(
                songIds = ids,
                index = prefs[KeyIndex] ?: 0,
                positionMs = prefs[KeyPosition] ?: 0L,
            )
        }
    }

    suspend fun save(songIds: List<Long>, index: Int, positionMs: Long) {
        context.queueDataStore.edit { prefs ->
            prefs[KeySongIds] = songIds.joinToString(",")
            prefs[KeyIndex] = index
            prefs[KeyPosition] = positionMs
        }
    }

    suspend fun clear() {
        context.queueDataStore.edit { it.clear() }
    }

    private companion object {
        val KeySongIds = stringPreferencesKey("song_ids")
        val KeyIndex = intPreferencesKey("index")
        val KeyPosition = longPreferencesKey("position_ms")
    }
}
