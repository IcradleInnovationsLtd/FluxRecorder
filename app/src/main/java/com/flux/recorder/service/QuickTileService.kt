package com.flux.recorder.service

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.flux.recorder.MainActivity

/**
 * Quick Settings Tile for instant recording access.
 *
 * Because MediaProjection permission must be granted interactively, we can't start recording
 * directly from the tile — we launch MainActivity instead. The tile state reflects whether a
 * recording is currently active by reading a SharedPreference written by RecorderService.
 */
class QuickTileService : TileService() {

    companion object {
        private const val TAG = "QuickTileService"
        const val ACTION_TOGGLE_RECORDING = "com.flux.recorder.TOGGLE_RECORDING"
    }

    override fun onStartListening() {
        super.onStartListening()
        val isRecording = getSharedPreferences(RecorderService.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(RecorderService.PREF_IS_RECORDING, false)
        updateTile(isRecording)
    }

    override fun onClick() {
        super.onClick()
        Log.d(TAG, "Quick tile clicked — launching MainActivity")

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TOGGLE_RECORDING
            flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Use PendingIntent overload (required on Android 14+, safe on all versions)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startActivityAndCollapse(pendingIntent)
    }

    private fun updateTile(isRecording: Boolean) {
        qsTile?.apply {
            state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isRecording) "Stop Recording" else "Start Recording"
            updateTile()
        }
    }
}
