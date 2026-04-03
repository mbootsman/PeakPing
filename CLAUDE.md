# PeakPing — Claude Code Guide

## Project Overview

PeakPing is an Android app that displays real-time GPS elevation and accuracy data. Written entirely in Kotlin with Jetpack Compose, licensed under GPLv3.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Location**: Google Play Services Fused Location Provider
- **Build**: Gradle with Kotlin DSL (Gradle 9.3.1)
- **Min SDK**: 33 | **Compile SDK**: 36

## Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/nl/marcel/peakping/MainActivity.kt` | Entry point, permission handling |
| `app/src/main/java/nl/marcel/peakping/ElevationScreen.kt` | Compose UI |
| `app/src/main/java/nl/marcel/peakping/ElevationViewModel.kt` | GPS state management |
| `app/src/main/java/nl/marcel/peakping/GpsState.kt` | Data class for GPS readings |
| `app/build.gradle.kts` | Dependencies and SDK config |
| `app/src/main/AndroidManifest.xml` | Permissions, activity declaration |

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew test                   # Run unit tests
./gradlew lint                   # Run lint checks
```

## Architecture Notes

- GPS updates are requested at **1-second intervals** with **high accuracy** priority via `FusedLocationProviderClient`
- `ElevationViewModel` holds `GpsState` as a `StateFlow` collected by the Compose UI
- Location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) are handled at runtime using the Accompanist permissions library
- Dark theme is enabled by default

## Permissions

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```