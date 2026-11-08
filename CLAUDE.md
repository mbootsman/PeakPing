# PeakPing — Claude Code Guide

## Project Overview

PeakPing is an Android app that displays real-time GPS elevation and accuracy data, with map, saved-locations, and floating-overlay features. Written entirely in Kotlin with Jetpack Compose, licensed under GPLv3.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Location**: Android `LocationManager` (`GPS_PROVIDER`, GPS hardware only — no Play Services dependency)
- **Build**: Gradle with Kotlin DSL (Gradle 9.3.1)
- **Min SDK**: 33 | **Compile SDK**: 36 | **Target SDK**: 36
- **Current version**: 1.7 (versionCode 8)

## Key Files

| File | Purpose |
|------|---------|
| `app/src/main/java/nl/marcel/peakping/MainActivity.kt` | Entry point, permission handling, navigation between screens |
| `app/src/main/java/nl/marcel/peakping/ElevationScreen.kt` | Main Compose UI — live elevation/accuracy display |
| `app/src/main/java/nl/marcel/peakping/LocationRepository.kt` | Process-wide singleton owning GPS, GNSS satellites, barometer fusion, reverse geocoding and update-interval throttling; shared by the UI and the overlay |
| `app/src/main/java/nl/marcel/peakping/ElevationViewModel.kt` | UI state: settings, saved pins, pulse; exposes `LocationRepository` state to Compose |
| `app/src/main/java/nl/marcel/peakping/GpsState.kt` | Data class for GPS readings |
| `app/src/main/java/nl/marcel/peakping/GeoidModel.kt` | EGM96 geoid undulation lookup — converts WGS84 ellipsoidal altitude to mean-sea-level height |
| `app/src/main/java/nl/marcel/peakping/MapScreen.kt` | Map view of current position/track |
| `app/src/main/java/nl/marcel/peakping/SavedLocationsScreen.kt` | List/manage saved pins, JSON export/import via share sheet and file picker |
| `app/src/main/java/nl/marcel/peakping/SavedPin.kt` | Data class for a saved location pin |
| `app/src/main/java/nl/marcel/peakping/SettingsScreen.kt` | App settings, incl. floating-window toggle |
| `app/src/main/java/nl/marcel/peakping/FloatingWindowService.kt` | Foreground service: draggable system-overlay window showing live altitude over other apps |
| `app/src/main/java/nl/marcel/peakping/OverlayController.kt` | Shared start/stop/permission-request logic for the floating overlay, reused by Compose UI, the QS tile, and the shortcut |
| `app/src/main/java/nl/marcel/peakping/OverlayTileService.kt` | Quick Settings tile to toggle the floating overlay without opening the app |
| `app/src/main/java/nl/marcel/peakping/OverlayShortcutActivity.kt` | No-UI trampoline activity backing the "Overlay" app shortcut (long-press launcher icon) |
| `app/src/main/java/nl/marcel/peakping/ShareUtils.kt` | Renders and shares elevation snapshots as images |
| `app/src/main/java/nl/marcel/peakping/Formatters.kt` | Number/coordinate formatting helpers |
| `app/src/main/java/nl/marcel/peakping/Theme.kt`, `ui/theme/*.kt` | Compose theming (Dark / Light / System modes) |
| `app/build.gradle.kts` | Dependencies and SDK config |
| `app/src/main/AndroidManifest.xml` | Permissions, activity/service declarations |

## Build & Run

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew test                   # Run unit tests
./gradlew lint                   # Run lint checks
```

Debug builds use `applicationIdSuffix = ".dev"` (package `nl.marcel.peakping.dev`, label "PeakPing Dev", version `x.y-dev`) so they install side-by-side with the Play Store version, which is signed with Google's Play App Signing key and can't be replaced by a locally signed APK. Debug-only overrides live in `app/src/debug/res/` (app name, tile/shortcut labels, and a copy of `xml/shortcuts.xml` whose `targetPackage` must stay in sync with the main one).

## Architecture Notes

- GPS updates are requested from `LocationManager.GPS_PROVIDER` at the user-selected **update interval** (Settings > Update frequency: 1/2/5/10 s, `UpdateInterval` enum, persisted as `update_interval`); the barometer is sampled continuously but barometer- and satellite-driven UI updates are throttled to the same interval. Because the overlay reads the same `LocationRepository` state, interval changes apply to a running overlay immediately
- `LocationRepository` is the single source of truth for live data: it holds `GpsState` as a `StateFlow`, collected by the Compose UI (via `ElevationViewModel`) and by `FloatingWindowService`, so both always show the same value. Sensors are reference-counted via `acquire()`/`release()` — they run while the main screen is resumed **or** the overlay is active, and stop when neither needs them
- Location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) are handled at runtime using the Accompanist permissions library
- Raw GPS altitude is WGS84 ellipsoidal; on Android 14+ (`UPSIDE_DOWN_CAKE`) native `Location.mslAltitudeMeters` is used when available, otherwise `GeoidModel` (EGM96, `egm96_1deg.bin`) converts ellipsoidal altitude to mean-sea-level height
- A barometer (`Sensor.TYPE_PRESSURE`) is calibrated against each GPS-derived MSL altitude fix and used to interpolate altitude between fixes
- `GnssStatus.Callback` tracks live satellite count (used-in-fix vs. total)
- The Android system `Geocoder` performs reverse geocoding to show locality/country names, throttled to at most one lookup per 30 s (`GEOCODE_INTERVAL_MS`); results are posted to the main thread. No fallback is used — on devices without a geocoder backend (e.g. de-Googled ROMs) the place name simply stays blank
- `FloatingWindowService` is a foreground service (`foregroundServiceType="location"`) that draws a draggable overlay via `SYSTEM_ALERT_WINDOW`; toggled from Settings > Display and requires the "draw over other apps" permission
- The overlay can also be toggled without opening the app via a Quick Settings tile (`OverlayTileService`) or a static app shortcut (`OverlayShortcutActivity`, declared in `res/xml/shortcuts.xml`); both route start/stop/permission-request logic through `OverlayController` so there's one source of truth
- `FloatingWindowService` is `foregroundServiceType="location"`, which Android only allows starting from a foreground/Activity caller — `OverlayTileService.onClick()` cannot start it directly and must launch `OverlayShortcutActivity` (via `startActivityAndCollapse`) to do so; stopping has no such restriction and is called directly from the tile
- Saved locations export/import uses a `FileProvider` (`app/src/main/res/xml/file_paths.xml`) to share/read `peakping_locations.json`
- Settings support Metric/Imperial units, Dark/Light/System theme, and a bottom-bar icon-label toggle, all persisted via `SharedPreferences`

## Changelog

After every `git commit`, update `CHANGELOG.md` at the project root to reflect the changes in that commit. Group entries under the appropriate version heading (or add a new `[Unreleased]` section if no version has been tagged yet). Use the same section structure already in the file: Added / Changed / Fixed / Removed.

## Keeping this file current

This file must stay accurate as the codebase evolves. Whenever a change adds/removes/renames a source file, changes SDK versions, adds a permission, or alters an architectural pattern described above, update the relevant section of this file in the same commit — don't let it drift out of sync with the code.

## Permissions

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```