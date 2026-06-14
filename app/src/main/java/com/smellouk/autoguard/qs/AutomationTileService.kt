package com.smellouk.autoguard.qs

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.service.WifiMonitorService

/**
 * Quick Settings tile that toggles the Automation master switch on/off.
 * Active (green) = automation on. Complements [AutoGuardTileService], which
 * cycles the manual override.
 */
class AutomationTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val settings = Settings(this)
        // Enabling needs a default tunnel + a trusted network; turning off always allowed.
        if (!settings.automationEnabled && !settings.canEnableAutomation()) { render(); return }
        settings.automationEnabled = !settings.automationEnabled
        WifiMonitorService.sync(this)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val settings = Settings(this)
        val on = settings.automationEnabled
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield)
        tile.label = getString(R.string.automation_title)
        tile.state = when {
            on -> Tile.STATE_ACTIVE
            !settings.canEnableAutomation() -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    on -> R.string.tile_on
                    !settings.canEnableAutomation() -> R.string.tile_setup_needed
                    else -> R.string.tile_off
                }
            )
        }
        tile.updateTile()
    }
}
