package com.cdelarue.localmusic.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cdelarue.localmusic.BuildConfig
import com.cdelarue.localmusic.data.Settings
import com.cdelarue.localmusic.data.TextSize
import com.cdelarue.localmusic.data.ThemeMode
import com.cdelarue.localmusic.util.pluralCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    supportsDynamicColor: Boolean,
    songCount: Int,
    folderCount: Int,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onMinTrackSeconds: (Int) -> Unit,
    onShowAlbums: (Boolean) -> Unit,
    onTextSize: (TextSize) -> Unit,
    onRescan: () -> Unit,
    onBrowseFolders: () -> Unit,
    duplicateCount: Int,
    onBrowseDuplicates: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .padding(insets)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Appearance")
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeMode(mode) }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { onThemeMode(mode) })
                    Text(mode.label())
                }
            }
            if (supportsDynamicColor) {
                SettingRow(
                    title = "Dynamic color",
                    subtitle = "Take the palette from the wallpaper",
                ) {
                    Switch(checked = settings.dynamicColor, onCheckedChange = onDynamicColor)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Text size", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Below 100% the text shrinks, so more rows fit on a small screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextSize.entries.forEach { size ->
                    FilterChip(
                        selected = settings.textSize == size,
                        onClick = { onTextSize(size) },
                        label = { Text(size.label) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle("Library")

            SettingRow(
                title = "Show albums",
                subtitle = "Adds the albums tab, and album results in search",
            ) {
                Switch(checked = settings.showAlbums, onCheckedChange = onShowAlbums)
            }

            LinkRow(
                title = "Browse by folder",
                subtitle = pluralCount(folderCount, "folder") + " on this device",
                onClick = onBrowseFolders,
            )

            LinkRow(
                title = "Remove duplicates",
                subtitle = if (duplicateCount == 0) {
                    "Every track appears once"
                } else {
                    pluralCount(duplicateCount, "extra copy", "extra copies") + " to review"
                },
                onClick = onBrowseDuplicates,
            )

            var sliderValue by remember(settings.minTrackSeconds) {
                mutableFloatStateOf(settings.minTrackSeconds.toFloat())
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Ignore tracks shorter than ${sliderValue.toInt()} s", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Keeps ringtones and voice memos out of the library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onMinTrackSeconds(sliderValue.toInt()) },
                    valueRange = 0f..120f,
                    steps = 23,
                )
            }

            SettingRow(
                title = "Rescan library",
                subtitle = pluralCount(songCount, "track") + " indexed",
            ) {
                TextButton(onClick = onRescan) { Text("Rescan") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle("About")
            Text(
                text = "Local music ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Text(
                text = "Offline player. No network permission, no accounts, no telemetry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow the system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
