package com.smellouk.autoguard.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smellouk.autoguard.BuildConfig
import com.smellouk.autoguard.R
import com.smellouk.autoguard.data.Settings
import com.smellouk.autoguard.ui.components.AGCard
import com.smellouk.autoguard.ui.components.CardTitle
import com.smellouk.autoguard.ui.components.Eyebrow
import com.smellouk.autoguard.ui.components.HDivider
import com.smellouk.autoguard.ui.components.NavRow
import com.smellouk.autoguard.ui.components.OutlineButton
import com.smellouk.autoguard.ui.components.PrimaryButton
import com.smellouk.autoguard.ui.theme.AG
import com.smellouk.autoguard.ui.theme.AGText
import com.smellouk.autoguard.ui.theme.Accent

/* ============================ ACKNOWLEDGEMENT (first launch) ============================ */

@Composable
fun AcknowledgeScreen(onAccept: () -> Unit, onViewTerms: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var agreed by remember { mutableStateOf(false) }

    ScreenFrame(wordmark = true) {
        AGCard(borderColor = AG.colors.green.copy(alpha = 0.24f), padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(AG.colors.greenGradient),
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Icon(Icons.Filled.Shield, null, tint = AG.colors.onGreen, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.size(16.dp))
                Eyebrow(stringResource(R.string.ack_eyebrow), Accent.GREEN)
                Spacer(Modifier.size(7.dp))
                androidx.compose.material3.Text(stringResource(R.string.ack_title), style = AGText.heroTitle, color = AG.colors.text)
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.Text(stringResource(R.string.ack_intro), style = AGText.body, color = AG.colors.textDim)
            }
        }

        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CardTitle(stringResource(R.string.ack_summary_title))
                androidx.compose.material3.Text(stringResource(R.string.ack_summary), style = AGText.body, color = AG.colors.textDim)
            }
        }

        androidx.compose.material3.Text(
            stringResource(R.string.ack_read_full),
            style = AGText.button, color = AG.colors.blue,
            modifier = Modifier.fillMaxWidth().clickable { onViewTerms() }.padding(vertical = 4.dp)
        )

        // Acknowledgement checkbox — accept stays disabled until ticked.
        Row(
            Modifier.fillMaxWidth().clickable { agreed = !agreed }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val c = AG.colors
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (agreed) c.green else c.surface2)
                    .border(1.dp, if (agreed) c.green else c.line2, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { if (agreed) androidx.compose.material3.Icon(Icons.Filled.Check, null, tint = c.onGreen, modifier = Modifier.size(15.dp)) }
            androidx.compose.material3.Text(stringResource(R.string.ack_checkbox), style = AGText.body, color = c.text, modifier = Modifier.weight(1f))
        }

        if (agreed) {
            PrimaryButton(stringResource(R.string.ack_agree), Icons.Filled.Check) {
                settings.termsAccepted = true
                onAccept()
            }
        } else {
            OutlineButton(stringResource(R.string.ack_agree), enabled = false) {}
        }
        OutlineButton(stringResource(R.string.ack_decline)) { context.findActivity()?.finish() }
    }
}

/* ============================ TERMS (read-only) ============================ */

@Composable
fun TermsScreen(onBack: () -> Unit) {
    ScreenFrame(title = stringResource(R.string.terms_title), onBack = onBack) {
        TermsContent()
    }
}

@Composable
private fun TermsContent() {
    AGCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            androidx.compose.material3.Text(
                stringResource(R.string.terms_effective, BuildConfig.VERSION_NAME),
                style = AGText.monoSm, color = AG.colors.textFaint
            )
            androidx.compose.material3.Text(stringResource(R.string.terms_body), style = AGText.body, color = AG.colors.text)
        }
    }
}

/* ============================ ABOUT (version + changelog + terms) ============================ */

private const val RELEASES_URL = "https://github.com/smellouk/autoguard/releases"

/** About content (version + what's new + changelog/terms), embedded inside Settings. */
@Composable
fun AboutSection(onViewTerms: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AGCard(padding = 22.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(AG.colors.greenGradient),
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Icon(Icons.Filled.Shield, null, tint = AG.colors.onGreen, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.size(12.dp))
                androidx.compose.material3.Text("AutoGuard", style = AGText.heroTitle, color = AG.colors.text)
                Spacer(Modifier.size(4.dp))
                androidx.compose.material3.Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = AGText.mono, color = AG.colors.textDim
                )
                Spacer(Modifier.size(8.dp))
                androidx.compose.material3.Text(stringResource(R.string.about_tagline), style = AGText.bodySm, color = AG.colors.textDim)
            }
        }

        AGCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.about_whatsnew_title), Accent.GREEN)
                androidx.compose.material3.Text(stringResource(R.string.about_whatsnew_body), style = AGText.body, color = AG.colors.textDim)
            }
        }

        AGCard(padding = 4.dp) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                NavRow(
                    Icons.Outlined.NewReleases,
                    stringResource(R.string.about_changelog),
                    stringResource(R.string.about_changelog_subtitle),
                    Icons.AutoMirrored.Filled.OpenInNew,
                ) {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(RELEASES_URL))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                HDivider()
                NavRow(
                    Icons.Outlined.Description,
                    stringResource(R.string.terms_title),
                    stringResource(R.string.about_terms_subtitle),
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                ) { onViewTerms() }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
