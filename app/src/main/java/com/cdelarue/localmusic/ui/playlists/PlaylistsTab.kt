package com.cdelarue.localmusic.ui.playlists

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cdelarue.localmusic.data.stats.PlaylistId
import com.cdelarue.localmusic.data.stats.PlaylistSummary
import com.cdelarue.localmusic.ui.components.TextRow
import com.cdelarue.localmusic.util.pluralCount

/**
 * Every playlist here is computed, not edited: favourites come from the heart button, the rest
 * from what has been played and when files landed on the phone.
 */
@Composable
fun PlaylistsTab(
    playlists: List<PlaylistSummary>,
    onOpen: (PlaylistId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byId = playlists.associateBy { it.id }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        section("Favourites")
        rows(byId, listOf(PlaylistId.FAVOURITES), onOpen)

        section("Most played")
        rows(
            byId,
            listOf(
                PlaylistId.MOST_PLAYED_MONTH,
                PlaylistId.MOST_PLAYED_YEAR,
                PlaylistId.MOST_PLAYED_ALL,
            ),
            onOpen,
        )

        section("Recently added")
        rows(
            byId,
            listOf(
                PlaylistId.ADDED_WEEK,
                PlaylistId.ADDED_MONTH,
                PlaylistId.ADDED_2_MONTHS,
                PlaylistId.ADDED_3_MONTHS,
            ),
            onOpen,
        )

        section("Waiting for you")
        rows(byId, listOf(PlaylistId.NEVER_PLAYED), onOpen)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: String) {
    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rows(
    byId: Map<PlaylistId, PlaylistSummary>,
    ids: List<PlaylistId>,
    onOpen: (PlaylistId) -> Unit,
) {
    items(ids, key = { it.name }) { id ->
        val summary = byId[id] ?: return@items
        TextRow(
            title = summary.title,
            subtitle = pluralCount(summary.count, "track"),
            onClick = { onOpen(id) },
        )
    }
}
