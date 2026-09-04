package com.cdelarue.localmusic.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteOutcome {
    /** The file is gone. */
    data object Deleted : DeleteOutcome

    /** Android wants the user to confirm; launch this and wait for the result. */
    data class NeedsConsent(val intentSender: IntentSender) : DeleteOutcome

    data class Failed(val reason: String) : DeleteOutcome
}

/**
 * Deletes an audio file from the device.
 *
 * Which path applies depends on the Android version: from 11 the system always asks the user
 * itself and hands back an intent to launch; on 10 the attempt throws and carries the same kind of
 * intent; below that the delete goes straight through, which is why the write permission is
 * declared only for those releases.
 */
@Singleton
class SongDeleter @Inject constructor(private val context: Context) {

    fun delete(song: Song): DeleteOutcome {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pendingIntent = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(uri),
                )
                DeleteOutcome.NeedsConsent(pendingIntent.intentSender)
            }

            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) DeleteOutcome.Deleted else DeleteOutcome.Failed("The file could not be deleted.")
            } catch (security: RecoverableSecurityException) {
                DeleteOutcome.NeedsConsent(security.userAction.actionIntent.intentSender)
            }

            else -> try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) DeleteOutcome.Deleted else DeleteOutcome.Failed("The file could not be deleted.")
            } catch (security: SecurityException) {
                DeleteOutcome.Failed("This app is not allowed to delete that file.")
            }
        }
    }
}
