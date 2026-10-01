package com.aryaxzell.truedown.util

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

enum class DohProvider(val key: String, val displayName: String, val endpointUrl: String) {
    SYSTEM("SYSTEM", "Default Sistem", ""),
    CLOUDFLARE("CLOUDFLARE", "Cloudflare (1.1.1.1)", "https://1.1.1.1/dns-query"),
    GOOGLE("GOOGLE", "Google (8.8.8.8)", "https://dns.google/resolve"),
    ADGUARD("ADGUARD", "AdGuard DNS", "https://dns.adguard-dns.com/resolve");

    companion object {
        fun fromKey(key: String): DohProvider {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class DohDns(private val providerKey: String) : Dns {

    private val provider = DohProvider.fromKey(providerKey)

    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        if (provider == DohProvider.SYSTEM || provider.endpointUrl.isBlank()) {
            return Dns.SYSTEM.lookup(hostname)
        }

        return try {
            val url = provider.endpointUrl.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("name", hostname)
                ?.addQueryParameter("type", "A")
                ?.build() ?: return Dns.SYSTEM.lookup(hostname)

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()

            bootstrapClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Dns.SYSTEM.lookup(hostname)
                val body = response.body?.string() ?: return Dns.SYSTEM.lookup(hostname)
                val json = JSONObject(body)
                val answers = json.optJSONArray("Answer") ?: return Dns.SYSTEM.lookup(hostname)

                val ips = mutableListOf<InetAddress>()
                for (i in 0 until answers.length()) {
                    val obj = answers.getJSONObject(i)
                    // Type 1 is A record (IPv4)
                    if (obj.optInt("type") == 1) {
                        val ipStr = obj.optString("data")
                        if (ipStr.isNotBlank()) {
                            ips.add(InetAddress.getByName(ipStr))
                        }
                    }
                }

                if (ips.isNotEmpty()) {
                    AppLogger.d("DoHDns", "Resolved '$hostname' via ${provider.displayName} -> $ips")
                    ips
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
        } catch (e: Exception) {
            AppLogger.w("DoHDns", "DoH lookup failed for '$hostname' via ${provider.displayName}, fallback to system: ${e.message}")
            Dns.SYSTEM.lookup(hostname)
        }
    }
}
