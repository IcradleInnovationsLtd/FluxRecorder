package com.flux.recorder.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.flux.recorder.core.editor.VideoTrimmer
import com.flux.recorder.data.Recording
import com.flux.recorder.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-performance In-App Video Player & Lossless Video Editor.
 * Powered by AndroidX Media3 ExoPlayer and hardware-accelerated MediaMuxer bitstream trimming.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    recording: Recording,
    onDismiss: () -> Unit,
    onShare: (Recording) -> Unit,
    onOpenExternal: (Recording) -> Unit,
    onTrimComplete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(recording.uri))
            prepare()
            playWhenReady = true
        }
    }

    var isEditorMode by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }

    // Trimmer state
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    // Track real playback duration and position
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val realDuration = exoPlayer.duration
                    if (realDuration > 0) {
                        totalDurationMs = realDuration
                        if (trimEndMs == recording.durationMs || trimEndMs <= 0) {
                            trimEndMs = realDuration
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Playhead polling job
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition
            delay(200)
        }
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val millis = (ms % 1000) / 100
        return String.format("%02d:%02d.%d", minutes, seconds, millis)
    }

    Dialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isExporting,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditorMode) "✂️ Video Editor & Trimmer" else recording.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        val meta = buildString {
                            if (totalDurationMs > 0) append("⏱ ${formatTime(totalDurationMs)}  •  ")
                            append(recording.getFormattedSize())
                            recording.resolution?.let { append("  •  $it") }
                        }
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = FluxCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Toggle Editor Button
                        IconButton(
                            onClick = {
                                isEditorMode = !isEditorMode
                                if (isEditorMode) {
                                    exoPlayer.pause()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditorMode) Icons.Default.Check else Icons.Default.ContentCut,
                                contentDescription = "Edit Video",
                                tint = if (isEditorMode) FluxCyan else TextPrimary
                            )
                        }

                        IconButton(onClick = { onShare(recording) }) {
                            Icon(Icons.Default.Share, "Share", tint = FluxCyan)
                        }
                        IconButton(onClick = { onOpenExternal(recording) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open in external player", tint = TextSecondary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                        }
                    }
                }

                // Video Surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isEditorMode) 0.6f else 1f)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceBlack),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Video Editor & Trimmer Panel
                AnimatedVisibility(
                    visible = isEditorMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Trim Time Information
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Cut In", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(formatTime(trimStartMs), style = MaterialTheme.typography.bodyMedium, color = FluxCyan, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Trimmed Length", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        formatTime((trimEndMs - trimStartMs).coerceAtLeast(0L)),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cut Out", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(formatTime(trimEndMs), style = MaterialTheme.typography.bodyMedium, color = FluxCyan, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Start Time Slider
                            Text("Start Marker:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Slider(
                                value = trimStartMs.toFloat(),
                                onValueChange = {
                                    val newStart = it.toLong().coerceAtMost(trimEndMs - 500L)
                                    trimStartMs = newStart
                                    exoPlayer.seekTo(newStart)
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = FluxCyan,
                                    activeTrackColor = FluxCyanDark,
                                    inactiveTrackColor = CardBlack
                                )
                            )

                            // End Time Slider
                            Text("End Marker:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Slider(
                                value = trimEndMs.toFloat(),
                                onValueChange = {
                                    val newEnd = it.toLong().coerceAtLeast(trimStartMs + 500L)
                                    trimEndMs = newEnd
                                    exoPlayer.seekTo(newEnd)
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = RecordingRed,
                                    activeTrackColor = RecordingRed,
                                    inactiveTrackColor = CardBlack
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Markers & Preview Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val pos = exoPlayer.currentPosition
                                        if (pos < trimEndMs - 500L) {
                                            trimStartMs = pos
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FluxCyan)
                                ) {
                                    Text("[ Set Start", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        exoPlayer.seekTo(trimStartMs)
                                        exoPlayer.play()
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Preview", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val pos = exoPlayer.currentPosition
                                        if (pos > trimStartMs + 500L) {
                                            trimEndMs = pos
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RecordingRed)
                                ) {
                                    Text("Set End ]", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Export Button / Progress
                            if (isExporting) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = { exportProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = FluxCyan,
                                        trackColor = CardBlack
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Trimming video losslessly... ${(exportProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FluxCyan
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (trimEndMs <= trimStartMs + 500L) {
                                            Toast.makeText(context, "Please select at least 1 second to trim", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isExporting = true
                                        exportProgress = 0.05f
                                        exoPlayer.pause()

                                        coroutineScope.launch {
                                            val result = VideoTrimmer.trimVideo(
                                                context = context,
                                                sourceUri = recording.uri,
                                                startMs = trimStartMs,
                                                endMs = trimEndMs,
                                                onProgress = { progress ->
                                                    exportProgress = progress
                                                }
                                            )

                                            isExporting = false
                                            result.onSuccess { trimmedFile ->
                                                Toast.makeText(context, "✅ Trimmed video saved to library!", Toast.LENGTH_LONG).show()
                                                onTrimComplete?.invoke()
                                                isEditorMode = false
                                            }.onFailure { err ->
                                                Toast.makeText(context, "❌ Trimming failed: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = FluxCyan, contentColor = VoidBlack),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cut & Save Video", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
