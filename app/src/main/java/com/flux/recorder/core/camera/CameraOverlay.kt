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
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton

/**
 * High-performance, hardware-accelerated native Camera2 facecam overlay.
 * Uses exact supported sensor resolutions, dedicated background handler thread,
 * and hardware-accelerated overlay window flags to guarantee continuous 30/60fps preview.
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

    private var selectedCameraId: String? = null
    private var optimalPreviewSize: Size = Size(640, 480)

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

        // Layout params for floating window with explicit hardware acceleration
        layoutParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
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
                cornerRadius = 20f * density
                setColor(Color.parseColor("#E60D0D0D"))
                setStroke((2f * density).toInt(), Color.parseColor("#4D00E5FF"))
            }
            background = shape
            clipToOutline = true
        }

        // TextureView for hardware-accelerated Camera2 preview
        textureView = TextureView(context).apply {
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture available: ${width}x$height, opening Camera2")
                    backgroundHandler?.post { openCamera() }
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    Log.d(TAG, "SurfaceTexture destroyed, closing Camera2")
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
            selectedCameraId = frontCameraId

            // Query supported resolutions for SurfaceTexture to match hardware capabilities
            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val supportedSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()

            // Pick 640x480 or closest matching standard supported resolution
            optimalPreviewSize = supportedSizes.firstOrNull { (it.width == 640 && it.height == 480) || (it.width == 480 && it.height == 640) }
                ?: supportedSizes.filter { it.width <= 1280 && it.height <= 720 }.minByOrNull { it.width * it.height }
                ?: supportedSizes.firstOrNull()
                ?: Size(640, 480)

            Log.d(TAG, "Selected optimal Camera2 preview size: ${optimalPreviewSize.width}x${optimalPreviewSize.height}")

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera2 device opened: ${camera.id}")
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera2 device disconnected: ${camera.id}")
                    closeCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera2 device error code: $error on camera ${camera.id}")
                    closeCamera()
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
        val handler = backgroundHandler ?: return

        try {
            // Set hardware buffer size from the supported output size
            texture.setDefaultBufferSize(optimalPreviewSize.width, optimalPreviewSize.height)
            val surface = Surface(texture)

            val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }

            @Suppress("DEPRECATION")
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            // Set repeating request on the background handler
                            session.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {
                                    override fun onCaptureFailed(
                                        session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        failure: CaptureFailure
                                    ) {
                                        Log.w(TAG, "Camera2 capture frame failed: reason=${failure.reason}")
                                    }
                                },
                                backgroundHandler
                            )
                            Log.d(TAG, "Camera2 repeating preview request running smoothly on background thread")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating preview request", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Camera2 preview session configuration failed")
                    }
                },
                handler
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create camera preview session", e)
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null
            Log.d(TAG, "Camera2 resources successfully closed")
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
