package com.cdelarue.localmusic.playback

import com.cdelarue.localmusic.data.Library
import com.cdelarue.localmusic.data.LibraryIndex
import com.cdelarue.localmusic.data.Song

enum class BrowseStyle { LIST, GRID }

data class QueueSelection(val songs: List<Song>, val startIndex: Int)

/**
 * Everything the browse tree needs to answer a request. Favourites come from the play history and
 * albums are optional, so neither can be derived from the library alone.
 */
data class BrowseContext(
    val library: Library,
    val favourites: List<Song> = emptyList(),
    val showAlbums: Boolean = false,
)

/** One entry in the browse tree, free of Android types so the whole tree is unit-testable. */
data class BrowseNode(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val playable: Boolean = false,
    val albumId: Long? = null,
    val songId: Long? = null,
    val childStyle: BrowseStyle = BrowseStyle.LIST,
)

/**
 * Media ids used by Android Auto and by anything else browsing the session.
 *
 * A playable id carries the parent it was picked from, so choosing track three of an album queues
 * the whole album from track three rather than that one track on its own.
 */
object BrowseIds {
    const val ROOT = "root"
    const val TAB_FAVOURITES = "tab|favourites"
    const val TAB_SONGS = "tab|songs"
    const val TAB_ALBUMS = "tab|albums"
    const val TAB_ARTISTS = "tab|artists"
    const val TAB_FOLDERS = "tab|folders"

    private const val SEP = '|'

    fun album(id: Long) = "album|$id"
    fun artist(id: Long) = "artist|$id"
    fun folder(path: String) = "folder|$path"

    fun song(songId: Long, parentId: String) = "song|$songId|$parentId"

    /**
     * The song id behind a media id, for both shapes in play: the plain id the phone UI uses and
     * the parent-carrying id from the browse tree. The saved queue depends on this, which is why
     * it is not simply [String.toLongOrNull].
     */
    fun songIdOf(mediaId: String): Long? = when {
        mediaId.startsWith("song|") -> mediaId.split(SEP, limit = 3).getOrNull(1)?.toLongOrNull()
        else -> mediaId.toLongOrNull()
    }

    fun parentOf(mediaId: String): String? =
        if (mediaId.startsWith("song|")) mediaId.split(SEP, limit = 3).getOrNull(2) else null

    fun payloadOf(prefix: String, mediaId: String): String? =
        if (mediaId.startsWith("$prefix$SEP")) mediaId.substringAfter(SEP) else null
}

object BrowseTree {

    /** Favourites first: at the wheel it is the list you actually reach for. */
    fun rootChildren(context: BrowseContext): List<BrowseNode> = buildList {
        add(BrowseNode(BrowseIds.TAB_FAVOURITES, "Favourites"))
        add(BrowseNode(BrowseIds.TAB_SONGS, "Songs"))
        if (context.showAlbums) add(BrowseNode(BrowseIds.TAB_ALBUMS, "Albums"))
        add(BrowseNode(BrowseIds.TAB_ARTISTS, "Artists"))
        add(BrowseNode(BrowseIds.TAB_FOLDERS, "Folders"))
    }

    fun childrenOf(context: BrowseContext, parentId: String): List<BrowseNode> {
        val library = context.library
        return when {
        parentId == BrowseIds.ROOT -> rootChildren(context)

        parentId == BrowseIds.TAB_FAVOURITES ->
            context.favourites.map { it.toNode(parentId) }

        parentId == BrowseIds.TAB_SONGS ->
            library.songs.map { it.toNode(parentId) }

        parentId == BrowseIds.TAB_ALBUMS -> library.albums.map { album ->
            BrowseNode(
                id = BrowseIds.album(album.id),
                title = album.title,
                subtitle = album.artist,
                albumId = album.id,
            )
        }

        parentId == BrowseIds.TAB_ARTISTS -> library.artists.map { artist ->
            BrowseNode(
                id = BrowseIds.artist(artist.id),
                title = artist.name,
                subtitle = "${artist.albumCount} albums",
            )
        }

        parentId == BrowseIds.TAB_FOLDERS -> library.folders.map { folder ->
            BrowseNode(
                id = BrowseIds.folder(folder.path),
                title = folder.name,
                subtitle = "${folder.songCount} tracks",
            )
        }

        else -> songsOf(context, parentId).map { it.toNode(parentId) }
        }
    }

