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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
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
import com.flux.recorder.service.FloatingControlService
import com.flux.recorder.ui.theme.*
import com.flux.recorder.utils.PermissionManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    recordingState: RecordingState,
    settings: RecordingSettings,
    onSettingsChanged: (RecordingSettings) -> Unit,
    onStartRecording: (Int, Intent) -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    autoStartRecording: Boolean = false
) {
    val context = LocalContext.current
    var isPreviewActive by remember { mutableStateOf(false) }

    // Required permissions via PermissionManager
    val requiredPermissions = remember(settings.enableFacecam) {
        PermissionManager.getRequiredPermissions(settings.enableFacecam)
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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

            // Quick Configuration & Pre-recording Preview Panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Test Facecam Preview Button
                if (!isActiveRecording) {
                    OutlinedButton(
                        onClick = {
                            if (!PermissionManager.hasCameraPermission(context) || !PermissionManager.hasOverlayPermission(context)) {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                                return@OutlinedButton
                            }
                            isPreviewActive = !isPreviewActive
                            val intent = Intent(context, FloatingControlService::class.java).apply {
                                action = if (isPreviewActive) {
                                    FloatingControlService.ACTION_SHOW_PREVIEW_ONLY
                                } else {
                                    FloatingControlService.ACTION_HIDE_PREVIEW_ONLY
                                }
                            }
                            context.startService(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isPreviewActive) FluxCyanDark.copy(alpha = 0.3f) else SurfaceBlack,
                            contentColor = if (isPreviewActive) FluxCyan else TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPreviewActive) "Close Facecam Preview" else "Preview & Position Facecam",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Clickable Settings Summary Card with Quick Toggles
                SettingsSummaryCard(
                    settings = settings,
                    onToggleFacecam = { enabled ->
                        onSettingsChanged(settings.copy(enableFacecam = enabled))
                    },
                    onToggleShake = { enabled ->
                        onSettingsChanged(settings.copy(enableShakeToStop = enabled))
                    },
                    onClick = onNavigateToSettings
                )
            }
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
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Pulse Ring
        if (isRecording && !isPaused) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = pulseScale * 1.08f
                        scaleY = pulseScale * 1.08f
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(RecordingRed)
            )
        }

        // Main Record Button
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(
                    brush = if (isRecording) {
                        Brush.linearGradient(
                            listOf(
                                if (isPaused) WarningYellow else RecordingRed,
                                if (isPaused) WarningYellow.copy(alpha = 0.7f) else RecordingRed.copy(alpha = 0.7f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(FluxCyan, FluxCyanDark)
                        )
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VoidBlack)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isPaused) "RESUME" else "STOP",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoidBlack,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(VoidBlack)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "REC",
                        style = MaterialTheme.typography.labelMedium,
                        color = VoidBlack,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSummaryCard(
    settings: RecordingSettings,
    onToggleFacecam: (Boolean) -> Unit,
    onToggleShake: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Quick Configuration",
                    style = MaterialTheme.typography.titleSmall,
                    color = FluxCyan,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All Settings", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SettingRow("Resolution", "${settings.videoQuality.displayName} @ ${settings.frameRate.displayName}")
            SettingRow("Audio Source", settings.audioSource.displayName)

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CardBlack)

            // Facecam Quick Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Facecam Overlay", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Switch(
                    checked = settings.enableFacecam,
                    onCheckedChange = onToggleFacecam,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FluxCyan,
                        checkedTrackColor = FluxCyanDark,
                        uncheckedTrackColor = CardBlack
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
                )
            }
            if (settings.enableFacecam) {
                Text(
                    "💡 Tip: Select 'Entire screen' in Android's prompt so Facecam is overlaid on your video.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FluxCyan,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Shake to Stop Quick Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shake to Stop", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Switch(
                    checked = settings.enableShakeToStop,
                    onCheckedChange = onToggleShake,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FluxCyan,
                        checkedTrackColor = FluxCyanDark,
                        uncheckedTrackColor = CardBlack
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = 0.8f; scaleY = 0.8f }
                )
            }
        }
    }
}

@Composable
fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
