package io.github.ofthestreet.barcarole.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ofthestreet.barcarole.data.Library
import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.LibraryRepository
import io.github.ofthestreet.barcarole.data.LibraryTab
import io.github.ofthestreet.barcarole.data.SearchResults
import io.github.ofthestreet.barcarole.data.Settings
import io.github.ofthestreet.barcarole.data.SettingsStore
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.data.SongSort
import io.github.ofthestreet.barcarole.data.DeleteOutcome
import io.github.ofthestreet.barcarole.data.SongDeleter
import io.github.ofthestreet.barcarole.data.SortOrder
import io.github.ofthestreet.barcarole.data.stats.PlaylistId
import io.github.ofthestreet.barcarole.data.stats.PlaylistRepository
import io.github.ofthestreet.barcarole.data.stats.PlaylistSummary
import io.github.ofthestreet.barcarole.data.TextSize
import io.github.ofthestreet.barcarole.data.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LibraryUiState(
    val library: Library = Library(),
    val settings: Settings = Settings(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
) {
    fun songsSorted(tab: LibraryTab): List<Song> =
        LibraryIndex.sortSongs(library.songs, settings.sortFor(tab))
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val settingsStore: SettingsStore,
    private val playlistRepository: PlaylistRepository,
    private val songDeleter: SongDeleter,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistSummary>> = combine(
        playlistRepository.data,
        repository.library,
    ) { _, _ -> playlistRepository.summaries() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favouriteIds: StateFlow<Set<Long>> = playlistRepository.favouriteIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun songsOf(id: PlaylistId): List<Song> = playlistRepository.songsOf(id)

    fun playCountsOf(id: PlaylistId): Map<Long, Int> = playlistRepository.playCounts(id)

    fun toggleFavourite(songId: Long) = playlistRepository.toggleFavourite(songId)

    /**
     * Deleting reaches the media provider over IPC, so it stays off the main thread; the outcome
     * comes back on the main dispatcher, where the caller may need to launch a consent dialog.
     */
    fun deleteSongs(songs: List<Song>, onOutcome: (DeleteOutcome) -> Unit) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { songDeleter.delete(songs) }
            onOutcome(outcome)
        }
    }

    /**
     * Called once the files are actually gone, whether we deleted them or the system did. The
     * library is rescanned once for the whole batch, not once per file.
     */
    fun onSongsDeleted(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            songIds.forEach { playlistRepository.forget(it) }
            repository.refresh()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.library,
        settingsStore.settings,
        repository.isScanning,
        repository.hasScanned,
    ) { library, settings, isScanning, hasScanned ->
        LibraryUiState(library, settings, isScanning, hasScanned)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<SearchResults> = combine(
        _searchQuery.debounce(200),
        repository.library,
    ) { query, library ->
        LibraryIndex.search(library, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    /** Called once the audio permission is granted; safe to call again on resume. */
    fun onPermissionGranted() = repository.start()

    fun rescan() {
        viewModelScope.launch { repository.refresh() }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSort(tab: LibraryTab, key: SongSort) {
        viewModelScope.launch {
            val current = uiState.value.settings.sortFor(tab)
            val next = if (current.key == key) {
                current.copy(ascending = !current.ascending)
            } else {
                SortOrder(key = key, ascending = true)
            }
            settingsStore.setSortOrder(tab, next)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setShowAlbums(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setShowAlbums(enabled) }
    }

    fun setTextSize(size: TextSize) {
        viewModelScope.launch { settingsStore.setTextSize(size) }
    }

    fun setMinTrackSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setMinTrackSeconds(seconds) }
    }
}
