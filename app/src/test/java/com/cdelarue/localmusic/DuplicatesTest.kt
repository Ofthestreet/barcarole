package com.cdelarue.localmusic

import com.cdelarue.localmusic.data.Duplicates
import com.cdelarue.localmusic.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatesTest {

    private fun song(
        id: Long,
        title: String = "Kashmir",
        artist: String = "Led Zeppelin",
        durationMs: Long = 508_000,
        sizeBytes: Long = 8_000_000,
        dateAddedSeconds: Long = 100,
        path: String = "/storage/emulated/0/Music/kashmir.mp3",
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = "Physical Graffiti",
        albumId = 1,
        artistId = 1,
        durationMs = durationMs,
        trackNumber = 1,
        year = 1975,
        dateAddedSeconds = dateAddedSeconds,
        path = path,
        sizeBytes = sizeBytes,
    )

    @Test
    fun `a single copy is not a duplicate`() {
        assertTrue(Duplicates.find(listOf(song(1))).isEmpty())
    }

    @Test
    fun `the largest copy is the one kept`() {
        val small = song(1, sizeBytes = 3_000_000, path = "/a/kashmir.mp3")
        val large = song(2, sizeBytes = 9_000_000, path = "/b/kashmir.flac")
        val groups = Duplicates.find(listOf(small, large))
        assertEquals(1, groups.size)
        assertEquals(large, groups.single().keep)
        assertEquals(listOf(small), groups.single().remove)
    }

    @Test
    fun `equal copies keep the one indexed first`() {
        val older = song(1, dateAddedSeconds = 10, path = "/a/kashmir.mp3")
        val newer = song(2, dateAddedSeconds = 99, path = "/b/kashmir.mp3")
        assertEquals(older, Duplicates.find(listOf(newer, older)).single().keep)
    }

    @Test
    fun `titles differing only in case and spacing still match`() {
        val one = song(1, title = "Kashmir", path = "/a.mp3")
        val two = song(2, title = " KASHMIR ", path = "/b.mp3", sizeBytes = 1)
        assertEquals(listOf(two), Duplicates.find(listOf(one, two)).single().remove)
    }

    @Test
    fun `a different length is a different recording`() {
        val studio = song(1, durationMs = 508_000)
        val live = song(2, durationMs = 611_000)
        assertTrue(Duplicates.find(listOf(studio, live)).isEmpty())
    }

    @Test
    fun `the same title by another artist is left alone`() {
        val one = song(1, artist = "Led Zeppelin")
        val two = song(2, artist = "Escala")
        assertTrue(Duplicates.find(listOf(one, two)).isEmpty())
    }

    @Test
    fun `three copies keep one and propose the other two`() {
        val songs = listOf(
            song(1, sizeBytes = 5, path = "/a.mp3"),
            song(2, sizeBytes = 9, path = "/b.mp3"),
            song(3, sizeBytes = 7, path = "/c.mp3"),
        )
        val group = Duplicates.find(songs).single()
        assertEquals(2L, group.keep.id)
        assertEquals(listOf(3L, 1L), group.remove.map { it.id })
        assertEquals(12L, group.wastedBytes)
    }

    @Test
    fun `groups are listed with the most wasted space first`() {
        val songs = listOf(
            song(1, title = "Small", sizeBytes = 2, path = "/a.mp3"),
            song(2, title = "Small", sizeBytes = 2, path = "/b.mp3"),
            song(3, title = "Big", sizeBytes = 100, path = "/c.mp3"),
            song(4, title = "Big", sizeBytes = 100, path = "/d.mp3"),
        )
        assertEquals(listOf("Big", "Small"), Duplicates.find(songs).map { it.keep.title })
    }
}
