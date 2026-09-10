package io.github.ofthestreet.barcarole.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteOutcome {
    /** The files are gone. */
    data object Deleted : DeleteOutcome

    /**
     * Android wants the user to confirm; launch this and wait for the result. On Android 10 the
     * consent covers a single file, so [remaining] carries what is left to attempt afterwards.
     */
    data class NeedsConsent(
        val intentSender: IntentSender,
        val remaining: List<Song> = emptyList(),
    ) : DeleteOutcome

    data class Failed(val reason: String) : DeleteOutcome
}

/**
 * Deletes audio files from the device.
 *
 * Which path applies depends on the Android version: from 11 the system asks the user itself and
 * hands back an intent to launch, one dialog for the whole batch; on 10 it throws an exception
 * carrying the same kind of intent, but only for the file it stopped on; below that the delete
 * goes straight through, which is why the write permission is declared only for those releases.
 */
@Singleton
class SongDeleter @Inject constructor(private val context: Context) {

    fun delete(song: Song): DeleteOutcome = delete(listOf(song))

    fun delete(songs: List<Song>): DeleteOutcome {
        if (songs.isEmpty()) return DeleteOutcome.Deleted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(context.contentResolver, songs.map(::uriOf))
            return DeleteOutcome.NeedsConsent(request.intentSender)
        }

        var deleted = 0
        songs.forEachIndexed { index, song ->
            try {
                deleted += context.contentResolver.delete(uriOf(song), null, null)
            } catch (security: SecurityException) {
                // Android 10 only: the user vouches for this one file, then we pick up the rest.
                val consent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    consentFrom(security)
                } else {
                    null
                }
                return if (consent != null) {
                    DeleteOutcome.NeedsConsent(intentSender = consent, remaining = songs.drop(index))
                } else {
                    failed(deleted, songs.size, "This app is not allowed to delete that file.")
                }
            }
        }
        return if (deleted > 0) {
            DeleteOutcome.Deleted
        } else {
            DeleteOutcome.Failed("The file could not be deleted.")
        }
    }

    /** Android 10 wraps the missing grant in an exception that carries the consent intent. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun consentFrom(security: SecurityException): IntentSender? =
        (security as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender

    private fun failed(deleted: Int, total: Int, reason: String): DeleteOutcome =
        if (deleted == 0) {
            DeleteOutcome.Failed(reason)
        } else {
            DeleteOutcome.Failed("Deleted $deleted of $total files. $reason")
        }

    private fun uriOf(song: Song): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
}
