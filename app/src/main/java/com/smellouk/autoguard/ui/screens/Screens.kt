package com.smellouk.autoguard.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.EventLog
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.net.Decision
import com.smellouk.autoguard.net.NetworkInspector
import com.smellouk.autoguard.net.NetworkRule
import com.smellouk.autoguard.net.OverrideMode
import com.smellouk.autoguard.net.ScannedNetwork
import com.smellouk.autoguard.net.TunnelDecider
import com.smellouk.autoguard.net.WifiScanner
import com.smellouk.autoguard.service.WifiMonitorService
import com.smellouk.autoguard.ui.home.HomeUiState
import com.smellouk.autoguard.ui.home.HomeViewModel
import com.smellouk.autoguard.ui.components.*
import com.smellouk.autoguard.ui.theme.AG
import com.smellouk.autoguard.ui.theme.Accent
import com.smellouk.autoguard.ui.theme.AGText
import com.smellouk.autoguard.ui.theme.Mono
import com.smellouk.autoguard.wireguard.WireGuardController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ============================ shared scaffolding ============================ */

@Composable
internal fun ScreenFrame(
    title: String? = null,
    wordmark: Boolean = false,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val c = AG.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back),
                    tint = c.text, modifier = Modifier.size(24.dp).clickable(onClick = onBack))
                Spacer(Modifier.width(12.dp))
            }
            if (wordmark) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                    )
                    Text(stringResource(R.string.app_name), style = AGText.wordmark, color = c.text)
                }
            } else if (title != null) {
                Text(title, style = AGText.screenTitle, color = c.text)
            }
            Spacer(Modifier.weight(1f))
            trailing()
        }
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.size(2.dp))
            content()
            Spacer(Modifier.size(8.dp))
        }
    }
}

