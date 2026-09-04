package com.cdelarue.localmusic

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cdelarue.localmusic.data.LibraryIndex
import com.cdelarue.localmusic.data.LibraryTab
import com.cdelarue.localmusic.data.DeleteOutcome
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.data.stats.PlaylistId
import com.cdelarue.localmusic.playback.BrowseIds
import com.cdelarue.localmusic.ui.detail.TrackListScreen
import com.cdelarue.localmusic.ui.library.LibraryScreen
import com.cdelarue.localmusic.ui.library.FoldersScreen
import com.cdelarue.localmusic.ui.library.LibraryViewModel
import com.cdelarue.localmusic.ui.permission.PermissionScreen
import com.cdelarue.localmusic.ui.player.MiniPlayer
import com.cdelarue.localmusic.ui.player.NowPlayingScreen
import com.cdelarue.localmusic.ui.player.PlayerViewModel
import com.cdelarue.localmusic.ui.components.DeleteSongDialog
import com.cdelarue.localmusic.ui.components.SongActionsSheet
import com.cdelarue.localmusic.ui.search.SearchScreen
import com.cdelarue.localmusic.ui.settings.SettingsScreen
import com.cdelarue.localmusic.ui.theme.LocalMusicTheme
import com.cdelarue.localmusic.ui.theme.supportsDynamicColor
import com.cdelarue.localmusic.util.AudioPermission
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LocalMusicApp() }
    }
}

private object Routes {
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val FOLDERS = "folders"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistId}"
    const val FOLDER = "folder?path={folderPath}"

    fun album(albumId: Long) = "album/$albumId"
    fun artist(artistId: Long) = "artist/$artistId"
    fun folder(path: String) = "folder?path=${Uri.encode(path)}"
}

