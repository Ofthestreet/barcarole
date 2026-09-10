package io.github.ofthestreet.barcarole.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ofthestreet.barcarole.data.DuplicateGroup
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.ui.components.EmptyState
import io.github.ofthestreet.barcarole.util.formatDuration
import io.github.ofthestreet.barcarole.util.formatSize
import io.github.ofthestreet.barcarole.util.pluralCount

/**
 * Lists the files that look like copies of one another, and deletes the ones the user leaves
 * ticked. Nothing is deleted without passing through this screen: the app proposes, the user
 * decides, and the files themselves go, not just their entries in the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    groups: List<DuplicateGroup>,
    onBack: () -> Unit,
    onDelete: (List<Song>) -> Unit,
) {
    // Keyed on the copy being kept, which is stable for as long as the library is.
    var excluded by remember { mutableStateOf(emptySet<Long>()) }
    var confirming by remember { mutableStateOf(false) }

    val selected = groups.filterNot { it.keep.id in excluded }
    val doomed = selected.flatMap { it.remove }
    val reclaimed = selected.sumOf { it.wastedBytes }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete ${pluralCount(doomed.size, "file")}?") },
            text = {
                Text(
                    "One copy of each track stays on the phone; the others are deleted and " +
                        "cannot be recovered. This frees ${formatSize(reclaimed)}.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onDelete(doomed)
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { insets ->
        if (groups.isEmpty()) {
            EmptyState(
                title = "No duplicates",
                body = "Every track in the library appears once.",
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }

        Column(modifier = Modifier.padding(insets).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = pluralCount(doomed.size, "file") + " to delete, " +
                        formatSize(reclaimed) + " freed",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Untick a track to keep all of its copies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { confirming = true },
                    enabled = doomed.isNotEmpty(),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Delete " + pluralCount(doomed.size, "file"))
                }
            }
            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups, key = { it.keep.id }) { group ->
                    GroupRow(
                        group = group,
                        checked = group.keep.id !in excluded,
                        onCheckedChange = { keepAll ->
                            excluded = if (keepAll) {
                                excluded - group.keep.id
                            } else {
                                excluded + group.keep.id
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: DuplicateGroup, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            Text(
                text = group.keep.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = group.keep.artist + " · " + formatDuration(group.keep.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CopyLine(song = group.keep, verb = "Keep", highlight = true)
            group.remove.forEach { copy ->
                CopyLine(song = copy, verb = "Delete", highlight = false)
            }
        }
    }
}

@Composable
private fun CopyLine(song: Song, verb: String, highlight: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = verb,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
        )
        Text(
            text = song.folderPath.substringAfterLast('/', missingDelimiterValue = song.folderPath),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatSize(song.sizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
