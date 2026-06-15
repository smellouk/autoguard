package com.smellouk.autoguard.wireguard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Controls the official WireGuard app via its public automation intents.
 *
 * Setup required once in the WireGuard app:
 *   Settings -> Advanced -> "Allow remote control apps" = ON
 *
 * These are the same intents Tasker uses, so no VPN permission is needed here
 * and our process stays tiny.
 */
object WireGuardController {

    const val WG_PACKAGE = "com.wireguard.android"

    /**
     * WireGuard's tunnel-control receiver is permission-protected, so we must
     * hold this (dangerous) permission for our broadcasts to be delivered. It's
     * defined by the WireGuard app, so it only exists once WireGuard is installed.
     */
    const val PERMISSION_CONTROL_TUNNELS = "com.wireguard.android.permission.CONTROL_TUNNELS"

    private const val ACTION_UP = "com.wireguard.android.action.SET_TUNNEL_UP"
    private const val ACTION_DOWN = "com.wireguard.android.action.SET_TUNNEL_DOWN"
    // WireGuard's IntentReceiver reads the tunnel name with getStringExtra("tunnel").
    private const val EXTRA_TUNNEL = "tunnel"

    fun setTunnel(context: Context, tunnelName: String, up: Boolean) {
        if (tunnelName.isBlank()) return
        val intent = Intent(if (up) ACTION_UP else ACTION_DOWN).apply {
            setPackage(WG_PACKAGE)
            putExtra(EXTRA_TUNNEL, tunnelName)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Whether we currently hold [PERMISSION_CONTROL_TUNNELS]. Without it the OS
     * silently drops our SET_TUNNEL_UP/DOWN broadcasts before WireGuard sees them.
     */
    fun hasControlPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, PERMISSION_CONTROL_TUNNELS) ==
            PackageManager.PERMISSION_GRANTED

    /** Whether the official WireGuard app is installed. */
    fun isWireGuardInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(WG_PACKAGE, 0)
        true
    } catch (e: Exception) {
        false
    }

    /** Launch the WireGuard app (e.g. to set per-app split tunnelling). */
    fun openWireGuard(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(WG_PACKAGE) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
