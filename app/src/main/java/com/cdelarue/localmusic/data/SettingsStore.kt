package com.cdelarue.localmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val minTrackSeconds: Int = 30,
    val sortOrders: Map<LibraryTab, SortOrder> = emptyMap(),
) {
    fun sortFor(tab: LibraryTab): SortOrder = sortOrders[tab] ?: SortOrder()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[KeyTheme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[KeyDynamicColor] ?: true,
            minTrackSeconds = prefs[KeyMinTrackSeconds] ?: 30,
            sortOrders = LibraryTab.entries.associateWith { tab ->
                val key = prefs[sortKeyFor(tab)]?.let { runCatching { SongSort.valueOf(it) }.getOrNull() } ?: SongSort.TITLE
                SortOrder(key = key, ascending = prefs[sortAscendingFor(tab)] ?: true)
            },
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[KeyTheme] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[KeyDynamicColor] = enabled }

    suspend fun setMinTrackSeconds(seconds: Int) = context.dataStore.edit { it[KeyMinTrackSeconds] = seconds }

    suspend fun setSortOrder(tab: LibraryTab, order: SortOrder) = context.dataStore.edit {
        it[sortKeyFor(tab)] = order.key.name
        it[sortAscendingFor(tab)] = order.ascending
    }

    private companion object {
        val KeyTheme = stringPreferencesKey("theme_mode")
        val KeyDynamicColor = booleanPreferencesKey("dynamic_color")
        val KeyMinTrackSeconds = intPreferencesKey("min_track_seconds")
        fun sortKeyFor(tab: LibraryTab) = stringPreferencesKey("sort_key_${tab.name}")
        fun sortAscendingFor(tab: LibraryTab) = booleanPreferencesKey("sort_asc_${tab.name}")
    }
}
