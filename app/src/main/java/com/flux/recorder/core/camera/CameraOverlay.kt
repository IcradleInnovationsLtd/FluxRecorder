package com.flux.recorder.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Manages the floating camera overlay using CameraX
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
    }

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
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

        // Reset lifecycle if it was previously destroyed
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) {
            lifecycleRegistry = LifecycleRegistry(this)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        // Create layout params
        layoutParams = WindowManager.LayoutParams(
            300, 400, // Default size
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // Container
        val container = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, android.R.drawable.dialog_holo_light_frame)
        }

        previewView = PreviewView(context)
        container.addView(
            previewView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Close button (Top Right) — closes the camera overlay only
        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener {
                stop()
            }
        }
        val closeParams = FrameLayout.LayoutParams(60, 60).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 10, 10, 0)
        }
        container.addView(closeButton, closeParams)

        overlayView = container

        // Touch listener for dragging
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
                            Log.e(TAG, "Error updating layout", e)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Add to window
        try {
            windowManager.addView(overlayView, layoutParams)
            startCamera()
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
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
                Log.e(TAG, "Error getting camera provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView?.surfaceProvider)

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
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
            Log.e(TAG, "Error stopping camera lifecycle", e)
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
