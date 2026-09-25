package io.github.ofthestreet.barcarole.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ofthestreet.barcarole.data.LibraryRepository
import io.github.ofthestreet.barcarole.data.SettingsStore
import io.github.ofthestreet.barcarole.data.stats.PlaylistRepository
import io.github.ofthestreet.barcarole.playback.BrowseContextSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    /** Application-lifetime scope for library scanning; outlives any single screen. */
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Assembled here rather than by constructor injection so the class itself stays free of the
     * repositories, and so the favourites come straight from the database flow instead of the
     * snapshot the playlists screen shares only while it is on screen.
     */
    @Provides
    @Singleton
    fun provideBrowseContextSource(
        libraryRepository: LibraryRepository,
        playlistRepository: PlaylistRepository,
        settingsStore: SettingsStore,
        scope: CoroutineScope,
    ): BrowseContextSource = BrowseContextSource(
        library = libraryRepository.library,
        favouriteIds = playlistRepository.favouriteIds,
        showAlbums = settingsStore.settings.map { it.showAlbums }.distinctUntilChanged(),
        scope = scope,
    )
}
