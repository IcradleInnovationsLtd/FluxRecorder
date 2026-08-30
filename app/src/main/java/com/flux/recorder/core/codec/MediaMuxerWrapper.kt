package com.flux.recorder.core.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Combines video and audio tracks into an MP4 container.
 *
 * Thread-safety: [writeVideoSample] and [writeAudioSample] may be called from different coroutines
 * simultaneously. A lock is used to guard all [MediaMuxer] access.
 */
class MediaMuxerWrapper(private val outputFile: File) {

    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false
    private var videoFormatReceived = false
    private var audioFormatReceived = false
    private var expectAudio = false

    /** Guards all MediaMuxer access to prevent concurrent write crashes. */
    private val lock = Any()

    /** Timestamp (ms) when the video format was received — used to time out audio track wait. */
    private var videoFormatReceivedTimeMs = 0L

    companion object {
        private const val TAG = "MediaMuxerWrapper"
        private const val AUDIO_TRACK_TIMEOUT_MS = 3000L
    }

    /** Initialize the muxer. */
    fun prepare() {
        try {
            mediaMuxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            Log.d(TAG, "MediaMuxer initialized: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaMuxer", e)
            throw e
        }
    }

    /** Set whether an audio track is expected. Must be called before [addVideoTrack]. */
    fun setAudioExpected(expected: Boolean) {
        expectAudio = expected
    }

    /** Add the video track. Starts the muxer if audio is not expected or already added. */
    fun addVideoTrack(format: MediaFormat): Int {
        synchronized(lock) {
            val muxer = mediaMuxer ?: throw IllegalStateException("Muxer not initialized")
            videoTrackIndex = muxer.addTrack(format)
            videoFormatReceived = true
            videoFormatReceivedTimeMs = System.currentTimeMillis()
            Log.d(TAG, "Video track added: $videoTrackIndex")
            tryStartMuxerLocked()
            return videoTrackIndex
        }
    }

    /** Add the audio track. Starts the muxer once both tracks are ready. */
    fun addAudioTrack(format: MediaFormat): Int {
        synchronized(lock) {
            val muxer = mediaMuxer ?: throw IllegalStateException("Muxer not initialized")
            audioTrackIndex = muxer.addTrack(format)
            audioFormatReceived = true
            Log.d(TAG, "Audio track added: $audioTrackIndex")
            tryStartMuxerLocked()
            return audioTrackIndex
        }
    }

    /**
     * Check if the muxer should start. If audio was expected but hasn't arrived within the
     * timeout window, start video-only to avoid blocking indefinitely.
     *
     * Must be called while holding [lock].
     */
    private fun tryStartMuxerLocked() {
        if (isMuxerStarted || !videoFormatReceived) return

        val audioReady = !expectAudio || audioFormatReceived
        val audioTimedOut = expectAudio && !audioFormatReceived &&
                videoFormatReceivedTimeMs > 0 &&
                (System.currentTimeMillis() - videoFormatReceivedTimeMs) > AUDIO_TRACK_TIMEOUT_MS

        if (audioReady || audioTimedOut) {
            if (audioTimedOut) {
                Log.w(TAG, "Audio track timed out — starting muxer with video only")
            }
            mediaMuxer?.start()
            isMuxerStarted = true
            Log.d(TAG, "MediaMuxer started (video=$videoTrackIndex, audio=$audioTrackIndex)")
        }
    }

    /**
     * Poll this occasionally (e.g., from the recording loop) so the audio-timeout path can
     * trigger even if [addAudioTrack] is never called.
     */
    fun checkAudioTimeout() {
        synchronized(lock) { tryStartMuxerLocked() }
    }

    /** Write a video sample. No-op if the muxer has not started yet. */
    fun writeVideoSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!isMuxerStarted) {
                Log.w(TAG, "Muxer not started, dropping video sample")
                return
            }
            if (videoTrackIndex < 0) return
            try {
                mediaMuxer?.writeSampleData(videoTrackIndex, buffer, bufferInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing video sample", e)
            }
        }
    }

    /** Write an audio sample. No-op if the muxer has not started or audio track is absent. */
    fun writeAudioSample(buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(lock) {
            if (!isMuxerStarted) {
                Log.w(TAG, "Muxer not started, dropping audio sample")
                return
            }
            if (audioTrackIndex < 0) return
            try {
                mediaMuxer?.writeSampleData(audioTrackIndex, buffer, bufferInfo)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing audio sample", e)
            }
        }
    }

    /** Stop and release the muxer. */
    fun release() {
        synchronized(lock) {
            try {
                if (isMuxerStarted) {
                    mediaMuxer?.stop()
                    isMuxerStarted = false
                }
                mediaMuxer?.release()
                mediaMuxer = null
                Log.d(TAG, "MediaMuxer released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMuxer", e)
            }
        }
    }

    fun isStarted(): Boolean = synchronized(lock) { isMuxerStarted }
}
