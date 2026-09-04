package com.cdelarue.localmusic

import com.cdelarue.localmusic.data.LibraryIndex
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.playback.BrowseIds
import com.cdelarue.localmusic.playback.BrowseStyle
import com.cdelarue.localmusic.playback.BrowseTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseTreeTest {

    private fun song(
        id: Long,
        title: String,
        artist: String = "Artist A",
        album: String = "Album A",
        albumId: Long = 1,
        artistId: Long = 10,
        track: Int = 1,
        path: String = "/storage/emulated/0/Music",
    ) = Song(id, title, artist, album, albumId, artistId, 200_000, track, 2001, 1_000, "$path/$title.mp3")

    private val library = LibraryIndex.build(
        listOf(
            song(1, "Alpha", track = 1),
            song(2, "Beta", track = 2),
            song(3, "Gamma", artist = "Artist B", album = "Album B", albumId = 2, artistId = 20, path = "/storage/emulated/0/Music/Rock"),
        ),
    )

    @Test
    fun `the root offers the same four tabs as the phone`() {
        val ids = BrowseTree.childrenOf(library, BrowseIds.ROOT).map { it.id }
        assertEquals(
            listOf(BrowseIds.TAB_SONGS, BrowseIds.TAB_ALBUMS, BrowseIds.TAB_ARTISTS, BrowseIds.TAB_FOLDERS),
            ids,
        )
    }

    @Test
    fun `albums and artists ask for a grid, songs and folders for a list`() {
        val byId = BrowseTree.rootChildren().associateBy { it.id }
        assertEquals(BrowseStyle.GRID, byId.getValue(BrowseIds.TAB_ALBUMS).childStyle)
        assertEquals(BrowseStyle.GRID, byId.getValue(BrowseIds.TAB_ARTISTS).childStyle)
        assertEquals(BrowseStyle.LIST, byId.getValue(BrowseIds.TAB_SONGS).childStyle)
        assertEquals(BrowseStyle.LIST, byId.getValue(BrowseIds.TAB_FOLDERS).childStyle)
    }

    @Test
    fun `browsing an album yields playable tracks in track order`() {
        val children = BrowseTree.childrenOf(library, BrowseIds.album(1))
        assertEquals(listOf("Alpha", "Beta"), children.map { it.title })
        assertTrue(children.all { it.playable })
    }

    @Test
    fun `a folder is browsable by its real path`() {
        val children = BrowseTree.childrenOf(library, BrowseIds.folder("/storage/emulated/0/Music/Rock"))
        assertEquals(listOf("Gamma"), children.map { it.title })
    }

    @Test
    fun `picking a track queues its whole album, positioned on that track`() {
        val trackTwo = BrowseTree.childrenOf(library, BrowseIds.album(1))[1]
        val selection = BrowseTree.queueFor(library, trackTwo.id)!!
        assertEquals(listOf("Alpha", "Beta"), selection.songs.map { it.title })
        assertEquals(1, selection.startIndex)
    }

    @Test
    fun `picking an album plays it from the start`() {
        val selection = BrowseTree.queueFor(library, BrowseIds.album(1))!!
        assertEquals(2, selection.songs.size)
        assertEquals(0, selection.startIndex)
    }

    @Test
    fun `an unknown id queues nothing rather than everything`() {
        assertNull(BrowseTree.queueFor(library, "album|999"))
        assertNull(BrowseTree.queueFor(library, "nonsense"))
    }

    @Test
    fun `voice search matches albums, artists and titles`() {
        val results = BrowseTree.search(library, "album b")
        assertTrue(results.any { it.id == BrowseIds.album(2) })

        val byTitle = BrowseTree.search(library, "gamma")
        assertTrue(byTitle.any { it.title == "Gamma" && it.playable })
    }

    @Test
    fun `a song id gives up its song id whichever shape it has`() {
        // The phone queues plain ids; the browse tree carries the parent alongside.
        assertEquals(42L, BrowseIds.songIdOf("42"))
        assertEquals(42L, BrowseIds.songIdOf(BrowseIds.song(42, BrowseIds.album(7))))
        assertEquals(BrowseIds.album(7), BrowseIds.parentOf(BrowseIds.song(42, BrowseIds.album(7))))
        assertNull(BrowseIds.songIdOf("tab|songs"))
    }

    @Test
    fun `a folder parent survives the slashes in its path`() {
        val mediaId = BrowseIds.song(3, BrowseIds.folder("/storage/emulated/0/Music/Rock"))
        assertEquals(3L, BrowseIds.songIdOf(mediaId))
        assertEquals(BrowseIds.folder("/storage/emulated/0/Music/Rock"), BrowseIds.parentOf(mediaId))
        assertEquals(listOf("Gamma"), BrowseTree.queueFor(library, mediaId)!!.songs.map { it.title })
    }
}
