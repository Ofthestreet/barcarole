package io.github.ofthestreet.barcarole.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device library out of MediaStore. Deliberately no filesystem walking: it is slower,
 * needs broader permissions and breaks under scoped storage. Folders are derived from each row's
 * DATA path instead.
 */
@Singleton
class MediaStoreLibrarySource @Inject constructor(
    private val context: Context,
) {

    suspend fun querySongs(minDurationSeconds: Int): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf((minDurationSeconds * 1000).toString())

        val songs = ArrayList<Song>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                songs += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol) ?: path.substringAfterLast('/'),
                    artist = cursor.getString(artistCol).orUnknown(UNKNOWN_ARTIST),
                    album = cursor.getString(albumCol).orUnknown(UNKNOWN_ALBUM),
                    albumId = cursor.getLong(albumIdCol),
                    artistId = cursor.getLong(artistIdCol),
                    durationMs = cursor.getLong(durationCol),
                    // TRACK encodes disc*1000 + track; only the track part is useful for ordering.
                    trackNumber = cursor.getInt(trackCol).let { if (it > 1000) it % 1000 else it },
                    year = cursor.getInt(yearCol),
                    dateAddedSeconds = cursor.getLong(dateAddedCol),
                    path = path,
                    sizeBytes = cursor.getLong(sizeCol),
                )
            }
        }
        songs
    }

    /** Emits once per MediaStore audio change so the library can refresh itself. */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun String?.orUnknown(fallback: String): String =
        if (this.isNullOrBlank() || this == UNKNOWN_TAG) fallback else this

    companion object {
        /** What MediaStore writes when a file carries no tag. */
        private const val UNKNOWN_TAG = "<unknown>"
        const val UNKNOWN_ARTIST = "Unknown artist"
        const val UNKNOWN_ALBUM = "Unknown album"
    }
}

/** Playable URI for a song. */
fun Song.contentUri(): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

/** Album artwork URI; Coil falls back to the placeholder when the album has no art. */
fun albumArtUri(albumId: Long): Uri =
    ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)