/** Recompute trigger that ticks on every ON_RESUME, so snapshots refresh. */
@Composable
private fun rememberResumeTick(): Int {
    var tick by remember { mutableStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

/* ================================== HOME ================================== */

@Composable
fun HomeScreen(onNavigate: (HomeDest) -> Unit) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel()
    val ui by vm.state.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()

    // Re-derive on resume (permission grants etc. aren't change events).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // The default-tunnel text field keeps local edit state (async round-trips would
    // jiggle the cursor); everything else binds straight to the reactive state.
    var defaultTunnel by remember { mutableStateOf(ui.defaultTunnel) }

    ScreenFrame(
        wordmark = true,
        trailing = {
            Icon(Icons.Filled.Settings, stringResource(R.string.settings_title), tint = AG.colors.text,
                modifier = Modifier.size(22.dp).clickable { onNavigate(HomeDest.PERMISSIONS) })
        }
    ) {
        if (ui.showRemoteHint) {
            RemoteControlBanner(
                onOpen = { WireGuardController.openWireGuard(context) },
                onDismiss = { vm.dismissRemoteHint() }
            )
        }

        if (ui.showTunnelFailed) {
            AGCard(borderColor = AG.colors.red.copy(alpha = 0.4f), background = AG.colors.wash(Accent.RED)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardTitle(stringResource(R.string.tunnel_failed_title))
                    Text(stringResource(R.string.tunnel_failed_body), style = AGText.bodySm, color = AG.colors.textDim)
                    PrimaryButton(stringResource(R.string.action_retry), Icons.Filled.Refresh) { vm.retryTunnel() }
                }
            }
        }

        Hero(
            kind = ui.kind, accent = ui.accent, eyebrow = ui.eyebrow, title = ui.title, subtitle = ui.subtitle,
            animate = ui.kind == HeroKind.PROTECTED && !reduceMotion,
            recovery = recoveryAction(ui.kind, ui.automationEnabled, ui.override) {
                vm.setAutomation(it.first); vm.setOverride(it.second)
            },
        ) {
            HeroNetworkChip(ui)
        }

        // Automation
        val automationBlocked = !ui.automationEnabled && !ui.canEnableAutomation
        val blockedMsgRes = when {
            ui.needsDefaultTunnel && ui.needsTrustedNetwork -> R.string.automation_need_both
            ui.needsDefaultTunnel -> R.string.automation_need_tunnel
            else -> R.string.automation_need_trusted
        }
        val blockedMsg = stringResource(blockedMsgRes)
        AGCard(
            // A disabled switch swallows its own tap; make the whole card tappable
            // when blocked so a tap explains *why* and *what to fix* instead of nothing.
            modifier = if (automationBlocked) Modifier.clickable {
                Toast.makeText(context, blockedMsg, Toast.LENGTH_LONG).show()
            } else Modifier
        ) {
            ToggleRow(
                title = stringResource(R.string.automation_title),
                subtitle = when {
                    automationBlocked -> stringResource(R.string.automation_subtitle_setup_needed)
                    !ui.automationEnabled -> stringResource(R.string.automation_subtitle_off)
                    ui.kind == HeroKind.AT_HOME -> stringResource(R.string.automation_subtitle_armed)
                    else -> stringResource(R.string.automation_subtitle_on)
                },
                checked = ui.automationEnabled, leading = Icons.Filled.Bolt,
                // Can't enable without a default tunnel + a trusted network; can always turn off.
                switchEnabled = ui.automationEnabled || ui.canEnableAutomation,
            ) { vm.setAutomation(it) }
        }

        // At home: when did we last switch off?
        if (ui.kind == HeroKind.AT_HOME && ui.lastSwitch != null) {
            AGCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(Icons.Filled.Schedule, accent = Accent.GREEN)
                    Column(Modifier.weight(1f)) {
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.home_last_switched_prefix))
                                withStyle(SpanStyle(fontFamily = Mono, color = AG.colors.textDim)) {
                                    append(relativeTime(context, ui.lastSwitch!!.timeMillis))
                                }
                            },
                            style = AGText.body, color = AG.colors.text, maxLines = 1
                        )
                        Text(eventDescription(ui.lastSwitch!!.message), style = AGText.subtitle, color = AG.colors.textDim, maxLines = 1)
                    }
                }
            }
        }

        // Manual override
        AGCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        CardTitle(stringResource(R.string.manual_override_title))
                        Text(stringResource(R.string.manual_override_subtitle), style = AGText.subtitle, color = AG.colors.textDim)
                    }
                }
                Spacer(Modifier.size(12.dp))
                SegmentedControl(
                    segments = listOf(
                        Segment(stringResource(R.string.segment_auto), Accent.GREEN),
                        Segment(stringResource(R.string.segment_force_on), Accent.GREEN),
                        Segment(stringResource(R.string.segment_force_off), Accent.RED),
                    ),
                    selected = when (ui.override) {
                        OverrideMode.AUTO -> 0; OverrideMode.FORCE_ON -> 1; OverrideMode.FORCE_OFF -> 2
                    },
                    onSelect = { idx ->
                        vm.setOverride(when (idx) { 0 -> OverrideMode.AUTO; 1 -> OverrideMode.FORCE_ON; else -> OverrideMode.FORCE_OFF })
                    }
                )
            }
        }

        // Default tunnel
        AGCard {
            Column {
                CardTitle(stringResource(R.string.default_tunnel_title))
                Spacer(Modifier.size(10.dp))
                EditableFieldRow(
                    icon = Icons.Filled.Shield,
                    value = defaultTunnel,
                    placeholder = stringResource(R.string.default_tunnel_placeholder),
                ) { defaultTunnel = it; vm.setDefaultTunnel(it) }
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.default_tunnel_hint),
                    style = AGText.bodySm, color = AG.colors.textFaint)
            }
        }

        // Configure
        AGCard(padding = 4.dp) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                NavRow(Icons.Filled.Wifi, stringResource(R.string.nav_networks), ui.networksSubtitle, Icons.AutoMirrored.Filled.KeyboardArrowRight) { onNavigate(HomeDest.NETWORKS) }
                HDivider()
                NavRow(Icons.Filled.Timeline, stringResource(R.string.nav_event_log), stringResource(R.string.nav_event_log_subtitle, ui.eventCount), Icons.AutoMirrored.Filled.KeyboardArrowRight) { onNavigate(HomeDest.EVENTS) }
                HDivider()
                NavRow(Icons.Filled.Layers, stringResource(R.string.nav_split), stringResource(R.string.nav_split_subtitle), Icons.AutoMirrored.Filled.KeyboardArrowRight) { onNavigate(HomeDest.SPLIT) }
            }
        }

        // Behaviour
        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CardTitle(stringResource(R.string.behaviour_title))
                ToggleRow(stringResource(R.string.behaviour_mobile_title), stringResource(R.string.behaviour_mobile_subtitle), ui.onMobile, titleStyle = AGText.listTitle) { vm.setOnMobile(it) }
                HDivider()
                ToggleRow(stringResource(R.string.behaviour_unknown_title), stringResource(R.string.behaviour_unknown_subtitle), ui.unknown, titleStyle = AGText.listTitle) { vm.setUnknown(it) }
            }
        }
    }
}

