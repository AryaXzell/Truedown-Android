package com.aryaxzell.truedown.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utilitas untuk menentukan aktivasi tema gelap (Dark Mode) secara otomatis
 * berdasarkan waktu sistem atau perhitungan matahari terbit & terbenam (sunset/sunrise).
 */
object AutoThemeHelper {

    data class SolarTimes(
        val sunriseHour: Int,
        val sunriseMinute: Int,
        val sunsetHour: Int,
        val sunsetMinute: Int
    ) {
        val sunriseFormatted: String
            get() = String.format(Locale.getDefault(), "%02d:%02d", sunriseHour, sunriseMinute)

        val sunsetFormatted: String
            get() = String.format(Locale.getDefault(), "%02d:%02d", sunsetHour, sunsetMinute)
    }

    /**
     * Memeriksa apakah mode gelap harus aktif berdasarkan pengaturan tema yang dipilih.
     */
    fun isDarkThemeActive(context: Context, themeMode: String): Boolean {
        val mode = themeMode.uppercase()
        return when (mode) {
            "LIGHT" -> false
            "DARK" -> true
            "AUTO_TIME" -> isNightBySystemTime()
            "AUTO_SUNSET" -> isNightBySolar(context)
            else -> false // Default fallback ke sistem ditangani oleh caller
        }
    }

    /**
     * Otomatis waktu sistem: Gelap antara 18:00 - 06:00.
     */
    fun isNightBySystemTime(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 18 || hour < 6
    }

    /**
     * Otomatis matahari: Gelap sebelum terbit atau sesudah terbenam.
     */
    fun isNightBySolar(context: Context): Boolean {
        return try {
            val solar = calculateSolarTimes(context)
            val cal = Calendar.getInstance()
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val sunriseMinutes = solar.sunriseHour * 60 + solar.sunriseMinute
            val sunsetMinutes = solar.sunsetHour * 60 + solar.sunsetMinute

            currentMinutes < sunriseMinutes || currentMinutes >= sunsetMinutes
        } catch (_: Throwable) {
            isNightBySystemTime()
        }
    }

    /**
     * Menghitung perkiraan waktu matahari terbit dan terbenam untuk hari ini
     * berdasarkan koordinat lokasi (atau fallback koordinat zona waktu).
     */
    fun calculateSolarTimes(context: Context): SolarTimes {
        return try {
            val coords = getCoordinates(context)
            val lat = coords.first
            val lon = coords.second

            val cal = Calendar.getInstance()
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val tzOffsetHours = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0

            val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)
            val eqtime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                    0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
            val declRad = Math.toRadians(0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                    0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
                    0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)) * (180.0 / Math.PI)

            val latRad = Math.toRadians(lat)
            val zenithRad = Math.toRadians(90.833)

            val cosHa = (cos(zenithRad) / (cos(latRad) * cos(Math.toRadians(declRad)))) -
                    (sin(latRad) * sin(Math.toRadians(declRad)) / (cos(latRad) * cos(Math.toRadians(declRad))))

            val haDegrees = if (cosHa.isNaN() || cosHa > 1.0) 0.0 else if (cosHa < -1.0) 180.0 else Math.toDegrees(acos(cosHa))

            val solarNoonUtcMinutes = 720.0 - (4.0 * lon) - eqtime
            val sunriseUtcMinutes = solarNoonUtcMinutes - (haDegrees * 4.0)
            val sunsetUtcMinutes = solarNoonUtcMinutes + (haDegrees * 4.0)

            val sunriseLocalMinutes = ((sunriseUtcMinutes + tzOffsetHours * 60.0).toInt() + 1440) % 1440
            val sunsetLocalMinutes = ((sunsetUtcMinutes + tzOffsetHours * 60.0).toInt() + 1440) % 1440

            SolarTimes(
                sunriseHour = (sunriseLocalMinutes / 60).coerceIn(0, 23),
                sunriseMinute = (sunriseLocalMinutes % 60).coerceIn(0, 59),
                sunsetHour = (sunsetLocalMinutes / 60).coerceIn(0, 23),
                sunsetMinute = (sunsetLocalMinutes % 60).coerceIn(0, 59)
            )
        } catch (_: Throwable) {
            SolarTimes(6, 0, 18, 0)
        }
    }

    private fun getCoordinates(context: Context): Pair<Double, Double> {
        try {
            val hasLocationPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPerm) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val loc: Location? = lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) {
                    return Pair(loc.latitude, loc.longitude)
                }
            }
        } catch (_: Exception) {}

        // Fallback akurat berdasarkan zona waktu lokal
        val tzId = TimeZone.getDefault().id.uppercase()
        val tzOffset = TimeZone.getDefault().rawOffset / 3600000

        return when {
            tzId.contains("JAKARTA") || tzId.contains("WESTERN_INDONESIA") -> Pair(-6.2, 106.8)
            tzId.contains("MAKASSAR") || tzId.contains("CENTRAL_INDONESIA") -> Pair(-5.1, 119.4)
            tzId.contains("JAYAPURA") || tzId.contains("EASTERN_INDONESIA") -> Pair(-2.5, 140.7)
            tzId.contains("BANGKOK") -> Pair(13.7, 100.5)
            tzId.contains("SINGAPORE") || tzId.contains("KUALA_LUMPUR") -> Pair(1.3, 103.8)
            tzId.contains("TOKYO") -> Pair(35.6, 139.7)
            tzId.contains("NEW_YORK") -> Pair(40.7, -74.0)
            tzId.contains("LOS_ANGELES") -> Pair(34.0, -118.2)
            tzId.contains("LONDON") -> Pair(51.5, -0.1)
            tzId.contains("PARIS") || tzId.contains("BERLIN") -> Pair(48.8, 2.3)
            else -> {
                // Perkiraan bujur dari offset jam zona waktu (15 derajat per jam offset)
                val estimatedLon = (tzOffset * 15.0).coerceIn(-180.0, 180.0)
                Pair(0.0, estimatedLon)
            }
        }
    }
}
