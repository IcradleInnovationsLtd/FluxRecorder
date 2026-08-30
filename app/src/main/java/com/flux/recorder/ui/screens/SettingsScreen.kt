package com.flux.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    onSettingsChanged: (RecordingSettings) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }
    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Quality
            item {
                SettingSection("Video Quality") {
                    VideoQuality.entries.forEach { quality ->
                        SettingRadioButton(
                            label = quality.displayName,
                            selected = currentSettings.videoQuality == quality,
                            onClick = {
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
                    FrameRate.entries.forEach { fps ->
                        SettingRadioButton(
                            label = fps.displayName,
                            selected = currentSettings.frameRate == fps,
                            onClick = {
                                currentSettings = currentSettings.copy(frameRate = fps)
                                onSettingsChanged(currentSettings)
                            }
                        )
                    }
                }
            }

            // Audio Source
            item {
                SettingSection("Audio Source") {
                    AudioSource.entries.forEach { source ->
                        SettingRadioButton(
                            label = source.displayName,
                            selected = currentSettings.audioSource == source,
                            onClick = {
                                currentSettings = currentSettings.copy(audioSource = source)
                                onSettingsChanged(currentSettings)
                            }
                        )
                    }
                }
            }

            // Facecam & Overlay
            item {
                SettingSection("Camera & Overlay") {
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
        colors = CardDefaults.cardColors(
            containerColor = SurfaceBlack
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = FluxCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingRadioButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ElectricViolet,
                unselectedColor = TextDisabled
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
