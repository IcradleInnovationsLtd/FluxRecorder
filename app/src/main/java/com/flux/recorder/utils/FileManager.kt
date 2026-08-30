package com.flux.recorder.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.flux.recorder.data.Recording
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages recording file creation, storage, querying, and deletion.
 *
 * On Android 10+ (API 29+) recordings are written to MediaStore and accessed via content URIs.
 * On older devices, legacy public external storage is used.
 */
class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
        private const val RECORDINGS_DIR = "Recordings"
        private const val FILE_PREFIX = "FluxRec_"
        private const val FILE_EXTENSION = ".mp4"
        /** Sub-folder inside Movies used on all API levels. */
        private const val PUBLIC_FOLDER = "FluxRecorder"
    }

    // -----------------------------------------------------------------------------------------
    // Recording file creation
    // -----------------------------------------------------------------------------------------

    /**
     * Returns the private app-specific directory used as a temp location during recording.
     * The file is later copied to public storage (MediaStore or legacy Movies dir) in stopRecording.
     */
    fun getRecordingsDirectory(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), RECORDINGS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Create a new temp recording file (stored in app-private space during capture). */
    fun createRecordingFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(getRecordingsDirectory(), "$FILE_PREFIX$timestamp$FILE_EXTENSION")
    }

    // -----------------------------------------------------------------------------------------
    // Querying the recordings library
    // -----------------------------------------------------------------------------------------

    /**
     * Returns all recordings created by Flux Recorder, sorted newest-first.
     *
     * On Android 10+: queries MediaStore (scoped storage — content URIs are fully accessible).
     * On Android <10: scans the public Movies/FluxRecorder directory.
     */
    fun getAllRecordings(): List<Recording> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStore()
        } else {
            queryLegacyStorage()
        }
    }

    private fun queryMediaStore(): List<Recording> {
        val recordings = mutableListOf<Recording>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION
        )

        val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$FILE_PREFIX%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idCol          = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol        = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol        = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol        = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val resolutionCol  = cursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION)

                while (cursor.moveToNext()) {
                    val id         = cursor.getLong(idCol)
                    val name       = cursor.getString(nameCol) ?: continue
                    val size       = cursor.getLong(sizeCol)
                    val duration   = cursor.getLong(durationCol)
                    val date       = cursor.getLong(dateCol)
                    val resolution = if (resolutionCol >= 0) cursor.getString(resolutionCol) else null

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )

                    recordings.add(
                        Recording(
                            id          = id,
                            contentUri  = contentUri,
                            displayName = name,
                            durationMs  = duration,
                            sizeBytes   = size,
                            timestamp   = date,
                            resolution  = resolution
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }

        return recordings
    }

    private fun queryLegacyStorage(): List<Recording> {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            PUBLIC_FOLDER
        )
        if (!publicDir.exists()) return emptyList()

        return publicDir.listFiles { f ->
            f.isFile && f.extension == "mp4" && f.name.startsWith(FILE_PREFIX)
        }?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                Recording(
                    fileUri     = Uri.fromFile(file),
                    displayName = file.name,
                    sizeBytes   = file.length(),
                    timestamp   = file.lastModified() / 1000
                )
            } ?: emptyList()
    }

    // -----------------------------------------------------------------------------------------
    // Deletion
    // -----------------------------------------------------------------------------------------

    /**
     * Delete a recording. Returns true if deletion succeeded.
     */
    fun deleteRecording(recording: Recording): Boolean {
        return if (recording.contentUri != null) {
            try {
                context.contentResolver.delete(recording.contentUri, null, null) > 0
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting recording from MediaStore", e)
                false
            }
        } else {
            // Legacy: resolve from file URI
            val file = recording.fileUri?.path?.let { File(it) } ?: return false
            file.delete()
        }
    }

    // -----------------------------------------------------------------------------------------
    // Public gallery copy (called after recording stops)
    // -----------------------------------------------------------------------------------------

    /**
     * Copy the private temp recording to the public gallery.
     *
     * - Android 10+: inserts into MediaStore; returns null (URI-based, no File returned).
     * - Android <10: copies to Movies/FluxRecorder; returns the public File.
     */
    fun copyToPublicGallery(privateFile: File): File? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, privateFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/$PUBLIC_FOLDER")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        privateFile.inputStream().use { it.copyTo(out) }
                    }
                }
                null // URI-based; caller uses MediaScanner for legacy compat
            } else {
                val publicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    PUBLIC_FOLDER
                ).also { if (!it.exists()) it.mkdirs() }

                val publicFile = File(publicDir, privateFile.name)
                privateFile.copyTo(publicFile, overwrite = true)
                publicFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to public gallery", e)
            null
        }
    }

    // -----------------------------------------------------------------------------------------
    // Storage helpers
    // -----------------------------------------------------------------------------------------

    fun getAvailableSpace(): Long = getRecordingsDirectory().usableSpace

    fun hasEnoughSpace(estimatedDurationMinutes: Int, bitrate: Int): Boolean {
        val estimatedBytes = estimatedDurationMinutes * 60L * bitrate / 8
        return getAvailableSpace() > (estimatedBytes * 1.2).toLong()
    }
}
