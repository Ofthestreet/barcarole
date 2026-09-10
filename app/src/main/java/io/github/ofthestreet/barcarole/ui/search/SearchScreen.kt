package io.github.ofthestreet.barcarole.ui.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.ofthestreet.barcarole.data.Album
import io.github.ofthestreet.barcarole.data.Artist
import io.github.ofthestreet.barcarole.data.Folder
import io.github.ofthestreet.barcarole.data.SearchResults
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.ui.components.ArtistRow
import io.github.ofthestreet.barcarole.ui.components.EmptyState
import io.github.ofthestreet.barcarole.ui.components.FolderRow
import io.github.ofthestreet.barcarole.ui.components.TextRow
import io.github.ofthestreet.barcarole.ui.components.SongRow
import io.github.ofthestreet.barcarole.util.pluralCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: SearchResults,
    showAlbums: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onFolderClick: (Folder) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Songs, albums, artists, folders") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { insets ->
        if (query.isBlank()) {
            EmptyState(
                title = "Search your library",
                body = "Type a title, an artist, an album or a folder name.",
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }
        if (results.isEmpty) {
            EmptyState(
                title = "No matches",
                body = "Nothing in the library matches \"$query\".",
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(insets).fillMaxSize()) {
            section("Songs", results.songs.size)
            items(results.songs, key = { "song-${it.id}" }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(results.songs, song) },
                    onLongClick = { onSongLongClick(song) },
                )
            }
            section("Albums", if (showAlbums) results.albums.size else 0)
            items(if (showAlbums) results.albums else emptyList(), key = { "album-${it.id}" }) { album ->
                TextRow(
                    title = album.title,
                    subtitle = "${album.artist} · ${pluralCount(album.songCount, "track")}",
                    onClick = { onAlbumClick(album) },
                )
            }
            section("Artists", results.artists.size)
            items(results.artists, key = { "artist-${it.id}" }) { artist ->
                ArtistRow(artist = artist, onClick = { onArtistClick(artist) })
            }
            section("Folders", results.folders.size)
            items(results.folders, key = { "folder-${it.path}" }) { folder ->
                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: String, count: Int) {
    if (count == 0) return
    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}
