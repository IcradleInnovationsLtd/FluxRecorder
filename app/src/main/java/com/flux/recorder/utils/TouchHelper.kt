package com.flux.recorder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Helper to manage Android's native visual touch pointer indicator ("Show taps / touches").
 * Allows Flux Recorder to automatically display visual touch feedback circles during screen recordings.
 */
object TouchHelper {
    private const val TAG = "TouchHelper"
    private const val SHOW_TOUCHES = "show_touches"

    /**
     * Check if Android's native visual touch pointer is currently active in system settings.
     */
    fun isSystemShowTouchesEnabled(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, SHOW_TOUCHES) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if app has permission to write system settings (API 23+).
     */
    fun canWriteSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /**
     * Enable or disable native screen touches indicator.
     * Requires WRITE_SETTINGS permission.
     */
    fun setShowTouches(context: Context, enabled: Boolean): Boolean {
        return try {
            if (canWriteSettings(context)) {
                Settings.System.putInt(context.contentResolver, SHOW_TOUCHES, if (enabled) 1 else 0)
                Log.d(TAG, "Successfully updated show_touches to $enabled")
                true
            } else {
                Log.w(TAG, "Cannot write settings: WRITE_SETTINGS permission not granted")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set show_touches to $enabled", e)
            false
        }
    }

    /**
     * Launch System "Modify system settings" permission screen for Flux Recorder.
     */
    fun openWriteSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not open write settings", e)
        }
    }

    /**
     * Open Developer Options settings directly so user can toggle "Show taps" manually.
     */
    fun openDeveloperSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (err: Exception) {
                Log.e(TAG, "Could not open settings", err)
            }
        }
    }
}
