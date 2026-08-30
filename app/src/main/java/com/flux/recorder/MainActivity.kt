package com.flux.recorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flux.recorder.data.Recording
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.RecordingState
import com.flux.recorder.service.RecorderService
import com.flux.recorder.service.QuickTileService
import com.flux.recorder.ui.screens.HomeScreen
import com.flux.recorder.ui.screens.RecordingsScreen
import com.flux.recorder.ui.screens.SettingsScreen
import com.flux.recorder.ui.theme.FluxRecorderTheme
import com.flux.recorder.ui.theme.VoidBlack
import com.flux.recorder.utils.FileManager
import com.flux.recorder.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var fileManager: FileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldStartRecording = intent?.action == QuickTileService.ACTION_TOGGLE_RECORDING

        setContent {
            var service by remember { mutableStateOf<RecorderService?>(null) }
            var autoStartRecording by remember { mutableStateOf(shouldStartRecording) }
            val context = LocalContext.current

            DisposableEffect(Unit) {
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        service = (binder as RecorderService.RecorderBinder).getService()
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {
                        service = null
                    }
                }
                val intent = Intent(context, RecorderService::class.java)
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                onDispose { context.unbindService(connection) }
            }

            FluxRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBlack
                ) {
                    val recordingState by service?.recordingState?.collectAsState()
                        ?: remember { mutableStateOf(RecordingState.Idle) }

                    FluxRecorderApp(
                        preferencesManager  = preferencesManager,
                        fileManager         = fileManager,
                        onStartRecording    = { resultCode, data, settings ->
                            startRecordingService(resultCode, data, settings)
                            autoStartRecording = false
                        },
                        onStopRecording     = { stopRecordingService() },
                        onPauseRecording    = { pauseRecordingService() },
                        onResumeRecording   = { resumeRecordingService() },
                        recordingState      = recordingState,
                        onPlayRecording     = { recording -> playRecording(recording) },
                        onShareRecording    = { recording -> shareRecording(recording) },
                        autoStartRecording  = autoStartRecording
                    )
                }
            }
        }
    }

    private fun startRecordingService(resultCode: Int, data: Intent, settings: RecordingSettings) {
        startService(Intent(this, RecorderService::class.java).apply {
            action = RecorderService.ACTION_START_RECORDING
            putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecorderService.EXTRA_RESULT_DATA, data)
            putExtra(RecorderService.EXTRA_SETTINGS, settings)
        })
    }

    private fun stopRecordingService() {
        startService(Intent(this, RecorderService::class.java).apply {
            action = RecorderService.ACTION_STOP_RECORDING
        })
    }

    private fun pauseRecordingService() {
        startService(Intent(this, RecorderService::class.java).apply {
            action = RecorderService.ACTION_PAUSE_RECORDING
        })
    }

    private fun resumeRecordingService() {
        startService(Intent(this, RecorderService::class.java).apply {
            action = RecorderService.ACTION_RESUME_RECORDING
        })
    }

    private fun playRecording(recording: Recording) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(recording.uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareRecording(recording: Recording) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, recording.uri)
                putExtra(Intent.EXTRA_SUBJECT, "Screen Recording")
                putExtra(Intent.EXTRA_TEXT, "Check out this screen recording from Flux Recorder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Recording"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun FluxRecorderApp(
    preferencesManager: PreferencesManager,
    fileManager: FileManager,
    onStartRecording: (Int, Intent, RecordingSettings) -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    recordingState: RecordingState,
    onPlayRecording: (Recording) -> Unit,
    onShareRecording: (Recording) -> Unit,
    autoStartRecording: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("home") }
    var settings by remember { mutableStateOf(preferencesManager.getRecordingSettings()) }
    var recordings by remember { mutableStateOf<List<Recording>>(emptyList()) }

    // Load recordings asynchronously on IO dispatcher to avoid main thread UI hitching
    val reloadRecordings: () -> Unit = {
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                fileManager.getAllRecordings()
            }
            recordings = list
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == "recordings") {
            reloadRecordings()
        }
    }

    // Buttery smooth screen transitions
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "settings" || targetState == "recordings") {
                (slideInHorizontally(animationSpec = tween(280)) { it / 3 } + fadeIn(animationSpec = tween(280)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeOut(animationSpec = tween(280)))
            } else {
                (slideInHorizontally(animationSpec = tween(280)) { -it / 3 } + fadeIn(animationSpec = tween(280)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { it / 3 } + fadeOut(animationSpec = tween(280)))
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            "home" -> {
                HomeScreen(
                    recordingState       = recordingState,
                    settings             = settings,
                    onStartRecording     = { resultCode, data -> onStartRecording(resultCode, data, settings) },
                    onStopRecording      = onStopRecording,
                    onPauseRecording     = onPauseRecording,
                    onResumeRecording    = onResumeRecording,
                    onNavigateToSettings = { currentScreen = "settings" },
                    onNavigateToRecordings = { currentScreen = "recordings" },
                    autoStartRecording   = autoStartRecording
                )
            }
            "settings" -> {
                SettingsScreen(
                    settings         = settings,
                    onSettingsChanged = { newSettings ->
                        settings = newSettings
                        preferencesManager.saveRecordingSettings(newSettings)
                    },
                    onNavigateBack = { currentScreen = "home" }
                )
            }
            "recordings" -> {
                RecordingsScreen(
                    recordings       = recordings,
                    onNavigateBack   = { currentScreen = "home" },
                    onRefresh        = reloadRecordings,
                    onDeleteRecording = { recording ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                fileManager.deleteRecording(recording)
                            }
                            reloadRecordings()
                        }
                    },
                    onShareRecording = onShareRecording,
                    onPlayRecording  = onPlayRecording
                )
            }
        }
    }
}
