package com.cdelarue.localmusic

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cdelarue.localmusic.data.LibraryIndex
import com.cdelarue.localmusic.data.Song
import com.cdelarue.localmusic.ui.detail.TrackListScreen
import com.cdelarue.localmusic.ui.library.LibraryScreen
import com.cdelarue.localmusic.ui.library.LibraryViewModel
import com.cdelarue.localmusic.ui.permission.PermissionScreen
import com.cdelarue.localmusic.ui.search.SearchScreen
import com.cdelarue.localmusic.ui.settings.SettingsScreen
import com.cdelarue.localmusic.ui.theme.LocalMusicTheme
import com.cdelarue.localmusic.ui.theme.supportsDynamicColor
import com.cdelarue.localmusic.util.AudioPermission
import dagger.hilt.android.AndroidEntryPoint
import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistId}"
    const val FOLDER = "folder?path={folderPath}"

    fun album(albumId: Long) = "album/$albumId"
    fun artist(artistId: Long) = "artist/$artistId"
    fun folder(path: String) = "folder?path=${Uri.encode(path)}"
}

@Composable
private fun LocalMusicApp(viewModel: LibraryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? ComponentActivity
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, AudioPermission.name)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.onPermissionGranted()
    }

    LocalMusicTheme(themeMode = state.settings.themeMode, dynamicColor = state.settings.dynamicColor) {
        if (!hasPermission) {
            PermissionScreen(
                permanentlyDenied = permanentlyDenied,
                onRequest = { launcher.launch(AudioPermission.name) },
            )
            return@LocalMusicTheme
        }

        val navController = rememberNavController()
        // Playback lands in P2; until then a tap confirms the library wiring works.
        val onSongClick: (Song) -> Unit = { song ->
            Toast.makeText(context, "${song.title} — playback arrives next", Toast.LENGTH_SHORT).show()
        }
        val onPlayList: (List<Song>) -> Unit = { songs ->
            Toast.makeText(context, "${songs.size} tracks queued — playback arrives next", Toast.LENGTH_SHORT).show()
        }

        NavHost(navController = navController, startDestination = Routes.LIBRARY) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    state = state,
                    onSort = viewModel::setSort,
                    onRescan = viewModel::rescan,
                    onSongClick = onSongClick,
                    onAlbumClick = { navController.navigate(Routes.album(it.id)) },
                    onArtistClick = { navController.navigate(Routes.artist(it.id)) },
                    onFolderClick = { navController.navigate(Routes.folder(it.path)) },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    query = searchQuery,
                    results = searchResults,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onBack = { navController.popBackStackSafely() },
                    onSongClick = onSongClick,
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
                    onBack = { navController.popBackStackSafely() },
                    onThemeMode = viewModel::setThemeMode,
                    onDynamicColor = viewModel::setDynamicColor,
                    onMinTrackSeconds = viewModel::setMinTrackSeconds,
                    onRescan = viewModel::rescan,
                )
            }

            composable(Routes.ALBUM) { entry ->
                val albumId = entry.arguments?.getString("albumId")?.toLongOrNull() ?: -1L
                val album = state.library.albums.firstOrNull { it.id == albumId }
                TrackListScreen(
                    title = album?.title ?: "Album",
                    subtitle = album?.artist.orEmpty(),
                    songs = LibraryIndex.tracksOfAlbum(state.library.songs, albumId),
                    artworkAlbumId = albumId,
                    onBack = { navController.popBackStackSafely() },
                    onSongClick = onSongClick,
                    onPlayAll = onPlayList,
                    onShuffleAll = { onPlayList(it.shuffled()) },
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
                    artworkAlbumId = songs.firstOrNull()?.albumId,
                    onBack = { navController.popBackStackSafely() },
                    onSongClick = onSongClick,
                    onPlayAll = onPlayList,
                    onShuffleAll = { onPlayList(it.shuffled()) },
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
                    artworkAlbumId = songs.firstOrNull()?.albumId,
                    onBack = { navController.popBackStackSafely() },
                    onSongClick = onSongClick,
                    onPlayAll = onPlayList,
                    onShuffleAll = { onPlayList(it.shuffled()) },
                )
            }
        }
    }
}

private fun NavHostController.popBackStackSafely() {
    if (!popBackStack()) navigate(Routes.LIBRARY)
}
