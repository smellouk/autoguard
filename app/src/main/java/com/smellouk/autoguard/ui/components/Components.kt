package com.smellouk.autoguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smellouk.autoguard.R
import com.smellouk.autoguard.ui.theme.AG
import com.smellouk.autoguard.ui.theme.Accent
import com.smellouk.autoguard.ui.theme.AGText
import com.smellouk.autoguard.ui.theme.GreenVivid

/* --------------------------- surfaces --------------------------- */

@Composable
fun AGCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    background: Color? = null,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val c = AG.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(background ?: c.surface)
            .border(1.dp, borderColor ?: c.line, RoundedCornerShape(19.dp))
            .padding(padding)
    ) { content() }
}

/* --------------------------- text bits --------------------------- */

@Composable
fun Eyebrow(text: String, accent: Accent = Accent.GREEN, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = AGText.eyebrow, color = AG.colors.accentText(accent), modifier = modifier)
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = AGText.cardTitle, color = AG.colors.text, modifier = modifier)
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, dim: Boolean = true) {
    Text(text, style = AGText.body, color = if (dim) AG.colors.textDim else AG.colors.text, modifier = modifier)
}

/* --------------------------- switch --------------------------- */

@Composable
fun AGSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, accent: Accent = Accent.GREEN, enabled: Boolean = true) {
    val c = AG.colors
    val on = c.accentText(accent)
    val track by animateColorAsState(if (checked) on else c.surface2, tween(180), label = "track")
    val knobX by animateDpAsState(if (checked) 24.dp else 3.dp, tween(180), label = "knob")
    Box(
        Modifier
            .size(52.dp, 31.dp)
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (enabled) 1f else 0.4f)
            .background(track)
            .border(1.dp, if (checked) Color.Transparent else c.line2, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Switch) { onCheckedChange(!checked) }
    ) {
        Box(
            Modifier
                .padding(start = knobX, top = 3.dp)
                .size(25.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color(0xFF6B7370))
        )
    }
}

/* --------------------------- segmented control --------------------------- */

data class Segment(val label: String, val accent: Accent)

@Composable
fun SegmentedControl(segments: List<Segment>, selected: Int, onSelect: (Int) -> Unit) {
    val c = AG.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(13.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        segments.forEachIndexed { i, seg ->
            val active = i == selected
            val isRed = seg.accent == Accent.RED
            val bgMod = when {
                active && isRed -> Modifier.background(c.wash(Accent.RED))
                active -> Modifier.background(c.greenGradient)
                else -> Modifier
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(bgMod)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                val color = when {
                    active && isRed -> c.red
                    active -> c.onGreen
                    else -> c.textDim
                }
                Text(seg.label, style = AGText.segment, color = color, maxLines = 1)
            }
        }
    }
}

/* --------------------------- badge & pill --------------------------- */

@Composable
fun Badge(text: String, accent: Accent) {
    val c = AG.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.wash(accent))
            .border(1.dp, c.washBorder(accent), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) { Text(text.uppercase(), style = AGText.badge, color = c.accentText(accent)) }
}

/** Tunnel/status pill: dot + mono label. */
@Composable
fun StatusPill(label: String, accent: Accent?, active: Boolean) {
    val c = AG.colors
    val dot = accent?.let { c.accentText(it) } ?: c.textFaint
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent != null) c.wash(accent) else c.surface2)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(label, style = AGText.monoSm, color = if (accent != null) c.accentText(accent) else c.textDim, maxLines = 1)
    }
}

/* --------------------------- icon tiles --------------------------- */

@Composable
fun IconTile(icon: ImageVector, accent: Accent? = null, tile: Dp = 40.dp, iconSize: Dp = 20.dp) {
    val c = AG.colors
    Box(
        Modifier
            .size(tile)
            .clip(RoundedCornerShape(if (tile <= 38.dp) 11.dp else 12.dp))
            .background(if (accent != null) c.wash(accent) else c.surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = accent?.let { c.accentText(it) } ?: c.textDim, modifier = Modifier.size(iconSize))
    }
}

/* --------------------------- buttons --------------------------- */

@Composable
fun PrimaryButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AG.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.greenGradient)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.onGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, style = AGText.button, color = c.onGreen, maxLines = 1)
    }
}

/** Full-width outlined (surface) button used for the two "add" actions. */
@Composable
fun OutlineButton(text: String, icon: ImageVector? = null, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = AG.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(c.surface)
            .border(1.dp, c.line2, RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (enabled) c.text else c.textFaint
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, style = AGText.button, color = tint, maxLines = 1)
    }
}

enum class PillStyle { SOLID, OUTLINE, SUCCESS }

@Composable
fun ActionPill(text: String, accent: Accent, style: PillStyle, onClick: () -> Unit) {
    val c = AG.colors
    val mod = when (style) {
        PillStyle.SOLID -> Modifier.background(c.wash(accent)).border(1.dp, c.washBorder(accent), RoundedCornerShape(120.dp))
        PillStyle.OUTLINE -> Modifier.border(1.dp, c.line2, RoundedCornerShape(120.dp))
        PillStyle.SUCCESS -> Modifier
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(120.dp))
            .then(mod)
            .clickable(enabled = style != PillStyle.SUCCESS, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val color = if (style == PillStyle.OUTLINE) c.text else c.accentText(accent)
        Text(text, style = AGText.button.copy(fontSize = AGText.subtitle.fontSize), color = color, maxLines = 1)
    }
}

