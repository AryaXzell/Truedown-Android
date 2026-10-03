package com.aryaxzell.truedown.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun getLocale(languageCode: String): Locale? {
        return when (languageCode.uppercase()) {
            "ID", "INDONESIA", "INDONESIAN" -> Locale("in", "ID")
            "EN", "ENGLISH" -> Locale.ENGLISH
            else -> null
        }
    }

    fun setLocale(context: Context, languageCode: String): Context {
        val targetLocale = getLocale(languageCode)

        if (targetLocale == null) {
            Locale.setDefault(Locale.getDefault())
            return context
        }

        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }

        return context.createConfigurationContext(config)
    }

    fun getLocalizedConfiguration(context: Context, languageCode: String): Configuration {
        val targetLocale = getLocale(languageCode) ?: Locale.getDefault()
        Locale.setDefault(targetLocale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
        }
        return config
    }

    fun applyLanguage(activity: Activity, languageCode: String) {
        val targetLocale = getLocale(languageCode)

        if (targetLocale != null) {
            Locale.setDefault(targetLocale)
            val resources = activity.resources
            val config = Configuration(resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(targetLocale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = targetLocale
            }
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
