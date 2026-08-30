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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.flux.recorder.R
import com.flux.recorder.core.camera.CameraOverlay
import com.flux.recorder.utils.PermissionManager
import kotlin.math.abs

/**
 * Service for a modern, collapsible floating control overlay (pause/resume/stop/camera)
 * shown during recording and camera preview.
 */
class FloatingControlService : Service() {

    private var cameraOverlay: CameraOverlay? = null
    private var controlOverlay: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private var isExpanded = true
    private var isPaused = false
    private var isFacecamActive = false

    companion object {
        private const val TAG = "FloatingControlService"
        const val EXTRA_ENABLE_CAMERA = "enable_camera"
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

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * density).toInt()
            y = (120 * density).toInt()
        }

        val rootLayout = FrameLayout(this)

        if (isExpanded) {
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
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return false // Allow clicks if no movement
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
                }
                return false
            }
        })

        controlOverlay = rootLayout
        try {
            windowManager.addView(rootLayout, params)
            Log.d(TAG, "Control overlay added to WindowManager (isExpanded=$isExpanded)")
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
                // Show facecam preview only (e.g. from Home Screen preview button)
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
