package com.cdelarue.localmusic.ui.search

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
import com.cdelarue.localmusic.data.Album
import com.cdelarue.localmusic.data.Artist
import com.cdelarue.localmusic.data.Folder
import com.cdelarue.localmusic.data.SearchResults
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.ui.components.ArtistRow
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.ui.components.FolderRow
import com.cdelarue.localmusic.ui.components.IconTextRow
import com.cdelarue.localmusic.ui.components.SongRow
import androidx.compose.material.icons.rounded.Album as AlbumIcon
import com.cdelarue.localmusic.util.pluralCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: SearchResults,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
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
                SongRow(song = song, onClick = { onSongClick(song) })
            }
            section("Albums", results.albums.size)
            items(results.albums, key = { "album-${it.id}" }) { album ->
                IconTextRow(
                    icon = Icons.Rounded.AlbumIcon,
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
