package com.flux.recorder.core.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/**
 * Hardware-accelerated H.264/AVC video encoder using MediaCodec
 */
class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val bitrate: Int,
    private val frameRate: Int
) {
    private var mediaCodec: MediaCodec? = null
    private var inputSurface: Surface? = null
    
    companion object {
        private const val TAG = "VideoEncoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
        private const val I_FRAME_INTERVAL = 2 // I-frame every 2 seconds (Better for size)
        private const val TIMEOUT_US = 2000L // 2ms non-blocking drain timeout
    }
    
    /**
     * Initialize the encoder
     */
    fun prepare(): Surface? {
        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000L / frameRate)
            }

            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            try {
                mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (e: Exception) {
                Log.w(TAG, "VBR configuration failed, falling back to default bitrate mode", e)
                format.removeKey(MediaFormat.KEY_BITRATE_MODE)
                mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            inputSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            Log.d(TAG, "Video encoder initialized: ${width}x${height} @ ${frameRate}fps, ${bitrate}bps")
            return inputSurface

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize video encoder", e)
            release()
            return null
        }
    }
    
    sealed interface EncoderOutput {
        data class Data(val buffer: ByteBuffer, val info: MediaCodec.BufferInfo, val index: Int) : EncoderOutput
        object FormatChanged : EncoderOutput
        object TryAgain : EncoderOutput
    }

    /**
     * Get encoded data
     * @return EncoderOutput result
     */
    fun getEncodedData(): EncoderOutput {
        val codec = mediaCodec ?: return EncoderOutput.TryAgain
        
        val bufferInfo = MediaCodec.BufferInfo()
        val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        
        return when {
            outputBufferIndex >= 0 -> {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null) {
                    EncoderOutput.Data(outputBuffer, bufferInfo, outputBufferIndex)
                } else {
                    EncoderOutput.TryAgain
                }
            }
            outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                EncoderOutput.FormatChanged
            }
            outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                EncoderOutput.TryAgain
            }
            else -> EncoderOutput.TryAgain
        }
    }
    
    /**
     * Release output buffer after processing
     */
    fun releaseOutputBuffer(index: Int) {
        mediaCodec?.releaseOutputBuffer(index, false)
    }
    
    /**
     * Signal end of stream
     */
    fun signalEndOfStream() {
        try {
            mediaCodec?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.e(TAG, "Error signaling end of stream", e)
        }
    }
    
    /**
     * Get output format (call after first buffer is dequeued)
     */
    fun getOutputFormat(): MediaFormat? {
        return mediaCodec?.outputFormat
    }
    
    /**
     * Release encoder resources
     */
    fun release() {
        try {
            // Stop and release the codec FIRST (it holds a reference to the surface)
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null

            // Then release the surface
            inputSurface?.release()
            inputSurface = null

            Log.d(TAG, "Video encoder released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing video encoder", e)
        }
    }
    
    /**
     * Check if encoder is active
     */
    fun isActive(): Boolean {
        return mediaCodec != null
    }
}
