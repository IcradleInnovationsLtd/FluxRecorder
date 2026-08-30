package com.flux.recorder.service

import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.flux.recorder.core.audio.AudioRecorder
import com.flux.recorder.core.codec.AudioEncoder
import com.flux.recorder.core.codec.MediaMuxerWrapper
import com.flux.recorder.core.codec.VideoEncoder
import com.flux.recorder.core.projection.ScreenCaptureManager
import com.flux.recorder.data.AudioSource
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.RecordingState
import com.flux.recorder.utils.FileManager
import com.flux.recorder.utils.NotificationHelper
import com.flux.recorder.utils.ShakeDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Foreground service that manages the screen recording process.
 */
@AndroidEntryPoint
class RecorderService : Service() {

    @Inject
    lateinit var fileManager: FileManager

    private val binder = RecorderBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var screenCaptureManager: ScreenCaptureManager

    private var videoEncoder: VideoEncoder? = null
    private var audioEncoder: AudioEncoder? = null
    private var audioRecorder: AudioRecorder? = null

    private var muxer: MediaMuxerWrapper? = null
    private var outputFile: File? = null
    private var recordingJob: Job? = null
    private var audioJob: Job? = null
    private var shakeDetector: ShakeDetector? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var startTime: Long = 0
    private var pausedDuration: Long = 0
    private var pauseStartTime: Long = 0

