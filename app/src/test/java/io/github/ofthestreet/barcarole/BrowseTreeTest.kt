package io.github.ofthestreet.barcarole

import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.playback.BrowseContext
import io.github.ofthestreet.barcarole.playback.BrowseIds
import io.github.ofthestreet.barcarole.playback.BrowseStyle
import io.github.ofthestreet.barcarole.playback.BrowseTree
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

    private val libraryData = LibraryIndex.build(
        listOf(
            song(1, "Alpha", track = 1),
            song(2, "Beta", track = 2),
            song(3, "Gamma", artist = "Artist B", album = "Album B", albumId = 2, artistId = 20, path = "/storage/emulated/0/Music/Rock"),
        ),
    )

    private val library = BrowseContext(library = libraryData, showAlbums = true)

    @Test
    fun `the root leads with favourites and lists the browse modes`() {
        val ids = BrowseTree.childrenOf(library, BrowseIds.ROOT).map { it.id }
        assertEquals(
            listOf(
                BrowseIds.TAB_FAVOURITES,
                BrowseIds.TAB_SONGS,
                BrowseIds.TAB_ALBUMS,
                BrowseIds.TAB_ARTISTS,
                BrowseIds.TAB_FOLDERS,
            ),
            ids,
        )
    }

    @Test
    fun `turning albums off removes them from the car as well`() {
        val withoutAlbums = library.copy(showAlbums = false)
        val ids = BrowseTree.childrenOf(withoutAlbums, BrowseIds.ROOT).map { it.id }
        assertEquals(
            listOf(BrowseIds.TAB_FAVOURITES, BrowseIds.TAB_SONGS, BrowseIds.TAB_ARTISTS, BrowseIds.TAB_FOLDERS),
            ids,
        )
        assertTrue(BrowseTree.search(withoutAlbums, "album b").none { it.id == BrowseIds.album(2) })
    }

    @Test
    fun `favourites browse and play in the order they were favourited`() {
        val favourites = listOf(libraryData.songs.first { it.title == "Gamma" })
        val context = library.copy(favourites = favourites)
        assertEquals(listOf("Gamma"), BrowseTree.childrenOf(context, BrowseIds.TAB_FAVOURITES).map { it.title })

        val node = BrowseTree.childrenOf(context, BrowseIds.TAB_FAVOURITES).single()
        assertEquals(listOf("Gamma"), BrowseTree.queueFor(context, node.id)!!.songs.map { it.title })
    }

    @Test
    fun `asking for favourites by voice plays the list, not a title that matches`() {
        val favourites = listOf(libraryData.songs.first { it.title == "Beta" })
        val context = library.copy(favourites = favourites)
        assertEquals(listOf("Beta"), BrowseTree.queueForSearch(context, "my favourites")!!.songs.map { it.title })
    }

    @Test
    fun `every root asks for a list, since there is no artwork to fill a grid`() {
        BrowseTree.rootChildren(library).forEach { node ->
            assertEquals(BrowseStyle.LIST, node.childStyle)
        }
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
    fun `a voice query prefers an album over a single matching track`() {
        val selection = BrowseTree.queueForSearch(library, "album b")!!
        assertEquals(listOf("Gamma"), selection.songs.map { it.title })
        assertEquals(0, selection.startIndex)
    }

    @Test
    fun `a voice query falling through to a title still plays something`() {
        val selection = BrowseTree.queueForSearch(library, "alpha")!!
        assertEquals("Alpha", selection.songs[selection.startIndex].title)
    }

    @Test
    fun `an empty voice query plays the whole library, an unmatched one plays nothing`() {
        assertEquals(3, BrowseTree.queueForSearch(library, "  ")!!.songs.size)
        assertNull(BrowseTree.queueForSearch(library, "nothing matches this"))
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
