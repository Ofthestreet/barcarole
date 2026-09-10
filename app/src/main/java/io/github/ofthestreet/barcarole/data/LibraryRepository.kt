package io.github.ofthestreet.barcarole.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the scanned library. The UI and (from P2) the playback service read
 * the same instance, so they can never disagree about what is in the library.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val source: MediaStoreLibrarySource,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
) {

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    private val scanLock = Mutex()
    private var observingChanges = false

    /** Called once the audio permission has been granted, and again on manual rescan. */
    fun start() {
        scope.launch { refresh() }
        if (!observingChanges) {
            observingChanges = true
            scope.launch {
                source.changes().collect { refresh() }
            }
            scope.launch {
                settingsStore.settings
                    .map { it.minTrackSeconds }
                    .distinctUntilChanged()
                    .collect { if (_hasScanned.value) refresh() }
            }
        }
    }

    suspend fun refresh() {
        scanLock.withLock {
            _isScanning.value = true
            try {
                val minSeconds = settingsStore.settings.first().minTrackSeconds
                _library.value = LibraryIndex.build(source.querySongs(minSeconds))
                _hasScanned.value = true
            } finally {
                _isScanning.value = false
            }
        }
    }
}
