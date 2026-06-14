package com.smellouk.autoguard.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.smellouk.autoguard.net.NetworkRule
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.net.TunnelConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tiny SharedPreferences-backed settings store. No extra dependencies, no
 * background threads — keeps the always-on process as light as possible.
 * Network rules are serialised as a compact JSON array via the built-in org.json.
 */
class Settings(context: Context) {

    private val prefs: SharedPreferences =
        deviceProtected(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Master switch — when false, AutoGuard never touches the tunnel. */
    var automationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Manual override from the Quick Settings tile. */
    var overrideMode: OverrideMode
        get() = runCatching { OverrideMode.valueOf(prefs.getString(KEY_OVERRIDE, null) ?: "") }
            .getOrDefault(OverrideMode.AUTO)
        set(value) = prefs.edit().putString(KEY_OVERRIDE, value.name).apply()

    /** Default tunnel(s) raised on any untrusted network with no specific rule. */
    var defaultTunnels: List<String>
        get() = parseList(prefs.getString(KEY_TUNNELS, "") ?: "")
        set(value) = prefs.edit().putString(KEY_TUNNELS, value.joinToString(",")).apply()

    /** Turn the tunnel on while on mobile data (no Wi-Fi) as well. */
    var enableOnMobileData: Boolean
        get() = prefs.getBoolean(KEY_MOBILE, true)
        set(value) = prefs.edit().putBoolean(KEY_MOBILE, value).apply()

    /**
     * When the SSID can't be read (e.g. location off), should we assume the
     * network is untrusted and turn the tunnel ON? Safer default = true.
     */
    var treatUnknownAsUntrusted: Boolean
        get() = prefs.getBoolean(KEY_UNKNOWN, true)
        set(value) = prefs.edit().putBoolean(KEY_UNKNOWN, value).apply()

    /**
     * Whether the user dismissed the "enable remote control in WireGuard" hint.
     * We can't detect that setting via the intent API, so we surface a dismissible
     * reminder instead of a fake gate. Also doubles as onboarding step 3 = done.
     */
    var remoteControlHintDismissed: Boolean
        get() = prefs.getBoolean(KEY_RC_HINT, false)
        set(value) = prefs.edit().putBoolean(KEY_RC_HINT, value).apply()

    /**
     * Onboarding step 2 ("create your tunnel") confirmed by the user. WireGuard
     * exposes no way to verify a tunnel exists, so this is a user-driven checkmark.
     */
    var tunnelSetupConfirmed: Boolean
        get() = prefs.getBoolean(KEY_TUNNEL_OK, false)
        set(value) = prefs.edit().putBoolean(KEY_TUNNEL_OK, value).apply()

    /** True once the user has finished or skipped first-run onboarding. */
    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** True once the user has accepted the Terms of Use (first-launch gate). */
    var termsAccepted: Boolean
        get() = prefs.getBoolean(KEY_TERMS, false)
        set(value) = prefs.edit().putBoolean(KEY_TERMS, value).apply()

    /** All configured per-network rules (home networks + per-network tunnels). */
    var rules: List<NetworkRule>
        get() = parseRules(prefs.getString(KEY_RULES, "[]") ?: "[]")
        set(value) = prefs.edit().putString(KEY_RULES, serializeRules(value)).apply()

    fun upsertRule(rule: NetworkRule) {
        val others = rules.filterNot { it.ssid == rule.ssid }
        rules = others + rule
    }

    fun removeRule(ssid: String) {
        rules = rules.filterNot { it.ssid == ssid }
    }

    /** Immutable snapshot handed to the pure decision logic. */
    fun toConfig() = TunnelConfig(
        automationEnabled = automationEnabled,
        override = overrideMode,
        defaultTunnels = defaultTunnels,
        rules = rules,
        enableOnMobileData = enableOnMobileData,
        treatUnknownAsUntrusted = treatUnknownAsUntrusted,
    )

    /**
     * Automation may only be enabled once there's something to act on:
     * a default tunnel name AND at least one trusted network.
     */
    fun canEnableAutomation(): Boolean =
        defaultTunnels.isNotEmpty() && rules.any { it.trusted }

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)

    // --- serialization helpers ---

    private fun parseList(s: String): List<String> =
        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun serializeRules(rules: List<NetworkRule>): String {
        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(JSONObject().apply {
                put("ssid", r.ssid)
                put("trusted", r.trusted)
                put("bssids", JSONArray(r.bssids.toList()))
                put("tunnels", JSONArray(r.tunnels))
            })
        }
        return arr.toString()
    }

    private fun parseRules(json: String): List<NetworkRule> = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            NetworkRule(
                ssid = o.getString("ssid"),
                bssids = o.optJSONArray("bssids").toStringSet(),
                trusted = o.optBoolean("trusted", true),
                tunnels = o.optJSONArray("tunnels").toStringList(),
            )
        }
    }.getOrDefault(emptyList())

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONArray?.toStringSet(): Set<String> = toStringList().toSet()

    companion object {
        /**
         * Device-protected storage is readable during Direct Boot (before the
         * user unlocks), unlike the default credential-encrypted storage. The
         * monitor needs its config at LOCKED_BOOT_COMPLETED, so everything lives
         * here. Migrates any pre-existing credential-storage prefs on first use.
         */
        fun deviceProtected(context: Context): Context {
            val app = context.applicationContext
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return app
            val dp = app.createDeviceProtectedStorageContext()
            runCatching { dp.moveSharedPreferencesFrom(app, PREFS) }
            runCatching { dp.moveSharedPreferencesFrom(app, EVENTS_PREFS) }
            return dp
        }

        private const val PREFS = "autoguard_settings"
        const val EVENTS_PREFS = "autoguard_events"
        private const val KEY_ENABLED = "automation_enabled"
        private const val KEY_OVERRIDE = "override_mode"
        private const val KEY_TUNNELS = "default_tunnels"
        private const val KEY_MOBILE = "enable_on_mobile"
        private const val KEY_UNKNOWN = "unknown_untrusted"
        private const val KEY_RULES = "network_rules"
        private const val KEY_RC_HINT = "rc_hint_dismissed"
        private const val KEY_TUNNEL_OK = "tunnel_setup_confirmed"
        private const val KEY_ONBOARDED = "onboarding_complete"
        private const val KEY_TERMS = "terms_accepted"
    }
}
