package com.aryaxzell.truedown.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material.icons.filled.Terminal
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
                    subtitle = stringResource(R.string.settings_app_version_format, com.aryaxzell.truedown.BuildConfig.VERSION_NAME),
                    onClick = {},
                    testTag = "settings_version_row",
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                SettingsClickableRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = stringResource(R.string.settings_check_update_title),
                    subtitle = "Buka halaman GitHub Releases",
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aryaxzell/truedown-android/releases"))
                        context.startActivity(browserIntent)
                    },
                    testTag = "settings_update_row",
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (enabled) {
                    onCheckedChange(!checked)
                } else {
                    onDisabledClick?.invoke()
                }
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
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}
