package com.flux.recorder.data

import android.net.Uri

/**
 * Represents a saved recording, loaded from MediaStore or the filesystem.
 *
 * On Android 10+ (Scoped Storage) the file is accessed via [contentUri].
 * On older Android, [fileUri] (file:// URI) is used instead.
 */
data class Recording(
    /** MediaStore row ID. -1 for legacy-storage recordings. */
    val id: Long = -1L,
    /** Content URI for MediaStore-backed files (Android 10+). Null on legacy storage. */
    val contentUri: Uri? = null,
    /** File URI for pre-Android-10 storage. Null when using MediaStore. */
    val fileUri: Uri? = null,
    /** Display name / filename of the recording. */
    val displayName: String,
    /** Duration of the recording in milliseconds. 0 if unknown. */
    val durationMs: Long = 0L,
    /** Size in bytes. */
    val sizeBytes: Long,
    /** When the file was created (epoch seconds). */
    val timestamp: Long,
    /** Resolution string, e.g. "1920x1080". Null if not available. */
    val resolution: String? = null
) {
    /** The URI to use for opening/sharing/deleting this recording. */
    val uri: Uri get() = contentUri ?: fileUri ?: Uri.EMPTY

    /** Format file size for display. */
    fun getFormattedSize(): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            else    -> "%.2f KB".format(kb)
        }
    }

    /** Format duration for display (HH:MM:SS or MM:SS). */
    fun getFormattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
