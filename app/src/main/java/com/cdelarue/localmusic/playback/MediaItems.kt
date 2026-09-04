package com.cdelarue.localmusic.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.data.albumArtUri
import com.cdelarue.localmusic.data.contentUri

/**
 * One place that turns a library Song into a Media3 MediaItem, so the notification, the lock
 * screen and (from P4) Android Auto all describe a track the same way.
 */
fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(contentUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setAlbumArtist(artist)
            .setTrackNumber(trackNumber)
            .setArtworkUri(albumArtUri(albumId))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build(),
    )
    .build()

fun List<Song>.toMediaItems(): List<MediaItem> = map { it.toMediaItem() }
