package io.github.ofthestreet.barcarole.playback

import io.github.ofthestreet.barcarole.data.Library
import io.github.ofthestreet.barcarole.data.stats.Playlists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the browse context the car reads, ready at every moment.
 *
 * The browse callbacks in [PlaybackService] are synchronous: whatever this holds when Android Auto
 * asks is exactly what the car shows. Sharing eagerly, for the whole life of the application scope,
 * is therefore the point of this class rather than a detail of it — a context assembled only while
 * a screen happens to be watching is empty precisely when the car asks first, which is what left
 * the lists blank in a car connected to a phone whose app had not been opened.
 *
 * Built from flows rather than from the repositories so the assembly is covered by a plain JVM test.
 */
class BrowseContextSource(
    library: Flow<Library>,
    favouriteIds: Flow<List<Long>>,
    showAlbums: Flow<Boolean>,
    scope: CoroutineScope,
) {

    val context: StateFlow<BrowseContext> = combine(
        library,
        favouriteIds,
        showAlbums,
    ) { scanned, favourites, albums ->
        BrowseContext(
            library = scanned,
            favourites = Playlists.favourites(scanned.songs, favourites),
            showAlbums = albums,
        )
    }.stateIn(scope, SharingStarted.Eagerly, BrowseContext(library = Library()))
}
