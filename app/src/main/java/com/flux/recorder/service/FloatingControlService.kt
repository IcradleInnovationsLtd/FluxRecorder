package com.flux.recorder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.flux.recorder.core.camera.CameraOverlay
import com.flux.recorder.utils.PermissionManager
import kotlin.math.abs

/**
 * Service for a floating control overlay (pause/resume/stop/camera) shown during recording.
 */
class FloatingControlService : Service() {

    private var cameraOverlay: CameraOverlay? = null
    private var controlOverlay: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private var enableCamera = false
    private var isPaused = false

    companion object {
        private const val TAG = "FloatingControlService"
        const val EXTRA_ENABLE_CAMERA = "enable_camera"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingControlService created")
        if (PermissionManager.hasOverlayPermission(this)) {
            createControlOverlay()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createControlOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(Color.parseColor("#E6121212"))
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
            background = bg
            elevation = 16f
        }

        // Helper to create styled circular buttons
        fun createOverlayButton(iconRes: Int, tintColor: Int, onClick: () -> Unit): ImageButton {
            return ImageButton(this).apply {
                setImageResource(iconRes)
                setColorFilter(tintColor)
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#26FFFFFF"))
                }
                background = shape
                setPadding(14, 14, 14, 14)
                layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                    setMargins(0, 4, 0, 4)
                }
                setOnClickListener { onClick() }
            }
        }

        // Pause/Resume button
        var pauseButton: ImageButton? = null
        pauseButton = createOverlayButton(
            iconRes = android.R.drawable.ic_media_pause,
            tintColor = Color.parseColor("#00E5FF")
        ) {
            isPaused = !isPaused
            if (isPaused) {
                pauseButton?.setImageResource(android.R.drawable.ic_media_play)
                startService(Intent(this, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_PAUSE_RECORDING
                })
            } else {
                pauseButton?.setImageResource(android.R.drawable.ic_media_pause)
                startService(Intent(this, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_RESUME_RECORDING
                })
            }
        }
        container.addView(pauseButton)

        // Stop button
        val stopButton = createOverlayButton(
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
            tintColor = Color.parseColor("#FF3B30")
        ) {
            Log.d(TAG, "Stop clicked from floating overlay")
            startService(Intent(this, RecorderService::class.java).apply {
                action = RecorderService.ACTION_STOP_RECORDING
            })
            stopSelf()
        }
        container.addView(stopButton)

        // Drag support
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (initialTouchX - event.rawX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        if (abs(deltaX) > 4 || abs(deltaY) > 4) {
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            try {
                                windowManager.updateViewLayout(container, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating floating control position", e)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        controlOverlay = container
        try {
            windowManager.addView(container, params)
            Log.d(TAG, "Control overlay added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add control overlay", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingControlService started")
        enableCamera = intent?.getBooleanExtra(EXTRA_ENABLE_CAMERA, false) ?: false

        if (enableCamera && PermissionManager.hasCameraPermission(this) && PermissionManager.hasOverlayPermission(this)) {
            if (cameraOverlay == null) {
                cameraOverlay = CameraOverlay(this)
            }
            cameraOverlay?.show()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingControlService destroyed")
        try {
            cameraOverlay?.stop()
            cameraOverlay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera overlay", e)
        }

        controlOverlay?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing control overlay", e)
            }
        }
        controlOverlay = null
    }
}
