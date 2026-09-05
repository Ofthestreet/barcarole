package com.cdelarue.localmusic.data

/**
 * A set of files the library considers to be the same recording: one copy worth keeping and the
 * others proposed for deletion.
 */
data class DuplicateGroup(
    val keep: Song,
    val remove: List<Song>,
) {
    val wastedBytes: Long get() = remove.sumOf { it.sizeBytes }
}

/**
 * Finds copies of the same recording.
 *
 * The match is deliberately strict - same title, same artist, same length to the second - because
 * the only thing anyone does with the answer is delete files. Two rips of one song at different
 * lengths, or with different tags, are left alone rather than guessed at.
 */
object Duplicates {

    fun find(songs: List<Song>): List<DuplicateGroup> = songs
        .groupBy { key(it) }
        .values
        .filter { it.size > 1 }
        .map { copies ->
            val ordered = copies.sortedWith(preference)
            DuplicateGroup(keep = ordered.first(), remove = ordered.drop(1))
        }
        .sortedByDescending { it.wastedBytes }

    /** Every file the groups propose to delete, in the order they are shown. */
    fun removable(groups: List<DuplicateGroup>): List<Song> = groups.flatMap { it.remove }

    /**
     * The copy to keep: the largest file, since that is usually the better encode. Ties fall back
     * to the one indexed first, then to the shallowest path, so the choice never changes between
     * two runs over the same library.
     */
    private val preference = compareByDescending<Song> { it.sizeBytes }
        .thenBy { it.dateAddedSeconds }
        .thenBy { song -> song.path.count { it == '/' } }
        .thenBy { it.path }

    private fun key(song: Song): String = listOf(
        normalise(song.title),
        normalise(song.artist),
        (song.durationMs / 1000).toString(),
    ).joinToString(" ")

    private fun normalise(value: String): String = value.trim().lowercase()
}
