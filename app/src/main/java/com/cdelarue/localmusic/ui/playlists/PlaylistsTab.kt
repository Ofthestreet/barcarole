package com.cdelarue.localmusic.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.data.stats.PlaylistId
import com.cdelarue.localmusic.data.stats.PlaylistSummary
import com.cdelarue.localmusic.data.stats.Playlists
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.ui.components.SongRow
import com.cdelarue.localmusic.util.pluralCount

private val MostPlayed = listOf(
    PlaylistId.MOST_PLAYED_MONTH to "This month",
    PlaylistId.MOST_PLAYED_YEAR to "This year",
    PlaylistId.MOST_PLAYED_ALL to "All time",
)

private val RecentlyAdded = listOf(
    PlaylistId.ADDED_WEEK to "1 week",
    PlaylistId.ADDED_MONTH to "1 month",
    PlaylistId.ADDED_2_MONTHS to "2 months",
    PlaylistId.ADDED_3_MONTHS to "3 months",
)

/**
 * The periods are variants of one idea, so they are chips rather than nine separate rows — and the
 * room that frees up goes to the selected list itself, which is what you actually came for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsTab(
    playlists: List<PlaylistSummary>,
    songs: List<Song>,
    playCounts: Map<Long, Int>,
    selected: PlaylistId,
    onSelect: (PlaylistId) -> Unit,
    onSongClick: (List<Song>, Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = playlists.associate { it.id to it.count }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "picker") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                PlaylistLine(
                    label = "Favourites",
                    count = counts[PlaylistId.FAVOURITES] ?: 0,
                    selected = selected == PlaylistId.FAVOURITES,
                    leading = {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    onClick = { onSelect(PlaylistId.FAVOURITES) },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                GroupLabel("Most played")
                ChipRow(MostPlayed, selected, onSelect)

                GroupLabel("Recently added")
                ChipRow(RecentlyAdded, selected, onSelect)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                PlaylistLine(
                    label = "Never played",
                    count = counts[PlaylistId.NEVER_PLAYED] ?: 0,
                    selected = selected == PlaylistId.NEVER_PLAYED,
                    onClick = { onSelect(PlaylistId.NEVER_PLAYED) },
                )
            }
        }

        item(key = "selection-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Playlists.title(selected),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralCount(songs.size, "track"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onPlayAll(songs) }, enabled = songs.isNotEmpty()) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play all")
                }
                IconButton(onClick = { onShuffleAll(songs) }, enabled = songs.isNotEmpty()) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle")
                }
            }
        }

        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "Nothing here yet",
                    body = emptyBody(selected),
                    modifier = Modifier.height(200.dp),
                )
            }
            return@LazyColumn
        }

        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                onClick = { onSongClick(songs, song) },
                onLongClick = { onSongLongClick(song) },
                trackNumber = index + 1,
                trailingText = playCounts[song.id]?.let { plays -> "$plays ×" },
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(
    entries: List<Pair<PlaylistId, String>>,
    selected: PlaylistId,
    onSelect: (PlaylistId) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (id, label) ->
            FilterChip(
                selected = id == selected,
                onClick = { onSelect(id) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun PlaylistLine(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun emptyBody(id: PlaylistId): String = when (id) {
    PlaylistId.FAVOURITES -> "Tap the heart on the playing bar to add one."
    PlaylistId.NEVER_PLAYED -> "You have played everything."
    PlaylistId.MOST_PLAYED_MONTH,
    PlaylistId.MOST_PLAYED_YEAR,
    PlaylistId.MOST_PLAYED_ALL,
    -> "Nothing has been played in this period yet."
    else -> "No files were added in this window."
}
