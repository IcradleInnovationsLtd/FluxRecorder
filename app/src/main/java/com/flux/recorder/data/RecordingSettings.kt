package com.flux.recorder.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Video quality/resolution options
 */
enum class VideoQuality(val width: Int, val height: Int, val displayName: String) {
    QUALITY_360P(640, 360, "360p (Low)"),
    QUALITY_480P(854, 480, "480p (SD)"),
    QUALITY_720P(1280, 720, "720p (HD)"),
    QUALITY_1080P(1920, 1080, "1080p (Full HD)"),
    QUALITY_2K(2560, 1440, "2K (1440p)"),
    QUALITY_4K(3840, 2160, "4K (2160p)")
}

/**
 * Frame rate options
 */
enum class FrameRate(val fps: Int, val displayName: String) {
    FPS_30(30, "30 FPS"),
    FPS_60(60, "60 FPS"),
    FPS_90(90, "90 FPS")
}

/**
 * Audio source configuration
 */
enum class AudioSource(val displayName: String) {
    NONE("No Audio"),
    INTERNAL("Internal Audio"),
    MICROPHONE("Microphone"),
    BOTH("Both")
}

/**
 * Recording configuration settings
 */
@Parcelize
data class RecordingSettings(
    val videoQuality: VideoQuality = VideoQuality.QUALITY_1080P,
    val frameRate: FrameRate = FrameRate.FPS_60,
    val audioSource: AudioSource = AudioSource.BOTH,
    val enableFacecam: Boolean = false,
    val enableShakeToStop: Boolean = true,
    val shakeSensitivity: Float = 12.0f
) : Parcelable {
    /**
     * Calculate optimal bitrate based on resolution and frame rate.
     * Formula: width × height × fps × motion_factor × 0.12 (balanced quality/size for VBR)
     */
    fun calculateBitrate(): Int {
        val pixels = videoQuality.width * videoQuality.height
        val motionFactor = 1.5f // Assume medium motion
        val qualityFactor = 0.12f // Balanced for VBR (High Quality, smaller size)
        return (pixels * frameRate.fps * motionFactor * qualityFactor).toInt()
    }
}
