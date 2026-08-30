package com.flux.recorder.core.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton

/**
 * Rock-solid native Camera2 floating facecam overlay.
 * Uses direct Camera2 API with TextureView to completely bypass Lifecycle limitations
 * in background services, guaranteeing continuous, zero-freeze camera preview on all devices.
 */
class CameraOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var overlayView: View? = null
    private var textureView: TextureView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    companion object {
        private const val TAG = "CameraOverlay"
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show camera overlay: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        startBackgroundThread()

        val density = context.resources.displayMetrics.density
        val widthPx = (115 * density).toInt()
        val heightPx = (150 * density).toInt()

        // Layout params for floating window
        layoutParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (20 * density).toInt()
            y = (80 * density).toInt()
        }

        // Modern rounded container with glass border
        val container = FrameLayout(context).apply {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density / 2.5f
                setColor(Color.parseColor("#E60D0D0D"))
                setStroke((2.5f * density).toInt(), Color.parseColor("#4D00E5FF"))
            }
            background = shape
            clipToOutline = true
        }

        // TextureView for hardware-accelerated Camera2 preview
        textureView = TextureView(context).apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture available: ${width}x$height, opening camera")
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }
        }
        container.addView(
            textureView,
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
        val btnSizePx = (28 * density).toInt()
        val closeParams = FrameLayout.LayoutParams(btnSizePx, btnSizePx).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, (6 * density).toInt(), (6 * density).toInt(), 0)
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

        // Add to window
        try {
            windowManager.addView(overlayView, layoutParams)
            Log.d(TAG, "CameraOverlay view added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add camera overlay to WindowManager", e)
            overlayView = null
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            val frontCameraId = getFrontFacingCameraId() ?: cameraManager.cameraIdList.firstOrNull()
            if (frontCameraId == null) {
                Log.e(TAG, "No camera found on this device")
                return
            }

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera2 device opened: ${camera.id}")
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera2 device disconnected: ${camera.id}")
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera2 device error: $error on camera ${camera.id}")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera via Camera2", e)
        }
    }

    private fun getFrontFacingCameraId(): String? {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return id
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding front camera", e)
        }
        return null
    }

    private fun createCameraPreviewSession() {
        val device = cameraDevice ?: return
        val texture = textureView?.surfaceTexture ?: return

        try {
            // Configure lightweight 480x640 buffer size for the TextureView
            texture.setDefaultBufferSize(480, 640)
            val surface = Surface(texture)

            val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                // Continuous auto-focus & auto-exposure
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outputConfig = android.hardware.camera2.params.OutputConfiguration(surface)
                val sessionConfig = android.hardware.camera2.params.SessionConfiguration(
                    android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfig),
                    context.mainExecutor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            captureSession = session
                            try {
                                session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                                Log.d(TAG, "Camera2 preview session started successfully (API 28+)")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start repeating preview request", e)
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Camera2 preview session configuration failed")
                        }
                    }
                )
                device.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                            Log.d(TAG, "Camera2 preview session started successfully (legacy)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating preview request", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Camera2 preview session configuration failed")
                    }
                }, backgroundHandler)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create camera preview session", e)
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null
            Log.d(TAG, "Camera2 resources closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Camera2", e)
        }
    }

    fun stop() {
        closeCamera()
        stopBackgroundThread()

        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
                Log.d(TAG, "CameraOverlay view removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
            textureView = null
        }
    }
}
