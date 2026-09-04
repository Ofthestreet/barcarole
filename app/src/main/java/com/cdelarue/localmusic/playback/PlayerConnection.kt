package com.cdelarue.localmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cdelarue.localmusic.data.LibraryRepository
import com.cdelarue.localmusic.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
)

data class PlayerState(
    val isConnected: Boolean = false,
    val current: QueueEntry? = null,
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueIndex: Int = 0,
    val queue: List<QueueEntry> = emptyList(),
)

/**
 * The UI's only handle on playback: a MediaController bound to [PlaybackService], mirrored into a
 * StateFlow. Nothing outside the service owns a player, so the screen, the notification and the
 * lock screen can never disagree.
 *
 * Every call here touches the controller on the main thread, which is what Media3 requires.
 */
@Singleton
class PlayerConnection @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val queueStore: QueueStore,
    private val libraryRepository: LibraryRepository,
) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Latest queue worth persisting; written out at a low rate rather than on every event. */
    private val pendingSave = MutableStateFlow<QueueSnapshot?>(null)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    /** Safe to call repeatedly; the second call is a no-op. */
    // PlaybackService carries Media3's opt-in marker, so naming it here needs the opt-in too.
    @androidx.annotation.OptIn(UnstableApi::class)
    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = runCatching { future.get() }.getOrNull()
                controller?.addListener(listener)
                publish()
                startPositionTicker()
                startQueuePersistence()
                restoreQueueIfIdle()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    // ---- playback commands -------------------------------------------------

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        player.setMediaItems(songs.toMediaItems(), startIndex.coerceIn(songs.indices), 0L)
        player.shuffleModeEnabled = false
        player.prepare()
        player.play()
    }

    fun shuffleAll(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        player.setMediaItems(songs.toMediaItems(), 0, 0L)
        player.shuffleModeEnabled = true
        player.prepare()
        player.play()
    }

    /** Drops the tracks in right after whatever is playing. */
    fun playNext(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        if (player.mediaItemCount == 0) {
            playAll(songs)
            return
        }
        player.addMediaItems(player.currentMediaItemIndex + 1, songs.toMediaItems())
    }

    fun addToQueue(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        if (player.mediaItemCount == 0) {
            playAll(songs)
            return
        }
        player.addMediaItems(songs.toMediaItems())
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    /** Restart the track first, the way every other player behaves, and only then skip back. */
    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > RESTART_THRESHOLD_MS || !player.hasPreviousMediaItem()) {
            player.seekTo(0)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekToQueueIndex(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount && index != player.currentMediaItemIndex) {
            player.seekToDefaultPosition(index)
        }
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // ---- queue editing -----------------------------------------------------

    fun moveQueueItem(from: Int, to: Int) {
        val player = controller ?: return
        val count = player.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) {
            player.moveMediaItem(from, to)
        }
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
        }
    }

    /** Drops every copy of a song from the queue, used once its file is gone. */
    fun removeSong(songId: Long) {
        val player = controller ?: return
        for (index in player.mediaItemCount - 1 downTo 0) {
            if (BrowseIds.songIdOf(player.getMediaItemAt(index).mediaId) == songId) {
                player.removeMediaItem(index)
            }
        }
    }

    /** Keeps the current track and drops everything else. */
    fun clearQueue() {
        val player = controller ?: return
        val keep = player.currentMediaItemIndex
        val count = player.mediaItemCount
        if (count > keep + 1) player.removeMediaItems(keep + 1, count)
        if (keep > 0) player.removeMediaItems(0, keep)
    }

    fun stopAndClear() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
        scope.launch { queueStore.clear() }
    }

    // ---- state plumbing ----------------------------------------------------

    private fun startPositionTicker() {
        scope.launch(Dispatchers.Main) {
            while (true) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.coerceAtLeast(0),
                    )
                    pendingSave.value = snapshotOf(player)
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun startQueuePersistence() {
        scope.launch {
            pendingSave
                .filterNotNull()
                .sample(SAVE_INTERVAL_MS)
                .collect { queueStore.save(it.songIds, it.index, it.positionMs) }
        }
    }

    private fun restoreQueueIfIdle() {
        scope.launch(Dispatchers.Main) {
            val player = controller ?: return@launch
            if (player.mediaItemCount > 0) return@launch
            val snapshot = queueStore.snapshot.first() ?: return@launch

            // The library has to be scanned before ids mean anything.
            libraryRepository.hasScanned.first { it }
            val byId = libraryRepository.library.value.songs.associateBy { it.id }
            val restored = QueueRestore.resolve(snapshot, byId) ?: return@launch

            // Restored paused: coming back to the app should not start blaring music.
            player.setMediaItems(
                restored.songs.toMediaItems(),
                restored.index,
                restored.positionMs,
            )
            player.prepare()
            publish()
        }
    }

    private fun snapshotOf(player: Player): QueueSnapshot? {
        val count = player.mediaItemCount
        if (count == 0) return null
        // Ids from the Auto browse tree carry their parent, so they are not plain numbers.
        val ids = (0 until count).mapNotNull { BrowseIds.songIdOf(player.getMediaItemAt(it).mediaId) }
        if (ids.isEmpty()) return null
        return QueueSnapshot(
            songIds = ids,
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0),
        )
    }

    private fun publish() {
        val player = controller ?: return
        val queue = (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            QueueEntry(
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
        val index = player.currentMediaItemIndex
        _state.value = PlayerState(
            isConnected = true,
            current = queue.getOrNull(index),
            album = player.mediaMetadata.albumTitle?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queueIndex = index,
            queue = queue,
        )
        pendingSave.value = snapshotOf(player)
    }

    private companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
        const val POSITION_TICK_MS = 500L
        const val SAVE_INTERVAL_MS = 3_000L
    }
}
