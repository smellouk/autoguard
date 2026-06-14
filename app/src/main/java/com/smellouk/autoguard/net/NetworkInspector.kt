package com.smellouk.autoguard.net

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/** A snapshot of the current network, all the monitor needs to decide. */
data class NetworkState(
    val hasInternet: Boolean,
    val isWifi: Boolean,
    val isCellular: Boolean,
    /** SSID of the connected Wi-Fi, or null if unknown / not on Wi-Fi. */
    val ssid: String?,
    /** BSSID (access-point MAC) of the connected Wi-Fi, or null if unknown. */
    val bssid: String?,
    /** True when the OS reports an active VPN transport — i.e. a tunnel is *actually* up. */
    val vpnActive: Boolean = false,
)

/** Reads the current network without holding any long-lived references. */
object NetworkInspector {

    // Placeholder BSSID returned when the caller lacks location permission.
    private const val NO_BSSID = "02:00:00:00:00:00"

    fun current(context: Context): NetworkState {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Look at the *physical* networks, ignoring any VPN. When a tunnel is up
        // cm.activeNetwork is the VPN, which would otherwise hide the real Wi-Fi.
        var wifiCaps: NetworkCapabilities? = null
        var cellularCaps: NetworkCapabilities? = null
        var hasInternet = false
        var vpnActive = false

        @Suppress("DEPRECATION")
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) hasInternet = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) { vpnActive = true; continue }
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiCaps = caps
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> cellularCaps = caps
            }
        }

        if (wifiCaps != null) {
            val info = wifiInfo(context, wifiCaps)
            return NetworkState(
                hasInternet = true,
                isWifi = true,
                isCellular = false,
                ssid = info?.let { cleanSsid(it.ssid) },
                bssid = info?.let { cleanBssid(it.bssid) },
                vpnActive = vpnActive,
            )
        }
        if (cellularCaps != null) {
            return NetworkState(hasInternet = true, isWifi = false, isCellular = true, ssid = null, bssid = null, vpnActive = vpnActive)
        }
        return NetworkState(hasInternet = hasInternet, isWifi = false, isCellular = false, ssid = null, bssid = null, vpnActive = vpnActive)
    }

    private fun wifiInfo(context: Context, caps: NetworkCapabilities): WifiInfo? {
        if (!hasLocationPermission(context)) return null

        // Android 12+ can expose WifiInfo through the network capabilities — but
        // it may be redacted; only use it if the SSID actually came through.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (caps.transportInfo as? WifiInfo)?.let { if (cleanSsid(it.ssid) != null) return it }
        }

        // Fallback (and the only path pre-12): the still-functional, if deprecated,
        // WifiManager connection info. Requires location permission + location ON.
        @Suppress("DEPRECATION")
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        return wifi.connectionInfo
    }

    private fun cleanSsid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val s = raw.removeSurrounding("\"")
        if (s.isEmpty() || s == WifiManager.UNKNOWN_SSID || s == "<unknown ssid>") return null
        return s
    }

    private fun cleanBssid(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == NO_BSSID || raw == "00:00:00:00:00:00") return null
        return raw
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
