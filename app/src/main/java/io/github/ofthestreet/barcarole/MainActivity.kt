package io.github.ofthestreet.barcarole

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.ofthestreet.barcarole.data.LibraryIndex
import io.github.ofthestreet.barcarole.data.LibraryTab
import io.github.ofthestreet.barcarole.data.DeleteOutcome
import io.github.ofthestreet.barcarole.data.Duplicates
import io.github.ofthestreet.barcarole.data.Song
import io.github.ofthestreet.barcarole.data.stats.PlaylistId
import io.github.ofthestreet.barcarole.playback.BrowseIds
import io.github.ofthestreet.barcarole.ui.detail.TrackListScreen
import io.github.ofthestreet.barcarole.ui.library.LibraryScreen
import io.github.ofthestreet.barcarole.ui.library.FoldersScreen
import io.github.ofthestreet.barcarole.ui.library.LibraryViewModel
import io.github.ofthestreet.barcarole.ui.permission.PermissionScreen
import io.github.ofthestreet.barcarole.ui.player.MiniPlayer
import io.github.ofthestreet.barcarole.ui.player.NowPlayingScreen
import io.github.ofthestreet.barcarole.ui.player.PlayerViewModel
import io.github.ofthestreet.barcarole.ui.components.DeleteSongDialog
import io.github.ofthestreet.barcarole.ui.components.SongActionsSheet
import io.github.ofthestreet.barcarole.ui.search.SearchScreen
import io.github.ofthestreet.barcarole.ui.settings.DuplicatesScreen
import io.github.ofthestreet.barcarole.ui.settings.SettingsScreen
import io.github.ofthestreet.barcarole.ui.theme.LocalMusicTheme
import io.github.ofthestreet.barcarole.ui.theme.supportsDynamicColor
import io.github.ofthestreet.barcarole.util.AudioPermission
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
    const val DUPLICATES = "duplicates"
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

    LocalMusicTheme(
        themeMode = state.settings.themeMode,
        dynamicColor = state.settings.dynamicColor,
        textSize = state.settings.textSize,
    ) {
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
        val duplicates = remember(state.library.songs) { Duplicates.find(state.library.songs) }
        val playingSongId = playerState.current?.mediaId?.let { BrowseIds.songIdOf(it) }
        val playingSong = playingSongId?.let { id -> state.library.songs.firstOrNull { it.id == id } }
        var pendingDeletion by remember { mutableStateOf<Song?>(null) }
        // Cleaned up once the system reports the batch deleted; empty on the Android 10 path,
        // where consent covers one file at a time and the rest is retried instead.
        var consentBatch by remember { mutableStateOf(emptyList<Long>()) }
        var consentRetry by remember { mutableStateOf(emptyList<Song>()) }
        var resumeDeletion by remember { mutableStateOf(emptyList<Song>()) }

        fun finishDeletion(songIds: List<Long>) {
            songIds.forEach { playerViewModel.removeSongFromQueue(it) }
            viewModel.onSongsDeleted(songIds)
        }

        // Android 11+ asks for its own confirmation and reports back here.
        val deleteConsentLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            val batch = consentBatch
            val retry = consentRetry
            consentBatch = emptyList()
            consentRetry = emptyList()
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                if (batch.isNotEmpty()) finishDeletion(batch)
                if (retry.isNotEmpty()) resumeDeletion = retry
            }
        }

        fun startDeletion(songs: List<Song>) {
            if (songs.isEmpty()) return
            viewModel.deleteSongs(songs) { outcome ->
                when (outcome) {
                    is DeleteOutcome.Deleted -> finishDeletion(songs.map { it.id })

                    is DeleteOutcome.NeedsConsent -> {
                        // Whatever the batch got through before it stopped is already gone.
                        val stillPending = outcome.remaining.map { it.id }.toSet()
                        val done = songs.map { it.id }.filterNot { it in stillPending }
                        consentBatch = if (outcome.remaining.isEmpty()) songs.map { it.id } else emptyList()
                        consentRetry = outcome.remaining
                        if (outcome.remaining.isNotEmpty() && done.isNotEmpty()) finishDeletion(done)
                        deleteConsentLauncher.launch(
                            IntentSenderRequest.Builder(outcome.intentSender).build(),
                        )
                    }

                    is DeleteOutcome.Failed ->
                        Toast.makeText(context, outcome.reason, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Before Android 10 the app deletes the file itself, and that needs the write permission.
        var pendingWriteGrantFor by remember { mutableStateOf(emptyList<Song>()) }
        val writeAccessLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val songs = pendingWriteGrantFor
            pendingWriteGrantFor = emptyList()
            when {
                songs.isEmpty() -> Unit
                granted -> startDeletion(songs)
                else -> Toast.makeText(
                    context,
                    "Deleting a file needs access to storage.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

        fun confirmDeletion(songs: List<Song>) {
            val needsWriteAccess = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            if (needsWriteAccess) {
                pendingWriteGrantFor = songs
                writeAccessLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                startDeletion(songs)
            }
        }

        // The Android 10 path comes back one file at a time; picking the rest up here keeps the
        // launcher and the delete call from having to reference each other.
        LaunchedEffect(resumeDeletion) {
            val remaining = resumeDeletion
            if (remaining.isNotEmpty()) {
                resumeDeletion = emptyList()
                startDeletion(remaining)
            }
        }

        pendingDeletion?.let { song ->
            DeleteSongDialog(
                song = song,
                onDismiss = { pendingDeletion = null },
                onConfirm = {
                    pendingDeletion = null
                    confirmDeletion(listOf(song))
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

                composable(Routes.SEARCH) {
                    SearchScreen(
                        query = searchQuery,
                        results = searchResults,
                        showAlbums = state.settings.showAlbums,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onBack = { navController.popBackStackSafely() },
                        onSongClick = onSongClick,
                        onSongLongClick = onSongLongClick,
                        onAlbumClick = { navController.navigate(Routes.album(it.id)) },
                        onArtistClick = { navController.navigate(Routes.artist(it.id)) },
                        onFolderClick = { navController.navigate(Routes.folder(it.path)) },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = state.settings,
                        supportsDynamicColor = supportsDynamicColor,
                        songCount = state.library.songs.size,
                        folderCount = state.library.folders.size,
                        onBack = { navController.popBackStackSafely() },
                        onThemeMode = viewModel::setThemeMode,
                        onDynamicColor = viewModel::setDynamicColor,
                        onMinTrackSeconds = viewModel::setMinTrackSeconds,
                        onShowAlbums = viewModel::setShowAlbums,
                        onTextSize = viewModel::setTextSize,
                        onRescan = viewModel::rescan,
                        onBrowseFolders = { navController.navigate(Routes.FOLDERS) },
                        duplicateCount = Duplicates.removable(duplicates).size,
                        onBrowseDuplicates = { navController.navigate(Routes.DUPLICATES) },
                    )
                }

                composable(Routes.FOLDERS) {
                    FoldersScreen(
                        folders = state.library.folders,
                        onBack = { navController.popBackStackSafely() },
                        onFolderClick = { navController.navigate(Routes.folder(it.path)) },
                    )
                }

                composable(Routes.DUPLICATES) {
                    DuplicatesScreen(
                        groups = duplicates,
                        onBack = { navController.popBackStackSafely() },
                        onDelete = { songs -> confirmDeletion(songs) },
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
