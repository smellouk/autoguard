package com.smellouk.autoguard.data

import android.content.Context
import android.content.SharedPreferences

/** One recorded transition. */
data class EventEntry(val timeMillis: Long, val message: String)

/**
 * A capped, newest-first event log persisted in its own SharedPreferences file
 * (so writes here don't trigger the settings change-listener). Stores at most
 * [MAX] entries as newline-separated "millis\tmessage" lines — no DB, no deps.
 *
 * A message is "TYPE[SEP]human description" (see [SEP]); the type drives the
 * timeline dot colour, the description is what the user reads.
 */
class EventLog(context: Context) {

    // Device-protected storage so transitions during Direct Boot are recorded too.
    private val prefs: SharedPreferences =
        Settings.deviceProtected(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun add(timeMillis: Long, message: String) {
        val lines = raw().toMutableList()
        lines.add(0, "$timeMillis\t${message.replace("\n", " ")}")
        while (lines.size > MAX) lines.removeAt(lines.size - 1)
        prefs.edit().putString(KEY, lines.joinToString("\n")).apply()
    }

    fun entries(): List<EventEntry> = raw().mapNotNull { line ->
        val tab = line.indexOf('\t')
        if (tab < 0) return@mapNotNull null
        val ts = line.substring(0, tab).toLongOrNull() ?: return@mapNotNull null
        EventEntry(ts, line.substring(tab + 1))
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    private fun raw(): List<String> =
        (prefs.getString(KEY, "") ?: "").split("\n").filter { it.isNotBlank() }

    companion object {
        private const val PREFS = "autoguard_events"
        private const val KEY = "log"
        const val MAX = 200

        /** Separates the event type token from its human description in a stored message. */
        const val SEP = "\u0001"
    }
}
