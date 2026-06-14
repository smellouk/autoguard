package com.smellouk.autoguard.ui.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.EventEntry
import com.smellouk.autoguard.data.EventLog
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.Decision
import com.smellouk.autoguard.net.NetworkInspector
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.net.TunnelDecider
import com.smellouk.autoguard.service.WifiMonitorService
import com.smellouk.autoguard.ui.components.HeroKind
import com.smellouk.autoguard.ui.theme.Accent
import com.smellouk.autoguard.wireguard.WireGuardController
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Single immutable state the Home screen renders from. */
data class HomeUiState(
    // Hero
    val kind: HeroKind,
    val accent: Accent,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    // Hero network chip
    val chipWifiAccent: Accent,
    val chipSsid: String,
    val chipStatus: String,
    val chipStatusAccent: Accent,
    val pinned: Boolean,
    val tunnelLabel: String,
    val tunnelAccent: Accent?,
    // Controls / cards
    val automationEnabled: Boolean,
    val override: OverrideMode,
    val onMobile: Boolean,
    val unknown: Boolean,
    val defaultTunnel: String,
    val canEnableAutomation: Boolean,
    val needsDefaultTunnel: Boolean,
    val needsTrustedNetwork: Boolean,
    val showRemoteHint: Boolean,
    val networksSubtitle: String,
    val eventCount: Int,
    val lastSwitch: EventEntry?,
)

