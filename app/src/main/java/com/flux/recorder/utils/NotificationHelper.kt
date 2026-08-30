package com.flux.recorder.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flux.recorder.MainActivity
import com.flux.recorder.R
import com.flux.recorder.service.RecorderService

/**
 * Helper class for creating and managing recording notifications with rich action buttons.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001

        private const val CHANNEL_NAME = "Screen Recording"
        private const val CHANNEL_DESC = "Shows screen recording controls and status"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESC
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create recording notification with Pause/Resume and Stop action buttons.
     */
    fun createRecordingNotification(
        title: String,
        message: String,
        isRecording: Boolean = true
    ): android.app.Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Action: Pause or Resume
        if (isRecording) {
            val pauseIntent = Intent(context, RecorderService::class.java).apply {
                action = RecorderService.ACTION_PAUSE_RECORDING
            }
            val pausePendingIntent = PendingIntent.getService(
                context,
                101,
                pauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
        } else {
            val resumeIntent = Intent(context, RecorderService::class.java).apply {
                action = RecorderService.ACTION_RESUME_RECORDING
            }
            val resumePendingIntent = PendingIntent.getService(
                context,
                102,
                resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            )
        }

        // Action: Stop
        val stopIntent = Intent(context, RecorderService::class.java).apply {
            action = RecorderService.ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            103,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopPendingIntent
        )

        return builder.build()
    }

    fun updateNotification(notification: android.app.Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
