package io.github.ofthestreet.barcarole.data

/** A single audio file on the device. Pure data: no Android types, so it is unit-testable. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val dateAddedSeconds: Long,
    val path: String,
    val sizeBytes: Long = 0,
) {
    val folderPath: String
        get() = path.substringBeforeLast('/', missingDelimiterValue = "")
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val year: Int,
    val songCount: Int,
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

data class Folder(
    val path: String,
    val songCount: Int,
) {
    val name: String
        get() = path.substringAfterLast('/', missingDelimiterValue = path).ifEmpty { path }
}

/** Everything derived from one MediaStore scan. */
data class Library(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty()
}

enum class SongSort { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION }

data class SortOrder(val key: SongSort = SongSort.TITLE, val ascending: Boolean = true)

/** The four library tabs. Folder browsing lives in settings, not here. */
enum class LibraryTab { SONGS, ALBUMS, ARTISTS, QUEUE, PLAYLISTS }
