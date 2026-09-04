package com.cdelarue.localmusic.ui.player

import androidx.lifecycle.ViewModel
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.playback.PlayerConnection
import com.cdelarue.localmusic.playback.PlayerState
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

    fun togglePlayPause() = connection.togglePlayPause()

    fun next() = connection.next()

    fun previous() = connection.previous()

    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)

    fun seekToQueueIndex(index: Int) = connection.seekToQueueIndex(index)

    fun toggleShuffle() = connection.toggleShuffle()

    fun cycleRepeatMode() = connection.cycleRepeatMode()
}
