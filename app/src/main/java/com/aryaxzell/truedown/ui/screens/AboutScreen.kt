package com.aryaxzell.truedown.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aryaxzell.truedown.BuildConfig
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.ui.components.ChangelogModal
import com.aryaxzell.truedown.ui.components.LicenseModal
import com.aryaxzell.truedown.util.NightlyUpdateManager
import com.aryaxzell.truedown.util.NightlyUpdateState
import com.aryaxzell.truedown.util.ReleaseUpdateManager
import com.aryaxzell.truedown.util.ReleaseUpdateState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateState by NightlyUpdateManager.updateState.collectAsState()
    val releaseUpdateState by ReleaseUpdateManager.updateState.collectAsState()

    var showNightlyOptionsDialog by remember { mutableStateOf(false) }
    var showChangelogModal by remember { mutableStateOf(false) }
    var showLicenseModal by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tentang Aplikasi",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero App Header Card (HIG / Material 3 Expressive)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo Container
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_truedown_logo),
                                contentDescription = "Truedown Logo",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Truedown Android",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Version Pill Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Versi ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aplikasi pengunduh video, slideshow foto, dan audio TikTok tanpa watermark secepat kilat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Saluran Pembaruan Versi Section
            AboutGroupCard(title = "Saluran Pembaruan Versi") {
                // Saluran Stabil (Official Release In-App Update)
                AboutClickableRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Saluran Stabil (Stable Release)",
                    subtitle = "Periksa dan pasang pembaruan rilis resmi langsung di dalam app",
                    onClick = {
                        scope.launch {
                            ReleaseUpdateManager.checkForReleaseUpdate(context)
                        }
                    },
                    testTag = "about_check_update_stable_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    showExternalIcon = false
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Saluran Nightly (In-App Installer & Browser Options)
                AboutClickableRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Saluran Nightly (In-App Update)",
                    subtitle = "Unduh dan pasang build otomatis langsung di dalam app",
                    onClick = {
                        showNightlyOptionsDialog = true
                    },
                    testTag = "about_check_update_nightly_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    showExternalIcon = false
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Catatan Rilis (Changelog Modal)
                AboutClickableRow(
                    icon = Icons.Default.History,
                    title = "Catatan Rilis (Changelog)",
                    subtitle = "Lihat riwayat pembaruan dan fitur baru aplikasi",
                    onClick = {
                        showChangelogModal = true
                    },
                    testTag = "about_changelog_row",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    showExternalIcon = false
                )
            }

            // Informasi & Tautan Pengembang Section
            AboutGroupCard(title = "Informasi & Pengembang") {
                // Developer Row
                AboutClickableRow(
                    icon = Icons.Default.Person,
                    title = "Pengembang Aplikasi",
                    subtitle = "Arya Vallencia (@aryaxzell)",
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/aryaxzell")
                        )
                        context.startActivity(browserIntent)
                    },
                    testTag = "about_developer_row",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    showExternalIcon = true
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // GitHub Source Code Row
                AboutClickableRow(
                    icon = Icons.Default.Code,
                    title = "Kode Sumber GitHub",
                    subtitle = "github.com/aryaxzell/truedown-android",
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/aryaxzell/truedown-android")
                        )
                        context.startActivity(browserIntent)
                    },
                    testTag = "about_github_repo_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    showExternalIcon = true
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // License Row (Opens in-app Full License Modal)
                AboutClickableRow(
                    icon = Icons.Default.Description,
                    title = "Lisensi Software",
                    subtitle = "MIT License — Perangkat Lunak Bebas & Terbuka",
                    onClick = {
                        showLicenseModal = true
                    },
                    testTag = "about_license_row",
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    showExternalIcon = false
                )
            }

            // App Tech Stack & Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Dibuat dengan ❤️ oleh Arya Vallencia",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kotlin • Jetpack Compose • Material 3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    // Nightly Update Option Dialog
    if (showNightlyOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showNightlyOptionsDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Pembaruan Saluran Nightly",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pilih metode pengunduhan untuk build prarilis terbaru (Nightly):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            showNightlyOptionsDialog = false
                            if (!NightlyUpdateManager.canInstallUnknownApps(context)) {
                                NightlyUpdateManager.openInstallPermissionSettings(context)
                            } else {
                                scope.launch {
                                    NightlyUpdateManager.downloadAndInstallNightly(context)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unduh & Install In-App (Langsung)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showNightlyOptionsDialog = false
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://nightly.link/AryaXzell/Truedown-Android/workflows/build/main")
                            )
                            context.startActivity(browserIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Tautan di Browser", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showNightlyOptionsDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // In-App Progress Dialog for Nightly Update
    if (updateState !is NightlyUpdateState.Idle) {
        val currentState = updateState
        com.aryaxzell.truedown.ui.components.UpdateProgressDialog(
            title = "Mengunduh Versi Nightly",
            progress = if (currentState is NightlyUpdateState.Downloading) currentState.progress else 0f,
            downloadedBytes = if (currentState is NightlyUpdateState.Downloading) currentState.downloadedBytes else 0L,
            totalBytes = if (currentState is NightlyUpdateState.Downloading) currentState.totalBytes else 0L,
            speedBytesPerSec = if (currentState is NightlyUpdateState.Downloading) currentState.speedBytesPerSec else 0L,
            remainingSeconds = if (currentState is NightlyUpdateState.Downloading) currentState.remainingSeconds else -1L,
            statusText = when (currentState) {
                is NightlyUpdateState.Checking -> "Memeriksa ketersediaan build Nightly terbaru di GitHub..."
                is NightlyUpdateState.Extracting -> currentState.status
                is NightlyUpdateState.ReadyToInstall -> "File APK Nightly (${currentState.selectedAbi}) berhasil diekstrak dan siap dipasang."
                else -> "Mengunduh berkas APK..."
            },
            isChecking = currentState is NightlyUpdateState.Checking,
            isVerifying = currentState is NightlyUpdateState.Extracting,
            isReady = currentState is NightlyUpdateState.ReadyToInstall,
            errorMessage = if (currentState is NightlyUpdateState.Error) currentState.message else null,
            onDismiss = {
                NightlyUpdateManager.cancelDownload(context)
            },
            onConfirm = {
                if (currentState is NightlyUpdateState.ReadyToInstall) {
                    NightlyUpdateManager.installApk(context, currentState.apkFile)
                }
            }
        )
    }

    // In-App Progress Dialog for Release Update
    if (releaseUpdateState !is ReleaseUpdateState.Idle) {
        val relState = releaseUpdateState
        if (relState is ReleaseUpdateState.Checking || relState is ReleaseUpdateState.UpToDate || relState is ReleaseUpdateState.UpdateAvailable) {
            AlertDialog(
                onDismissRequest = {
                    if (relState is ReleaseUpdateState.UpToDate) {
                        ReleaseUpdateManager.resetState()
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                icon = {
                    Icon(
                        imageVector = when (relState) {
                            is ReleaseUpdateState.UpToDate -> Icons.Default.CheckCircle
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = when (relState) {
                            is ReleaseUpdateState.Checking -> "Memeriksa Rilis Resmi"
                            is ReleaseUpdateState.UpToDate -> "Aplikasi Sudah Terbaru"
                            is ReleaseUpdateState.UpdateAvailable -> "Pembaruan Rilis Ditemukan (${relState.tag})"
                            else -> "Pembaruan Versi Rilis"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (relState) {
                            is ReleaseUpdateState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Menghubungkan ke GitHub Releases API...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            is ReleaseUpdateState.UpToDate -> {
                                Text(
                                    text = "Aplikasi Truedown Anda sudah menggunakan versi rilis stabil paling baru (${relState.currentVersion}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            is ReleaseUpdateState.UpdateAvailable -> {
                                Text(
                                    text = relState.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Catatan Rilis",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Dapat digulir",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 90.dp, max = 220.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = relState.releaseNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Arsitektur HP: ${relState.selectedAbi} • Ukuran File: ${formatBytes(relState.apkSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            else -> {}
                        }
                    }
                },
                confirmButton = {
                    when (relState) {
                        is ReleaseUpdateState.UpdateAvailable -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        ReleaseUpdateManager.downloadAndInstallRelease(context, relState)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Unduh & Pasang Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                        is ReleaseUpdateState.UpToDate -> {
                            Button(
                                onClick = {
                                    ReleaseUpdateManager.resetState()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Tutup", fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }
                },
                dismissButton = {
                    if (relState is ReleaseUpdateState.UpdateAvailable) {
                        TextButton(
                            onClick = {
                                ReleaseUpdateManager.resetState()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        } else {
            // Gunakan komponen UpdateProgressDialog kustom untuk melacak progres pengunduhan dan verifikasi
            com.aryaxzell.truedown.ui.components.UpdateProgressDialog(
                title = "Mengunduh Versi Rilis Resmi",
                progress = if (relState is ReleaseUpdateState.Downloading) relState.progress else 0f,
                downloadedBytes = if (relState is ReleaseUpdateState.Downloading) relState.downloadedBytes else 0L,
                totalBytes = if (relState is ReleaseUpdateState.Downloading) relState.totalBytes else 0L,
                speedBytesPerSec = if (relState is ReleaseUpdateState.Downloading) relState.speedBytesPerSec else 0L,
                remainingSeconds = if (relState is ReleaseUpdateState.Downloading) relState.remainingSeconds else -1L,
                statusText = when (relState) {
                    is ReleaseUpdateState.Verifying -> relState.status
                    is ReleaseUpdateState.ReadyToInstall -> "File APK Rilis Resmi (${relState.selectedAbi}) berhasil diunduh dan diverifikasi (SHA-256 Valid)."
                    else -> "Mengunduh berkas APK..."
                },
                isVerifying = relState is ReleaseUpdateState.Verifying,
                isReady = relState is ReleaseUpdateState.ReadyToInstall,
                errorMessage = if (relState is ReleaseUpdateState.Error) relState.message else null,
                onDismiss = {
                    ReleaseUpdateManager.cancelDownload(context)
                },
                onConfirm = {
                    if (relState is ReleaseUpdateState.ReadyToInstall) {
                        ReleaseUpdateManager.installApk(context, relState.apkFile)
                    }
                }
            )
        }
    }

    // Changelog Modal Dialog
    if (showChangelogModal) {
        ChangelogModal(
            onDismiss = { showChangelogModal = false }
        )
    }

    // Software License Modal Dialog
    if (showLicenseModal) {
        LicenseModal(
            onDismiss = { showLicenseModal = false }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 0 -> "Menghitung..."
        seconds == 0L -> "Hampir Selesai"
        seconds < 60 -> "$seconds dtk"
        else -> {
            val mins = seconds / 60
            val secs = seconds % 60
            if (secs > 0) "$mins mnt $secs dtk" else "$mins mnt"
        }
    }
}

@Composable
private fun AboutGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 2.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun AboutClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    iconContainerColor: androidx.compose.ui.graphics.Color? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    showExternalIcon: Boolean = false
) {
    val containerBg = iconContainerColor ?: MaterialTheme.colorScheme.primaryContainer
    val tint = iconTint ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerBg,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showExternalIcon) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
