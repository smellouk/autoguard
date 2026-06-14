package com.smellouk.autoguard.net

/**
 * A rule for one Wi-Fi network.
 *
 * @param ssid     the network name (case-sensitive, as Android reports it).
 * @param bssids   pinned access-point MAC addresses. If non-empty the rule only
 *                 matches when the connected BSSID is one of these — this is the
 *                 anti-spoofing guard: an attacker broadcasting your SSID on a
 *                 different router won't match, so it's treated as untrusted.
 * @param trusted  true = home/safe network → tunnel stays OFF here.
 *                 false = an untrusted network you want specific tunnel(s) on.
 * @param tunnels  which tunnel(s) to raise when this (untrusted) rule matches.
 *                 Empty falls back to the global default tunnels.
 */
data class NetworkRule(
    val ssid: String,
    val bssids: Set<String> = emptySet(),
    val trusted: Boolean = true,
    val tunnels: List<String> = emptyList(),
) {
    fun matches(currentSsid: String?, currentBssid: String?): Boolean {
        if (currentSsid == null || currentSsid != ssid) return false
        if (bssids.isEmpty()) return true
        return currentBssid != null && bssids.any { it.equals(currentBssid, ignoreCase = true) }
    }
}
