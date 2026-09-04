package com.cdelarue.localmusic.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cdelarue.localmusic.data.Folder
import com.cdelarue.localmusic.ui.components.EmptyState
import com.cdelarue.localmusic.ui.components.FolderRow

/** Folder browsing moved out of the tabs and into settings; this is where it lands. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<Folder>,
    onBack: () -> Unit,
    onFolderClick: (Folder) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { insets ->
        if (folders.isEmpty()) {
            EmptyState(
                title = "No folders",
                body = "Nothing in the library yet.",
                modifier = Modifier.padding(insets),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(insets).fillMaxSize()) {
            items(folders, key = { it.path }) { folder ->
                FolderRow(folder = folder, onClick = { onFolderClick(folder) })
            }
        }
    }
}
