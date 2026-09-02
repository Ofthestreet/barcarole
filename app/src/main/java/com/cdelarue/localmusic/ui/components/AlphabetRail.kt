package com.cdelarue.localmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A-Z rail down the right edge. Tapping or dragging jumps the list to the first entry with that
 * initial; letters with nothing behind them are dimmed rather than hidden, so the rail keeps a
 * stable shape as the library changes.
 */
@Composable
fun AlphabetRail(
    initials: List<String>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    headerOffset: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val letters = remember { listOf("#") + ('A'..'Z').map { it.toString() } }
    val firstIndexOf = remember(initials) {
        val map = LinkedHashMap<String, Int>()
        initials.forEachIndexed { index, initial -> if (!map.containsKey(initial)) map[initial] = index }
        map
    }
    val railHeight = remember { mutableIntStateOf(1) }

    fun jumpTo(letter: String) {
        val target = firstIndexOf[letter] ?: return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { listState.scrollToItem(target + headerOffset) }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .padding(vertical = 8.dp)
            .pointerInput(firstIndexOf) {
                railHeight.intValue = size.height.coerceAtLeast(1)
                detectVerticalDragGestures { change, _ ->
                    val fraction = (change.position.y / railHeight.intValue).coerceIn(0f, 0.999f)
                    jumpTo(letters[(fraction * letters.size).toInt()])
                }
            },
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val enabled = firstIndexOf.containsKey(letter)
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                modifier = Modifier
                    .clickable(enabled = enabled) { jumpTo(letter) }
                    .padding(vertical = 1.dp),
            )
        }
    }
}
