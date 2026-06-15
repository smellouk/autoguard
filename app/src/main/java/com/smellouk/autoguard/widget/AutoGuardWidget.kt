package com.smellouk.autoguard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.service.WifiMonitorService

// Theme-aware tokens matching the app (day = light, night = dark).
// Theme-aware: resolved from values/ (light) and values-night/ (dark).
private val CARD = ColorProvider(R.color.widget_card)
private val TRACK = ColorProvider(R.color.widget_track)
private val TEXT = ColorProvider(R.color.widget_text)
private val DIM = ColorProvider(R.color.widget_dim)
private val ON_GREEN = ColorProvider(R.color.widget_on_green)
private val RED = ColorProvider(R.color.widget_red)
private val LINE = ColorProvider(R.color.widget_line)

val MODE_KEY = ActionParameters.Key<String>("ag_override_mode")

/** Home-screen widget mirroring the app's "Manual override" card, built with Glance. */
class AutoGuardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val mode = Settings(context).overrideMode
        val labels = listOf(
            context.getString(R.string.segment_auto),
            context.getString(R.string.segment_force_on),
            context.getString(R.string.segment_force_off),
        )
        provideContent { WidgetBody(mode, labels) }
    }
}

@Composable
private fun WidgetBody(mode: OverrideMode, labels: List<String>) {
    // Rounding via a shape drawable (the device honours these, not cornerRadius()).
    // Root fills the cell but is transparent and centres the bar, so there's no
    // tall grey block — just the rounded, bordered segmented bar.
    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_track_bg)).padding(7.dp)) {
            Segment(labels[0], mode == OverrideMode.AUTO, false, OverrideMode.AUTO, GlanceModifier.defaultWeight())
            Segment(labels[1], mode == OverrideMode.FORCE_ON, false, OverrideMode.FORCE_ON, GlanceModifier.defaultWeight())
            Segment(labels[2], mode == OverrideMode.FORCE_OFF, true, OverrideMode.FORCE_OFF, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun Segment(label: String, active: Boolean, isRed: Boolean, target: OverrideMode, modifier: GlanceModifier) {
    val pill = if (active)
        GlanceModifier.background(ImageProvider(if (isRed) R.drawable.widget_seg_red else R.drawable.widget_seg_green))
    else GlanceModifier
    Box(
        modifier = modifier
            .then(pill)
            .padding(vertical = 9.dp)
            .clickable(actionRunCallback<OverrideAction>(actionParametersOf(MODE_KEY to target.name))),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (active) (if (isRed) RED else ON_GREEN) else DIM
        Text(label, style = TextStyle(color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium))
    }
}

/** Applies the tapped override, syncs the service, and refreshes the widget. */
class OverrideAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[MODE_KEY]?.let { runCatching { OverrideMode.valueOf(it) }.getOrNull() } ?: return
        Settings(context).overrideMode = mode
        WifiMonitorService.sync(context)
        AutoGuardWidget().updateAll(context)
    }
}
