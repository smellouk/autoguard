package com.smellouk.autoguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.OverrideMode

/** Cycles the manual override from the ongoing notification's "Override" action. */
class OverrideReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CYCLE) return
        val settings = Settings(context)
        settings.overrideMode = when (settings.overrideMode) {
            OverrideMode.AUTO -> OverrideMode.FORCE_ON
            OverrideMode.FORCE_ON -> OverrideMode.FORCE_OFF
            OverrideMode.FORCE_OFF -> OverrideMode.AUTO
        }
        WifiMonitorService.sync(context)
    }

    companion object {
        const val ACTION_CYCLE = "com.smellouk.autoguard.action.CYCLE_OVERRIDE"
    }
}
