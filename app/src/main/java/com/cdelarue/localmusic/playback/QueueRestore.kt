package com.cdelarue.localmusic.playback

import com.cdelarue.localmusic.data.Song

data class RestoredQueue(val songs: List<Song>, val index: Int, val positionMs: Long)

/**
 * Rebuilds a saved queue against the current library.
 *
 * Files come and go between sessions, so the saved index cannot be trusted on its own: dropping a
 * track that sat before it would silently shift playback onto a different song. The track that was
 * playing is resolved by id and the index is recomputed from where it actually landed.
 */
object QueueRestore {

    fun resolve(snapshot: QueueSnapshot, songsById: Map<Long, Song>): RestoredQueue? {
        val songs = snapshot.songIds.mapNotNull { songsById[it] }
        if (songs.isEmpty()) return null

        val playingId = snapshot.songIds.getOrNull(snapshot.index)
        val restoredIndex = songs.indexOfFirst { it.id == playingId }

        return if (restoredIndex >= 0) {
            RestoredQueue(songs, restoredIndex, snapshot.positionMs)
        } else {
            // The track that was playing is gone: start the nearest survivor from the top.
            val fallback = snapshot.songIds
                .take(snapshot.index)
                .count { songsById.containsKey(it) }
                .coerceIn(songs.indices)
            RestoredQueue(songs, fallback, 0L)
        }
    }
}
