package com.cdelarue.localmusic.data

/**
 * Pure grouping, sorting and search over a scanned song list. Kept free of Android types so the
 * behaviour that is easy to get wrong is covered by plain JVM unit tests.
 */
object LibraryIndex {

    fun build(songs: List<Song>): Library = Library(
        songs = sortSongs(songs, SortOrder()),
        albums = albumsOf(songs),
        artists = artistsOf(songs),
        folders = foldersOf(songs),
    )

    fun albumsOf(songs: List<Song>): List<Album> = songs
        .groupBy { it.albumId }
        .map { (albumId, tracks) ->
            val first = tracks.first()
            Album(
                id = albumId,
                title = first.album,
                artist = tracks.map { it.artist }.distinct().singleOrNull() ?: VARIOUS_ARTISTS,
                artistId = first.artistId,
                year = tracks.maxOf { it.year },
                songCount = tracks.size,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun artistsOf(songs: List<Song>): List<Artist> = songs
        .groupBy { it.artistId }
        .map { (artistId, tracks) ->
            Artist(
                id = artistId,
                name = tracks.first().artist,
                albumCount = tracks.map { it.albumId }.distinct().size,
                songCount = tracks.size,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun foldersOf(songs: List<Song>): List<Folder> = songs
        .groupBy { it.folderPath }
        .map { (path, tracks) -> Folder(path = path, songCount = tracks.size) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun sortSongs(songs: List<Song>, order: SortOrder): List<Song> {
        val comparator: Comparator<Song> = when (order.key) {
            SongSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SongSort.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SongSort.ALBUM -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.trackNumber }
            SongSort.DATE_ADDED -> compareBy { it.dateAddedSeconds }
            SongSort.DURATION -> compareBy { it.durationMs }
        }
        val tieBroken = comparator.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        return songs.sortedWith(if (order.ascending) tieBroken else tieBroken.reversed())
    }

    /** Album tracks in disc/track order, which is not the same as the library sort. */
    fun tracksOfAlbum(songs: List<Song>, albumId: Long): List<Song> = songs
        .filter { it.albumId == albumId }
        .sortedWith(compareBy<Song> { it.trackNumber }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun tracksOfArtist(songs: List<Song>, artistId: Long): List<Song> = songs
        .filter { it.artistId == artistId }
        .sortedWith(
            compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy { it.trackNumber },
        )

    fun tracksOfFolder(songs: List<Song>, folderPath: String): List<Song> = songs
        .filter { it.folderPath == folderPath }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

    fun search(library: Library, rawQuery: String): SearchResults {
        val query = rawQuery.trim()
        if (query.isEmpty()) return SearchResults()
        return SearchResults(
            songs = library.songs.filter {
                it.title.containsQuery(query) || it.artist.containsQuery(query) || it.album.containsQuery(query)
            },
            albums = library.albums.filter { it.title.containsQuery(query) || it.artist.containsQuery(query) },
            artists = library.artists.filter { it.name.containsQuery(query) },
            folders = library.folders.filter { it.name.containsQuery(query) || it.path.containsQuery(query) },
        )
    }

    /** First character used by the A-Z rail; anything non-alphabetic lands under "#". */
    fun sortInitial(value: String): String {
        val first = value.trim().firstOrNull() ?: return "#"
        return if (first.isLetter()) first.uppercaseChar().toString() else "#"
    }

    private fun String.containsQuery(query: String) = contains(query, ignoreCase = true)

    const val VARIOUS_ARTISTS = "Various artists"
}

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && folders.isEmpty()
}
