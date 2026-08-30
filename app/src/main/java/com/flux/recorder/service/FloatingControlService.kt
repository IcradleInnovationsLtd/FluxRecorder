package com.flux.recorder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import com.flux.recorder.R
import com.flux.recorder.core.camera.CameraOverlay
import kotlin.math.abs

/**
 * Service for a floating control overlay (pause/stop/camera) shown during recording.
 */
class FloatingControlService : Service() {

    private var cameraOverlay: CameraOverlay? = null
    private var controlOverlay: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private var enableCamera = false

    companion object {
        private const val TAG = "FloatingControlService"
        const val EXTRA_ENABLE_CAMERA = "enable_camera"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingControlService created")
        cameraOverlay = CameraOverlay(this)
        createControlOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createControlOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.dialog_holo_dark_frame)
        }

        // Pause button
        val pauseButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setBackgroundResource(android.R.drawable.btn_default)
            setPadding(12, 12, 12, 12)
            setOnClickListener {
                Log.d(TAG, "Pause clicked")
                val intent = Intent(this@FloatingControlService, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_PAUSE_RECORDING
                }
                startService(intent)
            }
        }
        container.addView(pauseButton)

        // Stop button
        val stopButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            setBackgroundResource(android.R.drawable.btn_default)
            setPadding(12, 12, 12, 12)
            setOnClickListener {
                Log.d(TAG, "Stop clicked")
                val intent = Intent(this@FloatingControlService, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_STOP_RECORDING
                }
                startService(intent)
            }
        }
        container.addView(stopButton)

        // Close (hide overlay only) button
        val closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundResource(android.R.drawable.btn_default)
            setPadding(12, 12, 12, 12)
            setOnClickListener { stopSelf() }
        }
        container.addView(closeButton)

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
                        if (abs(deltaX) > 5 || abs(deltaY) > 5) {
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            windowManager.updateViewLayout(container, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        controlOverlay = container
        windowManager.addView(container, params)
        Log.d(TAG, "Control overlay created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingControlService started")
        enableCamera = intent?.getBooleanExtra(EXTRA_ENABLE_CAMERA, false) ?: false
        if (enableCamera) {
            cameraOverlay?.show()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingControlService destroyed")
        cameraOverlay?.stop()
        cameraOverlay = null

        controlOverlay?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { /* already removed */ }
        }
        controlOverlay = null
    }
}
