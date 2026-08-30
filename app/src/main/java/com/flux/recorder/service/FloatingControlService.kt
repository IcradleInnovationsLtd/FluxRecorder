package com.flux.recorder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import com.flux.recorder.R
import com.flux.recorder.core.camera.CameraOverlay
import com.flux.recorder.utils.PermissionManager
import kotlin.math.abs

/**
 * Service for a modern, collapsible floating control overlay (pause/resume/stop/camera)
 * marked with FLAG_SECURE so it is invisible to screen capture during recording.
 */
class FloatingControlService : Service() {

    private var cameraOverlay: CameraOverlay? = null
    private var controlOverlay: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private var isExpanded = true
    private var isPaused = false
    private var isFacecamActive = false
    private var shouldShowControls = true

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDimRunnable = Runnable {
        if (!isExpanded) {
            controlOverlay?.animate()?.alpha(0.35f)?.setDuration(400)?.start()
        }
    }

    companion object {
        private const val TAG = "FloatingControlService"
        const val EXTRA_ENABLE_CAMERA = "enable_camera"
        const val EXTRA_SHOW_CONTROLS = "show_controls"
        const val ACTION_SHOW_PREVIEW_ONLY = "com.flux.recorder.SHOW_CAMERA_PREVIEW"
        const val ACTION_HIDE_PREVIEW_ONLY = "com.flux.recorder.HIDE_CAMERA_PREVIEW"
        const val ACTION_TOGGLE_FACECAM = "com.flux.recorder.TOGGLE_FACECAM"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingControlService created")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOrUpdateControlOverlay() {
        if (!shouldShowControls) {
            controlOverlay?.let {
                try { windowManager.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing overlay", e) }
            }
            controlOverlay = null
            return
        }

        if (controlOverlay != null) {
            try {
                windowManager.removeView(controlOverlay)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing old control overlay", e)
            }
            controlOverlay = null
        }

        if (!PermissionManager.hasOverlayPermission(this)) return

        val density = resources.displayMetrics.density

        // FLAG_SECURE prevents MediaProjection from capturing the floating controls into recorded video
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (120 * density).toInt()
        }

        val rootLayout = FrameLayout(this)

        if (isExpanded) {
            mainHandler.removeCallbacks(autoDimRunnable)
            rootLayout.alpha = 1.0f

            // Expanded Menu: Vertical panel with Pause, Stop, Facecam, and Collapse buttons
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f * density
                    setColor(Color.parseColor("#EE121212"))
                    setStroke((1.5f * density).toInt(), Color.parseColor("#33FFFFFF"))
                }
                background = bg
                elevation = 20f
            }

            fun createButton(iconRes: Int, tintColor: Int, bgTint: Int = Color.parseColor("#26FFFFFF"), onClick: () -> Unit): ImageButton {
                val btnSize = (44 * density).toInt()
                return ImageButton(this).apply {
                    setImageResource(iconRes)
                    setColorFilter(tintColor)
                    val shape = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(bgTint)
                    }
                    background = shape
                    setPadding((10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
                    layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                        setMargins(0, (3 * density).toInt(), 0, (3 * density).toInt())
                    }
                    setOnClickListener { onClick() }
                }
            }

            // 1. Pause/Resume button
            val pauseButton = createButton(
                iconRes = if (isPaused) R.drawable.ic_play_white else R.drawable.ic_pause_white,
                tintColor = Color.parseColor("#00E5FF")
            ) {
                isPaused = !isPaused
                if (isPaused) {
                    startService(Intent(this, RecorderService::class.java).apply {
                        action = RecorderService.ACTION_PAUSE_RECORDING
                    })
                } else {
                    startService(Intent(this, RecorderService::class.java).apply {
                        action = RecorderService.ACTION_RESUME_RECORDING
                    })
                }
                createOrUpdateControlOverlay()
            }
            panel.addView(pauseButton)

            // 2. Stop button
            val stopButton = createButton(
                iconRes = R.drawable.ic_stop_white,
                tintColor = Color.parseColor("#FF3B30"),
                bgTint = Color.parseColor("#33FF3B30")
            ) {
                Log.d(TAG, "Stop clicked from floating overlay")
                startService(Intent(this, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_STOP_RECORDING
                })
                stopSelf()
            }
            panel.addView(stopButton)

