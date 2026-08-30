package com.flux.recorder.core.projection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager

/**
 * Manages MediaProjection for hardware-accelerated screen capture.
 * Supports Android 14+ single-app recording callbacks with automatic pause/resume
 * on visibility changes to prevent black-screen recordings.
 */
class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    /** Optional callback invoked when the system terminates MediaProjection (e.g. from system status bar). */
    var onProjectionStopped: (() -> Unit)? = null

    /** Optional callback invoked when target single-app visibility changes in Android 14+. */
    var onCapturedContentVisibilityChanged: ((isVisible: Boolean) -> Unit)? = null

    /** Optional callback invoked when target single-app is resized in Android 14+. */
    var onCapturedContentResized: ((width: Int, height: Int) -> Unit)? = null

    companion object {
        private const val TAG = "ScreenCaptureManager"
    }

    private val displayMetrics: DisplayMetrics
        get() {
            val metrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val bounds = wm.currentWindowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
                metrics.densityDpi = context.resources.displayMetrics.densityDpi
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .defaultDisplay.getRealMetrics(metrics)
            }
            return metrics
        }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped by system")
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection = null
            onProjectionStopped?.invoke()
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            Log.d(TAG, "Single-app captured content visibility changed: isVisible=$isVisible")
            onCapturedContentVisibilityChanged?.invoke(isVisible)
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            Log.d(TAG, "Single-app captured content resized: ${width}x$height")
            onCapturedContentResized?.invoke(width, height)
        }
    }

    /**
     * Create intent for MediaProjection permission request
     */
    fun createScreenCaptureIntent(): Intent {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as MediaProjectionManager
        return projectionManager.createScreenCaptureIntent()
    }

    /**
     * Initialize MediaProjection from permission result
     */
    fun initializeProjection(resultCode: Int, data: Intent): Boolean {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, "MediaProjection permission denied by user (resultCode: $resultCode)")
            return false
        }

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as MediaProjectionManager

        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(projectionCallback, null)

        return mediaProjection != null
    }

    /**
     * Create virtual display for recording
     */
    fun createVirtualDisplay(
        surface: Surface,
        width: Int,
        height: Int,
        densityDpi: Int
    ): VirtualDisplay? {
        val projection = mediaProjection ?: return null

        virtualDisplay = projection.createVirtualDisplay(
            "FluxRecorder",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )

        return virtualDisplay
    }

    /**
     * Get screen dimensions
     */
    fun getScreenDimensions(): Pair<Int, Int> {
        val metrics = displayMetrics
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * Get screen density
     */
    fun getScreenDensity(): Int {
        return displayMetrics.densityDpi
    }

    /**
     * Stop screen capture and release resources
     */
    fun stop() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null

            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capture", e)
        }
    }

    fun getMediaProjection(): MediaProjection? = mediaProjection

    fun isActive(): Boolean = mediaProjection != null
}
