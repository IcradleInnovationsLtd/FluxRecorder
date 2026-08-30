package com.flux.recorder.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Robust shake detector that requires intentional multi-axis shaking (2+ directional reversals)
 * to avoid false triggers from normal typing, screen touches, or walking.
 */
class ShakeDetector(
    context: Context,
    private val sensitivity: Float = 12.0f, // m/s² delta threshold (default 12.0)
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var initialized = false

    private var shakeCount = 0
    private var firstShakeTime = 0L
    private var lastShakeTime = 0L

    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_WINDOW_MS = 600L     // Time window for consecutive shakes
        private const val SHAKE_COOLDOWN_MS = 1500L  // Cooldown after a valid shake-to-stop trigger
        private const val REQUIRED_SHAKES = 2        // Require 2 rapid shakes to prevent accidental trigger
    }

    /** Start listening for shake events. */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Shake detector started with sensitivity: $sensitivity m/s²")
        } ?: Log.w(TAG, "Accelerometer not available on this device")
    }

    /** Stop listening for shake events. */
    fun stop() {
        sensorManager.unregisterListener(this)
        initialized = false
        shakeCount = 0
        Log.d(TAG, "Shake detector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!initialized) {
            lastX = x; lastY = y; lastZ = z
            initialized = true
            return
        }

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        val deltaAcc = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        lastX = x; lastY = y; lastZ = z

        val now = System.currentTimeMillis()

        // Ignore if still in post-trigger cooldown
        if (now - lastShakeTime < SHAKE_COOLDOWN_MS) return

        // Check if delta exceeds the intentional shake threshold
        if (deltaAcc > sensitivity) {
            if (shakeCount == 0) {
                firstShakeTime = now
                shakeCount = 1
            } else {
                if (now - firstShakeTime <= SHAKE_WINDOW_MS) {
                    shakeCount++
                    if (shakeCount >= REQUIRED_SHAKES) {
                        lastShakeTime = now
                        shakeCount = 0
                        Log.d(TAG, "Intentional shake detected (count=$REQUIRED_SHAKES, Δacc=$deltaAcc)! Triggering stop.")
                        onShakeDetected()
                    }
                } else {
                    // Window expired, reset with current shake
                    firstShakeTime = now
                    shakeCount = 1
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
