package com.smellouk.autoguard.net

import android.content.Context
import android.net.wifi.WifiManager

/** A nearby network from a Wi-Fi scan, deduped by SSID (strongest BSSID kept). */
data class ScannedNetwork(val ssid: String, val bssid: String?, val level: Int)

/**
 * Lists nearby Wi-Fi networks from the system's scan cache. Reading results
 * requires location permission and location services on (same Android rule as
 * reading the connected SSID). All calls are wrapped so a SecurityException or
 * a disabled adapter just yields an empty list.
 */
object WifiScanner {

    fun results(context: Context): List<ScannedNetwork> {
        if (!NetworkInspector.hasLocationPermission(context)) return emptyList()
        val wifi = wifiManager(context) ?: return emptyList()

        @Suppress("DEPRECATION")
        val raw = runCatching { wifi.scanResults }.getOrNull().orEmpty()
        return raw
            .filterNot { it.SSID.isNullOrBlank() } // hidden networks report empty SSID
            .groupBy { it.SSID }
            .map { (ssid, list) ->
                val best = list.maxByOrNull { it.level }!!
                ScannedNetwork(ssid = ssid, bssid = best.BSSID, level = best.level)
            }
            .sortedByDescending { it.level }
    }

    /** Best-effort request for fresh results. Throttled by the OS on Android 9+. */
    fun requestScan(context: Context) {
        @Suppress("DEPRECATION")
        runCatching { wifiManager(context)?.startScan() }
    }

    private fun wifiManager(context: Context): WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
}
