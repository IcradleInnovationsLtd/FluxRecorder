package com.flux.recorder.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.RecordingState
import com.flux.recorder.service.RecorderService
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
    
    // Build list of required permissions based on Android version and settings
    val requiredPermissions = buildList {
        add(android.Manifest.permission.RECORD_AUDIO)
        
        // Camera permission if facecam is enabled
        if (settings.enableFacecam) {
            add(android.Manifest.permission.CAMERA)
        }
        
        // Notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Storage permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    // Multi-permission state
    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = requiredPermissions
    )
    
    // MediaProjection permission launcher (must be declared before LaunchedEffect)
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
            // Check permissions first
            if (multiplePermissionsState.allPermissionsGranted) {
                // Permissions granted, request MediaProjection
                val intent = (context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) 
                    as android.media.projection.MediaProjectionManager)
                    .createScreenCaptureIntent()
                mediaProjectionLauncher.launch(intent)
            } else {
                // Request permissions first
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
                        Icon(Icons.Default.VideoLibrary, "Recordings")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Recording status
            when (recordingState) {
                is RecordingState.Idle -> {
                    Text(
                        "Ready to Record",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextSecondary
                    )
                }
                is RecordingState.Recording -> {
                    Text(
                        "● Recording",
                        style = MaterialTheme.typography.headlineSmall,
                        color = RecordingRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatDuration(recordingState.durationMs),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                is RecordingState.Paused -> {
                    Text(
                        "⏸ Paused",
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarningYellow
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formatDuration(recordingState.durationMs),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                }
                is RecordingState.Processing -> {
                    Text(
                        "Processing...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = FluxCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { recordingState.progress / 100f },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        color = FluxCyan
                    )
                }
                is RecordingState.Error -> {
                    Text(
                        "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = RecordingRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        recordingState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Dismiss the error so the user can try again
                    Button(onClick = onStopRecording) { Text("Dismiss") }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Record button — shows STOP when recording or paused
            val isActiveRecording = recordingState is RecordingState.Recording
                    || recordingState is RecordingState.Paused
            RecordButton(
                isRecording = isActiveRecording,
                onClick = {
                    when (recordingState) {
                        is RecordingState.Idle -> {
                            if (multiplePermissionsState.allPermissionsGranted) {
                                val intent = (context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
                                    as android.media.projection.MediaProjectionManager)
                                    .createScreenCaptureIntent()
                                mediaProjectionLauncher.launch(intent)
                            } else {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                        // Stop from either Recording or Paused state
                        else -> onStopRecording()
                    }
                }
            )
            
            // Pause/Resume buttons when recording
            if (recordingState is RecordingState.Recording || recordingState is RecordingState.Paused) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pause/Resume button
                    Button(
                        onClick = {
                            if (recordingState is RecordingState.Recording) {
                                onPauseRecording()
                            } else {
                                onResumeRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FluxCyan
                        )
                    ) {
                        Text(
                            text = if (recordingState is RecordingState.Recording) "Pause" else "Resume",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Stop button
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecordingRed
                        )
                    ) {
                        Text(
                            text = "Stop",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Settings summary
            SettingsSummaryCard(settings)
        }
    }
}

@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Gradient background
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(180.dp)
                .scale(if (isRecording) scale else 1f),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) RecordingRed else ElectricViolet
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Text(
                if (isRecording) "STOP" else "RECORD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsSummaryCard(settings: RecordingSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceBlack
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Current Settings",
                style = MaterialTheme.typography.titleMedium,
                color = FluxCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingRow("Quality", settings.videoQuality.displayName)
            SettingRow("Frame Rate", settings.frameRate.displayName)
            SettingRow("Audio", settings.audioSource.displayName)
            if (settings.enableFacecam) {
                SettingRow("Facecam", "Enabled")
            }
        }
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
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
