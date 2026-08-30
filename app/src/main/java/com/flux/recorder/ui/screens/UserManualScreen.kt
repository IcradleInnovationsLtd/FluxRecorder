package com.flux.recorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flux.recorder.ui.theme.*

/**
 * In-app User Manual & Quick Start Guide for Flux Recorder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Manual & Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Important Callout Alert: Facecam Instructions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = FluxCyan,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "CRITICAL: Facecam Recording",
                                style = MaterialTheme.typography.titleMedium,
                                color = FluxCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "When recording with Facecam: Choose \"Entire screen\" when prompted by Android to have your camera video smoothly overlaid on top of your recording!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Note: If you choose \"A single app\", Android isolates that app and excludes camera overlays from the recorded video.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Section 1: Quick Start
            item {
                ManualSection(
                    icon = Icons.Default.PlayArrow,
                    title = "1. Quick Start",
                    steps = listOf(
                        "Configure your desired resolution (e.g. 1080p, 2K, 4K) and Frame Rate (30, 60, 90 FPS) in Settings.",
                        "Select your audio source: Microphone, Internal Sound, or Both for simultaneous commentary and game/system audio.",
                        "Tap the big REC button on the Home screen to start. FluxRecorder will automatically minimize to the background so it records your game or app, not itself."
                    )
                )
            }

            // Section 2: Facecam & Camera Preview
            item {
                ManualSection(
                    icon = Icons.Default.Videocam,
                    title = "2. Facecam & Camera Preview",
                    steps = listOf(
                        "Preview & Position: Tap 'Preview & Position Facecam' on the Home screen before recording to frame your shot and drag the camera anywhere.",
                        "Live Toggle: During an active recording, tap the Camera icon on the floating menu to turn your camera ON or OFF on the fly.",
                        "Draggable Overlay: Tap and hold the camera window to move it to any corner of your screen.",
                        "Closing: Tap the X button on the top-right of the camera window anytime to dismiss it."
                    )
                )
            }

            // Section 3: Floating Controls & Auto-Dimming
            item {
                ManualSection(
                    icon = Icons.Default.Settings,
                    title = "3. Floating Controls & Auto-Dimming",
                    steps = listOf(
                        "Compact Mode: Floating controls launch in a sleek 36dp circular bubble docked against your screen edge.",
                        "Expanding: Tap the bubble once to reveal Pause/Resume, Stop, Camera Toggle, and Minimize buttons.",
                        "Auto-Dimming: Inactive bubbles smoothly fade to 45% opacity after 2.5 seconds to stay out of your view.",
                        "Zero Black Boxes: The controls blend naturally without any black box artifacts.",
                        "Notification Bar: You can also pause, resume, and stop directly from your Android notification drawer."
                    )
                )
            }

            // Section 4: Shake to Stop Gesture
            item {
                ManualSection(
                    icon = Icons.Default.Refresh,
                    title = "4. Shake to Stop Gesture",
                    steps = listOf(
                        "Stop Instantly: Shake your phone with a quick double-shake motion to stop recording without touching any buttons on screen.",
                        "Calibrated Sensitivity: Sensitivity is calibrated to 12.0 m/s² so normal typing, gaming, and walking will never accidentally trigger a stop.",
                        "Customizable: You can adjust the shake sensitivity slider or turn this feature off in Settings."
                    )
                )
            }

            // Section 5: Single-App vs Full Screen Recording
            item {
                ManualSection(
                    icon = Icons.Default.Info,
                    title = "5. Single-App vs Entire Screen (Android 14+)",
                    steps = listOf(
                        "Entire Screen (Recommended): Records everything across your phone, including all apps, games, home screen, and the floating Facecam overlay.",
                        "Single App Mode: Records only one specific app. If you minimize or switch away from that app, FluxRecorder automatically pauses to prevent black screens, and automatically resumes when you return."
                    )
                )
            }

            // Section 6: Managing Your Recordings
            item {
                ManualSection(
                    icon = Icons.Default.VideoLibrary,
                    title = "6. Managing & Sharing Recordings",
                    steps = listOf(
                        "Library: Tap the Videos icon in the top-right corner of the Home screen to view all saved recordings.",
                        "In-App Player: Tap any recording to watch it with hardware-accelerated playback.",
                        "Share & Export: Tap the Share icon to share your video to YouTube, TikTok, WhatsApp, Discord, or any app.",
                        "Storage: Videos are saved directly to your device Movies/Recordings folder in standard MP4 format."
                    )
                )
            }

            // Section 7: Video Editing, Trimming & Cropping
            item {
                ManualSection(
                    icon = Icons.Default.ContentCut,
                    title = "7. Video Editing, Trimming & Cropping",
                    steps = listOf(
                        "Lossless Trimming: In the video player, tap the Edit button and use the Start and End sliders to cut unwanted beginnings or endings.",
                        "Spatial Cropping: Switch to the 'Crop Frame' tab to select social media aspect ratio presets (1:1, 9:16, 16:9, 4:5).",
                        "Touch Draggable Frame: Drag the cyan crop box directly over the video, or pull the circular corner handles to freely crop any region.",
                        "Instant Export: Tap 'Save Video' to export your edited clip directly into your library with hardware acceleration."
                    )
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Flux Recorder • Production Ready v1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextDisabled
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ManualSection(
    icon: ImageVector,
    title: String,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FluxCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FluxCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .padding(top = 7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(FluxCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
