package com.smellouk.autoguard.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.smellouk.autoguard.R

/*
 * Static per-weight font files (one resource per weight). This avoids the
 * variable-font pitfall where Android caches the resource by id and every weight
 * collapses to the file's default instance — which made all text render light.
 */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)
val Hanken = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
)
val Mono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

/**
 * Text styles straight from the handoff type table. Colour is applied by the
 * caller (component decides which token), so these carry family/size/weight only.
 */
object AGText {
    val screenTitle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em, lineHeight = 24.sp)
    val wordmark = TextStyle(fontFamily = SpaceGrotesk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    val heroTitle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val cardTitle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
    val rowTitle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
    val button = TextStyle(fontFamily = SpaceGrotesk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val segment = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

    // Body-style list/row titles (behaviour toggles, permission rows) — Hanken, not Space Grotesk.
    val listTitle = TextStyle(fontFamily = Hanken, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    val body = TextStyle(fontFamily = Hanken, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val bodySm = TextStyle(fontFamily = Hanken, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val subtitle = TextStyle(fontFamily = Hanken, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)

    val mono = TextStyle(fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val monoSm = TextStyle(fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val monoLg = TextStyle(fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val timestamp = TextStyle(fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val eyebrow = TextStyle(fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.em)
    val badge = TextStyle(fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em)
}
