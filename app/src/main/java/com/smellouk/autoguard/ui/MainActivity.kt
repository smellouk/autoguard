package com.smellouk.autoguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.smellouk.autoguard.ui.screens.AcknowledgeScreen
import com.smellouk.autoguard.ui.screens.EditNetworkScreen
import com.smellouk.autoguard.ui.screens.TermsScreen
import com.smellouk.autoguard.ui.screens.EventLogScreen
import com.smellouk.autoguard.ui.screens.HomeDest
import com.smellouk.autoguard.ui.screens.HomeScreen
import com.smellouk.autoguard.ui.screens.NetworksScreen
import com.smellouk.autoguard.ui.screens.OnboardingScreen
import com.smellouk.autoguard.ui.screens.SettingsScreen
import com.smellouk.autoguard.ui.screens.SplitTunnelScreen
import com.smellouk.autoguard.ui.theme.AG
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.ui.theme.AutoGuardTheme

private sealed interface Dest {
    val key: String
    data object Acknowledge : Dest { override val key = "acknowledge" }
    data object Onboarding : Dest { override val key = "onboarding" }
    data object Home : Dest { override val key = "home" }
    data object Terms : Dest { override val key = "terms" }
    data object Permissions : Dest { override val key = "permissions" }
    data object Networks : Dest { override val key = "networks" }
    data object Events : Dest { override val key = "events" }
    data object Split : Dest { override val key = "split" }
    data class EditNetwork(val index: Int) : Dest { override val key = "edit-$index" }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AutoGuardTheme {
                AutoGuardApp()
            }
        }
    }
}

@Composable
private fun AutoGuardApp() {
    val context = LocalContext.current
    // Show onboarding on first launch regardless of whether WireGuard is already
    // installed (the user still needs to confirm tunnel/remote-control + grant location).
    val start = remember {
        val settings = Settings(context)
        when {
            !settings.termsAccepted -> Dest.Acknowledge // first thing that starts
            !settings.onboardingComplete -> Dest.Onboarding
            else -> Dest.Home
        }
    }
    val stack = remember { mutableStateListOf<Dest>(start) }
    val current = stack.last()
    val holder = rememberSaveableStateHolder()

    fun go(dest: Dest) { stack.add(dest) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.size > 1) { back() }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(AG.colors.bg)) {
        holder.SaveableStateProvider(current.key) {
            when (val d = current) {
                Dest.Acknowledge -> AcknowledgeScreen(
                    onAccept = {
                        stack.clear()
                        stack.add(if (Settings(context).onboardingComplete) Dest.Home else Dest.Onboarding)
                    },
                    onViewTerms = { go(Dest.Terms) },
                )
                Dest.Onboarding -> OnboardingScreen(
                    onContinue = { stack.clear(); stack.add(Dest.Home) },
                    onOpenPermissions = { go(Dest.Permissions) },
                )
                Dest.Home -> HomeScreen(onNavigate = { dest ->
                    when (dest) {
                        HomeDest.PERMISSIONS -> go(Dest.Permissions)
                        HomeDest.NETWORKS -> go(Dest.Networks)
                        HomeDest.EVENTS -> go(Dest.Events)
                        HomeDest.SPLIT -> go(Dest.Split)
                    }
                })
                Dest.Terms -> TermsScreen(onBack = { back() })
                Dest.Permissions -> SettingsScreen(onBack = { back() }, onViewTerms = { go(Dest.Terms) })
                Dest.Networks -> NetworksScreen(onBack = { back() }, onEdit = { go(Dest.EditNetwork(it)) })
                Dest.Events -> EventLogScreen(onBack = { back() })
                Dest.Split -> SplitTunnelScreen(onBack = { back() })
                is Dest.EditNetwork -> EditNetworkScreen(index = d.index, onBack = { back() })
            }
        }
    }
}
