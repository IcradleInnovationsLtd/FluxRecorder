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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    onNavigateToManual: () -> Unit = {},
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Flux",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Recorder",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = FluxCyan
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToManual,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .background(SurfaceBlack)
                    ) {
                        Icon(Icons.Default.Info, "User Guide", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onNavigateToRecordings,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .background(SurfaceBlack)
                    ) {
                        Icon(Icons.Default.VideoLibrary, "Recordings", tint = FluxCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .background(SurfaceBlack)
                    ) {
                        Icon(Icons.Default.Settings, "Settings", tint = TextPrimary, modifier = Modifier.size(20.dp))
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Modern Status Pill Capsule
            Box(
                modifier = Modifier
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (recordingState) {
                    is RecordingState.Idle -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceBlack,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBlack)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Ready to Record",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is RecordingState.Recording -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = RecordingRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RecordingRed.copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(RecordingRed)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "RECORDING • ${formatDuration(recordingState.durationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RecordingRed,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    is RecordingState.Paused -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = WarningYellow.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningYellow.copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text("❚❚", color = WarningYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PAUSED • ${formatDuration(recordingState.durationMs)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarningYellow,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                    is RecordingState.Processing -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = FluxCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FluxCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = FluxCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Saving recording...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FluxCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is RecordingState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = RecordingRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RecordingRed)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Error, null, tint = RecordingRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    recordingState.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RecordingRed
                                )
                            }
                        }
                    }
                }
            }

            // Hero Center Record / Stop Button
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

            // In-flight Action Row (Pause / Resume / Stop)
            AnimatedVisibility(
                visible = isActiveRecording,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (recordingState is RecordingState.Recording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (recordingState is RecordingState.Recording) "Pause" else "Resume",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecordingRed,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
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
                // Facecam Pre-recording Preview Button
                if (!isActiveRecording) {
                    Surface(
                        onClick = {
                            if (!PermissionManager.hasCameraPermission(context) || !PermissionManager.hasOverlayPermission(context)) {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                                return@Surface
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
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPreviewActive) FluxCyanDark.copy(alpha = 0.25f) else SurfaceBlack,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPreviewActive) FluxCyan else CardBlack
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPreviewActive) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isPreviewActive) FluxCyan else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (isPreviewActive) "Close Facecam Preview" else "Preview & Position Facecam",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPreviewActive) FluxCyan else TextPrimary
                            )
                        }
                    }
                }

                // Quick Configuration Card with Modern Badges
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
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isRecording && !isPaused) 0.60f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Breathing Halo Ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    scaleX = pulseScale * 1.05f
                    scaleY = pulseScale * 1.05f
                    alpha = glowAlpha
                }
                .clip(CircleShape)
                .background(if (isRecording) RecordingRed else FluxCyan)
        )

        // Middle Dark Glass Border Ring
        Box(
            modifier = Modifier
                .size(156.dp)
                .clip(CircleShape)
                .background(CardBlack)
                .border(2.dp, if (isRecording) RecordingRed.copy(alpha = 0.5f) else FluxCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Vibrant Center Button
            Box(
                modifier = Modifier
                    .size(134.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isRecording) {
                            Brush.linearGradient(
                                listOf(
                                    if (isPaused) WarningYellow else RecordingRed,
                                    if (isPaused) WarningYellow.copy(alpha = 0.8f) else Color(0xFFD50000)
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
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(VoidBlack)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (isPaused) "RESUME" else "STOP",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoidBlack,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(RecordingRed)
                                .border(2.dp, VoidBlack, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "REC",
                            style = MaterialTheme.typography.labelLarge,
                            color = VoidBlack,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBlack)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Configuration Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resolution Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBlack,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, null, tint = FluxCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Resolution", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Text(
                                "${settings.videoQuality.displayName} @ ${settings.frameRate.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Audio Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBlack,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Audio Source", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                            Text(
                                settings.audioSource.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBlack)

            // Facecam Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Face, null, tint = FluxCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Facecam Overlay", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
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

            // Facecam Info Hint
            if (settings.enableFacecam) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = FluxCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tip: Select 'Entire screen' in Android prompt for camera overlay",
                            style = MaterialTheme.typography.labelSmall,
                            color = FluxCyan,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Shake to Stop Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shake to Stop", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
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
