package com.flux.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flux.recorder.data.AudioSource
import com.flux.recorder.data.FrameRate
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.VideoQuality
import com.flux.recorder.ui.theme.*
import com.flux.recorder.utils.TouchHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    onSettingsChanged: (RecordingSettings) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToManual: () -> Unit = {}
) {
    var currentSettings by remember { mutableStateOf(settings) }
    val context = LocalContext.current
    var showTouchPermissionDialog by remember { mutableStateOf(false) }

    // Dialog explaining how to grant touch permissions / open Developer Options
    if (showTouchPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showTouchPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = FluxCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Show Screen Taps", color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        "To automatically show visual touch circles during recordings and hide them when finished, Flux Recorder requires permission to modify system settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You can also enable 'Show taps' directly in Android Developer Options.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTouchPermissionDialog = false
                        TouchHelper.openWriteSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FluxCyan, contentColor = VoidBlack)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTouchPermissionDialog = false
                        TouchHelper.openDeveloperSettings(context)
                    }
                ) {
                    Text("Developer Options", color = FluxCyan)
                }
            },
            containerColor = SurfaceBlack,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Video Quality
            item {
                SettingSection("Video Quality") {
                    VideoQuality.values().forEach { quality ->
                        SettingRadioRow(
                            label = quality.displayName,
                            selected = currentSettings.videoQuality == quality,
                            onSelect = {
                                currentSettings = currentSettings.copy(videoQuality = quality)
                                onSettingsChanged(currentSettings)
                            }
                        )
                    }
                }
            }

            // Frame Rate
            item {
                SettingSection("Frame Rate") {
                    FrameRate.values().forEach { rate ->
                        SettingRadioRow(
                            label = rate.displayName,
                            selected = currentSettings.frameRate == rate,
                            onSelect = {
                                currentSettings = currentSettings.copy(frameRate = rate)
                                onSettingsChanged(currentSettings)
                            }
                        )
                    }
                }
            }

            // Audio Source
            item {
                SettingSection("Audio Source") {
                    AudioSource.values().forEach { source ->
                        SettingRadioRow(
                            label = source.displayName,
                            selected = currentSettings.audioSource == source,
                            onSelect = {
                                currentSettings = currentSettings.copy(audioSource = source)
                                onSettingsChanged(currentSettings)
                            }
                        )
                    }
                }
            }

            // Screen Taps & Touches (Touch Indicator)
            item {
                SettingSection("Screen Taps & Touch Indicator") {
                    SettingSwitchRow(
                        label = "Show Screen Taps",
                        checked = currentSettings.showTouches,
                        onCheckedChange = { enable ->
                            if (enable && !TouchHelper.canWriteSettings(context)) {
                                showTouchPermissionDialog = true
                            }
                            currentSettings = currentSettings.copy(showTouches = enable)
                            onSettingsChanged(currentSettings)
                        }
                    )
                    Text(
                        "Displays visual touch feedback circles on screen during video recordings",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    if (currentSettings.showTouches && !TouchHelper.canWriteSettings(context)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { TouchHelper.openWriteSettings(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FluxCyan)
                            ) {
                                Text("Auto-Toggle", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { TouchHelper.openDeveloperSettings(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Text("Dev Options", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Controls & Overlays
            item {
                SettingSection("Controls & Overlays") {
                    SettingSwitchRow(
                        label = "Show Floating Controls",
                        checked = currentSettings.showFloatingControls,
                        onCheckedChange = {
                            if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            currentSettings = currentSettings.copy(showFloatingControls = it)
                            onSettingsChanged(currentSettings)
                        }
                    )
                    Text(
                        "Floating bubble with quick pause, resume, stop, and facecam controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    HorizontalDivider(color = CardBlack, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        label = "Enable Facecam",
                        checked = currentSettings.enableFacecam,
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                            currentSettings = currentSettings.copy(enableFacecam = enable)
                            onSettingsChanged(currentSettings)
                        }
                    )
                    Text(
                        "Shows a floating front-camera preview window during screen recording",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Gestures
            item {
                SettingSection("Gestures") {
                    SettingSwitchRow(
                        label = "Shake to Stop",
                        checked = currentSettings.enableShakeToStop,
                        onCheckedChange = {
                            currentSettings = currentSettings.copy(enableShakeToStop = it)
                            onSettingsChanged(currentSettings)
                        }
                    )

                    if (currentSettings.enableShakeToStop) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Shake Sensitivity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            val label = when {
                                currentSettings.shakeSensitivity <= 9.0f -> "High (Easier)"
                                currentSettings.shakeSensitivity <= 14.0f -> "Medium (Recommended)"
                                else -> "Low (Harder)"
                            }
                            Text(
                                "$label (${String.format("%.1f", currentSettings.shakeSensitivity)} m/s²)",
                                style = MaterialTheme.typography.bodySmall,
                                color = FluxCyan
                            )
                        }

                        Slider(
                            value = currentSettings.shakeSensitivity,
                            onValueChange = {
                                currentSettings = currentSettings.copy(shakeSensitivity = it)
                                onSettingsChanged(currentSettings)
                            },
                            valueRange = 6.0f..20.0f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = FluxCyan,
                                activeTrackColor = FluxCyanDark,
                                inactiveTrackColor = CardBlack
                            )
                        )
                    }
                }
            }

            // Help & User Manual
            item {
                Card(
                    onClick = onNavigateToManual,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = FluxCyan)
                            Column {
                                Text(
                                    "User Manual & Guide",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Feature locations, step-by-step instructions & tips",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Storage Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Storage Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Movies/FluxRecorder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FluxCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Recordings are automatically saved to your device gallery under Movies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingRadioRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) FluxCyan else TextPrimary
        )
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = FluxCyan,
                unselectedColor = TextSecondary
            )
        )
    }
}

@Composable
fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FluxCyan,
                checkedTrackColor = FluxCyanDark,
                uncheckedThumbColor = TextDisabled,
                uncheckedTrackColor = CardBlack
            )
        )
    }
}
