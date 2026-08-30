package com.flux.recorder.core.projection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager

/**
 * Manages MediaProjection for screen capture
 */
class ScreenCaptureManager(private val context: Context) {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    
    private val displayMetrics: DisplayMetrics
        get() {
            val metrics = DisplayMetrics()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // API 30+: use currentWindowMetrics (accurate, no deprecation)
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
            // Handle cleanup if projection stops unexpectedly
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection = null
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
        if (resultCode != Activity.RESULT_OK || data == null) {
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
        virtualDisplay?.release()
        virtualDisplay = null
        
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    /**
     * Get the underlying MediaProjection instance
     */
    fun getMediaProjection(): MediaProjection? {
        return mediaProjection
    }

    /**
     * Check if projection is active
     */
    fun isActive(): Boolean {
        return mediaProjection != null
    }
}
