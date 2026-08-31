package com.flux.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flux.recorder.ui.theme.*

/**
 * In-App Legal, Privacy Policy, and Terms & Conditions screen.
 * Fully readable offline and compliant with Google Play Store policies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    initialTab: Int = 0, // 0: Privacy Policy, 1: Terms & Conditions
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedTab == 0) "Privacy Policy" else "Terms & Conditions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val url = if (selectedTab == 0) {
                                "https://github.com/IcradleInnovationsLtd/FluxRecorder/blob/main/PRIVACY_POLICY.md"
                            } else {
                                "https://github.com/IcradleInnovationsLtd/FluxRecorder/blob/main/TERMS_AND_CONDITIONS.md"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, "Open Online", tint = FluxCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = VoidBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceBlack,
                contentColor = FluxCyan,
                divider = { HorizontalDivider(color = CardBlack) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Privacy Policy",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Terms & Conditions",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (selectedTab == 0) {
                    // Privacy Policy Content
                    item {
                        LegalHeaderCard(
                            title = "Privacy Policy for Flux Recorder",
                            updatedDate = "August 31, 2026",
                            organization = "Icradle Innovations Ltd",
                            badgeText = "100% Local & Private"
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.Shield,
                            title = "1. Zero Data Collection Policy",
                            content = "Flux Recorder does NOT collect, store, transmit, sell, or share any personal information, video recordings, audio recordings, or device telemetry.\n\n" +
                                    "• No Personal Identifiers (No names, emails, device IDs, or IP tracking)\n" +
                                    "• No Analytics SDKs (No Firebase Analytics, Facebook SDK, or third-party trackers)\n" +
                                    "• No Advertising Networks or profiling cookies\n" +
                                    "• No External Servers (All video and audio streams remain strictly on your local device)"
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.Lock,
                            title = "2. Permissions Overview & Purpose",
                            content = "Flux Recorder requires minimal permissions solely to execute local recording features:\n\n" +
                                    "• Microphone (RECORD_AUDIO): Captures voiceover and internal system audio locally.\n" +
                                    "• Camera (CAMERA): Optional — renders the floating Facecam preview window only when explicitly enabled.\n" +
                                    "• Display Over Other Apps (SYSTEM_ALERT_WINDOW): Renders the floating controls and Facecam overlay.\n" +
                                    "• Notifications (POST_NOTIFICATIONS): Shows foreground recording controls (Pause, Resume, Stop).\n" +
                                    "• Modify System Settings (WRITE_SETTINGS): Optional — enables visual touch pointer dots during active recording.\n" +
                                    "• Storage / Media (READ_MEDIA_VIDEO): Allows the in-app library and editor to list, play, trim, and share your recordings."
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.Folder,
                            title = "3. Local Storage & Full User Control",
                            content = "You retain 100% ownership and control over all recordings. Files are saved in your device's public Movies/FluxRecorder directory. You can edit, trim, export, or permanently delete any recording at any time."
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.ChildCare,
                            title = "4. Children's Privacy & Safety",
                            content = "Flux Recorder does not collect information from any user, including children under 13 (COPPA compliant). The application contains no ads and is safe for all audiences."
                        )
                    }

                    item {
                        LegalContactCard(
                            company = "Icradle Innovations Ltd",
                            email = "icradleinnovations@gmail.com",
                            website = "https://icradle.io"
                        )
                    }
                } else {
                    // Terms and Conditions Content
                    item {
                        LegalHeaderCard(
                            title = "Terms & Conditions",
                            updatedDate = "August 31, 2026",
                            organization = "Icradle Innovations Ltd",
                            badgeText = "Standard Agreement"
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.Gavel,
                            title = "1. Acceptance of Terms",
                            content = "By downloading, installing, or using Flux Recorder, you agree to be bound by these Terms and Conditions. If you do not agree to these terms, please do not use the application."
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.VerifiedUser,
                            title = "2. Permitted Use & License",
                            content = "Icradle Innovations Ltd grants you a revocable, non-exclusive, non-transferable license to use Flux Recorder for personal, educational, and commercial content creation in compliance with all applicable laws.\n\n" +
                                    "You agree NOT to use the application to capture unauthorized copyright-protected content, violate wiretapping or privacy laws, or reverse engineer the software."
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.Movie,
                            title = "3. User Responsibility & Content Ownership",
                            content = "All video recordings, audio tracks, and edited files created with Flux Recorder belong to you and reside entirely on your device. You are solely responsible for ensuring you have all necessary rights and consent for content captured using the application."
                        )
                    }

                    item {
                        LegalSectionCard(
                            icon = Icons.Default.WarningAmber,
                            title = "4. Disclaimers & Limitations of Liability",
                            content = "Flux Recorder is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind. Icradle Innovations Ltd is not liable for any indirect, incidental, or consequential damages resulting from device use, data loss, or third-party platform sharing."
                        )
                    }

                    item {
                        LegalContactCard(
                            company = "Icradle Innovations Ltd",
                            email = "icradleinnovations@gmail.com",
                            website = "https://icradle.io"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalHeaderCard(
    title: String,
    updatedDate: String,
    organization: String,
    badgeText: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceBlack,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FluxCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = FluxCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Organization: $organization", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("Last Updated: $updatedDate", style = MaterialTheme.typography.bodySmall, color = TextDisabled)
        }
    }
}

@Composable
private fun LegalSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceBlack,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBlack),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = FluxCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun LegalContactCard(
    company: String,
    email: String,
    website: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceBlack,
        border = androidx.compose.foundation.BorderStroke(1.dp, FluxCyan.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Contact & Publisher Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FluxCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Company: $company", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text("Email: $email", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text("Website: $website", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }
}
