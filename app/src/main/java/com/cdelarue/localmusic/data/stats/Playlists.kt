package com.cdelarue.localmusic.data.stats

import com.cdelarue.localmusic.data.Song
import java.time.Instant
import java.time.ZoneId

enum class PlaylistId {
    FAVOURITES,
    MOST_PLAYED_MONTH,
    MOST_PLAYED_YEAR,
    MOST_PLAYED_ALL,
    ADDED_WEEK,
    ADDED_MONTH,
    ADDED_2_MONTHS,
    ADDED_3_MONTHS,
    NEVER_PLAYED,
}

data class PlaylistSummary(val id: PlaylistId, val title: String, val count: Int)

/**
 * The windows behind the playlists. "This month" and "this year" are calendar periods — the month
 * you are in, not the last thirty days — while the "recently added" ones are rolling windows
 * counted back from now, which is how people read them.
 */
object PlaylistWindows {

    fun startOfMonth(nowMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zone)
            .withDayOfMonth(1).toLocalDate().atStartOfDay(zone)
            .toInstant().toEpochMilli()

    fun startOfYear(nowMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zone)
            .withDayOfYear(1).toLocalDate().atStartOfDay(zone)
            .toInstant().toEpochMilli()

    fun daysAgo(nowMillis: Long, days: Long): Long = nowMillis - days * MILLIS_PER_DAY

    const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
}

/** Pure list building, so the ordering rules are covered by plain JVM tests. */
object Playlists {

    /** Most recently favourited first: the order the ids arrive in. */
    fun favourites(songs: List<Song>, favouriteIds: List<Long>): List<Song> {
        val byId = songs.associateBy { it.id }
        return favouriteIds.mapNotNull { byId[it] }
    }

    /** Play counts arrive already ordered by the database; songs that vanished drop out. */
    fun mostPlayed(songs: List<Song>, counts: List<PlayCount>, limit: Int = 100): List<Song> {
        val byId = songs.associateBy { it.id }
        return counts.asSequence().mapNotNull { byId[it.songId] }.take(limit).toList()
    }

    /** MediaStore keeps the added date in seconds, not milliseconds. */
    fun addedSince(songs: List<Song>, sinceMillis: Long): List<Song> {
        val sinceSeconds = sinceMillis / 1000
        return songs.filter { it.dateAddedSeconds >= sinceSeconds }
            .sortedByDescending { it.dateAddedSeconds }
    }

    fun neverPlayed(songs: List<Song>, playedIds: Set<Long>): List<Song> =
        songs.filterNot { it.id in playedIds }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun title(id: PlaylistId): String = when (id) {
        PlaylistId.FAVOURITES -> "Favourites"
        PlaylistId.MOST_PLAYED_MONTH -> "Most played this month"
        PlaylistId.MOST_PLAYED_YEAR -> "Most played this year"
        PlaylistId.MOST_PLAYED_ALL -> "Most played of all time"
        PlaylistId.ADDED_WEEK -> "Added this week"
        PlaylistId.ADDED_MONTH -> "Added this month"
        PlaylistId.ADDED_2_MONTHS -> "Added in the last 2 months"
        PlaylistId.ADDED_3_MONTHS -> "Added in the last 3 months"
        PlaylistId.NEVER_PLAYED -> "Never played"
    }
}
