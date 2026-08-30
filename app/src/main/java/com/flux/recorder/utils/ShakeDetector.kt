package com.flux.recorder.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Detects shake gestures using accelerometer.
 * The sensitivity parameter is used directly as the delta-acceleration threshold (m/s²).
 * Default 2.5 m/s² means the phone must change acceleration by 2.5 m/s² between samples.
 */
class ShakeDetector(
    context: Context,
    private val sensitivity: Float = 2.5f, // delta m/s² threshold between consecutive samples
    private val onShakeDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var initialized = false

    // Minimum ms between consecutive shake events to avoid rapid-fire callbacks
    private var lastShakeTime: Long = 0

    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_COOLDOWN_MS = 500L
    }

    /** Start listening for shake events. */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Shake detector started with sensitivity: $sensitivity m/s²")
        } ?: Log.w(TAG, "Accelerometer not available on this device")
    }

    /** Stop listening for shake events. */
    fun stop() {
        sensorManager.unregisterListener(this)
        initialized = false
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

        // Euclidean magnitude of the acceleration change vector
        val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        lastX = x; lastY = y; lastZ = z

        if (acceleration > sensitivity) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now
                Log.d(TAG, "Shake detected! Δacceleration = $acceleration m/s² (threshold $sensitivity)")
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
