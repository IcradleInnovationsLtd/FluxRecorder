package com.flux.recorder.core.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.DefaultVideoFrameProcessor
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import com.flux.recorder.utils.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

/**
 * Aspect Ratio Presets for spatial video cropping
 */
enum class CropAspectRatio(val displayName: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    RATIO_1_1("1:1 Square", 1.0f),
    RATIO_9_16("9:16 TikTok/Shorts", 9f / 16f),
    RATIO_16_9("16:9 YouTube", 16f / 9f),
    RATIO_4_5("4:5 Feed", 4f / 5f)
}

/**
 * High-performance, hardware-accelerated Spatial Video Cropper & Trimmer.
 * Powered by AndroidX Media3 Transformer, OpenGL ES FrameProcessor, and Hardware MediaCodec.
 */
@OptIn(UnstableApi::class)
object VideoCropper {

    private const val TAG = "VideoCropper"

    /**
     * Crop and trim a video with hardware acceleration and automatic encoder fallback.
     * @param context Android Context
     * @param sourceUri Video source Uri
     * @param cropLeft Normalized left bound (0.0 to 1.0)
     * @param cropTop Normalized top bound (0.0 to 1.0)
     * @param cropRight Normalized right bound (0.0 to 1.0)
     * @param cropBottom Normalized bottom bound (0.0 to 1.0)
     * @param startMs Start time in milliseconds
     * @param endMs End time in milliseconds
     * @param onProgress Progress callback (0.0 to 1.0)
     */
    suspend fun cropAndTrimVideo(
        context: Context,
        sourceUri: Uri,
        cropLeft: Float = 0f,
        cropTop: Float = 0f,
        cropRight: Float = 1f,
        cropBottom: Float = 1f,
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        val fileManager = FileManager(context)
        val tempDir = fileManager.getRecordingsDirectory()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempOutputFile = File(tempDir, "FluxRec_CROP_${timestamp}.mp4")

        try {
            Log.d(TAG, "Starting crop: [L=$cropLeft, T=$cropTop, R=$cropRight, B=$cropBottom], time: ${startMs}ms - ${endMs}ms")
            onProgress(0.05f)

            // 1. Inspect source video dimensions and rotation
            var sourceWidth = 1080
            var sourceHeight = 1920
            var rotation = 0

            try {
                val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                pfd?.use {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(it.fileDescriptor)
                    val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    rotation = rotStr?.toIntOrNull() ?: 0
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
                    if (rotation == 90 || rotation == 270) {
                        sourceWidth = h
                        sourceHeight = w
                    } else {
                        sourceWidth = w
                        sourceHeight = h
                    }
                    retriever.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not extract video metadata, using defaults", e)
            }

            // 2. Convert normalized (0..1) coordinates to OpenGL NDC coordinates (-1..1)
            // Left/Right: 0 -> -1, 1 -> +1
            // Top/Bottom: 0 -> +1 (top), 1 -> -1 (bottom)
            val glLeft = (cropLeft * 2f - 1f).coerceIn(-1f, 1f)
            val glRight = (cropRight * 2f - 1f).coerceIn(-1f, 1f)
            val glTop = (1f - cropTop * 2f).coerceIn(-1f, 1f)
            val glBottom = (1f - cropBottom * 2f).coerceIn(-1f, 1f)

            val isFullCrop = cropLeft <= 0.01f && cropTop <= 0.01f && cropRight >= 0.99f && cropBottom >= 0.99f

            // Calculate even target output resolution for encoder compliance
            val rawCropWidth = ((cropRight - cropLeft) * sourceWidth).toInt()
            val rawCropHeight = ((cropBottom - cropTop) * sourceHeight).toInt()
            val evenWidth = (rawCropWidth / 2 * 2).coerceAtLeast(160)
            val evenHeight = (rawCropHeight / 2 * 2).coerceAtLeast(160)

            Log.d(TAG, "Cropped target resolution: ${evenWidth}x${evenHeight} (source: ${sourceWidth}x${sourceHeight})")

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(sourceUri)

            if (startMs > 0 || (endMs > 0 && endMs < Long.MAX_VALUE)) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                if (startMs > 0) clipBuilder.setStartPositionMs(startMs)
                if (endMs > 0 && endMs < Long.MAX_VALUE) clipBuilder.setEndPositionMs(endMs)
                mediaItemBuilder.setClippingConfiguration(clipBuilder.build())
            }

            val mediaItem = mediaItemBuilder.build()

            val effectsList = mutableListOf<Effect>()
            if (!isFullCrop) {
                // Apply spatial crop
                effectsList.add(Crop(glLeft, glRight, glBottom, glTop))
                // Ensure output container resolution fits the cropped aspect ratio cleanly
                effectsList.add(Presentation.createForWidthAndHeight(evenWidth, evenHeight, Presentation.LAYOUT_SCALE_TO_FIT))
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(emptyList(), effectsList))
                .build()

            return@withContext suspendCancellableCoroutine { continuation ->
                var transformer: Transformer? = null

                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Log.d(TAG, "Video crop & trim export completed successfully: ${tempOutputFile.length() / 1024} KB")
                        onProgress(1.0f)

                        // Copy to public gallery and scan
                        fileManager.copyToPublicGallery(tempOutputFile)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(tempOutputFile.absolutePath),
                            arrayOf("video/mp4"),
                            null
                        )

                        if (continuation.isActive) {
                            continuation.resume(Result.success(tempOutputFile))
                        }
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        Log.e(TAG, "Transformer crop export error", exportException)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exportException))
                        }
                    }
                }

                transformer = Transformer.Builder(context)
                    .setVideoFrameProcessorFactory(DefaultVideoFrameProcessor.Factory.Builder().build())
                    .setEncoderFactory(DefaultEncoderFactory.Builder(context).setEnableFallback(true).build())
                    .addListener(listener)
                    .build()

                continuation.invokeOnCancellation {
                    try { transformer.cancel() } catch (e: Exception) {}
                }

                transformer.start(editedMediaItem, tempOutputFile.absolutePath)

                // Polling progress
                val progressHolder = ProgressHolder()
                val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                val progressRunnable = object : Runnable {
                    override fun run() {
                        if (continuation.isActive) {
                            val state = transformer.getProgress(progressHolder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(progressHolder.progress / 100f)
                            }
                            mainHandler.postDelayed(this, 150)
                        }
                    }
                }
                mainHandler.post(progressRunnable)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop video", e)
            Result.failure(e)
        }
    }
}
