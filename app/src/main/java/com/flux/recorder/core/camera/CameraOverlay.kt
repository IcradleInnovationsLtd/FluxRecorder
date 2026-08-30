package com.flux.recorder.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Manages the floating facecam camera overlay using CameraX.
 * Configured with TextureView compatibility mode to prevent display freezes during screen capture.
 */
class CameraOverlay(private val context: Context) : LifecycleOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var lifecycleRegistry = LifecycleRegistry(this)

    private var overlayView: View? = null
    private var previewView: PreviewView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val TAG = "CameraOverlay"
        private const val OVERLAY_WIDTH = 320
        private const val OVERLAY_HEIGHT = 420
    }

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show camera overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        // Reset lifecycle
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            lifecycleRegistry = LifecycleRegistry(this)
        }
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // Layout params for overlay window
        layoutParams = WindowManager.LayoutParams(
            OVERLAY_WIDTH,
            OVERLAY_HEIGHT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 120
        }

        // Modern rounded container with dark border
        val container = FrameLayout(context).apply {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#E60D0D0D"))
                setStroke(3, Color.parseColor("#4D00E5FF"))
            }
            background = shape
            clipToOutline = true
        }

        // PreviewView with COMPATIBLE mode (TextureView) to prevent SurfaceFlinger display locks
        previewView = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        container.addView(
            previewView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Close button (Top Right)
        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            val btnShape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#99000000"))
            }
            background = btnShape
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                stop()
            }
        }
        val closeParams = FrameLayout.LayoutParams(64, 64).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 12, 12, 0)
        }
        container.addView(closeButton, closeParams)

        overlayView = container

        // Drag listener
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager.updateViewLayout(overlayView, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating overlay position", e)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Add to window and start camera
        try {
            windowManager.addView(overlayView, layoutParams)
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            startCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add camera overlay to WindowManager", e)
            overlayView = null
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .setTargetResolution(android.util.Size(480, 640))
            .build()

        preview.setSurfaceProvider(previewView?.surfaceProvider)

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            provider.unbindAll()
            if (provider.hasCamera(cameraSelector)) {
                provider.bindToLifecycle(this, cameraSelector, preview)
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                Log.d(TAG, "Facecam bound successfully with FRONT camera")
            } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
                Log.d(TAG, "Facecam fallback bound with BACK camera")
            } else {
                Log.w(TAG, "No suitable camera found on device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera use cases", e)
        }
    }

    fun stop() {
        try {
            cameraProvider?.unbindAll()
            cameraProvider = null
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera", e)
        }

        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
            previewView = null
        }
    }
}
