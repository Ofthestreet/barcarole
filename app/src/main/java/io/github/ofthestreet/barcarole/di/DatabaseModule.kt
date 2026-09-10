package io.github.ofthestreet.barcarole.di

import android.content.Context
import androidx.room.Room
import io.github.ofthestreet.barcarole.data.stats.FavouriteDao
import io.github.ofthestreet.barcarole.data.stats.LocalMusicDatabase
import io.github.ofthestreet.barcarole.data.stats.PlayHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocalMusicDatabase =
        Room.databaseBuilder(context, LocalMusicDatabase::class.java, "local-music.db").build()

    @Provides
    fun providePlayHistoryDao(database: LocalMusicDatabase): PlayHistoryDao = database.playHistoryDao()

    @Provides
    fun provideFavouriteDao(database: LocalMusicDatabase): FavouriteDao = database.favouriteDao()
}