/**
 * MVVM-lite: combines reactive Settings changes + network changes (and a resume
 * refresh) into one [HomeUiState]. No reducer/intent ceremony — actions are plain
 * methods that write through to [Settings]; the change-listener flow re-emits.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val refresh = MutableStateFlow(0)

    val state: StateFlow<HomeUiState> = combine(
        settingsChanges(),
        networkChanges(),
        refresh,
    ) { _, _, _ -> compute(getApplication(), settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), compute(getApplication(), settings))

    /* ---- actions ---- */

    /** Enabling is blocked until [Settings.canEnableAutomation]; turning off is always allowed. */
    fun setAutomation(on: Boolean) {
        if (on && !settings.canEnableAutomation()) return
        settings.automationEnabled = on
        WifiMonitorService.sync(getApplication())
    }
    fun setOverride(mode: OverrideMode) { settings.overrideMode = mode; WifiMonitorService.sync(getApplication()) }
    fun setOnMobile(on: Boolean) { settings.enableOnMobileData = on }
    fun setUnknown(on: Boolean) { settings.treatUnknownAsUntrusted = on }
    fun setDefaultTunnel(text: String) {
        settings.defaultTunnels = text.split(",").map(String::trim).filter(String::isNotEmpty)
    }
    fun dismissRemoteHint() { settings.remoteControlHintDismissed = true }
    /** Re-derive after returning to the screen (permission grants etc. aren't change events). */
    fun refresh() { refresh.value++ }

    /* ---- reactive sources ---- */

    private fun settingsChanges(): Flow<Unit> = callbackFlow {
        val l = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        settings.registerListener(l)
        trySend(Unit)
        awaitClose { settings.unregisterListener(l) }
    }

    private fun networkChanges(): Flow<Unit> = callbackFlow {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(Unit) }
            override fun onLost(network: Network) { trySend(Unit) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(Unit) }
        }
        cm.registerDefaultNetworkCallback(cb)
        trySend(Unit)
        awaitClose { runCatching { cm.unregisterNetworkCallback(cb) } }
    }

    companion object {
        fun compute(context: Context, settings: Settings): HomeUiState {
            val config = settings.toConfig()
            val state = NetworkInspector.current(context)
            val rule = config.matchingRule(state.ssid, state.bssid)
            val trusted = rule?.trusted == true
            val pinned = rule?.bssids?.isNotEmpty() == true
            val desiredUp = (TunnelDecider.decide(state, config) as? Decision.Apply)?.tunnelsUp ?: emptySet()
            val up = desiredUp.isNotEmpty()

            val ssid = when {
                state.isCellular -> context.getString(R.string.chip_ssid_mobile)
                state.isWifi -> state.ssid ?: context.getString(R.string.chip_ssid_unknown)
                !state.hasInternet -> context.getString(R.string.chip_ssid_no_connection)
                else -> context.getString(R.string.chip_ssid_connected)
            }
            val chipStatus = when {
                state.isCellular -> context.getString(R.string.chip_ssid_mobile)
                trusted && pinned -> context.getString(R.string.chip_status_trusted_pinned)
                trusted -> context.getString(R.string.chip_status_trusted)
                state.isWifi -> context.getString(R.string.chip_status_untrusted)
                else -> context.getString(R.string.chip_status_dash)
            }

            val kind: HeroKind
            val eyebrow: String
            val title: String
            val subtitle: String
            when {
                !config.automationEnabled -> {
                    kind = HeroKind.OFF
                    eyebrow = context.getString(R.string.hero_eyebrow_not_protected)
                    title = context.getString(R.string.hero_title_tunnel_off)
                    subtitle = context.getString(R.string.hero_subtitle_automation_off)
                }
                config.override == OverrideMode.FORCE_OFF -> {
                    kind = HeroKind.OFF
                    eyebrow = context.getString(R.string.hero_eyebrow_protection_off)
                    title = context.getString(R.string.hero_title_tunnel_off)
                    subtitle = context.getString(R.string.hero_subtitle_protection_off)
                }
                up && state.vpnActive -> {
                    // Green only when the OS confirms a VPN transport is actually up.
                    kind = HeroKind.PROTECTED
                    eyebrow = context.getString(R.string.hero_eyebrow_protected)
                    title = context.getString(R.string.hero_title_tunnel_up)
                    subtitle = if (config.override == OverrideMode.FORCE_ON) context.getString(R.string.hero_subtitle_forced_on)
                    else context.getString(R.string.hero_subtitle_encrypted)
                }
                up -> {
                    // Told WireGuard to connect, but no VPN transport yet — connecting or failed.
                    kind = HeroKind.OFF
                    eyebrow = context.getString(R.string.hero_eyebrow_connecting)
                    title = context.getString(R.string.hero_title_connecting)
                    subtitle = context.getString(R.string.hero_subtitle_connecting)
                }
                trusted -> {
                    kind = HeroKind.AT_HOME
                    eyebrow = context.getString(R.string.hero_eyebrow_trusted)
                    title = context.getString(R.string.hero_title_home)
                    subtitle = context.getString(R.string.hero_subtitle_trusted)
                }
                else -> {
                    kind = HeroKind.OFF
                    eyebrow = context.getString(R.string.hero_eyebrow_not_protected)
                    title = context.getString(R.string.hero_title_tunnel_off)
                    subtitle = context.getString(R.string.hero_subtitle_not_protecting)
                }
            }
            val connecting = up && !state.vpnActive
            val accent = when {
                kind == HeroKind.PROTECTED -> Accent.GREEN
                kind == HeroKind.AT_HOME -> Accent.BLUE
                connecting || config.override == OverrideMode.FORCE_OFF -> Accent.AMBER
                else -> Accent.RED
            }

            val rules = config.rules
            val log = EventLog(context)
            return HomeUiState(
                kind = kind, accent = accent, eyebrow = eyebrow, title = title, subtitle = subtitle,
                chipWifiAccent = if (trusted) Accent.BLUE else Accent.AMBER,
                chipSsid = ssid, chipStatus = chipStatus,
                chipStatusAccent = if (trusted) Accent.BLUE else Accent.AMBER, pinned = pinned,
                tunnelLabel = if (up) desiredUp.first() else context.getString(R.string.tunnel_label_off),
                tunnelAccent = when { up && state.vpnActive -> Accent.GREEN; up -> Accent.AMBER; else -> null },
                automationEnabled = config.automationEnabled,
                override = config.override,
                onMobile = config.enableOnMobileData,
                unknown = config.treatUnknownAsUntrusted,
                defaultTunnel = config.defaultTunnels.joinToString(", "),
                canEnableAutomation = settings.canEnableAutomation(),
                needsDefaultTunnel = config.defaultTunnels.isEmpty(),
                needsTrustedNetwork = rules.none { it.trusted },
                showRemoteHint = WireGuardController.isWireGuardInstalled(context) && !settings.remoteControlHintDismissed,
                networksSubtitle = context.getString(R.string.networks_subtitle, rules.count { it.trusted }, rules.count { it.bssids.isNotEmpty() }),
                eventCount = log.entries().size,
                lastSwitch = log.entries().firstOrNull(),
            )
        }
    }
}
