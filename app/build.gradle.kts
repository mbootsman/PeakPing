plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "nl.marcel.peakping"
    compileSdk = 36

    defaultConfig {
        applicationId = "nl.marcel.peakping"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

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
    implementation(libs.play.services.location)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}