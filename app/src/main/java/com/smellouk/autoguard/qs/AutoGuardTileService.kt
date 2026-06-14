package com.smellouk.autoguard.qs

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.service.WifiMonitorService

/**
 * Quick Settings tile. Tapping it cycles the manual override:
 *   AUTO → FORCE ON → FORCE OFF → AUTO
 *
 * The change is written to settings; if the monitor service is running its
 * preference listener picks it up immediately and re-applies the tunnels.
 */
class AutoGuardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val settings = Settings(this)
        settings.overrideMode = when (settings.overrideMode) {
            OverrideMode.AUTO -> OverrideMode.FORCE_ON
            OverrideMode.FORCE_ON -> OverrideMode.FORCE_OFF
            OverrideMode.FORCE_OFF -> OverrideMode.AUTO
        }
        // Make sure the monitor is alive to act on the new override (Force on/off
        // run even with automation off).
        WifiMonitorService.sync(this)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val settings = Settings(this)

        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield)
        tile.label = getString(R.string.app_name)
        if (!settings.automationEnabled) {
            tile.state = Tile.STATE_UNAVAILABLE
            setSubtitle(tile, getString(R.string.tile_automation_off))
        } else when (settings.overrideMode) {
            OverrideMode.AUTO -> {
                tile.state = Tile.STATE_ACTIVE
                setSubtitle(tile, getString(R.string.tile_auto))
            }
            OverrideMode.FORCE_ON -> {
                tile.state = Tile.STATE_ACTIVE
                setSubtitle(tile, getString(R.string.tile_forced_on))
            }
            OverrideMode.FORCE_OFF -> {
                tile.state = Tile.STATE_INACTIVE
                setSubtitle(tile, getString(R.string.tile_forced_off))
            }
        }
        tile.updateTile()
    }

    private fun setSubtitle(tile: Tile, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = text
    }
}
