package com.smellouk.autoguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.EventLog
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.Decision
import com.smellouk.autoguard.net.NetworkInspector
import com.smellouk.autoguard.net.NetworkState
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.net.TunnelConfig
import com.smellouk.autoguard.net.TunnelDecider
import com.smellouk.autoguard.ui.MainActivity
import androidx.glance.appwidget.updateAll
import com.smellouk.autoguard.widget.AutoGuardWidget
import com.smellouk.autoguard.wireguard.WireGuardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A minimal foreground service. It registers ONE network callback and otherwise
 * sleeps — no polling, no wake locks, no timers running in the background. Memory
 * footprint is a few hundred KB; the OS only wakes us when connectivity changes.
 */
class WifiMonitorService : Service() {

    private lateinit var settings: Settings
    private lateinit var eventLog: EventLog
    private lateinit var cm: ConnectivityManager
    private val main = Handler(Looper.getMainLooper())

    /** Tunnels we currently believe are up because we raised them. */
    private var activeTunnels: Set<String> = emptySet()
    private var firstRun = true
    private var lastStatusText = ""

    private val debounce = Runnable { evaluate() }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = schedule()
        override fun onLost(network: Network) = schedule()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = schedule()
    }

    // Real OS signal: a VPN transport coming up / going down. Logged as events and
    // re-evaluates so the notification reflects the actual tunnel state.
    private var vpnUp = false
    private var verifyRetries = 0
    private val verify = Runnable { verifyTunnel() }
    private val vpnCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!vpnUp) { vpnUp = true; eventLog.add(System.currentTimeMillis(), "VPN_UP" + EventLog.SEP + getString(R.string.event_vpn_up_desc)) }
            // The tunnel really came up — clear any pending verification / failure.
            verifyRetries = 0
            main.removeCallbacks(verify)
            settings.tunnelFailed = false
            schedule()
        }
        override fun onLost(network: Network) {
            if (vpnUp) { vpnUp = false; eventLog.add(System.currentTimeMillis(), "VPN_DOWN" + EventLog.SEP + getString(R.string.event_vpn_down_desc)) }
            schedule()
        }
    }

    // React immediately if the user edits settings or flips the QS override.
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> schedule() }

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        eventLog = EventLog(this)
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createChannel()
        startForeground(NOTIF_ID, buildNotification(getString(R.string.notif_starting), COLOR_NEUTRAL))

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, networkCallback)
        val vpnRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        runCatching { cm.registerNetworkCallback(vpnRequest, vpnCallback) }
        settings.registerListener(prefsListener)
        schedule()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RETRY) {
            settings.tunnelFailed = false
            activeTunnels = emptySet() // force the next evaluate to re-send the up intent
        }
        // Re-evaluate on every (re)start — e.g. the BOOT_COMPLETED that follows
        // LOCKED_BOOT_COMPLETED once the user unlocks and the SSID becomes readable.
        schedule()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { cm.unregisterNetworkCallback(networkCallback) }
        runCatching { cm.unregisterNetworkCallback(vpnCallback) }
        runCatching { settings.unregisterListener(prefsListener) }
        main.removeCallbacks(debounce)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Coalesce bursts of callbacks (a single reconnect fires several). */
    private fun schedule() {
        main.removeCallbacks(debounce)
        main.postDelayed(debounce, 1500L)
    }

    private fun evaluate() {
        val config = settings.toConfig()
        val state = NetworkInspector.current(this)
        val decision = TunnelDecider.decide(state, config)

        if (decision !is Decision.Apply) {
            updateNotification(plainStatus(state, config, false), COLOR_NEUTRAL)
            return
        }

        val desired = decision.tunnelsUp
        // Reconcile: bring up what's newly wanted, drop what we previously raised.
        val managed = config.allTunnels() + activeTunnels
        val toUp = desired - activeTunnels
        val toDown = (if (firstRun) managed else activeTunnels) - desired
        val up = desired.isNotEmpty()

        // Drift: we still want the tunnel up but the OS VPN isn't actually up — e.g.
        // the user disconnected inside WireGuard, or it dropped on its own. The plain
        // diff is empty here (desired == activeTunnels), so re-raise explicitly.
        val needsReraise = up && !vpnUp && toUp.isEmpty()

        toUp.forEach { WireGuardController.setTunnel(this, it, up = true) }
        if (needsReraise) desired.forEach { WireGuardController.setTunnel(this, it, up = true) }
        toDown.forEach { WireGuardController.setTunnel(this, it, up = false) }

        if (toUp.isNotEmpty() || toDown.isNotEmpty()) {
            eventLog.add(System.currentTimeMillis(), eventMessage(state, config, up, desired))
        }

        activeTunnels = desired
        firstRun = false

        // Verify a raise actually brings the OS VPN up; auto-retry then flag failure.
        if (toUp.isNotEmpty() || needsReraise) {
            verifyRetries = 0
            main.removeCallbacks(verify)
            main.postDelayed(verify, VERIFY_MS)
        } else if (!up) {
            // Tunnel intentionally down — nothing to verify, clear any failure.
            main.removeCallbacks(verify)
            settings.tunnelFailed = false
        }

        updateNotification(plainStatus(state, config, up), notifColor(state, config, up))
    }

    /** Runs [VERIFY_MS] after an "up" intent: retry once, then mark the tunnel failed. */
    private fun verifyTunnel() {
        if (vpnUp || activeTunnels.isEmpty()) return // came up, or no longer wanted
        if (verifyRetries < MAX_RETRIES) {
            verifyRetries++
            activeTunnels.forEach { WireGuardController.setTunnel(this, it, up = true) }
            main.postDelayed(verify, VERIFY_MS)
            return
        }
        settings.tunnelFailed = true
        eventLog.add(System.currentTimeMillis(), "FAILED" + EventLog.SEP + getString(R.string.event_tunnel_failed_desc))
        updateNotification(getString(R.string.notif_tunnel_failed), COLOR_RED)
    }

    /** "TYPEhuman description" — type drives the timeline dot colour. */
    private fun eventMessage(state: NetworkState, config: TunnelConfig, up: Boolean, desired: Set<String>): String {
        val type = when {
            config.override == OverrideMode.FORCE_ON && up -> "FORCED_ON"
            config.override == OverrideMode.FORCE_OFF -> "FORCED_OFF"
            up -> "ON"
            else -> "OFF"
        }
        val net = when {
            state.isCellular -> getString(R.string.event_net_mobile)
            state.isWifi -> state.ssid ?: getString(R.string.event_net_wifi)
            else -> getString(R.string.event_net_no_connection)
        }
        val trusted = config.matchingRule(state.ssid, state.bssid)?.trusted == true
        val tunnels = desired.joinToString(", ")
        val desc = when {
            config.override == OverrideMode.FORCE_OFF -> getString(R.string.event_desc_force_off)
            config.override == OverrideMode.FORCE_ON -> getString(R.string.event_desc_force_on, net, tunnels)
            up -> getString(R.string.event_desc_up, net, tunnels)
            trusted -> getString(R.string.event_desc_back, net)
            else -> getString(R.string.event_desc_not_protecting, net)
        }
        return "$type${EventLog.SEP}$desc"
    }

    /** Plain-language status for the notification, matching the redesign's voice. */
    private fun plainStatus(state: NetworkState, config: TunnelConfig, up: Boolean): String {
        if (!config.automationEnabled) return getString(R.string.notif_automation_off)
        if (config.override == OverrideMode.FORCE_OFF) return getString(R.string.notif_protection_off)
        val net = when {
            state.isCellular -> getString(R.string.notif_net_mobile)
            state.isWifi -> state.ssid ?: getString(R.string.notif_net_wifi)
            else -> getString(R.string.notif_net_this_network)
        }
        if (up && state.vpnActive) return getString(R.string.notif_protected_on, net)
        if (up) return getString(R.string.notif_connecting)
        val trusted = config.matchingRule(state.ssid, state.bssid)?.trusted == true
        return if (trusted) getString(R.string.notif_at_home) else getString(R.string.notif_tunnel_off)
    }

    private fun notifColor(state: NetworkState, config: TunnelConfig, up: Boolean): Int = when {
        !config.automationEnabled -> COLOR_NEUTRAL
        config.override == OverrideMode.FORCE_OFF -> COLOR_AMBER
        up && state.vpnActive -> COLOR_GREEN
        up -> COLOR_AMBER
        config.matchingRule(state.ssid, state.bssid)?.trusted == true -> COLOR_BLUE
        else -> COLOR_NEUTRAL
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_MIN // silent, collapsed
            ).apply { description = getString(R.string.notif_channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, color: Int): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val override = PendingIntent.getBroadcast(
            this, 1, Intent(this, OverrideReceiver::class.java).setAction(OverrideReceiver.ACTION_CYCLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setColorized(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(tap)
            .addAction(R.drawable.ic_notification, getString(R.string.notif_action_override), override)
            .build()
    }

    private fun updateNotification(text: String, color: Int) {
        if (text == lastStatusText) return
        lastStatusText = text
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text, color))
    }

    companion object {
        private const val CHANNEL_ID = "autoguard_status"
        private const val NOTIF_ID = 1001
        private const val COLOR_GREEN = 0xFF35E3A4.toInt()
        private const val COLOR_BLUE = 0xFF74B6FF.toInt()
        private const val COLOR_AMBER = 0xFFFFC24B.toInt()
        private const val COLOR_NEUTRAL = 0xFF8C9692.toInt()
        private const val COLOR_RED = 0xFFFF5D6B.toInt()
        private const val VERIFY_MS = 8000L
        private const val MAX_RETRIES = 1
        const val ACTION_RETRY = "com.smellouk.autoguard.action.RETRY_TUNNEL"

        /** Force a re-send of the desired tunnel(s) — used by the "Retry" banner. */
        fun retry(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java).setAction(ACTION_RETRY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun start(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WifiMonitorService::class.java))
        }

        /**
         * Run the monitor whenever it has something to do: automation on, or a
         * manual override active (Force on/off must work even with automation off).
         */
        fun sync(context: Context) {
            val s = Settings(context)
            if (s.automationEnabled || s.overrideMode != OverrideMode.AUTO) {
                start(context)
                WatchdogWorker.schedule(context) // periodic restart in case the OEM kills us
            } else {
                stop(context)
                WatchdogWorker.cancel(context)
            }
            // Every override/automation change funnels through sync(), so refresh the
            // home-screen widget here (covers app, QS tiles, notification and widget itself).
            CoroutineScope(Dispatchers.Default).launch {
                runCatching { AutoGuardWidget().updateAll(context) }
            }
        }
    }
}
