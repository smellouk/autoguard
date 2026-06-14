package com.smellouk.autoguard.net

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic tests — no Android runtime needed.
 * Run with: ./gradlew testDebugUnitTest
 */
class TunnelDeciderTest {

    private fun config(
        enabled: Boolean = true,
        override: OverrideMode = OverrideMode.AUTO,
        default: List<String> = listOf("home"),
        rules: List<NetworkRule> = listOf(NetworkRule(ssid = "MyHome", trusted = true)),
        mobile: Boolean = true,
        unknownUntrusted: Boolean = true,
    ) = TunnelConfig(enabled, override, default, rules, mobile, unknownUntrusted)

    private fun wifi(ssid: String?, bssid: String? = null) =
        NetworkState(hasInternet = true, isWifi = true, isCellular = false, ssid = ssid, bssid = bssid)

    private fun up(vararg t: String) = Decision.Apply(t.toSet())

    @Test fun homeWifi_turnsTunnelOff() {
        assertEquals(Decision.Apply(emptySet()), TunnelDecider.decide(wifi("MyHome"), config()))
    }

    @Test fun publicWifi_raisesDefaultTunnel() {
        assertEquals(up("home"), TunnelDecider.decide(wifi("Starbucks"), config()))
    }

    @Test fun bssidPin_blocksSpoofedSsid() {
        // Home rule pinned to a specific router MAC.
        val cfg = config(rules = listOf(
            NetworkRule(ssid = "MyHome", bssids = setOf("AA:BB:CC:DD:EE:FF"), trusted = true)
        ))
        // Same SSID, correct BSSID -> trusted -> off.
        assertEquals(Decision.Apply(emptySet()),
            TunnelDecider.decide(wifi("MyHome", "aa:bb:cc:dd:ee:ff"), cfg))
        // Same SSID, attacker's BSSID -> NOT trusted -> tunnel up.
        assertEquals(up("home"),
            TunnelDecider.decide(wifi("MyHome", "11:22:33:44:55:66"), cfg))
    }

    @Test fun perNetworkTunnel_overridesDefault() {
        val cfg = config(rules = listOf(
            NetworkRule(ssid = "Work", trusted = false, tunnels = listOf("work-vpn"))
        ))
        assertEquals(up("work-vpn"), TunnelDecider.decide(wifi("Work"), cfg))
    }

    @Test fun forceOn_and_forceOff_overrideNetwork() {
        assertEquals(up("home"),
            TunnelDecider.decide(wifi("MyHome"), config(override = OverrideMode.FORCE_ON)))
        assertEquals(Decision.Apply(emptySet()),
            TunnelDecider.decide(wifi("Starbucks"), config(override = OverrideMode.FORCE_OFF)))
    }

    @Test fun override_beatsAutomationOff() {
        // STATES §2 precedence: Force on/off act even when the master switch is off.
        assertEquals(up("home"),
            TunnelDecider.decide(wifi("MyHome"), config(enabled = false, override = OverrideMode.FORCE_ON)))
        assertEquals(Decision.Apply(emptySet()),
            TunnelDecider.decide(wifi("Starbucks"), config(enabled = false, override = OverrideMode.FORCE_OFF)))
    }

    @Test fun unknownSsid_followsSetting() {
        assertEquals(up("home"),
            TunnelDecider.decide(wifi(null), config(unknownUntrusted = true)))
        assertEquals(Decision.LeaveAlone,
            TunnelDecider.decide(wifi(null), config(unknownUntrusted = false)))
    }

    @Test fun mobileData_respectsToggle() {
        val cell = NetworkState(true, false, true, null, null)
        assertEquals(up("home"), TunnelDecider.decide(cell, config(mobile = true)))
        assertEquals(Decision.Apply(emptySet()), TunnelDecider.decide(cell, config(mobile = false)))
    }

    @Test fun disabled_orNoTunnel_leavesAlone() {
        assertEquals(Decision.LeaveAlone,
            TunnelDecider.decide(wifi("Starbucks"), config(enabled = false)))
        assertEquals(Decision.LeaveAlone,
            TunnelDecider.decide(wifi("Starbucks"), config(default = emptyList(), rules = emptyList())))
    }

    @Test fun noInternet_leavesAlone() {
        assertEquals(Decision.LeaveAlone,
            TunnelDecider.decide(NetworkState(false, false, false, null, null), config()))
    }
}
