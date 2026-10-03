package com.aryaxzell.truedown.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "truedown_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_feedback")
        val KEY_QUALITY = stringPreferencesKey("default_quality")
        val KEY_FALLBACK = stringPreferencesKey("quality_fallback")
        val KEY_DUPLICATE = stringPreferencesKey("duplicate_rule")
        val KEY_NOTIF_ACTIONS = booleanPreferencesKey("notif_actions")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val KEY_DOH_PROVIDER = stringPreferencesKey("doh_provider")
        val KEY_BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_AUTO_DOWNLOAD_ON_DETECT = booleanPreferencesKey("auto_download_on_detect")
        val KEY_CUSTOM_DOWNLOAD_URI = stringPreferencesKey("custom_download_uri")
        val KEY_CUSTOM_DOWNLOAD_NAME = stringPreferencesKey("custom_download_name")
        val KEY_AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[KEY_THEME] ?: "SYSTEM",
            dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: true,
            language = preferences[KEY_LANGUAGE] ?: "SYSTEM",
            hapticFeedback = preferences[KEY_HAPTIC] ?: true,
            defaultQuality = preferences[KEY_QUALITY] ?: "STANDARD",
            qualityFallback = preferences[KEY_FALLBACK] ?: "AUTO",
            duplicateRule = preferences[KEY_DUPLICATE] ?: "SKIP",
            showNotificationActions = preferences[KEY_NOTIF_ACTIONS] ?: true,
            onboardingCompleted = preferences[KEY_ONBOARDING] ?: false,
            developerMode = preferences[KEY_DEVELOPER_MODE] ?: false,
            dohProvider = preferences[KEY_DOH_PROVIDER] ?: "SYSTEM",
            batterySaver = preferences[KEY_BATTERY_SAVER] ?: false,
            wifiOnly = preferences[KEY_WIFI_ONLY] ?: false,
            autoDownloadOnDetect = preferences[KEY_AUTO_DOWNLOAD_ON_DETECT] ?: false,
            customDownloadDirectoryUri = preferences[KEY_CUSTOM_DOWNLOAD_URI] ?: "",
            customDownloadDirectoryName = preferences[KEY_CUSTOM_DOWNLOAD_NAME] ?: "",
            autoClearCacheOnExit = preferences[KEY_AUTO_CLEAR_CACHE] ?: false
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC] = enabled }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { it[KEY_QUALITY] = quality }
    }

    suspend fun setQualityFallback(fallback: String) {
        context.dataStore.edit { it[KEY_FALLBACK] = fallback }
    }

    suspend fun setDuplicateRule(rule: String) {
        context.dataStore.edit { it[KEY_DUPLICATE] = rule }
    }

    suspend fun setShowNotificationActions(show: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_ACTIONS] = show }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = completed }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEVELOPER_MODE] = enabled }
    }

    suspend fun setDohProvider(provider: String) {
        context.dataStore.edit { it[KEY_DOH_PROVIDER] = provider }
    }

    suspend fun setBatterySaver(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BATTERY_SAVER] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setAutoDownloadOnDetect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_DOWNLOAD_ON_DETECT] = enabled }
    }

    suspend fun setCustomDownloadDirectory(uri: String, name: String) {
        context.dataStore.edit {
            it[KEY_CUSTOM_DOWNLOAD_URI] = uri
            it[KEY_CUSTOM_DOWNLOAD_NAME] = name
        }
    }

    suspend fun setAutoClearCacheOnExit(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CLEAR_CACHE] = enabled }
    }
}
