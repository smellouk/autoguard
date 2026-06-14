import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Signing material is read from (in priority order):
//   1. environment variables  -> used by CI (GitHub Actions)
//   2. keystore.properties     -> used for local release builds (git-ignored)
// If neither is present, the release build is simply left unsigned.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}
fun signing(env: String, prop: String): String? =
    System.getenv(env) ?: keystoreProps.getProperty(prop)

val storeFilePath = signing("KEYSTORE_FILE", "storeFile")
val hasSigning = storeFilePath != null

// Version is derived from the release tag in CI: the workflow passes
// -PversionName=<tag without leading 'v'> (e.g. v1.4.2 -> 1.4.2). versionCode is
// computed from the semver so it always increases monotonically. Local builds
// without the property fall back to a dev version.
val resolvedVersionName: String =
    (project.findProperty("versionName") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "0.0.1-dev"
val resolvedVersionCode: Int =
    (project.findProperty("versionCode") as String?)?.toIntOrNull()
        ?: Regex("""(\d+)\.(\d+)\.(\d+)""").find(resolvedVersionName)?.destructured
            ?.let { (major, minor, patch) -> major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt() }
        ?: 1

android {
    namespace = "com.smellouk.autoguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.smellouk.autoguard"
        minSdk = 26
        targetSdk = 37
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                // rootProject.file() keeps absolute paths (CI) and resolves
                // relative ones (keystore.properties) against the repo root.
                storeFile = rootProject.file(storeFilePath!!)
                storePassword = signing("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signing("KEY_ALIAS", "keyAlias")
                keyPassword = signing("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true // generates BuildConfig.VERSION_NAME for the About screen
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
