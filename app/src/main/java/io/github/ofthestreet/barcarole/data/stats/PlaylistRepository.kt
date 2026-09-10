package io.github.ofthestreet.barcarole.data.stats

import io.github.ofthestreet.barcarole.data.LibraryRepository
import io.github.ofthestreet.barcarole.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistData(
    val favouriteIds: List<Long> = emptyList(),
    val playedIds: Set<Long> = emptySet(),
    val countsThisMonth: List<PlayCount> = emptyList(),
    val countsThisYear: List<PlayCount> = emptyList(),
    val countsAllTime: List<PlayCount> = emptyList(),
)

/** Turns the play history into the playlists the UI and the car both read. */
@Singleton
class PlaylistRepository @Inject constructor(
    private val history: PlayHistoryDao,
    private val favourites: FavouriteDao,
    private val libraryRepository: LibraryRepository,
    private val scope: CoroutineScope,
) {

    private fun clock(): Long = System.currentTimeMillis()

    private val zone: ZoneId get() = ZoneId.systemDefault()

    val data: StateFlow<PlaylistData> = combine(
        favourites.favouriteIds(),
        history.playedSongIds().map { it.toSet() },
        history.countsSince(PlaylistWindows.startOfMonth(clock(), zone)),
        history.countsSince(PlaylistWindows.startOfYear(clock(), zone)),
        history.countsSince(0L),
    ) { favouriteIds, playedIds, month, year, all ->
        PlaylistData(favouriteIds, playedIds, month, year, all)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), PlaylistData())

    val favouriteIds: Flow<List<Long>> = favourites.favouriteIds()

    fun songsOf(id: PlaylistId): List<Song> {
        val songs = libraryRepository.library.value.songs
        val d = data.value
        val now = clock()
        return when (id) {
            PlaylistId.FAVOURITES -> Playlists.favourites(songs, d.favouriteIds)
            PlaylistId.MOST_PLAYED_MONTH -> Playlists.mostPlayed(songs, d.countsThisMonth)
            PlaylistId.MOST_PLAYED_YEAR -> Playlists.mostPlayed(songs, d.countsThisYear)
            PlaylistId.MOST_PLAYED_ALL -> Playlists.mostPlayed(songs, d.countsAllTime)
            PlaylistId.ADDED_WEEK -> Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 7))
            PlaylistId.ADDED_MONTH -> Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 30))
            PlaylistId.ADDED_2_MONTHS -> Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 60))
            PlaylistId.ADDED_3_MONTHS -> Playlists.addedSince(songs, PlaylistWindows.daysAgo(now, 90))
            PlaylistId.NEVER_PLAYED -> Playlists.neverPlayed(songs, d.playedIds)
        }
    }

    /** Play counts behind a "most played" list, so the screen can show them. Empty elsewhere. */
    fun playCounts(id: PlaylistId): Map<Long, Int> {
        val counts = when (id) {
            PlaylistId.MOST_PLAYED_MONTH -> data.value.countsThisMonth
            PlaylistId.MOST_PLAYED_YEAR -> data.value.countsThisYear
            PlaylistId.MOST_PLAYED_ALL -> data.value.countsAllTime
            else -> return emptyMap()
        }
        return counts.associate { it.songId to it.plays }
    }

    fun summaries(): List<PlaylistSummary> = PlaylistId.entries.map { id ->
        PlaylistSummary(id = id, title = Playlists.title(id), count = songsOf(id).size)
    }

    suspend fun isFavourite(songId: Long): Boolean = favourites.count(songId) > 0

    fun toggleFavourite(songId: Long) {
        scope.launch {
            if (favourites.count(songId) > 0) {
                favourites.remove(songId)
            } else {
                favourites.add(Favourite(songId = songId, addedAt = clock()))
            }
        }
    }

    /** After a file is deleted there is nothing left to remember about it. */
    suspend fun forget(songId: Long) {
        favourites.remove(songId)
        history.forget(songId)
    }

    suspend fun recordPlay(songId: Long) {
        history.recordPlay(PlayEvent(songId = songId, playedAt = clock()))
    }
}
