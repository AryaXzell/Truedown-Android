package com.aryaxzell.truedown.data.preferences

data class UserPreferences(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val language: String = "SYSTEM",
    val hapticFeedback: Boolean = true,
    val defaultQuality: String = "STANDARD",
    val qualityFallback: String = "AUTO",
    val duplicateRule: String = "SKIP",
    val showNotificationActions: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val developerMode: Boolean = false,
    val dohProvider: String = "SYSTEM"
)