@Composable
private fun LocalMusicApp(
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favouriteIds by viewModel.favouriteIds.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? ComponentActivity
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, AudioPermission.name)
        }
    }

    // Playback works without this one; only the notification would be missing.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        viewModel.onPermissionGranted()
        playerViewModel.connect()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LocalMusicTheme(themeMode = state.settings.themeMode, dynamicColor = state.settings.dynamicColor) {
        if (!hasPermission) {
            PermissionScreen(
                permanentlyDenied = permanentlyDenied,
                onRequest = { audioLauncher.launch(AudioPermission.name) },
            )
            return@LocalMusicTheme
        }

        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
        var selectedPlaylist by rememberSaveable { mutableStateOf(PlaylistId.MOST_PLAYED_MONTH) }
        // Recomputed when the selection or the underlying data changes, not on every frame.
        val playlistSongs = remember(selectedPlaylist, playlists) { viewModel.songsOf(selectedPlaylist) }
        val playlistCounts = remember(selectedPlaylist, playlists) { viewModel.playCountsOf(selectedPlaylist) }
        val playingSongId = playerState.current?.mediaId?.let { BrowseIds.songIdOf(it) }
        val playingSong = playingSongId?.let { id -> state.library.songs.firstOrNull { it.id == id } }
        var pendingDeletion by remember { mutableStateOf<Song?>(null) }
        var awaitingConsentFor by remember { mutableStateOf<Long?>(null) }

        // Android 11+ asks for its own confirmation and reports back here.
        val deleteConsentLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            val songId = awaitingConsentFor
            awaitingConsentFor = null
            if (result.resultCode == android.app.Activity.RESULT_OK && songId != null) {
                playerViewModel.removeSongFromQueue(songId)
                viewModel.onSongDeleted(songId)
            }
        }

        fun startDeletion(song: Song) {
            when (val outcome = viewModel.deleteSong(song)) {
                is DeleteOutcome.Deleted -> {
                    playerViewModel.removeSongFromQueue(song.id)
                    viewModel.onSongDeleted(song.id)
                }

                is DeleteOutcome.NeedsConsent -> {
                    awaitingConsentFor = song.id
                    deleteConsentLauncher.launch(
                        IntentSenderRequest.Builder(outcome.intentSender).build(),
                    )
                }

                is DeleteOutcome.Failed ->
                    Toast.makeText(context, outcome.reason, Toast.LENGTH_LONG).show()
            }
        }

        pendingDeletion?.let { song ->
            DeleteSongDialog(
                song = song,
                onDismiss = { pendingDeletion = null },
                onConfirm = {
                    pendingDeletion = null
                    startDeletion(song)
                },
            )
        }
        val playingIsFavourite = playingSongId != null && playingSongId in favouriteIds
        val onSongClick: (List<Song>, Song) -> Unit = playerViewModel::play
        var actionSheetSong by remember { mutableStateOf<Song?>(null) }
        val onSongLongClick: (Song) -> Unit = { actionSheetSong = it }

        actionSheetSong?.let { song ->
            SongActionsSheet(
                song = song,
                onDismiss = { actionSheetSong = null },
                onPlayNext = {
                    playerViewModel.playNext(listOf(song))
                    actionSheetSong = null
                },
                onAddToQueue = {
                    playerViewModel.addToQueue(listOf(song))
                    actionSheetSong = null
                },
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.LIBRARY,
                modifier = Modifier.weight(1f),
            ) {
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        state = state,
                        playerState = playerState,
                        playlists = playlists,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onSort = viewModel::setSort,
                        onRescan = viewModel::rescan,
                        onSongClick = onSongClick,
                        onSongLongClick = onSongLongClick,
                        onAlbumClick = { navController.navigate(Routes.album(it.id)) },
                        onArtistClick = { navController.navigate(Routes.artist(it.id)) },
                        onSearchClick = { navController.navigate(Routes.SEARCH) },
                        onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                        onQueuePlayIndex = playerViewModel::seekToQueueIndex,
                        onQueueMove = playerViewModel::moveQueueItem,
                        onQueueRemove = playerViewModel::removeFromQueue,
                        onQueueClear = playerViewModel::clearQueue,
                        onToggleShuffle = playerViewModel::toggleShuffle,
                        selectedPlaylist = selectedPlaylist,
                        playlistSongs = playlistSongs,
                        playlistCounts = playlistCounts,
                        onSelectPlaylist = { selectedPlaylist = it },
                        onPlayAll = playerViewModel::playAll,
                        onShuffleAll = playerViewModel::shuffleAll,
                    )
                }

                composable(Routes.PLAYER) {
                    NowPlayingScreen(
                        state = playerState,
                        isFavourite = playingIsFavourite,
                        onBack = { navController.popBackStackSafely() },
                        onToggleFavourite = { playingSongId?.let(viewModel::toggleFavourite) },
                        onOpenQueue = {
                            selectedTab = LibraryTab.QUEUE
                            navController.popBackStackSafely()
                        },
                        onPlayPause = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::next,
                        onPrevious = playerViewModel::previous,
                        onSeek = playerViewModel::seekTo,
                        onSeekToIndex = playerViewModel::seekToQueueIndex,
                        onToggleShuffle = playerViewModel::toggleShuffle,
                        onCycleRepeat = playerViewModel::cycleRepeatMode,
                    )
                }

                composable(Routes.ALBUM) { entry ->
                    val albumId = entry.arguments?.getString("albumId")?.toLongOrNull() ?: -1L
                    val album = state.library.albums.firstOrNull { it.id == albumId }
                    TrackListScreen(
                        title = album?.title ?: "Album",
                        subtitle = album?.artist.orEmpty(),
                        songs = LibraryIndex.tracksOfAlbum(state.library.songs, albumId),
                        onBack = { navController.popBackStackSafely() },
                        onSongClick = onSongClick,
                        onSongLongClick = onSongLongClick,
                        onPlayAll = playerViewModel::playAll,
                        onShuffleAll = playerViewModel::shuffleAll,
                    )
                }

                composable(Routes.ARTIST) { entry ->
                    val artistId = entry.arguments?.getString("artistId")?.toLongOrNull() ?: -1L
                    val artist = state.library.artists.firstOrNull { it.id == artistId }
                    val songs = LibraryIndex.tracksOfArtist(state.library.songs, artistId)
                    TrackListScreen(
                        title = artist?.name ?: "Artist",
                        subtitle = artist?.let { "${it.albumCount} albums" }.orEmpty(),
                        songs = songs,
                        onBack = { navController.popBackStackSafely() },
                        onSongClick = onSongClick,
                        onSongLongClick = onSongLongClick,
                        onPlayAll = playerViewModel::playAll,
                        onShuffleAll = playerViewModel::shuffleAll,
                    )
                }

                composable(
                    route = Routes.FOLDER,
                    arguments = listOf(
                        navArgument("folderPath") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { entry ->
                    val path = entry.arguments?.getString("folderPath").orEmpty()
                    val songs = LibraryIndex.tracksOfFolder(state.library.songs, path)
                    TrackListScreen(
                        title = path.substringAfterLast('/', missingDelimiterValue = path),
                        subtitle = path,
                        songs = songs,
                        onBack = { navController.popBackStackSafely() },
                        onSongClick = onSongClick,
                        onSongLongClick = onSongLongClick,
                        onPlayAll = playerViewModel::playAll,
                        onShuffleAll = playerViewModel::shuffleAll,
                    )
                }
            }

            // The mini player sits under every screen except the full player it expands into.
            if (currentRoute != Routes.PLAYER && playerState.current != null) {
                MiniPlayer(
                    state = playerState,
                    isFavourite = playingIsFavourite,
                    onExpand = { navController.navigate(Routes.PLAYER) },
                    onToggleFavourite = { playingSongId?.let(viewModel::toggleFavourite) },
                    onDelete = { playingSong?.let { pendingDeletion = it } },
                    onPlayPause = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

private fun NavHostController.popBackStackSafely() {
    if (!popBackStack()) navigate(Routes.LIBRARY)
}
