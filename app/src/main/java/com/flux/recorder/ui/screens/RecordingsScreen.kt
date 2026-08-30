package com.flux.recorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flux.recorder.data.Recording
import com.flux.recorder.ui.components.VideoPlayerDialog
import com.flux.recorder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    recordings: List<Recording>,
    onNavigateBack: () -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onShareRecording: (Recording) -> Unit,
    onPlayRecording: (Recording) -> Unit,
    onRefresh: (() -> Unit)? = null
) {
    var selectedVideoForPlayback by remember { mutableStateOf<Recording?>(null) }

    // In-App Video Player & Editor Dialog
    selectedVideoForPlayback?.let { recording ->
        VideoPlayerDialog(
            recording = recording,
            onDismiss = { selectedVideoForPlayback = null },
            onShare = onShareRecording,
            onOpenExternal = onPlayRecording,
            onTrimComplete = {
                onRefresh?.invoke()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recordings (${recordings.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (onRefresh != null) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = TextPrimary)
                        }
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
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No recordings yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap RECORD on the home screen to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = recordings,
                    key = { it.contentUri?.toString() ?: it.fileUri?.toString() ?: it.displayName }
                ) { recording ->
                    RecordingCard(
                        recording = recording,
                        onDelete  = { onDeleteRecording(recording) },
                        onShare   = { onShareRecording(recording) },
                        onPlay    = { selectedVideoForPlayback = recording }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingCard(
    recording: Recording,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recording?") },
            text  = { Text("\"${recording.displayName}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = RecordingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = SurfaceBlack
        )
    }

    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(recording.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Play icon overlay
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = recording.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))

                // Duration
                if (recording.durationMs > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = FluxCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = recording.getFormattedDuration(),
                            style = MaterialTheme.typography.bodySmall,
                            color = FluxCyan
                        )
                    }
                }

                // Size + resolution
                val meta = buildString {
                    append(recording.getFormattedSize())
                    recording.resolution?.let { append("  •  $it") }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPlay,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FluxCyan)
                    ) {
                        Icon(Icons.Default.ContentCut, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RecordingRed)
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
