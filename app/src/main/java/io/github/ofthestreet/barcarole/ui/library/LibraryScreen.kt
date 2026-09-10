package io.github.ofthestreet.barcarole.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.ofthestreet.barcarole.R
import io.github.ofthestreet.barcarole.data.Album
import io.github.ofthestreet.barcarole.data.Artist
import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.LibraryTab
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.data.SongSort
import io.github.ofthestreet.barcarole.playback.PlayerState
import io.github.ofthestreet.barcarole.ui.components.AlbumRow
import io.github.ofthestreet.barcarole.ui.components.AlphabetRail
import io.github.ofthestreet.barcarole.ui.components.ArtistRow
import io.github.ofthestreet.barcarole.ui.components.EmptyState
import io.github.ofthestreet.barcarole.ui.components.SongRow
import io.github.ofthestreet.barcarole.data.stats.PlaylistId
import io.github.ofthestreet.barcarole.data.stats.PlaylistSummary
import io.github.ofthestreet.barcarole.ui.playlists.PlaylistsTab
import io.github.ofthestreet.barcarole.ui.queue.QueueTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    playerState: PlayerState,
    playlists: List<PlaylistSummary>,
    selectedTab: LibraryTab,
    onSelectTab: (LibraryTab) -> Unit,
    onSort: (LibraryTab, SongSort) -> Unit,
    onRescan: () -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onQueuePlayIndex: (Int) -> Unit,
    onQueueMove: (Int, Int) -> Unit,
    onQueueRemove: (Int) -> Unit,
    onQueueClear: () -> Unit,
    onToggleShuffle: () -> Unit,
    selectedPlaylist: PlaylistId,
    playlistSongs: List<Song>,
    playlistCounts: Map<Long, Int>,
    onSelectPlaylist: (PlaylistId) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Albums are optional, so the tab strip is a filtered list and its index is a position in
    // that list, never the enum's ordinal.
    val tabs = LibraryTab.entries.filter { it != LibraryTab.ALBUMS || state.settings.showAlbums }
    val effectiveTab = if (selectedTab in tabs) selectedTab else LibraryTab.SONGS
    val sortable = effectiveTab == LibraryTab.SONGS ||
        effectiveTab == LibraryTab.ALBUMS ||
        effectiveTab == LibraryTab.ARTISTS

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The launcher artwork fills its own square, so it only needs rounding.
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_illustration),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Barcarole")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                    if (sortable) {
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.Rounded.SortByAlpha, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                SongSort.entries.forEach { key ->
                                    val current = state.settings.sortFor(effectiveTab)
                                    val marker = if (current.key == key) {
                                        if (current.ascending) "  ↑" else "  ↓"
                                    } else {
                                        ""
                                    }
                                    DropdownMenuItem(
                                        text = { Text(key.label() + marker) },
                                        onClick = {
                                            onSort(effectiveTab, key)
                                            sortMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan library")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            if (state.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            TabRow(selectedTabIndex = tabs.indexOf(effectiveTab).coerceAtLeast(0)) {
                tabs.forEach { tab ->
                    Tab(
                        selected = tab == effectiveTab,
                        onClick = { onSelectTab(tab) },
                        // The label survives as the description: a screen reader still says it.
                        icon = { Icon(tab.icon(), contentDescription = tab.label()) },
                    )
                }
            }

            if (state.hasScanned && state.library.isEmpty && sortable) {
                EmptyState(
                    title = "No music found",
                    body = "Copy audio files onto the phone, then use the rescan button in the top bar.",
                )
                return@Column
            }

            when (effectiveTab) {
                LibraryTab.SONGS -> SongsTab(state.songsSorted(LibraryTab.SONGS), onSongClick, onSongLongClick)
                LibraryTab.ALBUMS -> AlbumsTab(state.library.albums, onAlbumClick)
                LibraryTab.ARTISTS -> ArtistsTab(state.library.artists, onArtistClick)
                LibraryTab.PLAYLISTS -> PlaylistsTab(
                    playlists = playlists,
                    songs = playlistSongs,
                    playCounts = playlistCounts,
                    selected = selectedPlaylist,
                    onSelect = onSelectPlaylist,
                    onSongClick = onSongClick,
                    onSongLongClick = onSongLongClick,
                    onPlayAll = onPlayAll,
                    onShuffleAll = onShuffleAll,
                )
                LibraryTab.QUEUE -> QueueTab(
                    state = playerState,
                    onPlayIndex = onQueuePlayIndex,
                    onMove = onQueueMove,
                    onRemove = onQueueRemove,
                    onClear = onQueueClear,
                    onToggleShuffle = onToggleShuffle,
                )
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    onSongClick: (List<Song>, Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
) {
    val listState = rememberLazyListState()
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(songs, song) },
                    onLongClick = { onSongLongClick(song) },
                )
            }
        }
        AlphabetRail(
            initials = songs.map { LibraryIndex.sortInitial(it.title) },
            listState = listState,
        )
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val listState = rememberLazyListState()
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(albums, key = { it.id }) { album ->
                AlbumRow(album = album, onClick = { onAlbumClick(album) })
            }
        }
        AlphabetRail(
            initials = albums.map { LibraryIndex.sortInitial(it.title) },
            listState = listState,
        )
    }
}

@Composable
private fun ArtistsTab(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    val listState = rememberLazyListState()
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(artists, key = { it.id }) { artist ->
                ArtistRow(artist = artist, onClick = { onArtistClick(artist) })
            }
        }
        AlphabetRail(
            initials = artists.map { LibraryIndex.sortInitial(it.name) },
            listState = listState,
        )
    }
}

/**
 * One glyph per tab, chosen so the silhouettes differ rather than the details: a queue and a
 * set of playlists are both lists of tracks, and at this size only the outline reads.
 */
private fun LibraryTab.icon(): ImageVector = when (this) {
    LibraryTab.SONGS -> Icons.Rounded.MusicNote
    LibraryTab.ALBUMS -> Icons.Rounded.Album
    LibraryTab.ARTISTS -> Icons.Rounded.Person
    LibraryTab.QUEUE -> Icons.Rounded.QueueMusic
    LibraryTab.PLAYLISTS -> Icons.Rounded.Star
}

private fun LibraryTab.label(): String = when (this) {
    LibraryTab.SONGS -> "Songs"
    LibraryTab.ALBUMS -> "Albums"
    LibraryTab.ARTISTS -> "Artists"
    LibraryTab.QUEUE -> "Queue"
    LibraryTab.PLAYLISTS -> "Playlists"
}

private fun SongSort.label(): String = when (this) {
    SongSort.TITLE -> "Title"
    SongSort.ARTIST -> "Artist"
    SongSort.ALBUM -> "Album"
    SongSort.DATE_ADDED -> "Date added"
    SongSort.DURATION -> "Duration"
}
