package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.MainViewModel
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

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    testTag = "settings_theme_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                    testTag = "settings_dynamic_color_switch"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                    testTag = "settings_language_row"
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
                    testTag = "settings_quality_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                    testTag = "settings_fallback_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                    testTag = "settings_duplicate_row"
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
                    testTag = "settings_notif_system_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_notif_actions_label),
                    subtitle = stringResource(R.string.settings_notif_actions_desc),
                    checked = preferences.showNotificationActions,
                    enabled = true,
                    onCheckedChange = { enabled ->
                        viewModel.updateShowNotificationActions(enabled)
                    },
                    testTag = "settings_notif_actions_switch"
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
                    testTag = "settings_storage_location_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableRow(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_clear_history_title),
                    subtitle = stringResource(R.string.settings_clear_history_desc),
                    onClick = {
                        clearFromGallery = false
                        showClearLibraryDialog = true
                    },
                    testTag = "settings_clear_history_row"
                )
            }

            // Card 5: Tentang
            SettingsGroupCard(title = stringResource(R.string.settings_group_about)) {
                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    title = "Truedown Android",
                    subtitle = "Versi 1.0.0 (Build Release)",
                    onClick = {},
                    testTag = "settings_version_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableRow(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = stringResource(R.string.settings_check_update_title),
                    subtitle = "Buka halaman GitHub Releases",
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aryaxzell/truedown-android/releases"))
                        context.startActivity(browserIntent)
                    },
                    testTag = "settings_update_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableRow(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.settings_restart_onboarding),
                    subtitle = stringResource(R.string.settings_restart_onboarding_desc),
                    onClick = {
                        viewModel.resetOnboarding()
                    },
                    testTag = "settings_reset_onboarding_row"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_lang_label)) },
            text = {
                Column {
                    val langs = listOf(
                        "SYSTEM" to stringResource(R.string.settings_lang_system),
                        "ID" to stringResource(R.string.settings_lang_id),
                        "EN" to stringResource(R.string.settings_lang_en)
                    )
                    langs.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.language == code,
                                onClick = {
                                    viewModel.updateLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme_label)) },
            text = {
                Column {
                    val themes = listOf(
                        "SYSTEM" to stringResource(R.string.settings_theme_system),
                        "LIGHT" to stringResource(R.string.settings_theme_light),
                        "DARK" to stringResource(R.string.settings_theme_dark)
                    )
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateTheme(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    viewModel.updateTheme(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(stringResource(R.string.settings_default_quality_label)) },
            text = {
                Column {
                    val options = listOf(
                        "STANDARD" to stringResource(R.string.quality_standard),
                        "HD" to stringResource(R.string.quality_hd)
                    )
                    options.forEach { (q, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateDefaultQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.defaultQuality == q,
                                onClick = {
                                    viewModel.updateDefaultQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Fallback Dialog
    if (showFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showFallbackDialog = false },
            title = { Text(stringResource(R.string.settings_fallback_label)) },
            text = {
                Column {
                    val options = listOf(
                        "AUTO" to stringResource(R.string.settings_fallback_auto),
                        "FAIL" to stringResource(R.string.settings_fallback_fail)
                    )
                    options.forEach { (fb, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateQualityFallback(fb)
                                    showFallbackDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.qualityFallback == fb,
                                onClick = {
                                    viewModel.updateQualityFallback(fb)
                                    showFallbackDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFallbackDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Duplicate Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text(stringResource(R.string.settings_duplicate_label)) },
            text = {
                Column {
                    val options = listOf(
                        "SKIP" to stringResource(R.string.settings_duplicate_skip),
                        "RE_DOWNLOAD" to stringResource(R.string.settings_duplicate_redownload)
                    )
                    options.forEach { (rule, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateDuplicateRule(rule)
                                    showDuplicateDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.duplicateRule == rule,
                                onClick = {
                                    viewModel.updateDuplicateRule(rule)
                                    showDuplicateDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Clear Library Confirmation Dialog
    if (showClearLibraryDialog) {
        AlertDialog(
            onDismissRequest = { showClearLibraryDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_all_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_clear_all_message))
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { clearFromGallery = !clearFromGallery }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = clearFromGallery,
                            onCheckedChange = { clearFromGallery = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_also_gallery),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (clearFromGallery) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.dialog_delete_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLibrary(clearFromGallery)
                        showClearLibraryDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Riwayat Library berhasil dibersihkan")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLibraryDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
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
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
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
