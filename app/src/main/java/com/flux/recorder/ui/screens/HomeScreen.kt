package com.flux.recorder.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.RecordingState
import com.flux.recorder.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    recordingState: RecordingState,
    settings: RecordingSettings,
    onStartRecording: (Int, Intent) -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    autoStartRecording: Boolean = false
) {
    val context = LocalContext.current

    // Required permissions via PermissionManager
    val requiredPermissions = remember(settings.enableFacecam) {
        com.flux.recorder.utils.PermissionManager.getRequiredPermissions(settings.enableFacecam)
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = requiredPermissions
    )

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            onStartRecording(result.resultCode, result.data!!)
        }
    }

    // Auto-start recording if launched from Quick Tile
    LaunchedEffect(autoStartRecording) {
        if (autoStartRecording && recordingState is RecordingState.Idle) {
            if (multiplePermissionsState.allPermissionsGranted) {
                val intent = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as android.media.projection.MediaProjectionManager)
                    .createScreenCaptureIntent()
                mediaProjectionLauncher.launch(intent)
            } else {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Flux Recorder",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToRecordings) {
                        Icon(Icons.Default.VideoLibrary, "Recordings", tint = TextPrimary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = VoidBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Recording status & timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (recordingState) {
                    is RecordingState.Idle -> {
                        Text(
                            "Ready to Record",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is RecordingState.Recording -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(RecordingRed)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "RECORDING",
                                style = MaterialTheme.typography.labelLarge,
                                color = RecordingRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatDuration(recordingState.durationMs),
                            style = MaterialTheme.typography.displayLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is RecordingState.Paused -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "PAUSED",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarningYellow,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            formatDuration(recordingState.durationMs),
                            style = MaterialTheme.typography.displayLarge,
                            color = WarningYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is RecordingState.Processing -> {
                        Text(
                            "Saving recording...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = FluxCyan
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { recordingState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .clip(RoundedCornerShape(4.dp)),
                            color = FluxCyan,
                            trackColor = SurfaceBlack
                        )
                    }
                    is RecordingState.Error -> {
                        Text(
                            "Error Occurred",
                            style = MaterialTheme.typography.headlineSmall,
                            color = RecordingRed,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            recordingState.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FluxCyan)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Big Record / Stop Button with GPU graphicsLayer animation
            val isActiveRecording = recordingState is RecordingState.Recording || recordingState is RecordingState.Paused
            RecordButton(
                isRecording = isActiveRecording,
                isPaused = recordingState is RecordingState.Paused,
                onClick = {
                    when (recordingState) {
                        is RecordingState.Idle -> {
                            if (multiplePermissionsState.allPermissionsGranted) {
                                val intent = (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                                    as android.media.projection.MediaProjectionManager)
                                    .createScreenCaptureIntent()
                                mediaProjectionLauncher.launch(intent)
                            } else {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                        else -> onStopRecording()
                    }
                }
            )

            // In-flight Control Buttons (Pause / Resume / Stop)
            AnimatedVisibility(
                visible = isActiveRecording,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume Button
                    FilledTonalButton(
                        onClick = {
                            if (recordingState is RecordingState.Recording) {
                                onPauseRecording()
                            } else {
                                onResumeRecording()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (recordingState is RecordingState.Paused) FluxCyan else SurfaceBlack,
                            contentColor = if (recordingState is RecordingState.Paused) VoidBlack else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (recordingState is RecordingState.Recording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (recordingState is RecordingState.Recording) "Pause" else "Resume",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Stop Button
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecordingRed,
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Clickable Settings Summary Card
            SettingsSummaryCard(settings = settings, onClick = onNavigateToSettings)

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RecordButton(
    isRecording: Boolean,
    isPaused: Boolean,
    onClick: () -> Unit
) {
    // Pulse animation using GPU graphicsLayer for 60/120Hz zero-jank rendering
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isRecording && !isPaused) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Pulse Ring
        if (isRecording && !isPaused) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = pulseScale * 1.08f
                        scaleY = pulseScale * 1.08f
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(RecordingRed)
            )
        }

        // Main Action Button
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(175.dp)
                .graphicsLayer {
                    scaleX = if (isRecording && !isPaused) pulseScale else 1f
                    scaleY = if (isRecording && !isPaused) pulseScale else 1f
                },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) RecordingRed else ElectricViolet
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isRecording) "STOP" else "RECORD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun SettingsSummaryCard(
    settings: RecordingSettings,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    color = FluxCyan,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Edit settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            SettingRow("Resolution", settings.videoQuality.displayName)
            SettingRow("Frame Rate", settings.frameRate.displayName)
            SettingRow("Audio Track", settings.audioSource.displayName)
            if (settings.enableFacecam) {
                SettingRow("Facecam Overlay", "Active")
            }
            if (settings.enableShakeToStop) {
                SettingRow("Shake to Stop", "Active (${String.format("%.1f", settings.shakeSensitivity)} m/s²)")
            }
        }
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