@Composable
private fun HeroNetworkChip(ui: HomeUiState) {
    val c = AG.colors
    Row(
        Modifier.fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(15.dp))
            .background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(15.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Icon(Icons.Filled.Wifi, null, tint = c.accentText(ui.chipWifiAccent), modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(ui.chipSsid, style = AGText.mono, color = c.text, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (ui.pinned) Icon(Icons.Filled.Lock, null, tint = c.accentText(ui.chipStatusAccent), modifier = Modifier.size(11.dp))
                Text(ui.chipStatus, style = AGText.subtitle, color = c.accentText(ui.chipStatusAccent), maxLines = 1)
            }
        }
        StatusPill(ui.tunnelLabel, ui.tunnelAccent, ui.tunnelAccent != null)
    }
}

@Composable
private fun EditableFieldRow(icon: ImageVector, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val c = AG.colors
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp)).background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = c.green, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = true,
                textStyle = AGText.mono.copy(color = c.text),
                cursorBrush = SolidColor(c.green),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = AGText.mono, color = c.textFaint, maxLines = 1)
                    inner()
                }
            )
        }
        Icon(Icons.Filled.Edit, null, tint = c.textFaint, modifier = Modifier.size(16.dp))
    }
}

enum class HomeDest { PERMISSIONS, NETWORKS, EVENTS, SPLIT }

/* =============================== ONBOARDING =============================== */

private enum class StepState { DONE, ACTIVE, PENDING }

@Composable
fun OnboardingScreen(onContinue: () -> Unit, onOpenPermissions: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val tick = rememberResumeTick()

    // Re-checked on every resume — install WireGuard, come back, step 1 auto-completes.
    val installed = remember(tick) { WireGuardController.isWireGuardInstalled(context) }
    var tunnelDone by remember(tick) { mutableStateOf(settings.tunnelSetupConfirmed) }
    var remoteDone by remember(tick) { mutableStateOf(settings.remoteControlHintDismissed) }
    var locationGranted by remember(tick) { mutableStateOf(NetworkInspector.hasLocationPermission(context)) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        locationGranted = NetworkInspector.hasLocationPermission(context)
    }
    fun requestLocation() = locationLauncher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    fun finish() { settings.onboardingComplete = true; onContinue() }

    val active = when {
        !installed -> 1
        !tunnelDone -> 2
        !remoteDone -> 3
        !locationGranted -> 4
        else -> 5
    }
    val allDone = installed && tunnelDone && remoteDone && locationGranted

    fun stateOf(step: Int, done: Boolean) = when {
        done -> StepState.DONE
        active == step -> StepState.ACTIVE
        else -> StepState.PENDING
    }

    ScreenFrame(wordmark = true) {
        AGCard(borderColor = AG.colors.amber.copy(alpha = 0.22f), padding = 24.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(78.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(AG.colors.surface2)
                        .border(1.dp, AG.colors.amber.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.GppMaybe, null, tint = AG.colors.amber, modifier = Modifier.size(36.dp)) }
                Spacer(Modifier.size(16.dp))
                Eyebrow(if (allDone) stringResource(R.string.onboarding_eyebrow_all_set) else stringResource(R.string.onboarding_eyebrow_one_step), Accent.AMBER)
                Spacer(Modifier.size(7.dp))
                Text(if (allDone) stringResource(R.string.onboarding_title_ready) else stringResource(R.string.onboarding_title_almost), style = AGText.heroTitle, color = AG.colors.text)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.onboarding_intro),
                    style = AGText.body, color = AG.colors.textDim)
            }
        }

        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CardTitle(stringResource(R.string.onboarding_steps_title))
                StepRow(1, stateOf(1, installed), stringResource(R.string.onboarding_step1_title),
                    AnnotatedString(stringResource(R.string.onboarding_step1_body)))
                StepRow(2, stateOf(2, tunnelDone), stringResource(R.string.onboarding_step2_title),
                    AnnotatedString(stringResource(R.string.onboarding_step2_body))) {
                    if (active == 2) StepActions(
                        onOpen = { if (!WireGuardController.openWireGuard(context)) openWireGuardInstall(context) },
                        confirmLabel = stringResource(R.string.onboarding_step2_confirm),
                        onConfirm = { tunnelDone = true; settings.tunnelSetupConfirmed = true },
                    )
                }
                StepRow(3, stateOf(3, remoteDone), stringResource(R.string.onboarding_step3_title),
                    buildAnnotatedString {
                        append(stringResource(R.string.onboarding_step3_body_prefix))
                        withStyle(SpanStyle(fontFamily = Mono)) { append(stringResource(R.string.onboarding_step3_body_highlight)) }
                        append(stringResource(R.string.onboarding_step3_body_suffix))
                    }) {
                    if (active == 3) StepActions(
                        onOpen = { if (!WireGuardController.openWireGuard(context)) openWireGuardInstall(context) },
                        confirmLabel = stringResource(R.string.onboarding_step3_confirm),
                        onConfirm = { remoteDone = true; settings.remoteControlHintDismissed = true },
                    )
                }
                StepRow(4, stateOf(4, locationGranted), stringResource(R.string.onboarding_step4_title),
                    AnnotatedString(stringResource(R.string.onboarding_step4_body)))
            }
        }

        when {
            allDone -> PrimaryButton(stringResource(R.string.onboarding_continue), Icons.Filled.Check) { finish() }
            !installed -> {
                PrimaryButton(stringResource(R.string.onboarding_install), Icons.Filled.PlayArrow) { openWireGuardInstall(context) }
                Text(stringResource(R.string.onboarding_already_installed),
                    style = AGText.subtitle, color = AG.colors.textFaint, modifier = Modifier.fillMaxWidth())
            }
            active == 4 -> PrimaryButton(stringResource(R.string.perm_grant_location), Icons.Filled.LocationOn) { requestLocation() }
            else -> {
                Text(stringResource(R.string.onboarding_checked_on_return),
                    style = AGText.subtitle, color = AG.colors.textFaint, modifier = Modifier.fillMaxWidth())
                OutlineButton(stringResource(R.string.onboarding_skip)) { finish() }
            }
        }

        // Surface the full permissions screen (background location, battery, notifications)
        // so it isn't hidden behind the Home gear.
        Text(
            stringResource(R.string.onboarding_review_permissions),
            style = AGText.button, color = AG.colors.blue,
            modifier = Modifier.fillMaxWidth().clickable { onOpenPermissions() }.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun StepActions(onOpen: () -> Unit, confirmLabel: String, onConfirm: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OutlineButton(stringResource(R.string.action_open_wireguard), Icons.AutoMirrored.Filled.OpenInNew) { onOpen() }
        }
        ActionPill(confirmLabel, Accent.GREEN, PillStyle.SOLID) { onConfirm() }
    }
}

