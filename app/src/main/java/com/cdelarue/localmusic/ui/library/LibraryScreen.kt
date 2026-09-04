package com.cdelarue.localmusic.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SortByAlpha
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cdelarue.localmusic.data.Album
import com.cdelarue.localmusic.data.Artist
import com.cdelarue.localmusic.data.LibraryIndex
import com.cdelarue.localmusic.data.LibraryTab
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.data.SongSort
import com.cdelarue.localmusic.playback.PlayerState
import com.cdelarue.localmusic.ui.components.AlbumRow
import com.cdelarue.localmusic.ui.components.AlphabetRail
import com.cdelarue.localmusic.ui.components.ArtistRow
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.ui.components.SongRow
import com.cdelarue.localmusic.data.stats.PlaylistId
import com.cdelarue.localmusic.data.stats.PlaylistSummary
import com.cdelarue.localmusic.ui.playlists.PlaylistsTab
import com.cdelarue.localmusic.ui.queue.QueueTab

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
    onOpenPlaylist: (PlaylistId) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    val sortable = selectedTab == LibraryTab.SONGS ||
        selectedTab == LibraryTab.ALBUMS ||
        selectedTab == LibraryTab.ARTISTS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
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
                                    val current = state.settings.sortFor(selectedTab)
                                    val marker = if (current.key == key) {
                                        if (current.ascending) "  ↑" else "  ↓"
                                    } else {
                                        ""
                                    }
                                    DropdownMenuItem(
                                        text = { Text(key.label() + marker) },
                                        onClick = {
                                            onSort(selectedTab, key)
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
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { onSelectTab(tab) },
                        text = { Text(tab.label()) },
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

            when (selectedTab) {
                LibraryTab.SONGS -> SongsTab(state.songsSorted(LibraryTab.SONGS), onSongClick, onSongLongClick)
                LibraryTab.ALBUMS -> AlbumsTab(state.library.albums, onAlbumClick)
                LibraryTab.ARTISTS -> ArtistsTab(state.library.artists, onArtistClick)
                LibraryTab.PLAYLISTS -> PlaylistsTab(playlists = playlists, onOpen = onOpenPlaylist)
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
