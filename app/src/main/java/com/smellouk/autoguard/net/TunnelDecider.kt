package com.smellouk.autoguard.net

/** The set of tunnels AutoGuard wants up, or "don't touch anything". */
sealed interface Decision {
    /** Leave the tunnels exactly as they are (automation off, no internet, etc.). */
    data object LeaveAlone : Decision

    /** These tunnels should be UP; everything else AutoGuard manages should be DOWN. */
    data class Apply(val tunnelsUp: Set<String>) : Decision
}

/** Plain, Android-free snapshot of the user's settings used by the decider. */
data class TunnelConfig(
    val automationEnabled: Boolean,
    val override: OverrideMode,
    val defaultTunnels: List<String>,
    val rules: List<NetworkRule>,
    val enableOnMobileData: Boolean,
    val treatUnknownAsUntrusted: Boolean,
) {
    /** Every tunnel name AutoGuard could ever bring up. */
    fun allTunnels(): Set<String> = (defaultTunnels + rules.flatMap { it.tunnels }).toSet()

    fun matchingRule(ssid: String?, bssid: String?): NetworkRule? =
        rules.firstOrNull { it.matches(ssid, bssid) }
}

/**
 * Pure decision logic — no Android types — so it is trivial to reason about
 * and unit-test.
 *
 * Rule of thumb: protect everything that isn't an explicitly trusted home network.
 */
object TunnelDecider {

    fun decide(state: NetworkState, config: TunnelConfig): Decision {
        // Precedence (STATES.md §2): manual override beats the automation master
        // switch, so Force on/off work even when automation is disabled.
        when (config.override) {
            OverrideMode.FORCE_OFF -> return Decision.Apply(emptySet())
            OverrideMode.FORCE_ON -> return Decision.Apply(config.defaultTunnels.toSet())
            OverrideMode.AUTO -> { /* fall through to auto logic */ }
        }

        if (!config.automationEnabled) return Decision.LeaveAlone
        if (config.allTunnels().isEmpty()) return Decision.LeaveAlone
        if (!state.hasInternet) return Decision.LeaveAlone

        if (state.isWifi) {
            val rule = config.matchingRule(state.ssid, state.bssid)
            return when {
                rule != null && rule.trusted -> Decision.Apply(emptySet())
                rule != null -> Decision.Apply(rule.tunnels.ifEmpty { config.defaultTunnels }.toSet())
                // No rule matched. A known SSID that isn't trusted is untrusted.
                state.ssid != null -> Decision.Apply(config.defaultTunnels.toSet())
                // SSID/BSSID unreadable (e.g. no location permission): follow the toggle.
                config.treatUnknownAsUntrusted -> Decision.Apply(config.defaultTunnels.toSet())
                else -> Decision.LeaveAlone
            }
        }

        if (state.isCellular) {
            return if (config.enableOnMobileData) Decision.Apply(config.defaultTunnels.toSet())
            else Decision.Apply(emptySet())
        }

        return Decision.LeaveAlone
    }
}