@Composable
private fun StepRow(n: Int, state: StepState, title: String, body: AnnotatedString, action: @Composable () -> Unit = {}) {
    val c = AG.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Active/done circles are SOLID fills (per design); pending is a flat surface-2.
        val fill = when (state) {
            StepState.DONE -> c.green
            StepState.ACTIVE -> c.amber
            StepState.PENDING -> c.surface2
        }
        Box(
            Modifier.size(26.dp).clip(androidx.compose.foundation.shape.CircleShape).background(fill),
            contentAlignment = Alignment.Center
        ) {
            if (state == StepState.DONE) {
                Icon(Icons.Filled.Check, stringResource(R.string.cd_done), tint = c.onGreen, modifier = Modifier.size(15.dp))
            } else {
                Text("$n", style = AGText.monoSm, color = if (state == StepState.ACTIVE) c.onAmber else c.textFaint)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text(title, style = AGText.rowTitle, color = if (state == StepState.PENDING) c.textDim else c.text)
                Text(body, style = AGText.subtitle, color = c.textDim)
            }
            action()
        }
    }
}

/* =============================== PERMISSIONS =============================== */

@Composable
fun SettingsScreen(onBack: () -> Unit, onViewTerms: () -> Unit) {
    val context = LocalContext.current
    val tick = rememberResumeTick()

    val location = remember(tick) { NetworkInspector.hasLocationPermission(context) }
    val background = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val battery = remember(tick) {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
    }
    val notifications = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val allGranted = location && background && battery && notifications

    ScreenFrame(title = stringResource(R.string.settings_title), onBack = onBack) {
        AGCard(borderColor = AG.colors.amber.copy(alpha = 0.22f), background = AG.colors.wash(Accent.AMBER)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Info, null, tint = AG.colors.amber, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.permissions_info),
                    style = AGText.body, color = AG.colors.text)
            }
        }

        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow(Icons.Filled.LocationOn, stringResource(R.string.perm_location_title), stringResource(R.string.perm_location_subtitle),
                    granted = location, accent = Accent.AMBER, actionLabel = stringResource(R.string.perm_grant), style = PillStyle.SOLID) {
                    locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                HDivider()
                PermissionRow(Icons.Filled.LocationOn, stringResource(R.string.perm_background_title), stringResource(R.string.perm_background_subtitle),
                    granted = background, accent = Accent.BLUE, actionLabel = stringResource(R.string.perm_set), style = PillStyle.OUTLINE) {
                    openAppDetails(context)
                }
                HDivider()
                PermissionRow(Icons.Filled.Bolt, stringResource(R.string.perm_battery_title), stringResource(R.string.perm_battery_subtitle),
                    granted = battery, accent = Accent.BLUE, actionLabel = stringResource(R.string.perm_disable), style = PillStyle.OUTLINE) {
                    runCatching {
                        context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                    }
                }
                HDivider()
                PermissionRow(Icons.Filled.Notifications, stringResource(R.string.perm_notifications_title), stringResource(R.string.perm_notifications_subtitle),
                    granted = notifications, accent = Accent.AMBER, actionLabel = stringResource(R.string.perm_grant), style = PillStyle.SOLID) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (!allGranted) {
            PrimaryButton(stringResource(R.string.perm_grant_location), Icons.Filled.LocationOn) {
                locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        } else {
            Text(stringResource(R.string.perm_all_set), style = AGText.body, color = AG.colors.green, modifier = Modifier.fillMaxWidth())
        }

        // App version, changelog and terms, flattened into Settings.
        AboutSection(onViewTerms = onViewTerms)
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, subtitle: String, granted: Boolean, accent: Accent, actionLabel: String, style: PillStyle, onAction: () -> Unit) {
    val c = AG.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon, accent = accent, tile = 38.dp, iconSize = 19.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = AGText.listTitle, color = c.text)
            Text(subtitle, style = AGText.subtitle, color = c.textDim)
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Check, null, tint = c.green, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.perm_granted), style = AGText.segment, color = c.green)
            }
        } else {
            ActionPill(actionLabel, accent, style, onAction)
        }
    }
}

