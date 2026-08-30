package com.flux.recorder.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for FluxRecorder.
 */
object PermissionManager {

    /**
     * Get dynamic list of required runtime permissions based on settings and API level.
     */
    fun getRequiredPermissions(enableFacecam: Boolean = false): List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)

        if (enableFacecam) {
            add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check if a specific runtime permission is granted.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all required runtime permissions are granted.
     */
    fun hasRequiredPermissions(context: Context, enableFacecam: Boolean = false): Boolean {
        return getRequiredPermissions(enableFacecam).all { isPermissionGranted(context, it) }
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW (Draw Over Other Apps) permission is granted.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Check if audio recording permission is granted.
     */
    fun hasAudioPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Check if camera permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }
}