    companion object {
        private const val TAG = "RecorderService"
        const val ACTION_START_RECORDING = "com.flux.recorder.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.flux.recorder.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.flux.recorder.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.flux.recorder.RESUME_RECORDING"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_SETTINGS = "settings"

        // SharedPreferences key used by QuickTileService to read recording state
        const val PREF_IS_RECORDING = "is_recording"
        const val PREFS_NAME = "flux_recorder_tile_prefs"
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        screenCaptureManager = ScreenCaptureManager(this)
        Log.d(TAG, "RecorderService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                val settings = intent.getParcelableExtra<RecordingSettings>(EXTRA_SETTINGS)

                if (resultData != null && settings != null) {
                    startRecording(resultCode, resultData, settings)
                }
            }
            ACTION_STOP_RECORDING -> {
                // Must run off the main thread (avoids Thread.sleep ANR)
                serviceScope.launch(Dispatchers.IO) { stopRecording() }
            }
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startRecording(resultCode: Int, data: Intent, settings: RecordingSettings) {
        if (_recordingState.value !is RecordingState.Idle) {
            Log.w(TAG, "Recording already in progress")
            return
        }

        try {
            // Start foreground service
            val notification = notificationHelper.createRecordingNotification(
                "Recording",
                "Screen recording in progress..."
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                var foregroundServiceType =
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    foregroundServiceType =
                        foregroundServiceType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NotificationHelper.NOTIFICATION_ID, notification, foregroundServiceType)
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }

            // Initialize MediaProjection
            if (!screenCaptureManager.initializeProjection(resultCode, data)) {
                _recordingState.value = RecordingState.Error("Failed to initialize screen capture")
                stopSelf()
                return
            }

            // Create output file
            outputFile = fileManager.createRecordingFile()

            // Calculate dimensions based on orientation
            var width = settings.videoQuality.width
            var height = settings.videoQuality.height

            val (screenWidth, screenHeight) = screenCaptureManager.getScreenDimensions()
            val isScreenPortrait = screenHeight > screenWidth
            val isSettingPortrait = height > width

            if (isScreenPortrait && !isSettingPortrait) {
                val temp = width; width = height; height = temp
            } else if (!isScreenPortrait && isSettingPortrait) {
                val temp = width; width = height; height = temp
            }

            // Initialize video encoder
            val bitrate = settings.calculateBitrate()
            videoEncoder = VideoEncoder(width, height, bitrate, settings.frameRate.fps)

            val surface = videoEncoder?.prepare()
            if (surface == null) {
                _recordingState.value = RecordingState.Error("Failed to initialize encoder")
                stopSelf()
                return
            }

            // Create virtual display
            val virtualDisplay = screenCaptureManager.createVirtualDisplay(
                surface, width, height, screenCaptureManager.getScreenDensity()
            )
            if (virtualDisplay == null) {
                _recordingState.value = RecordingState.Error("Failed to create virtual display")
                stopSelf()
                return
            }

            // Setup Audio Encoder & Recorder
            var audioEnabled = false
            Log.d(TAG, "Audio Source Setting: ${settings.audioSource}")

            if (settings.audioSource != AudioSource.NONE) {
                Log.d(TAG, "Initializing audio encoder and recorder...")
                audioEncoder = AudioEncoder()
                audioEncoder?.prepare()

                audioRecorder = AudioRecorder()
                val success = audioRecorder?.start(
                    screenCaptureManager.getMediaProjection(),
                    settings.audioSource
                ) ?: false

                if (success) {
                    audioEnabled = true
                    Log.d(TAG, "✅ Audio recording enabled: ${settings.audioSource}")
                } else {
                    Log.e(TAG, "❌ Failed to start audio recorder for: ${settings.audioSource}")
                    audioEncoder?.release()
                    audioEncoder = null
                }
            } else {
                Log.w(TAG, "⚠️ Audio source is NONE — no audio will be recorded")
            }

            // Initialize muxer
            muxer = MediaMuxerWrapper(outputFile!!).apply {
                prepare()
                setAudioExpected(audioEnabled)
            }

            // Mark recording state and persist for QuickTile
            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(0)
            setTileRecordingState(true)

            // Start recording loops
            recordingJob = serviceScope.launch { recordingLoop() }
            if (audioEnabled) {
                audioJob = serviceScope.launch(Dispatchers.IO) { audioLoop() }
            }

            Log.d(TAG, "Recording started")

            // Always show the floating control overlay; pass camera preference
            val floatingIntent = Intent(this, FloatingControlService::class.java).apply {
                putExtra(FloatingControlService.EXTRA_ENABLE_CAMERA, settings.enableFacecam)
            }
            startService(floatingIntent)

            // Start shake detector if enabled
            if (settings.enableShakeToStop) {
                shakeDetector = ShakeDetector(
                    context = this,
                    sensitivity = settings.shakeSensitivity
                ) {
                    Log.d(TAG, "Shake detected — stopping recording")
                    serviceScope.launch(Dispatchers.IO) { stopRecording() }
                }
                shakeDetector?.start()
                Log.d(TAG, "Shake-to-stop enabled with sensitivity: ${settings.shakeSensitivity}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            stopSelf()
        }
    }

    /**
     * Recording loop — keeps running while Recording OR Paused.
     * When paused: drains the encoder output and discards it (prevents buffer overflow)
     *              without writing to the muxer, so the video is seamlessly resumed.
     */
    private suspend fun recordingLoop() {
        var videoTrackAdded = false

        while (coroutineContext.isActive) {
            val currentState = _recordingState.value

            // Only exit on terminal states
            if (currentState is RecordingState.Idle || currentState is RecordingState.Error) break

            val isPaused = currentState is RecordingState.Paused

            try {
                val output = videoEncoder?.getEncodedData() ?: VideoEncoder.EncoderOutput.TryAgain

                when (output) {
                    is VideoEncoder.EncoderOutput.FormatChanged -> {
                        val format = videoEncoder?.getOutputFormat()
                        if (format != null && !videoTrackAdded) {
                            muxer?.addVideoTrack(format)
                            videoTrackAdded = true
                            Log.d(TAG, "Video track added to muxer")
                        }
                    }
                    is VideoEncoder.EncoderOutput.Data -> {
                        val (buffer, bufferInfo, bufferIndex) = output

                        // Write to muxer only when not paused and track is ready
                        if (videoTrackAdded && !isPaused &&
                            (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            muxer?.writeVideoSample(buffer, bufferInfo)
                        }
                        videoEncoder?.releaseOutputBuffer(bufferIndex)
                    }
                    is VideoEncoder.EncoderOutput.TryAgain -> { /* nothing yet */ }
                }

                // Update timer only while actively recording
                if (!isPaused) {
                    val currentDuration = System.currentTimeMillis() - startTime - pausedDuration
                    _recordingState.value = RecordingState.Recording(currentDuration)
                }

                delay(10)

            } catch (e: Exception) {
                Log.e(TAG, "Error in recording loop", e)
                break
            }
        }
    }

    /**
     * Audio loop — same pause logic as recordingLoop.
     */
    private suspend fun audioLoop() {
        var audioTrackAdded = false
        val bufferSize = audioRecorder?.getBufferSize() ?: 4096
        val audioBuffer = ByteArray(bufferSize)
        var readCount = 0

        Log.d(TAG, "🎤 Starting audio loop with buffer size: $bufferSize")

        while (coroutineContext.isActive) {
            val currentState = _recordingState.value

            // Only exit on terminal states
            if (currentState is RecordingState.Idle || currentState is RecordingState.Error) break

            val isPaused = currentState is RecordingState.Paused

            // When paused, skip reading to avoid saturating the audio buffer
            if (isPaused) {
                delay(50)
                continue
            }

            try {
                val readResult = audioRecorder?.read(audioBuffer, bufferSize) ?: -1

                if (readResult > 0) {
                    readCount++
                    if (readCount % 100 == 0) {
                        Log.d(TAG, "🎤 Audio read count: $readCount, bytes: $readResult")
                    }

                    val timestampUs = System.nanoTime() / 1000
                    audioEncoder?.encode(audioBuffer, readResult, timestampUs)

                    // Drain encoded output
                    var outputAvailable = true
                    while (outputAvailable) {
                        val output = audioEncoder?.getEncodedData() ?: AudioEncoder.Output.TryAgain
                        when (output) {
                            is AudioEncoder.Output.Data -> {
                                // Filter codec config frames — writing them as audio data corrupts the file
                                val isConfig = (output.info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                                if (output.buffer != null && output.info.size > 0 && audioTrackAdded && !isConfig) {
                                    muxer?.writeAudioSample(output.buffer, output.info)
                                }
                                audioEncoder?.releaseOutputBuffer(output.index)
                            }
                            is AudioEncoder.Output.FormatChanged -> {
                                val format = audioEncoder?.getOutputFormat()
                                if (format != null && !audioTrackAdded) {
                                    muxer?.addAudioTrack(format)
                                    audioTrackAdded = true
                                    Log.d(TAG, "✅ Audio track added to muxer")
                                }
                            }
                            is AudioEncoder.Output.TryAgain -> {
                                outputAvailable = false
                            }
                        }
                    }
                } else {
                    if (readCount == 0 && readResult == -1) {
                        Log.e(TAG, "❌ Audio recorder returning -1 (no data)")
                    }
                    delay(5)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in audio loop", e)
                break
            }
        }

        Log.d(TAG, "🎤 Audio loop ended. Total reads: $readCount")
    }

    private fun pauseRecording() {
        val currentState = _recordingState.value
        if (currentState is RecordingState.Recording) {
            pauseStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Paused(currentState.durationMs)

            val notification = notificationHelper.createRecordingNotification(
                "Recording Paused",
                "Tap to resume",
                isRecording = false
            )
            notificationHelper.updateNotification(notification)
        }
    }

    private fun resumeRecording() {
        val currentState = _recordingState.value
        if (currentState is RecordingState.Paused) {
            pausedDuration += System.currentTimeMillis() - pauseStartTime
            _recordingState.value = RecordingState.Recording(currentState.durationMs)

            val notification = notificationHelper.createRecordingNotification(
                "Recording",
                "Screen recording in progress..."
            )
            notificationHelper.updateNotification(notification)
        }
    }

    /**
     * Must be called from a background thread (not main thread) to avoid ANR.
     */
    private fun stopRecording() {
        Log.d(TAG, "Stopping recording")

        // Cancel recording jobs
        recordingJob?.cancel()
        recordingJob = null

        audioJob?.cancel()
        audioJob = null

        // Signal end of stream and wait briefly for last frames
        videoEncoder?.signalEndOfStream()
        Thread.sleep(150)

        // Release resources in order
        muxer?.release()
        muxer = null

        videoEncoder?.release()
        videoEncoder = null

        audioRecorder?.stop()
        audioRecorder = null

        audioEncoder?.release()
        audioEncoder = null

        shakeDetector?.stop()
        shakeDetector = null

        screenCaptureManager.stop()

        // Copy to public gallery so it appears in Files / Photos
        outputFile?.let { file ->
            android.media.MediaScannerConnection.scanFile(
                this, arrayOf(file.absolutePath), arrayOf("video/mp4"), null
            )
            val publicFile = fileManager.copyToPublicGallery(file)
            if (publicFile != null) {
                android.media.MediaScannerConnection.scanFile(
                    this, arrayOf(publicFile.absolutePath), arrayOf("video/mp4"), null
                )
                Log.d(TAG, "Copied and scanned public file: ${publicFile.absolutePath}")
            } else {
                Log.d(TAG, "Copied to MediaStore (Scoped Storage)")
            }

            // Delete the private temp file to avoid duplicates
            try {
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted private temp file: $deleted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete private file", e)
            }
        }

        // Clear the tile state
        setTileRecordingState(false)

        // Stop floating overlay
        stopService(Intent(this, FloatingControlService::class.java))

        // Transition to idle on the main thread
        _recordingState.value = RecordingState.Idle

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d(TAG, "Recording stopped, file: ${outputFile?.absolutePath}")
    }

    /** Persist recording state so QuickTileService can reflect it. */
    private fun setTileRecordingState(isRecording: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_RECORDING, isRecording)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch(Dispatchers.IO) { stopRecording() }
        serviceScope.cancel()
        Log.d(TAG, "RecorderService destroyed")
    }

    inner class RecorderBinder : Binder() {
        fun getService(): RecorderService = this@RecorderService
    }
}