/* ================================ NETWORKS ================================ */

@Composable
fun NetworksScreen(onBack: () -> Unit, onEdit: (Int) -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var rules by remember { mutableStateOf(settings.rules) }
    val hasLocation = remember { NetworkInspector.hasLocationPermission(context) }
    var showPicker by remember { mutableStateOf(false) }

    fun save(newRules: List<NetworkRule>) { rules = newRules; settings.rules = newRules }
    fun addTrusted(ssid: String) {
        if (ssid.isNotBlank() && rules.none { it.ssid == ssid }) save(rules + NetworkRule(ssid = ssid, trusted = true))
    }

    if (showPicker) {
        WifiPickerDialog(existing = rules.map { it.ssid }.toSet(),
            onPick = { addTrusted(it) }, onDismiss = { showPicker = false })
    }

    ScreenFrame(title = stringResource(R.string.networks_title), onBack = onBack) {
        Text(stringResource(R.string.networks_intro),
            style = AGText.bodySm, color = AG.colors.textDim)

        if (!hasLocation) {
            AGCard(borderColor = AG.colors.amber.copy(alpha = 0.22f), background = AG.colors.wash(Accent.AMBER)) {
                Text(stringResource(R.string.networks_need_location),
                    style = AGText.body, color = AG.colors.text)
            }
        }

        OutlineButton(stringResource(R.string.networks_add_current), Icons.Filled.Wifi, enabled = hasLocation) {
            val s = NetworkInspector.current(context)
            when {
                s.ssid != null -> { addTrusted(s.ssid); Toast.makeText(context, context.getString(R.string.networks_added_toast, s.ssid), Toast.LENGTH_SHORT).show() }
                !s.isWifi -> Toast.makeText(context, context.getString(R.string.networks_not_connected), Toast.LENGTH_LONG).show()
                else -> Toast.makeText(context, context.getString(R.string.networks_cant_read), Toast.LENGTH_LONG).show()
            }
        }
        OutlineButton(stringResource(R.string.networks_pick_nearby), Icons.Filled.Radar, enabled = hasLocation) { showPicker = true }

        LabeledDivider(stringResource(R.string.networks_saved, rules.size))

        if (rules.isEmpty()) {
            Text(stringResource(R.string.networks_empty),
                style = AGText.body, color = AG.colors.textDim, modifier = Modifier.fillMaxWidth())
        }
        rules.forEachIndexed { index, rule ->
            NetworkCard(rule) { onEdit(index) }
        }

        OutlineButton(stringResource(R.string.networks_add_manually), Icons.Filled.Add) {
            save(rules + NetworkRule(ssid = "", trusted = true)); onEdit(rules.size)
        }
    }
}

@Composable
private fun NetworkCard(rule: NetworkRule, onClick: () -> Unit) {
    val c = AG.colors
    AGCard {
        Column(Modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(rule.ssid.ifEmpty { stringResource(R.string.network_unnamed) }, style = AGText.mono, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                Badge(if (rule.trusted) stringResource(R.string.badge_trusted) else stringResource(R.string.badge_untrusted), if (rule.trusted) Accent.BLUE else Accent.AMBER)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    rule.bssids.isNotEmpty() -> {
                        Icon(Icons.Filled.Lock, null, tint = c.blue, modifier = Modifier.size(12.dp))
                        Text(stringResource(R.string.network_pinned_suffix, rule.bssids.first()), style = AGText.monoSm, color = c.textDim, maxLines = 1)
                    }
                    !rule.trusted && rule.tunnels.isNotEmpty() -> Text(
                        buildAnnotatedString {
                            append(stringResource(R.string.network_tunnel_prefix))
                            withStyle(SpanStyle(fontFamily = Mono, color = c.green)) { append(rule.tunnels.first()) }
                            append(stringResource(R.string.network_tunnel_suffix))
                        },
                        style = AGText.subtitle, color = c.textDim, maxLines = 1
                    )
                    else -> Text(stringResource(R.string.network_no_bssid), style = AGText.subtitle, color = c.textDim, maxLines = 1)
                }
            }
        }
    }
}

