package io.github.ofthestreet.barcarole.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.ofthestreet.barcarole.data.Song

/**
 * Deleting removes the file from the phone, not just from the library, so the dialog names the
 * track and says so plainly. On Android 11 and later the system asks a second time on top of this.
 */
@Composable
fun DeleteSongDialog(song: Song, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this file?") },
        text = {
            Text(
                "\"${song.title}\" by ${song.artist} will be deleted from this phone. " +
                    "This cannot be undone.",
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
