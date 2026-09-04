package com.cdelarue.localmusic

import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.data.stats.PlayCount
import com.cdelarue.localmusic.data.stats.PlaylistWindows
import com.cdelarue.localmusic.data.stats.Playlists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PlaylistsTest {

    private fun song(id: Long, title: String, addedSeconds: Long = 0) = Song(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        albumId = 1,
        artistId = 1,
        durationMs = 200_000,
        trackNumber = 1,
        year = 2020,
        dateAddedSeconds = addedSeconds,
        path = "/storage/emulated/0/Music/$title.mp3",
    )

    private val zone = ZoneId.of("Europe/Paris")
    private val now = ZonedDateTime.of(2026, 9, 5, 14, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `favourites keep the order they were added in and ignore missing files`() {
        val songs = listOf(song(1, "One"), song(2, "Two"))
        val result = Playlists.favourites(songs, listOf(2, 99, 1))
        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    @Test
    fun `most played keeps the database ordering and honours the limit`() {
        val songs = listOf(song(1, "One"), song(2, "Two"), song(3, "Three"))
        val counts = listOf(
            PlayCount(3, plays = 9, lastPlayedAt = 0),
            PlayCount(1, plays = 4, lastPlayedAt = 0),
            PlayCount(2, plays = 1, lastPlayedAt = 0),
        )
        assertEquals(listOf(3L, 1L, 2L), Playlists.mostPlayed(songs, counts).map { it.id })
        assertEquals(listOf(3L, 1L), Playlists.mostPlayed(songs, counts, limit = 2).map { it.id })
    }

    @Test
    fun `recently added compares seconds against a millisecond window`() {
        // MediaStore stores DATE_ADDED in seconds; a naive comparison would return everything.
        val eightDaysAgo = (now - 8 * PlaylistWindows.MILLIS_PER_DAY) / 1000
        val twoDaysAgo = (now - 2 * PlaylistWindows.MILLIS_PER_DAY) / 1000
        val songs = listOf(song(1, "Old", eightDaysAgo), song(2, "New", twoDaysAgo))

        val week = Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 7))
        assertEquals(listOf(2L), week.map { it.id })

        val month = Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 30))
        assertEquals(listOf(2L, 1L), month.map { it.id })
    }

    @Test
    fun `never played excludes anything with a listen and sorts by title`() {
        val songs = listOf(song(1, "beta"), song(2, "Alpha"), song(3, "Gamma"))
        val result = Playlists.neverPlayed(songs, setOf(3))
        assertEquals(listOf("Alpha", "beta"), result.map { it.title })
    }

    @Test
    fun `this month starts on the first of the month, not thirty days ago`() {
        val start = PlaylistWindows.startOfMonth(now, zone)
        val expected = ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, start)
        assertTrue(start < now)
    }

    @Test
    fun `this year starts on the first of January`() {
        val start = PlaylistWindows.startOfYear(now, zone)
        val expected = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, start)
    }

    @Test
    fun `a listen early in the month still counts for this month`() {
        val firstOfMonth = ZonedDateTime.of(2026, 9, 1, 0, 5, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(firstOfMonth >= PlaylistWindows.startOfMonth(now, zone))
    }
}