/* ============================= EDIT NETWORK ============================= */

@Composable
fun EditNetworkScreen(index: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val initial = remember { settings.rules.getOrNull(index) ?: NetworkRule(ssid = "", trusted = true) }

    var ssid by remember { mutableStateOf(initial.ssid) }
    var trusted by remember { mutableStateOf(initial.trusted) }
    var bssids by remember { mutableStateOf(initial.bssids.toList()) }
    var tunnels by remember { mutableStateOf(initial.tunnels.joinToString(", ")) }
    var newBssid by remember { mutableStateOf("") }
    val hasLocation = remember { NetworkInspector.hasLocationPermission(context) }

    fun persist() {
        val rule = NetworkRule(
            ssid = ssid.trim(), trusted = trusted, bssids = bssids.toSet(),
            tunnels = tunnels.split(",").map(String::trim).filter(String::isNotEmpty),
        )
        val list = settings.rules.toMutableList()
        if (index in list.indices) list[index] = rule else list.add(rule)
        settings.rules = list
    }

    ScreenFrame(title = stringResource(R.string.edit_network_title), onBack = { persist(); onBack() }) {
        Text(stringResource(R.string.edit_network_name_label).uppercase(), style = AGText.eyebrow, color = AG.colors.textDim)
        AGField(value = ssid, placeholder = stringResource(R.string.edit_network_name_placeholder), onValueChange = { ssid = it }, mono = true, dashed = true)

        AGCard {
            ToggleRow(stringResource(R.string.edit_trusted_title), stringResource(R.string.edit_trusted_subtitle), trusted, accent = Accent.BLUE, titleStyle = AGText.listTitle) { trusted = it }
        }

        if (!trusted) {
            AGCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardTitle(stringResource(R.string.edit_tunnel_title))
                    AGField(value = tunnels, placeholder = stringResource(R.string.edit_tunnel_placeholder), onValueChange = { tunnels = it }, mono = true)
                }
            }
        }

        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, null, tint = AG.colors.text, modifier = Modifier.size(18.dp))
                    CardTitle(stringResource(R.string.edit_pinned_title))
                }
                if (bssids.isEmpty()) Text(stringResource(R.string.edit_no_bssid), style = AGText.subtitle, color = AG.colors.textDim)
                bssids.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b, style = AGText.mono, color = AG.colors.text, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Close, stringResource(R.string.cd_remove), tint = AG.colors.textFaint,
                            modifier = Modifier.size(18.dp).clickable { bssids = bssids - b })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        AGField(value = newBssid, placeholder = stringResource(R.string.edit_bssid_placeholder), onValueChange = { newBssid = it }, mono = true, dashed = true)
                    }
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(AG.colors.surface2)
                            .clickable {
                                val b = newBssid.trim()
                                if (b.isNotEmpty()) { bssids = bssids + b; newBssid = "" }
                            },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Add, stringResource(R.string.cd_add_bssid), tint = AG.colors.text, modifier = Modifier.size(20.dp)) }
                }
                ActionPill(stringResource(R.string.edit_pin_current), Accent.BLUE, PillStyle.OUTLINE) {
                    val s = NetworkInspector.current(context)
                    if (s.bssid != null && hasLocation) { bssids = (bssids + s.bssid).distinct(); Toast.makeText(context, context.getString(R.string.edit_pinned_toast, s.bssid), Toast.LENGTH_SHORT).show() }
                    else Toast.makeText(context, context.getString(R.string.edit_cant_read_bssid), Toast.LENGTH_LONG).show()
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                    .border(1.dp, AG.colors.red.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                    .clickable {
                        val list = settings.rules.toMutableList()
                        if (index in list.indices) { list.removeAt(index); settings.rules = list }
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Delete, stringResource(R.string.cd_delete), tint = AG.colors.red, modifier = Modifier.size(20.dp)) }
            Box(Modifier.weight(1f)) { PrimaryButton(stringResource(R.string.edit_save), Icons.Filled.Check) { persist(); onBack() } }
        }
    }
}

/* ============================== EVENT LOG ============================== */