/* --------------------------- rows --------------------------- */

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String, chevron: ImageVector, onClick: () -> Unit) {
    val c = AG.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = c.blue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AGText.rowTitle, color = c.text)
            Text(subtitle, style = AGText.subtitle, color = c.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(chevron, null, tint = c.textFaint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Accent = Accent.GREEN,
    leading: ImageVector? = null,
    titleStyle: TextStyle = AGText.rowTitle,
    switchEnabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = AG.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) IconTile(leading)
        Column(Modifier.weight(1f)) {
            Text(title, style = titleStyle, color = c.text)
            Text(subtitle, style = AGText.subtitle, color = c.textDim)
        }
        AGSwitch(checked, onCheckedChange, accent, enabled = switchEnabled)
    }
}

@Composable
fun HDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AG.colors.line))
}

/** Centred mono section divider, e.g. `SAVED · 3`. */
@Composable
fun LabeledDivider(label: String) {
    val c = AG.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(c.line))
        Text(label.uppercase(), style = AGText.eyebrow, color = c.textFaint)
        Box(Modifier.weight(1f).height(1.dp).background(c.line))
    }
}

/* --------------------------- hero --------------------------- */

enum class HeroKind { PROTECTED, AT_HOME, OFF }

/**
 * The state-driven hero. Pulse + breathing glow run only in PROTECTED (gated by
 * [animate], which the caller turns off for reduce-motion).
 */
@Composable
fun Hero(
    kind: HeroKind,
    accent: Accent,
    eyebrow: String,
    title: String,
    subtitle: String,
    animate: Boolean,
    recovery: (@Composable () -> Unit)? = null,
    networkChip: @Composable () -> Unit,
) {
    val c = AG.colors
    val border = when (kind) {
        HeroKind.PROTECTED -> GreenVivid.copy(alpha = 0.24f)
        HeroKind.AT_HOME -> c.line
        HeroKind.OFF -> c.accentText(accent).copy(alpha = 0.22f)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(c.surface)
            .background(Brush.verticalGradient(listOf(c.wash(accent), Color.Transparent)))
            .border(1.dp, border, RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 26.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HeroShield(kind, animate)
            Spacer(Modifier.height(18.dp))
            Eyebrow(eyebrow, accent)
            Spacer(Modifier.height(7.dp))
            Text(title, style = AGText.heroTitle, color = c.text, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = AGText.bodySm, color = c.textDim)
            Spacer(Modifier.height(16.dp))
            networkChip()
            if (recovery != null) {
                Spacer(Modifier.height(12.dp))
                recovery()
            }
        }
    }
}

@Composable
private fun HeroShield(kind: HeroKind, animate: Boolean) {
    val c = AG.colors
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        if (kind == HeroKind.PROTECTED && animate) {
            val t = rememberInfiniteTransition(label = "pulse")
            // breathing glow (agGlow): disc's halo brightens & swells, 3.5s ease-in-out
            val glow by t.animateFloat(
                0.45f, 0.85f,
                infiniteRepeatable(tween(3500), RepeatMode.Reverse),
                label = "glow"
            )
            Box(
                Modifier
                    .size(104.dp)
                    .graphicsLayer { alpha = glow; scaleX = 0.85f + glow * 0.18f; scaleY = 0.85f + glow * 0.18f }
                    .background(
                        Brush.radialGradient(listOf(GreenVivid.copy(alpha = 0.55f), Color.Transparent)),
                        CircleShape
                    )
            )
            // two staggered rings (agPulse)
            listOf(0, 1500).forEach { delay ->
                val scale by t.animateFloat(
                    1f, 1.7f,
                    infiniteRepeatable(tween(3000, easing = LinearOutSlowInEasing), RepeatMode.Restart,
                        initialStartOffset = StartOffset(delay, StartOffsetType.FastForward)),
                    label = "scale$delay"
                )
                val alpha by t.animateFloat(
                    0.55f, 0f,
                    infiniteRepeatable(tween(3000, easing = LinearOutSlowInEasing), RepeatMode.Restart,
                        initialStartOffset = StartOffset(delay, StartOffsetType.FastForward)),
                    label = "alpha$delay"
                )
                Box(
                    Modifier
                        .size(78.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                        .border(2.dp, GreenVivid.copy(alpha = 0.45f), CircleShape)
                )
            }
        }
        // disc
        when (kind) {
            HeroKind.PROTECTED -> Box(
                Modifier.size(78.dp).clip(CircleShape).background(c.greenGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.GppGood, stringResource(R.string.cd_protected),
                    tint = c.onGreen, modifier = Modifier.size(38.dp))
            }
            HeroKind.AT_HOME -> Box(
                Modifier.size(78.dp).clip(CircleShape).background(c.surface2)
                    .border(1.dp, c.blue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Home, stringResource(R.string.cd_at_home),
                    tint = c.blue, modifier = Modifier.size(36.dp))
            }
            HeroKind.OFF -> Box(
                Modifier.size(78.dp).clip(CircleShape).background(c.surface2)
                    .border(1.dp, c.line2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Shield, stringResource(R.string.cd_not_protected),
                    tint = c.textDim, modifier = Modifier.size(36.dp))
            }
        }
    }
}
