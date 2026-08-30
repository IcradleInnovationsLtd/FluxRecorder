package com.flux.recorder.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.flux.recorder.core.editor.CropAspectRatio
import com.flux.recorder.core.editor.VideoCropper
import com.flux.recorder.core.editor.VideoTrimmer
import com.flux.recorder.data.Recording
import com.flux.recorder.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-performance In-App Video Player with integrated Touch-Draggable Spatial Cropping & Temporal Trimming.
 * Powered by AndroidX Media3 ExoPlayer, Media3 Transformer, and OpenGL FrameProcessor.
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
    var selectedEditorTab by remember { mutableIntStateOf(0) } // 0: Trim, 1: Crop

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }

    // Trimmer State
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(recording.durationMs.coerceAtLeast(1000L)) }

    // Normalized Draggable Crop Box State (0.0 to 1.0)
    var cropLeft by remember { mutableFloatStateOf(0.0f) }
    var cropTop by remember { mutableFloatStateOf(0.0f) }
    var cropRight by remember { mutableFloatStateOf(1.0f) }
    var cropBottom by remember { mutableFloatStateOf(1.0f) }
    var selectedAspectRatio by remember { mutableStateOf(CropAspectRatio.ORIGINAL) }

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

    val isSpatialCropActive = cropLeft > 0.01f || cropTop > 0.01f || cropRight < 0.99f || cropBottom < 0.99f || selectedAspectRatio != CropAspectRatio.ORIGINAL

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
                            text = if (isEditorMode) "✂️ Video Editor & Cropper" else recording.displayName,
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

                // Video Surface with Interactive Draggable/Resizable Crop Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isEditorMode) 0.50f else 1f)
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

                    // Interactive Draggable Crop Overlay
                    if (isEditorMode && (selectedEditorTab == 1 || isSpatialCropActive)) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val containerWidthPx = constraints.maxWidth.toFloat()
                            val containerHeightPx = constraints.maxHeight.toFloat()

                            val boxLeftPx = (cropLeft * containerWidthPx)
                            val boxTopPx = (cropTop * containerHeightPx)
                            val boxWidthPx = ((cropRight - cropLeft) * containerWidthPx).coerceAtLeast(40f)
                            val boxHeightPx = ((cropBottom - cropTop) * containerHeightPx).coerceAtLeast(40f)

                            // Outer dark mask
                            // Top mask
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((cropTop * maxHeight.value).dp)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .align(Alignment.TopStart)
                            )
                            // Bottom mask
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(((1f - cropBottom) * maxHeight.value).dp)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .align(Alignment.BottomStart)
                            )
                            // Left mask
                            Box(
                                modifier = Modifier
                                    .width((cropLeft * maxWidth.value).dp)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .align(Alignment.TopStart)
                            )
                            // Right mask
                            Box(
                                modifier = Modifier
                                    .width(((1f - cropRight) * maxWidth.value).dp)
                                    .fillMaxHeight()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .align(Alignment.TopEnd)
                            )

                            // The Active Crop Box (Draggable Body)
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(boxLeftPx.roundToInt(), boxTopPx.roundToInt()) }
                                    .size(
                                        width = (boxWidthPx / containerWidthPx * maxWidth.value).dp,
                                        height = (boxHeightPx / containerHeightPx * maxHeight.value).dp
                                    )
                                    .border(2.dp, FluxCyan, RoundedCornerShape(4.dp))
                                    .background(FluxCyan.copy(alpha = 0.05f))
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaXNorm = dragAmount.x / containerWidthPx
                                            val deltaYNorm = dragAmount.y / containerHeightPx

                                            val currentWidthNorm = cropRight - cropLeft
                                            val currentHeightNorm = cropBottom - cropTop

                                            val newLeft = (cropLeft + deltaXNorm).coerceIn(0f, 1f - currentWidthNorm)
                                            val newTop = (cropTop + deltaYNorm).coerceIn(0f, 1f - currentHeightNorm)

                                            cropLeft = newLeft
                                            cropRight = newLeft + currentWidthNorm
                                            cropTop = newTop
                                            cropBottom = newTop + currentHeightNorm
                                        }
                                    }
                            ) {
                                // Rule of thirds grid lines
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FluxCyan.copy(alpha = 0.25f)))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FluxCyan.copy(alpha = 0.25f)))
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(FluxCyan.copy(alpha = 0.25f)))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(FluxCyan.copy(alpha = 0.25f)))
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // Aspect ratio / drag label badge
                                Text(
                                    text = "✋ Drag to move • ${selectedAspectRatio.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoidBlack,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(FluxCyan, RoundedCornerShape(bottomEnd = 6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                // 4 Corner Resize Handles
                                // Top-Left Handle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopStart)
                                        .offset((-8).dp, (-8).dp)
                                        .background(FluxCyan, CircleShape)
                                        .border(2.dp, VoidBlack, CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaX = dragAmount.x / containerWidthPx
                                                val deltaY = dragAmount.y / containerHeightPx
                                                cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - 0.15f)
                                                cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - 0.15f)
                                                selectedAspectRatio = CropAspectRatio.ORIGINAL
                                            }
                                        }
                                )

                                // Top-Right Handle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(8.dp, (-8).dp)
                                        .background(FluxCyan, CircleShape)
                                        .border(2.dp, VoidBlack, CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaX = dragAmount.x / containerWidthPx
                                                val deltaY = dragAmount.y / containerHeightPx
                                                cropRight = (cropRight + deltaX).coerceIn(cropLeft + 0.15f, 1f)
                                                cropTop = (cropTop + deltaY).coerceIn(0f, cropBottom - 0.15f)
                                                selectedAspectRatio = CropAspectRatio.ORIGINAL
                                            }
                                        }
                                )

                                // Bottom-Left Handle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomStart)
                                        .offset((-8).dp, 8.dp)
                                        .background(FluxCyan, CircleShape)
                                        .border(2.dp, VoidBlack, CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaX = dragAmount.x / containerWidthPx
                                                val deltaY = dragAmount.y / containerHeightPx
                                                cropLeft = (cropLeft + deltaX).coerceIn(0f, cropRight - 0.15f)
                                                cropBottom = (cropBottom + deltaY).coerceIn(cropTop + 0.15f, 1f)
                                                selectedAspectRatio = CropAspectRatio.ORIGINAL
                                            }
                                        }
                                )

                                // Bottom-Right Handle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(8.dp, 8.dp)
                                        .background(FluxCyan, CircleShape)
                                        .border(2.dp, VoidBlack, CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaX = dragAmount.x / containerWidthPx
                                                val deltaY = dragAmount.y / containerHeightPx
                                                cropRight = (cropRight + deltaX).coerceIn(cropLeft + 0.15f, 1f)
                                                cropBottom = (cropBottom + deltaY).coerceIn(cropTop + 0.15f, 1f)
                                                selectedAspectRatio = CropAspectRatio.ORIGINAL
                                            }
                                        }
                                )
                            }
                        }
                    }
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
                            .padding(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Editor Mode Tabs: Trim (Time) vs Crop (Dimensions)
                            TabRow(
                                selectedTabIndex = selectedEditorTab,
                                containerColor = CardBlack,
                                contentColor = FluxCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .padding(bottom = 12.dp)
                            ) {
                                Tab(
                                    selected = selectedEditorTab == 0,
                                    onClick = { selectedEditorTab = 0 },
                                    text = { Text("✂️ Trim Video", fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = selectedEditorTab == 1,
                                    onClick = { selectedEditorTab = 1 },
                                    text = { Text("📐 Crop Frame", fontWeight = FontWeight.Bold) }
                                )
                            }

                            if (selectedEditorTab == 0) {
                                // TAB 0: TEMPORAL TRIMMING
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
                                        Text("Cut Length", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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

                                Spacer(modifier = Modifier.height(8.dp))

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

                                Spacer(modifier = Modifier.height(4.dp))

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

                            } else {
                                // TAB 1: SPATIAL CROP
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Aspect Ratio Presets:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    TextButton(
                                        onClick = {
                                            cropLeft = 0f
                                            cropTop = 0f
                                            cropRight = 1f
                                            cropBottom = 1f
                                            selectedAspectRatio = CropAspectRatio.ORIGINAL
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Reset Frame", color = FluxCyan, fontSize = 12.sp)
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(CropAspectRatio.values()) { preset ->
                                        val isSelected = selectedAspectRatio == preset
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) FluxCyan else CardBlack,
                                            modifier = Modifier.clickable {
                                                selectedAspectRatio = preset
                                                when (preset) {
                                                    CropAspectRatio.ORIGINAL -> {
                                                        cropLeft = 0f
                                                        cropTop = 0f
                                                        cropRight = 1f
                                                        cropBottom = 1f
                                                    }
                                                    CropAspectRatio.RATIO_1_1 -> {
                                                        cropLeft = 0.15f
                                                        cropRight = 0.85f
                                                        cropTop = 0.10f
                                                        cropBottom = 0.80f
                                                    }
                                                    CropAspectRatio.RATIO_9_16 -> {
                                                        cropLeft = 0.12f
                                                        cropRight = 0.88f
                                                        cropTop = 0f
                                                        cropBottom = 1f
                                                    }
                                                    CropAspectRatio.RATIO_16_9 -> {
                                                        cropLeft = 0f
                                                        cropRight = 1f
                                                        cropTop = 0.20f
                                                        cropBottom = 0.80f
                                                    }
                                                    CropAspectRatio.RATIO_4_5 -> {
                                                        cropLeft = 0.10f
                                                        cropRight = 0.90f
                                                        cropTop = 0.05f
                                                        cropBottom = 0.95f
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = preset.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) VoidBlack else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "💡 Tip: Touch & drag the frame on the video to move it, or drag the cyan circular corner handles to freely resize!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FluxCyan,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Unified Export Button / Progress
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
                                        "Processing video (${if (isSpatialCropActive) "Cropping & Trimming" else "Lossless Trimming"})... ${(exportProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FluxCyan
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (trimEndMs <= trimStartMs + 500L) {
                                            Toast.makeText(context, "Please select at least 1 second to export", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        isExporting = true
                                        exportProgress = 0.05f
                                        exoPlayer.pause()

                                        coroutineScope.launch {
                                            val result = if (isSpatialCropActive) {
                                                // Spatial Crop + Trim with Media3 Transformer
                                                VideoCropper.cropAndTrimVideo(
                                                    context = context,
                                                    sourceUri = recording.uri,
                                                    cropLeft = cropLeft,
                                                    cropTop = cropTop,
                                                    cropRight = cropRight,
                                                    cropBottom = cropBottom,
                                                    startMs = trimStartMs,
                                                    endMs = trimEndMs,
                                                    onProgress = { progress -> exportProgress = progress }
                                                )
                                            } else {
                                                // Lossless Bitstream Remuxing for pure trim
                                                VideoTrimmer.trimVideo(
                                                    context = context,
                                                    sourceUri = recording.uri,
                                                    startMs = trimStartMs,
                                                    endMs = trimEndMs,
                                                    onProgress = { progress -> exportProgress = progress }
                                                )
                                            }

                                            isExporting = false
                                            result.onSuccess {
                                                Toast.makeText(context, "✅ Edited video saved to library!", Toast.LENGTH_LONG).show()
                                                onTrimComplete?.invoke()
                                                isEditorMode = false
                                            }.onFailure { err ->
                                                Toast.makeText(context, "❌ Editing failed: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = FluxCyan, contentColor = VoidBlack),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.MovieFilter, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isSpatialCropActive) "Crop, Cut & Save Video" else "Cut & Save Video",
                                        fontWeight = FontWeight.Bold
                                    )
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