@Composable
fun EventLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val log = remember { EventLog(context) }
    var entries by remember { mutableStateOf(log.entries()) }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ScreenFrame(
        title = stringResource(R.string.event_log_title), onBack = onBack,
        trailing = {
            Text(stringResource(R.string.action_clear), style = AGText.button, color = AG.colors.green,
                modifier = Modifier.clickable { log.clear(); entries = emptyList() })
        }
    ) {
        val eventHeader = stringResource(R.string.event_log_events, entries.size) +
            if (entries.isNotEmpty()) stringResource(R.string.event_log_today_suffix) else ""
        Text(eventHeader, style = AGText.monoSm, color = AG.colors.textDim)

        if (entries.isEmpty()) {
            Text(stringResource(R.string.event_log_empty), style = AGText.body, color = AG.colors.textDim, modifier = Modifier.fillMaxWidth())
        } else {
            AGCard {
                Column {
                    entries.forEachIndexed { i, e ->
                        val (label, accent) = when (eventType(e.message)) {
                            "FORCED_ON" -> stringResource(R.string.event_label_forced_on) to Accent.GREEN
                            "FORCED_OFF" -> stringResource(R.string.event_label_forced_off) to Accent.AMBER
                            "ON" -> stringResource(R.string.event_label_on) to Accent.GREEN
                            "VPN_UP" -> stringResource(R.string.event_label_vpn_up) to Accent.GREEN
                            "VPN_DOWN" -> stringResource(R.string.event_label_vpn_down) to (null as Accent?)
                            "FAILED" -> stringResource(R.string.event_label_failed) to Accent.RED
                            else -> stringResource(R.string.event_label_off) to null
                        }
                        TimelineEntry(
                            label = label,
                            time = fmt.format(Date(e.timeMillis)),
                            desc = eventDescription(e.message),
                            accent = accent,
                            last = i == entries.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(label: String, time: String, desc: String, accent: Accent?, last: Boolean) {
    val c = AG.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 4px bg halo so the dot "punches through" the timeline line.
            Box(
                Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.bg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(16.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (accent != null) c.accentText(accent) else c.surface2)
                        .border(if (accent == null) 1.dp else 0.dp, c.line2, androidx.compose.foundation.shape.CircleShape)
                )
            }
            if (!last) Box(Modifier.width(2.dp).heightIn(min = 26.dp).weight(1f).background(c.line))
        }
        Column(Modifier.weight(1f).padding(bottom = if (last) 0.dp else 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = AGText.rowTitle, color = if (accent != null) c.accentText(accent) else c.text, modifier = Modifier.weight(1f), maxLines = 1)
                Text(time, style = AGText.timestamp, color = c.textDim)
            }
            Text(desc, style = AGText.bodySm, color = c.textDim)
        }
    }
}

/* ============================= SPLIT TUNNEL ============================= */

@Composable
fun SplitTunnelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    ScreenFrame(title = stringResource(R.string.split_title), onBack = onBack) {
        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardTitle(stringResource(R.string.split_card_title))
                Text(stringResource(R.string.split_card_body),
                    style = AGText.body, color = AG.colors.textDim)
            }
        }
        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow(stringResource(R.string.split_eyebrow), Accent.GREEN)
                StepMono("01", stringResource(R.string.split_step1))
                StepMono("02", stringResource(R.string.split_step2))
                StepMono("03", stringResource(R.string.split_step3))
                StepMono("04", stringResource(R.string.split_step4))
            }
        }
        PrimaryButton(stringResource(R.string.action_open_wireguard), Icons.AutoMirrored.Filled.OpenInNew) {
            if (!WireGuardController.openWireGuard(context)) openWireGuardInstall(context)
        }
    }
}

@Composable
private fun StepMono(n: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(n, style = AGText.mono, color = AG.colors.green)
        Text(text, style = AGText.body, color = AG.colors.text, modifier = Modifier.weight(1f))
    }
}

/* ========================== NEARBY WIFI DIALOG ========================== */

@Composable
private fun WifiPickerDialog(existing: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var networks by remember { mutableStateOf(WifiScanner.results(context)) }
    val added = remember { mutableStateListOf<String>() }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { networks = WifiScanner.results(context) }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context, receiver, android.content.IntentFilter(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        WifiScanner.requestScan(context)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val c = AG.colors
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
                .border(1.dp, c.line, RoundedCornerShape(20.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Radar, null, tint = c.green, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.wifi_picker_title), style = AGText.wordmark, color = c.text)
            }
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (networks.isEmpty()) Text(stringResource(R.string.wifi_picker_empty), style = AGText.bodySm, color = AG.colors.textDim)
                networks.forEach { net -> WifiPickerRow(net, net.ssid in existing || net.ssid in added) { onPick(net.ssid); added.add(net.ssid) } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.weight(1f))
                ActionPill(stringResource(R.string.wifi_picker_rescan), Accent.BLUE, PillStyle.OUTLINE) { WifiScanner.requestScan(context); networks = WifiScanner.results(context) }
                Box(
                    Modifier.clip(RoundedCornerShape(120.dp)).background(c.surface2)
                        .border(1.dp, c.line2, RoundedCornerShape(120.dp))
                        .clickable { onDismiss() }.padding(horizontal = 15.dp, vertical = 9.dp)
                ) { Text(stringResource(R.string.action_done), style = AGText.segment, color = c.green) }
            }
        }
    }
}

