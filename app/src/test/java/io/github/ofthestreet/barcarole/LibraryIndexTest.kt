package io.github.ofthestreet.barcarole

import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.data.SongSort
import io.github.ofthestreet.barcarole.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexTest {

    private fun song(
        id: Long,
        title: String,
        artist: String = "Artist A",
        album: String = "Album A",
        albumId: Long = 1,
        artistId: Long = 10,
        durationMs: Long = 200_000,
        track: Int = 1,
        year: Int = 2001,
        dateAdded: Long = 1_000,
        path: String = "/storage/emulated/0/Music/$title.mp3",
    ) = Song(id, title, artist, album, albumId, artistId, durationMs, track, year, dateAdded, path)

    private val library = listOf(
        song(1, "Beta", track = 2),
        song(2, "alpha", track = 1),
        song(3, "Gamma", artist = "Artist B", album = "Album B", albumId = 2, artistId = 20, durationMs = 100_000, dateAdded = 5_000, path = "/storage/emulated/0/Music/Rock/Gamma.mp3"),
        song(4, "9 Lives", artist = "Artist B", album = "Album B", albumId = 2, artistId = 20, track = 2, path = "/storage/emulated/0/Music/Rock/9 Lives.mp3"),
    )

    @Test
    fun `folder path comes from the file path`() {
        assertEquals("/storage/emulated/0/Music", library[0].folderPath)
        assertEquals("/storage/emulated/0/Music/Rock", library[2].folderPath)
    }

    @Test
    fun `albums group by album id and count their tracks`() {
        val albums = LibraryIndex.albumsOf(library)
        assertEquals(listOf("Album A", "Album B"), albums.map { it.title })
        assertEquals(listOf(2, 2), albums.map { it.songCount })
    }

    @Test
    fun `an album with two different artists is credited to various artists`() {
        val mixed = listOf(song(1, "One"), song(2, "Two", artist = "Someone else", artistId = 11))
        assertEquals(LibraryIndex.VARIOUS_ARTISTS, LibraryIndex.albumsOf(mixed).single().artist)
    }

    @Test
    fun `artists count distinct albums`() {
        val artists = LibraryIndex.artistsOf(library)
        assertEquals(listOf("Artist A", "Artist B"), artists.map { it.name })
        assertEquals(listOf(1, 1), artists.map { it.albumCount })
        assertEquals(listOf(2, 2), artists.map { it.songCount })
    }

    @Test
    fun `folders group by directory`() {
        val folders = LibraryIndex.foldersOf(library)
        assertEquals(listOf("Music", "Rock"), folders.map { it.name })
        assertEquals(listOf(2, 2), folders.map { it.songCount })
    }

    @Test
    fun `title sort ignores case`() {
        val sorted = LibraryIndex.sortSongs(library, SortOrder(SongSort.TITLE, ascending = true))
        assertEquals(listOf("9 Lives", "alpha", "Beta", "Gamma"), sorted.map { it.title })
    }

    @Test
    fun `descending sort reverses the ascending order`() {
        val ascending = LibraryIndex.sortSongs(library, SortOrder(SongSort.TITLE, ascending = true))
        val descending = LibraryIndex.sortSongs(library, SortOrder(SongSort.TITLE, ascending = false))
        assertEquals(ascending.reversed().map { it.id }, descending.map { it.id })
    }

    @Test
    fun `duration sort is numeric`() {
        val sorted = LibraryIndex.sortSongs(library, SortOrder(SongSort.DURATION, ascending = true))
        assertEquals("Gamma", sorted.first().title)
    }

    @Test
    fun `date added sort puts the newest last when ascending`() {
        val sorted = LibraryIndex.sortSongs(library, SortOrder(SongSort.DATE_ADDED, ascending = true))
        assertEquals("Gamma", sorted.last().title)
    }

    @Test
    fun `album tracks come back in track order, not alphabetical`() {
        val tracks = LibraryIndex.tracksOfAlbum(library, albumId = 1)
        assertEquals(listOf("alpha", "Beta"), tracks.map { it.title })
    }

    @Test
    fun `folder tracks are limited to that folder`() {
        val tracks = LibraryIndex.tracksOfFolder(library, "/storage/emulated/0/Music/Rock")
        assertEquals(listOf("9 Lives", "Gamma"), tracks.map { it.title })
    }

    @Test
    fun `search matches title artist album and folder, case insensitively`() {
        val built = LibraryIndex.build(library)
        val results = LibraryIndex.search(built, "artist b")
        assertEquals(2, results.songs.size)
        assertEquals(1, results.artists.size)
        assertEquals(1, results.albums.size)

        val byFolder = LibraryIndex.search(built, "rock")
        assertEquals(1, byFolder.folders.size)
    }

    @Test
    fun `an empty query returns nothing rather than everything`() {
        assertTrue(LibraryIndex.search(LibraryIndex.build(library), "   ").isEmpty)
    }

    @Test
    fun `sort initial buckets non letters under hash`() {
        assertEquals("A", LibraryIndex.sortInitial("alpha"))
        assertEquals("#", LibraryIndex.sortInitial("9 Lives"))
        assertEquals("#", LibraryIndex.sortInitial(""))
    }
}
