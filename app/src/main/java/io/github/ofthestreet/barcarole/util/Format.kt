package io.github.ofthestreet.barcarole.util

import java.util.Locale
import java.util.concurrent.TimeUnit

/** mm:ss, or h:mm:ss once a track runs past an hour. */
fun formatDuration(durationMs: Long): String {
    val safe = durationMs.coerceAtLeast(0)
    val hours = TimeUnit.MILLISECONDS.toHours(safe)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun pluralCount(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"

/** Storage sizes as the file managers show them: one decimal from a megabyte up. */
fun formatSize(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0)
    return when {
        safe >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", safe / 1_000_000_000.0)
        safe >= 1_000_000 -> String.format(Locale.US, "%.1f MB", safe / 1_000_000.0)
        safe >= 1_000 -> String.format(Locale.US, "%d kB", safe / 1_000)
        else -> "$safe B"
    }
}
