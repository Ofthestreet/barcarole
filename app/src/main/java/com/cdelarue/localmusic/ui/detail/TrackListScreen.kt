package com.cdelarue.localmusic.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.ui.components.SongRow
import com.cdelarue.localmusic.util.formatDuration
import com.cdelarue.localmusic.util.pluralCount

/** Shared by album, artist and folder detail — the three differ only in the header. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { insets ->
        if (songs.isEmpty()) {
            EmptyState(
                title = "Nothing here",
                body = "These tracks are no longer in the library.",
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(insets).fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "${pluralCount(songs.size, "track")} · ${formatDuration(songs.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = { onPlayAll(songs) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text("Play all", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { onShuffleAll(songs) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Text("Shuffle", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(songs, song) },
                    onLongClick = { onSongLongClick(song) },
                    trackNumber = index + 1,
                )
            }
        }
    }
}
