package com.smellouk.autoguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smellouk.autoguard.service.WifiMonitorService

/** Restarts the monitor after reboot or app update if automation or an override needs it. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WifiMonitorService.sync(context)
    }
}
