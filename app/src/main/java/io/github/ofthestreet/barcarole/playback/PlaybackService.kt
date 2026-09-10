package io.github.ofthestreet.barcarole.playback

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import io.github.ofthestreet.barcarole.data.LibraryRepository
import io.github.ofthestreet.barcarole.data.SettingsStore
import io.github.ofthestreet.barcarole.data.stats.PlaylistId
import io.github.ofthestreet.barcarole.data.stats.PlaylistRepository
import io.github.ofthestreet.barcarole.data.albumArtUri
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.common.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the one and only ExoPlayer instance. The UI never builds a player of its own; it connects a
 * MediaController to this session, which is what keeps the app, the notification and the lock
 * screen showing the same playback state.
 *
 * A MediaLibraryService rather than a plain MediaSessionService because Android Auto browses the
 * library through this same session, using the tree in [BrowseTree].
 */
@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var libraryRepository: LibraryRepository

    @Inject lateinit var playlistRepository: PlaylistRepository

    @Inject lateinit var settingsStore: SettingsStore

    @Inject lateinit var appScope: CoroutineScope

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Pause instead of blaring out of the phone speaker when headphones are pulled.
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        exoPlayer.addListener(trackChanges)
        player = exoPlayer
        session = MediaLibrarySession.Builder(this, exoPlayer, LibraryCallback()).build()
        startListenTracking()
        serviceScope.launch {
            settingsStore.settings.collect { showAlbums = it.showAlbums }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away while paused should not leave a dead notification behind.
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    /**
     * A listen counts once the track has actually been listened to — half of it, or thirty
     * seconds, whichever comes first — so skipping through the library does not inflate the
     * playlists. Player events do not arrive while a track simply plays on, so the position is
     * sampled instead; the flag resets on every transition, which is what makes repeat-one count
     * each time round.
     */
    private var countedCurrent = false

    /** Mirrored from settings so the browse callbacks, which are synchronous, can read it. */
    private var showAlbums = false

    private fun browseContext() = BrowseContext(
        library = libraryRepository.library.value,
        favourites = playlistRepository.songsOf(PlaylistId.FAVOURITES),
        showAlbums = showAlbums,
    )

    private val trackChanges = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            countedCurrent = false
        }
    }

    private fun startListenTracking() {
        serviceScope.launch {
            while (true) {
                delay(SAMPLE_MS)
                val current = player ?: continue
                if (countedCurrent || !current.isPlaying) continue
                val mediaId = current.currentMediaItem?.mediaId ?: continue
                val duration = current.duration
                val threshold = if (duration > 0) minOf(duration / 2, COUNT_AFTER_MS) else COUNT_AFTER_MS
                if (current.currentPosition < threshold) continue
                countedCurrent = true
                val songId = BrowseIds.songIdOf(mediaId) ?: continue
                appScope.launch { playlistRepository.recordPlay(songId) }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        player?.removeListener(trackChanges)
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(BrowseIds.ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Barcarole")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, rootParams()))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = BrowseTree.childrenOf(browseContext(), parentId)
            val items = children.map { it.toMediaItem() }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val library = libraryRepository.library.value
            val songId = BrowseIds.songIdOf(mediaId)
            val song = library.songs.firstOrNull { it.id == songId }
            return if (song != null) {
                Futures.immediateFuture(LibraryResult.ofItem(song.toMediaItem(), null))
            } else {
                val node = BrowseTree.rootChildren(browseContext()).firstOrNull { it.id == mediaId }
                if (node != null) {
                    Futures.immediateFuture(LibraryResult.ofItem(node.toMediaItem(), null))
                } else {
                    Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            val count = BrowseTree.search(browseContext(), query).size
            session.notifySearchResultChanged(browser, query, count, params)
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = BrowseTree.search(browseContext(), query).map { it.toMediaItem() }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        /**
         * Items arriving from a browser carry an id but no playable URI. Picking a track queues the
         * whole parent it came from, positioned on that track.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val context = browseContext()
            val library = context.library
            val single = mediaItems.singleOrNull()
            if (single != null && single.localConfiguration == null) {
                // "Play Discovery" from the Assistant arrives as an item carrying only a query.
                val query = single.requestMetadata.searchQuery
                val selection = if (!query.isNullOrBlank()) {
                    BrowseTree.queueForSearch(context, query)
                } else {
                    BrowseTree.queueFor(context, single.mediaId)
                }
                if (selection != null) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            selection.songs.toMediaItems(),
                            selection.startIndex,
                            C.TIME_UNSET,
                        ),
                    )
                }
            }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolve(mediaItems), startIndex, startPositionMs),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(resolve(mediaItems).toMutableList())

        /** Fills in the URI for items that only carry a media id. */
        private fun resolve(mediaItems: List<MediaItem>): List<MediaItem> {
            val songsById = libraryRepository.library.value.songs.associateBy { it.id }
            return mediaItems.mapNotNull { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    BrowseIds.songIdOf(item.mediaId)?.let { songsById[it] }?.toMediaItem()
                }
            }
        }
    }

    private fun rootParams(): LibraryParams = LibraryParams.Builder()
        .setExtras(
            Bundle().apply {
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
            },
        )
        .build()

    private fun BrowseNode.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putInt(
                CONTENT_STYLE_BROWSABLE_HINT,
                if (childStyle == BrowseStyle.GRID) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST,
            )
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle.ifEmpty { null })
            .setArtist(subtitle.ifEmpty { null })
            .setIsBrowsable(!playable)
            .setIsPlayable(playable)
            .setArtworkUri(albumId?.let { albumArtUri(it) })
            .setExtras(extras)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }

    private companion object {
        // Android Auto reads these from the browse extras to decide between a list and a grid.
        const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_LIST = 1
        const val CONTENT_STYLE_GRID = 2
        const val COUNT_AFTER_MS = 30_000L
        const val SAMPLE_MS = 5_000L
    }
}
