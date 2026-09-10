package io.github.ofthestreet.barcarole.ui.player

import androidx.lifecycle.ViewModel
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.playback.PlayerConnection
import io.github.ofthestreet.barcarole.playback.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: PlayerConnection,
) : ViewModel() {

    val state: StateFlow<PlayerState> = connection.state

    fun connect() = connection.connect()

    fun play(songs: List<Song>, song: Song) {
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        connection.playAll(songs, index)
    }

    fun playAll(songs: List<Song>) = connection.playAll(songs, 0)

    fun shuffleAll(songs: List<Song>) = connection.shuffleAll(songs)

    fun playNext(songs: List<Song>) = connection.playNext(songs)

    fun addToQueue(songs: List<Song>) = connection.addToQueue(songs)

    fun moveQueueItem(from: Int, to: Int) = connection.moveQueueItem(from, to)

    fun removeFromQueue(index: Int) = connection.removeFromQueue(index)

    fun clearQueue() = connection.clearQueue()

    fun removeSongFromQueue(songId: Long) = connection.removeSong(songId)

    fun togglePlayPause() = connection.togglePlayPause()

    fun next() = connection.next()

    fun previous() = connection.previous()

    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun seekToQueueIndex(index: Int) = connection.seekToQueueIndex(index)

    fun toggleShuffle() = connection.toggleShuffle()

    fun cycleRepeatMode() = connection.cycleRepeatMode()
}