@Composable
private fun WifiPickerRow(net: ScannedNetwork, alreadyAdded: Boolean, onAdd: () -> Unit) {
    val c = AG.colors
    val bars = when { net.level >= -55 -> 4; net.level >= -67 -> 3; net.level >= -78 -> 2; else -> 1 }
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !alreadyAdded, onClick = onAdd).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(net.ssid, style = AGText.mono, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            (1..4).forEach { i ->
                Box(Modifier.width(3.dp).heightIn(min = (3 + i * 2).dp).background(if (i <= bars) c.green else c.line2))
            }
        }
        Spacer(Modifier.width(6.dp))
        if (alreadyAdded) Text(stringResource(R.string.wifi_picker_added), style = AGText.subtitle, color = c.green)
        else Icon(Icons.Filled.Add, stringResource(R.string.cd_add), tint = c.green, modifier = Modifier.size(20.dp))
    }
}

/* ============================== shared bits ============================== */

@Composable
private fun AGField(value: String, placeholder: String, onValueChange: (String) -> Unit, mono: Boolean, dashed: Boolean = false) {
    val c = AG.colors
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(c.surface)
            .border(1.dp, if (dashed) c.line2 else c.line, RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 13.dp)
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = (if (mono) AGText.mono else AGText.body).copy(color = c.text),
            cursorBrush = SolidColor(c.green),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = (if (mono) AGText.mono else AGText.body), color = c.textFaint)
                inner()
            }
        )
    }
}

/** Recovery CTA shown inside the Off hero (STATES 2b), or null when there's no single fix. */
private fun recoveryAction(
    kind: HeroKind,
    automation: Boolean,
    override: OverrideMode,
    apply: (Pair<Boolean, OverrideMode>) -> Unit,
): (@Composable () -> Unit)? {
    if (kind != HeroKind.OFF) return null
    if (!automation) return { OutlineButton(stringResource(R.string.recovery_turn_automation_on), Icons.Filled.Bolt) { apply(true to override) } }
    if (override == OverrideMode.FORCE_OFF) return { OutlineButton(stringResource(R.string.recovery_back_to_auto)) { apply(automation to OverrideMode.AUTO) } }
    return null
}

/** Dismissible reminder — we can't detect WireGuard's remote-control setting via the intent API. */
@Composable
private fun RemoteControlBanner(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val c = AG.colors
    AGCard(borderColor = c.amber.copy(alpha = 0.22f), background = c.wash(Accent.AMBER)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Info, null, tint = c.amber, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.remote_banner_title), style = AGText.rowTitle, color = c.text)
                    Text(stringResource(R.string.remote_banner_subtitle), style = AGText.subtitle, color = c.textDim)
                }
                Icon(Icons.Filled.Close, stringResource(R.string.cd_dismiss), tint = c.textFaint,
                    modifier = Modifier.size(18.dp).clickable(onClick = onDismiss))
            }
            ActionPill(stringResource(R.string.action_open_wireguard), Accent.AMBER, PillStyle.SOLID, onOpen)
        }
    }
}

private fun relativeTime(context: Context, millis: Long): String {
    val min = (System.currentTimeMillis() - millis) / 60000
    return when {
        min < 1 -> context.getString(R.string.time_just_now)
        min < 60 -> context.getString(R.string.time_minutes_ago, min)
        min < 1440 -> context.getString(R.string.time_hours_ago, min / 60)
        else -> context.getString(R.string.time_days_ago, min / 1440)
    }
}

private fun eventType(message: String): String =
    if (message.contains(EventLog.SEP)) message.substringBefore(EventLog.SEP) else ""

private fun eventDescription(message: String): String =
    if (message.contains(EventLog.SEP)) message.substringAfter(EventLog.SEP) else message

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        AndroidSettings.Global.getFloat(context.contentResolver, AndroidSettings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

internal fun openWireGuardInstall(context: Context) {
    val pkg = WireGuardController.WG_PACKAGE
    val targets = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wireguard.com/install/")),
    )
    for (intent in targets) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
}

private fun openAppDetails(context: Context) {
    runCatching {
        context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
}
