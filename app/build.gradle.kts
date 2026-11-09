import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

val keystorePropsFile: File = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(keystorePropsFile.inputStream())
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "nl.marcel.peakping"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = keystoreProps.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "nl.marcel.peakping"
        minSdk = 33
        targetSdk = 36
        versionCode = 10
        versionName = "1.8.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Installs side-by-side with the Play/store version (which is signed
            // with Google's key and can't be replaced by a locally signed build).
            // Name/shortcut overrides live in src/debug/res.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        release {
            // Without keystore.properties (e.g. F-Droid's build server, which signs
            // with its own key) this produces an unsigned app-release-unsigned.apk.
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependenciesInfo {
        // The APK's dependency-metadata block is encrypted with a Google key and
        // can't be verified; F-Droid rejects it. Play reads it from the AAB only.
        includeInApk = false
        includeInBundle = true
    }
}

// ── Google Play publishing (Gradle Play Publisher) ───────────────────────────
// Upload a release:  ./gradlew publishReleaseBundle
// Credentials: a Google Play service-account JSON key, located via
//   keystore.properties → playServiceAccountFile=/absolute/or/root-relative/path.json
// or the ANDROID_PUBLISHER_CREDENTIALS environment variable (JSON contents, for CI).
// Never commit the JSON key.
// Release notes ("What's new"): app/src/main/play/release-notes/<locale>/default.txt
play {
    keystoreProps.getProperty("playServiceAccountFile")?.let {
        serviceAccountCredentials.set(rootProject.file(it))
    }
    track.set("internal")              // promote to production from Play Console
    defaultToAppBundles.set(true)      // Play requires AABs
}

// Only use the *Release* tasks: the debug build is "nl.marcel.peakping.dev", which doesn't
// exist on Play, so publishDebug* would fail.

// ── EGM96 geoid asset check ───────────────────────────────────────────────────
// egm96_1deg.bin (127 KB) provides MSL altitude correction on Android < 14.
// Generate it once by running: python3 scripts/generate_geoid.py
// Then commit the file so CI and other developers don't need to regenerate it.
tasks.register("checkEgm96Asset") {
    doLast {
        val f = layout.projectDirectory.file("src/main/assets/egm96_1deg.bin").asFile
        if (!f.exists()) {
            logger.warn("⚠ EGM96 geoid asset missing — altitude will be WGS84 on Android < 14.")
            logger.warn("  Fix: python3 scripts/generate_geoid.py")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

afterEvaluate {
    tasks.named("preBuild") { dependsOn("checkEgm96Asset") }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.osmdroid)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}