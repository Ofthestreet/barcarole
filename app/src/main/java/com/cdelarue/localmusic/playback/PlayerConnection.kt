package com.cdelarue.localmusic.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cdelarue.localmusic.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
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
) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    /** Safe to call repeatedly; the second call is a no-op. */
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
            },
            ContextCompat.getMainExecutor(context),
        )
    }

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

    private fun startPositionTicker() {
        scope.launch(Dispatchers.Main) {
            while (true) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.coerceAtLeast(0),
                    )
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun publish() {
        val player = controller ?: return
        val queue = (0 until player.mediaItemCount).map { index ->
            val metadata = player.getMediaItemAt(index).mediaMetadata
            QueueEntry(
                mediaId = player.getMediaItemAt(index).mediaId,
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                artworkUri = metadata.artworkUri,
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
    }

    private companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
        const val POSITION_TICK_MS = 500L
    }
}
