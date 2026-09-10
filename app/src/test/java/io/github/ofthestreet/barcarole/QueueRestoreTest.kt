package io.github.ofthestreet.barcarole

import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.playback.QueueRestore
import io.github.ofthestreet.barcarole.playback.QueueSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueRestoreTest {

    private fun song(id: Long) = Song(
        id = id,
        title = "Track $id",
        artist = "Artist",
        album = "Album",
        albumId = 1,
        artistId = 1,
        durationMs = 180_000,
        trackNumber = id.toInt(),
        year = 2020,
        dateAddedSeconds = 0,
        path = "/storage/emulated/0/Music/track$id.mp3",
    )

    private fun libraryOf(vararg ids: Long) = ids.toList().associateWith { song(it) }

    @Test
    fun `an intact queue comes back exactly as it was saved`() {
        val restored = QueueRestore.resolve(
            QueueSnapshot(songIds = listOf(1, 2, 3), index = 1, positionMs = 42_000),
            libraryOf(1, 2, 3),
        )!!
        assertEquals(listOf(1L, 2L, 3L), restored.songs.map { it.id })
        assertEquals(1, restored.index)
        assertEquals(42_000, restored.positionMs)
    }

    @Test
    fun `a track deleted before the current one does not shift playback onto its neighbour`() {
        // Saved: [1, 2, 3] playing index 2 (track 3). Track 1 is gone, so track 3 is now index 1.
        val restored = QueueRestore.resolve(
            QueueSnapshot(songIds = listOf(1, 2, 3), index = 2, positionMs = 10_000),
            libraryOf(2, 3),
        )!!
        assertEquals(listOf(2L, 3L), restored.songs.map { it.id })
        assertEquals(1, restored.index)
        assertEquals(3L, restored.songs[restored.index].id)
        assertEquals(10_000, restored.positionMs)
    }

    @Test
    fun `when the playing track is gone the position is not carried over to another song`() {
        val restored = QueueRestore.resolve(
            QueueSnapshot(songIds = listOf(1, 2, 3), index = 1, positionMs = 90_000),
            libraryOf(1, 3),
        )!!
        assertEquals(listOf(1L, 3L), restored.songs.map { it.id })
        assertEquals(1, restored.index)
        assertEquals(0, restored.positionMs)
    }

    @Test
    fun `an empty library restores nothing rather than an empty queue`() {
        assertNull(
            QueueRestore.resolve(
                QueueSnapshot(songIds = listOf(1, 2), index = 0, positionMs = 0),
                emptyMap(),
            ),
        )
    }

    @Test
    fun `the fallback index stays inside the surviving queue`() {
        val restored = QueueRestore.resolve(
            QueueSnapshot(songIds = listOf(1, 2, 3), index = 2, positionMs = 5_000),
            libraryOf(1),
        )!!
        assertEquals(listOf(1L), restored.songs.map { it.id })
        assertEquals(0, restored.index)
    }
}
