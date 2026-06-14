plugins {
    // AGP 9+ has built-in Kotlin, so the kotlin-android plugin is no longer applied.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
