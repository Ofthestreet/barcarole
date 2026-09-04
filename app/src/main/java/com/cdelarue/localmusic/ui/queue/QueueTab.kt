package com.cdelarue.localmusic.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cdelarue.localmusic.playback.PlayerState
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.util.formatDuration
import com.cdelarue.localmusic.util.pluralCount

private val RowHeight = 56.dp

/**
 * The queue is a library tab, not a screen of its own, so it carries its own action row instead of
 * a top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueTab(
    state: PlayerState,
    onPlayIndex: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dragging moves the item a row at a time and hands the move straight to the player, so the
    // queue never holds a second, temporary ordering.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }

    if (state.queue.isEmpty()) {
        EmptyState(
            title = "The queue is empty",
            body = "Play something from the library and it shows up here.",
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralCount(state.queue.size, "track"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            TextButton(onClick = onClear) { Text("Clear") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(
                items = state.queue,
                key = { index, entry -> "$index-${entry.mediaId}" },
            ) { index, entry ->
                val isDragging = dragIndex == index
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onRemove(index)
                            true
                        } else {
                            false
                        }
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                    backgroundContent = { RemoveBackground() },
                ) {
                    QueueRow(
                        title = entry.title,
                        artist = entry.artist,
                        isCurrent = index == state.queueIndex,
                        isPlaying = state.isPlaying && index == state.queueIndex,
                        onClick = { onPlayIndex(index) },
                        handleModifier = Modifier.pointerInput(state.queue.size) {
                            detectDragGestures(
                                onDragStart = {
                                    dragIndex = index
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    dragIndex = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragIndex = null
                                    dragOffset = 0f
                                },
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val from = dragIndex ?: return@detectDragGestures
                                val steps = (dragOffset / rowHeightPx).toInt()
                                if (steps != 0) {
                                    val to = (from + steps).coerceIn(0, state.queue.lastIndex)
                                    if (to != from) {
                                        onMove(from, to)
                                        dragIndex = to
                                        dragOffset -= (to - from) * rowHeightPx
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    title: String,
    artist: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    handleModifier: Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = handleModifier,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isPlaying) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Playing",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun RemoveBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
