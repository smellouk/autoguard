package com.smellouk.autoguard.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Reliability watchdog: some OEMs kill foreground services. This periodic job
 * simply re-runs [WifiMonitorService.sync], which restarts the monitor if it was
 * killed (and is a no-op if it's already running). 15 min is WorkManager's floor.
 */
class WatchdogWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        WifiMonitorService.sync(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "autoguard-watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
