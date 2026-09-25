package io.github.ofthestreet.barcarole

import io.github.ofthestreet.barcarole.data.Library
import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.playback.BrowseContextSource
import io.github.ofthestreet.barcarole.playback.BrowseIds
import io.github.ofthestreet.barcarole.playback.BrowseTree
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The browse tree's own test builds a context by hand, which is the one thing the car never does.
 * These drive the tree through the context the service actually reads, which is where the car found
 * the lists empty: the tabs were there, and every one of them was blank.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BrowseContextSourceTest {

    private fun song(id: Long, title: String, artist: String = "Artist A", album: String = "Album A") =
        Song(id, title, artist, album, 1, 10, 200_000, id.toInt(), 2001, 1_000, "/storage/emulated/0/Music/$title.mp3")

    private val scanned = LibraryIndex.build(listOf(song(1, "Alpha"), song(2, "Beta"), song(3, "Gamma")))

    /** The three inputs the car's view of the library is assembled from. */
    private class Inputs(
        val library: MutableStateFlow<Library> = MutableStateFlow(Library()),
        val favouriteIds: MutableStateFlow<List<Long>> = MutableStateFlow(emptyList()),
        val showAlbums: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ) {
        fun source(scope: TestScope) = BrowseContextSource(library, favouriteIds, showAlbums, scope.backgroundScope)
    }

    @Test
    fun `the car sees songs and favourites without the phone screen ever being opened`() = runTest {
        val inputs = Inputs(favouriteIds = MutableStateFlow(listOf(3L, 1L)))
        inputs.library.value = scanned
        val source = inputs.source(this)
        advanceUntilIdle()

        // Read through .value, with nothing collecting: that is all a browse callback gets to do.
        val context = source.context.value
        assertEquals(listOf("Alpha", "Beta", "Gamma"), BrowseTree.childrenOf(context, BrowseIds.TAB_SONGS).map { it.title })
        // Most recently favourited first, as the playlists screen shows them.
        assertEquals(listOf("Gamma", "Alpha"), BrowseTree.childrenOf(context, BrowseIds.TAB_FAVOURITES).map { it.title })
    }

    @Test
    fun `favourites are still there long after any screen would have stopped watching`() = runTest {
        val inputs = Inputs(favouriteIds = MutableStateFlow(listOf(2L)))
        inputs.library.value = scanned
        val source = inputs.source(this)
        advanceUntilIdle()

        // A snapshot shared only while subscribed to would have lapsed by now; this one may not.
        advanceTimeBy(FIVE_MINUTES_MS)
        advanceUntilIdle()
        assertEquals(listOf("Beta"), BrowseTree.childrenOf(source.context.value, BrowseIds.TAB_FAVOURITES).map { it.title })
    }

    @Test
    fun `a scan landing after the car has already asked changes the context, so it can be announced`() = runTest {
        val inputs = Inputs()
        val source = inputs.source(this)
        advanceUntilIdle()

        // The car connects first: this is the empty answer it caches.
        val beforeScan = source.context.value
        assertTrue(BrowseTree.childrenOf(beforeScan, BrowseIds.TAB_SONGS).isEmpty())

        inputs.library.value = scanned
        inputs.favouriteIds.value = listOf(1L)
        advanceUntilIdle()

        // A new value is what the service notifies the car about; an unchanged one would be silent.
        val afterScan = source.context.value
        assertNotEquals(beforeScan, afterScan)
        assertEquals(3, BrowseTree.childrenOf(afterScan, BrowseIds.TAB_SONGS).size)
        assertEquals(listOf("Alpha"), BrowseTree.childrenOf(afterScan, BrowseIds.TAB_FAVOURITES).map { it.title })
    }

    @Test
    fun `the albums tab follows the setting, in the car as on the phone`() = runTest {
        val inputs = Inputs()
        inputs.library.value = scanned
        val source = inputs.source(this)
        advanceUntilIdle()
        assertTrue(BrowseTree.rootChildren(source.context.value).none { it.id == BrowseIds.TAB_ALBUMS })

        inputs.showAlbums.value = true
        advanceUntilIdle()
        assertTrue(BrowseTree.rootChildren(source.context.value).any { it.id == BrowseIds.TAB_ALBUMS })
    }

    @Test
    fun `a favourite whose file has gone drops out instead of emptying the list`() = runTest {
        val inputs = Inputs(favouriteIds = MutableStateFlow(listOf(99L, 2L)))
        inputs.library.value = scanned
        val source = inputs.source(this)
        advanceUntilIdle()

        assertEquals(listOf("Beta"), BrowseTree.childrenOf(source.context.value, BrowseIds.TAB_FAVOURITES).map { it.title })
    }

    private companion object {
        /** Well past the window a snapshot shared only while subscribed to would survive. */
        const val FIVE_MINUTES_MS = 5L * 60 * 1000
    }
}
