package io.github.ofthestreet.barcarole.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ofthestreet.barcarole.R

@Composable
fun PermissionScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_illustration),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(28.dp))
                .padding(bottom = 8.dp),
        )
        Text(
            text = "Access to your audio files",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "This app only reads the music already stored on this phone. It has no network " +
                "access and sends nothing anywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (permanentlyDenied) {
            Text(
                text = "The permission was denied for good, so it has to be granted from system settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            }) {
                Text("Open app settings")
            }
            TextButton(onClick = onRequest) { Text("Try again") }
        } else {
            Button(onClick = onRequest) { Text("Grant access") }
        }
    }
}