            // 3. Facecam Toggle button (turn facecam on/off anytime during recording)
            val facecamButton = createButton(
                iconRes = R.drawable.ic_camera_white,
                tintColor = if (isFacecamActive) Color.parseColor("#00E5FF") else Color.parseColor("#88FFFFFF"),
                bgTint = if (isFacecamActive) Color.parseColor("#3300E5FF") else Color.parseColor("#26FFFFFF")
            ) {
                toggleFacecam()
                createOrUpdateControlOverlay()
            }
            panel.addView(facecamButton)

            // 4. Collapse / Hide button (minimizes to a tiny bubble)
            val minimizeButton = createButton(
                iconRes = R.drawable.ic_minimize,
                tintColor = Color.parseColor("#AAAAAA")
            ) {
                isExpanded = false
                createOrUpdateControlOverlay()
            }
            panel.addView(minimizeButton)

            rootLayout.addView(panel)

        } else {
            // Collapsed Bubble: Small 44dp circular badge that expands on tap
            val bubbleSize = (44 * density).toInt()
            val bubble = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(bubbleSize, bubbleSize)
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#EE121212"))
                    setStroke((2f * density).toInt(), Color.parseColor("#00E5FF"))
                }
                background = bg
                elevation = 20f
                setOnClickListener {
                    isExpanded = true
                    createOrUpdateControlOverlay()
                }
            }

            val icon = ImageButton(this).apply {
                setImageResource(R.drawable.ic_record)
                setColorFilter(Color.parseColor("#FF3B30"))
                background = null
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                setOnClickListener {
                    isExpanded = true
                    createOrUpdateControlOverlay()
                }
            }
            bubble.addView(icon)
            rootLayout.addView(bubble)

            // Schedule auto-dimming after 3 seconds
            mainHandler.removeCallbacks(autoDimRunnable)
            mainHandler.postDelayed(autoDimRunnable, 3000)
        }

        // Drag support for the root layout
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rootLayout.alpha = 1.0f
                        mainHandler.removeCallbacks(autoDimRunnable)
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (initialTouchX - event.rawX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        if (abs(deltaX) > 6 || abs(deltaY) > 6) {
                            isDragging = true
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            try {
                                windowManager.updateViewLayout(rootLayout, params)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating floating control position", e)
                            }
                        }
                        return isDragging
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isExpanded) {
                            mainHandler.postDelayed(autoDimRunnable, 3000)
                        }
                    }
                }
                return false
            }
        })

        controlOverlay = rootLayout
        try {
            windowManager.addView(rootLayout, params)
            Log.d(TAG, "Control overlay added to WindowManager (FLAG_SECURE active, isExpanded=$isExpanded)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add control overlay", e)
        }
    }

    private fun toggleFacecam() {
        if (isFacecamActive) {
            cameraOverlay?.stop()
            cameraOverlay = null
            isFacecamActive = false
            Log.d(TAG, "Facecam turned OFF")
        } else {
            if (PermissionManager.hasCameraPermission(this) && PermissionManager.hasOverlayPermission(this)) {
                if (cameraOverlay == null) {
                    cameraOverlay = CameraOverlay(this)
                }
                cameraOverlay?.show()
                isFacecamActive = true
                Log.d(TAG, "Facecam turned ON")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingControlService onStartCommand action: ${intent?.action}")

        when (intent?.action) {
            ACTION_SHOW_PREVIEW_ONLY -> {
                if (PermissionManager.hasCameraPermission(this) && PermissionManager.hasOverlayPermission(this)) {
                    if (cameraOverlay == null) {
                        cameraOverlay = CameraOverlay(this)
                    }
                    cameraOverlay?.show()
                    isFacecamActive = true
                }
                return START_NOT_STICKY
            }
            ACTION_HIDE_PREVIEW_ONLY -> {
                cameraOverlay?.stop()
                cameraOverlay = null
                isFacecamActive = false
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_FACECAM -> {
                toggleFacecam()
                createOrUpdateControlOverlay()
                return START_NOT_STICKY
            }
        }

        val enableCamera = intent?.getBooleanExtra(EXTRA_ENABLE_CAMERA, false) ?: false
        shouldShowControls = intent?.getBooleanExtra(EXTRA_SHOW_CONTROLS, true) ?: true

        if (PermissionManager.hasOverlayPermission(this)) {
            createOrUpdateControlOverlay()
        }

        if (enableCamera && PermissionManager.hasCameraPermission(this) && PermissionManager.hasOverlayPermission(this)) {
            if (cameraOverlay == null) {
                cameraOverlay = CameraOverlay(this)
            }
            cameraOverlay?.show()
            isFacecamActive = true
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingControlService destroyed")
        mainHandler.removeCallbacks(autoDimRunnable)
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
