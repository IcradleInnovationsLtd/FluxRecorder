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
 * In-App User Manual & Comprehensive Feature Guide for Flux Recorder.
 * Provides detailed locations, step-by-step instructions, and pro tips for all features.
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
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Critical Callout: Facecam Recording Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                                "CRITICAL: Facecam Recording Setup",
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
                                "Note: If you choose \"A single app\", Android's security model isolates that app and excludes overlay windows from the capture.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Section 1: Quick Start & Recording
            item {
                ManualSection(
                    icon = Icons.Default.PlayArrow,
                    title = "1. Quick Start & Screen Recording",
                    location = "Home Screen",
                    description = "Start ultra-high-definition screen capture with zero setup lag.",
                    steps = listOf(
                        "Tap the big glowing REC button on the Home Screen.",
                        "Grant the Android Screen Recording prompt (select 'Entire screen').",
                        "FluxRecorder automatically minimizes itself to the background so it records your game or app cleanly without recording its own UI.",
                        "To Stop: Pull down your Android notification drawer and tap Stop, tap Stop on the floating edge bubble, or use the Shake to Stop gesture."
                    )
                )
            }

            // Section 2: Facecam & Pre-Recording Preview
            item {
                ManualSection(
                    icon = Icons.Default.Videocam,
                    title = "2. Facecam & Camera Preview",
                    location = "Home Screen & Settings > Controls & Overlay",
                    description = "Hardware-accelerated live camera overlay with zero black-box artifacts.",
                    steps = listOf(
                        "Pre-Recording Preview: Tap 'Preview & Position Facecam' on the Home screen to check lighting, test your camera, and position it anywhere on screen before starting.",
                        "Draggable Overlay: Touch and drag the camera window to any corner of the display.",
                        "Auto-Hiding Close Button: Tap the camera window anytime to reveal the close (X) button, which automatically dims and hides after 2.5 seconds.",
                        "Live Camera Toggle: Turn your camera ON or OFF on the fly during an active recording from the floating control bubble.",
                        "Zero-Placeholder Startup: The camera overlay stays invisible until the hardware sensor begins streaming, eliminating black loading boxes from your videos."
                    )
                )
            }

            // Section 3: Built-In Lossless Video Editor & Trimmer
            item {
                ManualSection(
                    icon = Icons.Default.ContentCut,
                    title = "3. Lossless Video Editor & Time Trimmer",
                    location = "Recordings Screen > Edit Button (or Player > Scissors Icon)",
                    description = "Cut off unwanted beginnings (setup screens) and endings (stopping the app) in under 1 second.",
                    steps = listOf(
                        "Open the Recordings library and tap 'Edit' on any video card (or tap the scissors icon inside the player).",
                        "In the 'Trim Video' tab, drag the cyan Start Marker and red End Marker sliders to set your in-point and out-point.",
                        "Quick Playhead Locking: Tap '[ Set Start' or 'Set End ]' while watching the video to instantly lock markers to the current frame.",
                        "Tap 'Preview' to review only the trimmed portion.",
                        "Tap 'Cut & Save Video': Extracts and remuxes bitstream packets directly without re-encoding, preserving 100% original video/audio quality in under 1 second!"
                    )
                )
            }

            // Section 4: Spatial Video Cropper & Aspect Ratio Presets
            item {
                ManualSection(
                    icon = Icons.Default.Crop,
                    title = "4. Spatial Video Cropper & Frame Formatting",
                    location = "Recordings Screen > Edit > 'Crop Frame' Tab",
                    description = "Crop unwanted screen areas or reformat videos for social media platforms.",
                    steps = listOf(
                        "Switch to the 'Crop Frame' tab in the Video Editor.",
                        "Aspect Ratio Presets: Tap '1:1 Square' (Instagram), '9:16 Portrait' (TikTok, Shorts, Reels), '16:9 Landscape' (YouTube), or '4:5 Feed'.",
                        "Touch-Draggable Crop Box: Place your finger inside the glowing cyan crop frame on top of the video and drag it anywhere across the screen.",
                        "4 Corner Resize Handles: Drag any circular cyan corner handle to freely expand or shrink the crop box.",
                        "Rule of Thirds Grid: Align your subjects using the subtle 3x3 framing grid lines.",
                        "Tap 'Crop, Cut & Save Video': Hardware-accelerated Media3 Transformer encodes the cropped frame cleanly with 100% encoder compliance."
                    )
                )
            }

            // Section 5: Edge-Docked Floating Controls
            item {
                ManualSection(
                    icon = Icons.Default.Settings,
                    title = "5. Floating Controls & Auto-Dimming",
                    location = "Settings > Controls & Overlay > Floating Controls",
                    description = "Ultra-compact on-screen overlay controls that dock against screen edges.",
                    steps = listOf(
                        "Compact Mode: Floating controls rest as a 36dp circular bubble docked flush against your screen edge.",
                        "Expand Controls: Tap the bubble once to reveal Pause/Resume, Stop, Camera Toggle, and Minimize buttons.",
                        "Auto-Dimming: Inactive bubbles smoothly fade to 45% transparency after 2.5 seconds to stay unobtrusive.",
                        "Clean Screen Mode (Recommended): Turn Floating Controls OFF in Settings to enjoy a completely clean screen with ZERO on-screen buttons, controlling recordings via Notifications or Shake to Stop."
                    )
                )
            }

            // Section 6: Shake to Stop Gesture
            item {
                ManualSection(
                    icon = Icons.Default.Refresh,
                    title = "6. Shake to Stop Gesture",
                    location = "Settings > Gestures > Shake to Stop",
                    description = "End recordings hands-free without showing control buttons in your final video.",
                    steps = listOf(
                        "Enable 'Shake to Stop' in Settings.",
                        "Shake your phone with a quick double-shake motion anytime during recording to instantly stop and save.",
                        "Calibrated Threshold: Set to 12.0 m/s² by default so gaming, typing, and walking will never trigger accidental stops.",
                        "Sensitivity Slider: Adjust the slider in Settings from 6.0 m/s² (easier) to 20.0 m/s² (firmer shake required)."
                    )
                )
            }

            // Section 7: Single-App vs. Full Screen (Android 14+)
            item {
                ManualSection(
                    icon = Icons.Default.Info,
                    title = "7. Single-App vs. Entire Screen Recording",
                    location = "Android System Recording Permission Prompt",
                    description = "Smart capture modes with automatic background pause & resume.",
                    steps = listOf(
                        "Entire Screen Mode: Records everything across your phone, including multitasking, notifications, and Facecam overlays.",
                        "Single App Mode: Records only one specific app.",
                        "Auto-Pause & Resume: If you switch away from or minimize the recorded app, FluxRecorder automatically pauses to prevent black screens, and automatically resumes the instant you return!"
                    )
                )
            }

            // Section 8: Recordings Library & Sharing
            item {
                ManualSection(
                    icon = Icons.Default.VideoLibrary,
                    title = "8. Recordings Library & Media Management",
                    location = "Home Screen Top Bar > Videos Icon",
                    description = "Watch, trim, crop, share, or delete saved MP4 recordings.",
                    steps = listOf(
                        "Tap the Videos icon in the top-right corner of the Home screen.",
                        "All recordings are displayed with video thumbnails, formatted file size, resolution, and duration.",
                        "In-App Player: Tap any card to watch your recording with hardware-accelerated playback.",
                        "Share: Tap 'Share' on any card or in the player to send videos directly to YouTube, TikTok, WhatsApp, Discord, or Google Drive.",
                        "Storage Location: Videos are stored permanently in your public 'Movies/Recordings' folder."
                    )
                )
            }

            // Section 9: Video Quality & Audio Source Configuration
            item {
                ManualSection(
                    icon = Icons.Default.Tune,
                    title = "9. Pro Video Quality & Audio Sources",
                    location = "Settings Screen",
                    description = "Configure custom resolutions, frame rates, and multi-source audio.",
                    steps = listOf(
                        "Video Quality: Select 720p HD, 1080p Full HD, 1440p 2K, or 2160p 4K Ultra HD.",
                        "Frame Rate: Choose 30 FPS (battery-saver), 60 FPS (standard gaming), 90 FPS, or 120 FPS (high refresh rate displays).",
                        "Audio Source - Microphone: Records your voice commentary through external phone microphones.",
                        "Audio Source - Internal Audio: Records crisp game and system audio with zero room noise.",
                        "Audio Source - Both: Simultaneously captures game/media audio AND your voice commentary on mixed stereo tracks."
                    )
                )
            }

            // Section 10: Show Screen Taps (Touch Indicator)
            item {
                ManualSection(
                    icon = Icons.Default.TouchApp,
                    title = "10. Show Screen Taps (Touch Indicator)",
                    location = "Home Screen (Quick Configuration) & Settings Screen",
                    description = "Visually renders circular touch dots on your screen for tutorials, demonstrations, and app walkthroughs.",
                    steps = listOf(
                        "Enable 'Show Screen Taps' in Settings or Quick Configuration on the Home Screen.",
                        "Automatic Mode: Grant 'Modify system settings' permission to automatically turn touch circles ON when recording starts and OFF when recording stops.",
                        "Developer Options Mode: Alternatively, tap 'Developer Options' and enable 'Show taps' under the Input section in Android settings.",
                        "Touch dots are captured directly into your screen recordings."
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
                        "Flux Recorder • Complete Production Guide v1.0",
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
    location: String,
    description: String,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Icon and Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FluxCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FluxCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Location: $location",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBlack)

            // Step-by-Step Instructions
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
