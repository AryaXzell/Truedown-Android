package com.aryaxzell.truedown.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.aryaxzell.truedown.ui.components.FloatingPillSnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aryaxzell.truedown.R
import com.aryaxzell.truedown.ui.components.StorageCleanerModal
import com.aryaxzell.truedown.ui.DeleteResult
import com.aryaxzell.truedown.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferences by viewModel.preferences.collectAsState()

    var showClearLibraryDialog by remember { mutableStateOf(false) }
    var clearFromGallery by remember { mutableStateOf(false) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showFallbackDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDohDialog by remember { mutableStateOf(false) }
    var showDeveloperLogsDialog by remember { mutableStateOf(false) }
    var showStorageCleanerModal by remember { mutableStateOf(false) }
    var showUpdateHistoryDialog by remember { mutableStateOf(false) }

    val db = remember { com.aryaxzell.truedown.data.local.TruedownDatabase.getInstance(context) }
    val updateHistoryList by db.updateHistoryDao().getAllHistory().collectAsState(initial = emptyList())

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)

                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                val folderName = docFile?.name ?: "Folder Kustom"

                viewModel.updateCustomDownloadDirectory(uri.toString(), folderName)
                Toast.makeText(context, "Folder unduhan diubah ke: $folderName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menetapkan folder: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        snackbarHost = {
            FloatingPillSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Card 1: Tampilan
            SettingsGroupCard(title = stringResource(R.string.settings_group_appearance)) {
                // Theme Option
                val themeLabel = when (preferences.themeMode) {
                    "LIGHT" -> stringResource(R.string.settings_theme_light)
                    "DARK" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                }
                SettingsClickableRow(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_label),
                    subtitle = themeLabel,
                    onClick = { showThemeDialog = true },
                    testTag = "settings_theme_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Dynamic Color Option
                val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsSwitchRow(
                    icon = Icons.Default.ColorLens,
                    title = stringResource(R.string.settings_dynamic_color_label),
                    subtitle = if (isDynamicColorSupported) {
                        stringResource(R.string.settings_dynamic_color_desc)
                    } else {
                        stringResource(R.string.settings_dynamic_color_unsupported)
                    },
                    checked = preferences.dynamicColor && isDynamicColorSupported,
                    enabled = isDynamicColorSupported,
                    onCheckedChange = { enabled ->
                        viewModel.updateDynamicColor(enabled)
                    },
                    onDisabledClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_dynamic_color_unsupported))
                        }
                    },
                    testTag = "settings_dynamic_color_switch",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Language Option
                val langLabel = when (preferences.language) {
                    "ID" -> stringResource(R.string.settings_lang_id)
                    "EN" -> stringResource(R.string.settings_lang_en)
                    else -> stringResource(R.string.settings_lang_system)
                }
                SettingsClickableRow(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_lang_label),
                    subtitle = langLabel,
                    onClick = { showLanguageDialog = true },
                    testTag = "settings_language_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Haptic Feedback Switch
                SettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.settings_haptic_title),
                    subtitle = if (preferences.hapticFeedback) {
                        stringResource(R.string.settings_haptic_desc_on)
                    } else {
                        stringResource(R.string.settings_haptic_desc_off)
                    },
                    checked = preferences.hapticFeedback,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateHapticFeedback(enabled)
                        if (enabled) {
                            com.aryaxzell.truedown.util.HapticFeedbackHelper.triggerSuccess(context, true)
                        }
                    },
                    testTag = "settings_haptic_feedback_switch",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // Card 2: Unduhan
            SettingsGroupCard(title = stringResource(R.string.settings_group_download)) {
                // Default Quality
                val qualityLabel = when (preferences.defaultQuality) {
                    "HD" -> stringResource(R.string.quality_hd)
                    else -> stringResource(R.string.quality_standard)
                }
                SettingsClickableRow(
                    icon = Icons.Default.HighQuality,
                    title = stringResource(R.string.settings_default_quality_label),
                    subtitle = qualityLabel,
                    onClick = { showQualityDialog = true },
                    testTag = "settings_quality_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Fallback Quality
                val fallbackLabel = when (preferences.qualityFallback) {
                    "FAIL" -> stringResource(R.string.settings_fallback_fail)
                    else -> stringResource(R.string.settings_fallback_auto)
                }
                SettingsClickableRow(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.settings_fallback_label),
                    subtitle = fallbackLabel,
                    onClick = { showFallbackDialog = true },
                    testTag = "settings_fallback_row",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Duplicate Rule
                val duplicateLabel = when (preferences.duplicateRule) {
                    "RE_DOWNLOAD" -> stringResource(R.string.settings_duplicate_redownload)
                    else -> stringResource(R.string.settings_duplicate_skip)
                }
                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_duplicate_label),
                    subtitle = duplicateLabel,
                    onClick = { showDuplicateDialog = true },
                    testTag = "settings_duplicate_row",
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Battery Saver Toggle
                SettingsSwitchRow(
                    icon = Icons.Default.BatteryAlert,
                    title = stringResource(R.string.settings_battery_saver_label),
                    subtitle = stringResource(R.string.settings_battery_saver_desc),
                    checked = preferences.batterySaver,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateBatterySaver(enabled)
                    },
                    testTag = "settings_battery_saver_switch",
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Wi-Fi Only Toggle
                SettingsSwitchRow(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.settings_wifi_only_label),
                    subtitle = stringResource(R.string.settings_wifi_only_desc),
                    checked = preferences.wifiOnly,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateWifiOnly(enabled)
                    },
                    testTag = "settings_wifi_only_switch",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Auto-Download Toggle
                SettingsSwitchRow(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.settings_auto_download_label),
                    subtitle = stringResource(R.string.settings_auto_download_desc),
                    checked = preferences.autoDownloadOnDetect,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateAutoDownloadOnDetect(enabled)
                    },
                    testTag = "settings_auto_download_switch",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Card: Penyimpanan & Cache
            SettingsGroupCard(title = "Penyimpanan & Cache") {
                // Folder Unduhan Kustom
                val folderSubtitle = if (preferences.customDownloadDirectoryName.isNotBlank()) {
                    preferences.customDownloadDirectoryName
                } else {
                    "Bawaan Sistem (Download/Truedown)"
                }
                SettingsClickableRow(
                    icon = Icons.Default.Folder,
                    title = "Folder Lokasi Unduhan",
                    subtitle = folderSubtitle,
                    onClick = { safFolderLauncher.launch(null) },
                    testTag = "settings_custom_folder_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                // Pembersih Cache Media
                SettingsClickableRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Pembersih Cache Media",
                    subtitle = "Bersihkan berkas temporary dan cache pratinjau media",
                    onClick = { showStorageCleanerModal = true },
                    testTag = "settings_storage_cleaner_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Card 3: Notifikasi
            SettingsGroupCard(title = stringResource(R.string.settings_group_notifications)) {
                SettingsClickableRow(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.settings_notif_system_label),
                    subtitle = stringResource(R.string.notif_btn_settings),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val detailIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(detailIntent)
                        }
                    },
                    testTag = "settings_notif_system_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingsSwitchRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_notif_actions_label),
                    subtitle = stringResource(R.string.settings_notif_actions_desc),
                    checked = preferences.showNotificationActions,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateShowNotificationActions(enabled)
                    },
                    testTag = "settings_notif_actions_switch",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            // Card 4: Penyimpanan & Library
            SettingsGroupCard(title = stringResource(R.string.settings_group_storage)) {
                SettingsClickableRow(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.settings_storage_location_title),
                    subtitle = stringResource(R.string.settings_storage_location_desc),
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("File disimpan di Pictures/Truedown, Movies/Truedown, dan Music/Truedown")
                        }
                    },
                    testTag = "settings_storage_location_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                val storageStatusText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "Status Akses: Aktif (MediaStore API Ready)"
                } else {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    if (granted) "Status Akses: Aktif" else "Status Akses: Perlu Izin Izin Penyimpanan"
                }

                SettingsClickableRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Status Izin Penyimpanan",
                    subtitle = storageStatusText,
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(storageStatusText)
                        }
                    },
                    testTag = "settings_storage_status_row",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingsClickableRow(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_clear_history_title),
                    subtitle = stringResource(R.string.settings_clear_history_desc),
                    onClick = {
                        clearFromGallery = false
                        showClearLibraryDialog = true
                    },
                    testTag = "settings_clear_history_row",
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }

            // Card: Jaringan & DNS (DoH)
            SettingsGroupCard(title = "Jaringan & DNS") {
                val currentDohName = com.aryaxzell.truedown.util.DohProvider.fromKey(preferences.dohProvider).displayName
                SettingsClickableRow(
                    icon = Icons.Default.Dns,
                    title = "DNS over HTTPS (DoH)",
                    subtitle = "Provider aktif: $currentDohName",
                    onClick = { showDohDialog = true },
                    testTag = "settings_doh_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            // Card: Pembaruan & Riwayat
            SettingsGroupCard(title = "Pembaruan & Riwayat") {
                SettingsClickableRow(
                    icon = Icons.Default.History,
                    title = "Riwayat Pembaruan Aplikasi",
                    subtitle = "Lihat status keberhasilan pembaruan sebelumnya",
                    onClick = {
                        scope.launch {
                            com.aryaxzell.truedown.util.UpdateHistoryLogger.ensureVersionHistoryInitialized(context)
                        }
                        showUpdateHistoryDialog = true
                    },
                    testTag = "settings_update_history_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // Card: Opsi Developer
            SettingsGroupCard(title = "Opsi Developer") {
                SettingsSwitchRow(
                    icon = Icons.Default.BugReport,
                    title = "Mode Developer",
                    subtitle = if (preferences.developerMode) "Aktif — Akses log aplikasi terbuka" else "Nonaktifkan fitur debugging",
                    checked = preferences.developerMode,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateDeveloperMode(enabled)
                    },
                    testTag = "settings_developer_mode_switch",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                if (preferences.developerMode) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    SettingsClickableRow(
                        icon = Icons.Default.Terminal,
                        title = "Log Aplikasi (Logs)",
                        subtitle = "Buka riwayat log aplikasi secara detail",
                        onClick = { showDeveloperLogsDialog = true },
                        testTag = "settings_logs_row",
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Card 5: Tentang
            SettingsGroupCard(title = stringResource(R.string.settings_group_about)) {
                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    title = "Truedown Android",
                    subtitle = stringResource(R.string.settings_app_version_format, com.aryaxzell.truedown.BuildConfig.VERSION_NAME) + " • Informasi & Pengembang",
                    onClick = { viewModel.navigateTo(com.aryaxzell.truedown.ui.AppScreen.About) },
                    testTag = "settings_version_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingsClickableRow(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.settings_restart_onboarding),
                    subtitle = stringResource(R.string.settings_restart_onboarding_desc),
                    onClick = {
                        viewModel.resetOnboarding()
                    },
                    testTag = "settings_reset_onboarding_row",
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.settings_lang_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val langs = listOf(
                        "SYSTEM" to stringResource(R.string.settings_lang_system),
                        "ID" to stringResource(R.string.settings_lang_id),
                        "EN" to stringResource(R.string.settings_lang_en)
                    )
                    langs.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = preferences.language == code,
                                    onClick = {
                                        viewModel.updateLanguage(code)
                                        showLanguageDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.language == code,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val themes = listOf(
                        "SYSTEM" to stringResource(R.string.settings_theme_system),
                        "LIGHT" to stringResource(R.string.settings_theme_light),
                        "DARK" to stringResource(R.string.settings_theme_dark)
                    )
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = preferences.themeMode == mode,
                                    onClick = {
                                        viewModel.updateTheme(mode)
                                        showThemeDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.settings_default_quality_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val options = listOf(
                        "STANDARD" to stringResource(R.string.quality_standard),
                        "HD" to stringResource(R.string.quality_hd)
                    )
                    options.forEach { (q, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = preferences.defaultQuality == q,
                                    onClick = {
                                        viewModel.updateDefaultQuality(q)
                                        showQualityDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.defaultQuality == q,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showQualityDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Fallback Dialog
    if (showFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showFallbackDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.settings_fallback_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val options = listOf(
                        "AUTO" to stringResource(R.string.settings_fallback_auto),
                        "FAIL" to stringResource(R.string.settings_fallback_fail)
                    )
                    options.forEach { (fb, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = preferences.qualityFallback == fb,
                                    onClick = {
                                        viewModel.updateQualityFallback(fb)
                                        showFallbackDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.qualityFallback == fb,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFallbackDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Duplicate Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.settings_duplicate_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    val options = listOf(
                        "SKIP" to stringResource(R.string.settings_duplicate_skip),
                        "RE_DOWNLOAD" to stringResource(R.string.settings_duplicate_redownload)
                    )
                    options.forEach { (rule, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = preferences.duplicateRule == rule,
                                    onClick = {
                                        viewModel.updateDuplicateRule(rule)
                                        showDuplicateDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.duplicateRule == rule,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDuplicateDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Clear Library Confirmation Dialog
    if (showClearLibraryDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearLibraryDialog = false
                clearFromGallery = false
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.dialog_clear_all_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_clear_all_message))
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { clearFromGallery = !clearFromGallery }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = clearFromGallery,
                                onCheckedChange = { clearFromGallery = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.dialog_delete_also_gallery),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (clearFromGallery) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deleteGal = clearFromGallery
                        showClearLibraryDialog = false
                        clearFromGallery = false
                        viewModel.clearAllLibrary(deleteGal) { result ->
                            scope.launch {
                                if (result is DeleteResult.DeletedButFilesFailed) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.delete_files_failed))
                                } else {
                                    snackbarHostState.showSnackbar("Riwayat Library berhasil dibersihkan")
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearLibraryDialog = false
                        clearFromGallery = false
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // DoH Dialog
    if (showDohDialog) {
        val providers = listOf(
            com.aryaxzell.truedown.util.DohProvider.SYSTEM,
            com.aryaxzell.truedown.util.DohProvider.CLOUDFLARE,
            com.aryaxzell.truedown.util.DohProvider.GOOGLE,
            com.aryaxzell.truedown.util.DohProvider.ADGUARD
        )
        AlertDialog(
            onDismissRequest = { showDohDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Pilih DNS over HTTPS (DoH)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    Text(
                        text = "Pilih DNS over HTTPS (DoH)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    providers.forEach { provider ->
                        val isSelected = preferences.dohProvider.equals(provider.key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateDohProvider(provider.key)
                                        showDohDialog = false
                                    },
                                    role = androidx.compose.ui.semantics.Role.RadioButton
                                )
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDohDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_close), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Developer Logs Dialog
    if (showDeveloperLogsDialog) {
        com.aryaxzell.truedown.ui.components.DeveloperLogsDialog(
            onDismiss = { showDeveloperLogsDialog = false }
        )
    }

    // Storage Cleaner Modal
    if (showStorageCleanerModal) {
        StorageCleanerModal(
            autoClearOnExit = preferences.autoClearCacheOnExit,
            onToggleAutoClear = { enabled -> viewModel.updateAutoClearCacheOnExit(enabled) },
            onCacheCleared = { freedStr ->
                scope.launch {
                    snackbarHostState.showSnackbar("Cache media sebesar $freedStr berhasil dibersihkan")
                }
            },
            onDismiss = { showStorageCleanerModal = false }
        )
    }

    // Update History Dialog
    if (showUpdateHistoryDialog) {
        val sdf = remember { java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showUpdateHistoryDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Pembaruan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (updateHistoryList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    db.updateHistoryDao().clearHistory()
                                }
                            }
                        ) {
                            Text("Bersihkan", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (updateHistoryList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada riwayat pembaruan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(updateHistoryList) { history ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = history.versionName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${history.updateType} • ${sdf.format(java.util.Date(history.timestamp))}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            // Status Badge
                                            val badgeBg = when (history.status) {
                                                "Success" -> MaterialTheme.colorScheme.primaryContainer
                                                "Failed" -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                            val badgeText = when (history.status) {
                                                "Success" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                "Failed" -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = badgeBg
                                            ) {
                                                Text(
                                                    text = when (history.status) {
                                                        "Success" -> "Selesai"
                                                        "Failed" -> "Gagal"
                                                        else -> history.status
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = badgeText,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (!history.errorMessage.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Error: ${history.errorMessage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUpdateHistoryDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsGroupCard(
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
private fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    iconContainerColor: androidx.compose.ui.graphics.Color? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null
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
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDisabledClick: (() -> Unit)? = null,
    testTag: String,
    iconContainerColor: androidx.compose.ui.graphics.Color? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null
) {
    val containerBg = iconContainerColor ?: MaterialTheme.colorScheme.secondaryContainer
    val tint = iconTint ?: MaterialTheme.colorScheme.secondary

    val activeState = stringResource(R.string.summary_active)
    val inactiveState = stringResource(R.string.summary_inactive)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = true,
                onClick = {
                    if (enabled) {
                        onCheckedChange(!checked)
                    } else {
                        onDisabledClick?.invoke()
                    }
                }
            )
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) activeState else inactiveState
                toggleableState = ToggleableState(checked)
            }
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (enabled) containerBg else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}