    /** The tracks a parent stands for, which is also the queue built when one is picked. */
    fun songsOf(context: BrowseContext, parentId: String): List<Song> {
        val library = context.library
        return when {
        parentId == BrowseIds.TAB_FAVOURITES -> context.favourites
        parentId == BrowseIds.TAB_SONGS -> library.songs
        parentId == BrowseIds.ROOT -> emptyList()

        BrowseIds.payloadOf("album", parentId) != null ->
            LibraryIndex.tracksOfAlbum(library.songs, BrowseIds.payloadOf("album", parentId)!!.toLongOrNull() ?: -1L)

        BrowseIds.payloadOf("artist", parentId) != null ->
            LibraryIndex.tracksOfArtist(library.songs, BrowseIds.payloadOf("artist", parentId)!!.toLongOrNull() ?: -1L)

        BrowseIds.payloadOf("folder", parentId) != null ->
            LibraryIndex.tracksOfFolder(library.songs, BrowseIds.payloadOf("folder", parentId)!!)

        else -> emptyList()
        }
    }

    /** Voice search: "play Discovery" has to match a title, an artist or an album. */
    fun search(context: BrowseContext, query: String): List<BrowseNode> {
        val results = LibraryIndex.search(context.library, query)
        val fromAlbums = if (!context.showAlbums) emptyList() else results.albums.map { album ->
            BrowseNode(
                id = BrowseIds.album(album.id),
                title = album.title,
                subtitle = album.artist,
                albumId = album.id,
            )
        }
        val fromArtists = results.artists.map { artist ->
            BrowseNode(id = BrowseIds.artist(artist.id), title = artist.name, subtitle = "Artist")
        }
        val fromSongs = results.songs.map { it.toNode(BrowseIds.TAB_SONGS) }
        return fromAlbums + fromArtists + fromSongs
    }

    /**
     * What to play when an id is chosen in the car: the whole parent as the queue, positioned on
     * the track that was picked, so skipping back stays possible.
     */
    fun queueFor(context: BrowseContext, mediaId: String): QueueSelection? {
        if (mediaId.startsWith("song|")) {
            val songId = BrowseIds.songIdOf(mediaId) ?: return null
            val parent = BrowseIds.parentOf(mediaId).orEmpty()
            val songs = songsOf(context, parent)
            val index = songs.indexOfFirst { it.id == songId }
            if (index >= 0) return QueueSelection(songs, index)
            // The parent no longer holds it: fall back to the track on its own.
            val single = context.library.songs.filter { it.id == songId }
            return if (single.isEmpty()) null else QueueSelection(single, 0)
        }
        val songs = songsOf(context, mediaId)
        return if (songs.isEmpty()) null else QueueSelection(songs, 0)
    }

    /**
     * What "play Discovery" should start. An album or an artist is a better answer than a single
     * track, so those win over a title match; an empty query means "play everything".
     */
    fun queueForSearch(context: BrowseContext, query: String): QueueSelection? {
        val library = context.library
        if (query.isBlank()) {
            return library.songs.takeIf { it.isNotEmpty() }?.let { QueueSelection(it, 0) }
        }
        // "play my favourites" should reach the list, not a track that happens to match.
        if (query.contains("favourite", ignoreCase = true) || query.contains("favorite", ignoreCase = true)) {
            context.favourites.takeIf { it.isNotEmpty() }?.let { return QueueSelection(it, 0) }
        }
        val results = LibraryIndex.search(library, query)
        if (context.showAlbums) {
            results.albums.firstOrNull()?.let { album ->
                return queueFor(context, BrowseIds.album(album.id))
            }
        }
        results.artists.firstOrNull()?.let { artist ->
            return queueFor(context, BrowseIds.artist(artist.id))
        }
        results.songs.firstOrNull()?.let { song ->
            return QueueSelection(results.songs, results.songs.indexOf(song))
        }
        return null
    }

    private fun Song.toNode(parentId: String) = BrowseNode(
        id = BrowseIds.song(id, parentId),
        title = title,
        subtitle = "$artist · $album",
        playable = true,
        albumId = albumId,
        songId = id,
    )
}
