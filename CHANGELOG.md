# Changelog

All notable changes to PeakPing are documented in this file.

## [1.4] — 2026-05-13

### Added
- GPS update frequency setting: new slider in Settings › GPS Update Frequency lets users choose 1 s / 2 s / 5 s / 10 s update intervals; selection is persisted and takes effect immediately without restarting the app
- Landscape layout: two-column view with the elevation ring on the left and scrollable detail rows + action bar on the right; all details fit on-screen in compact mode
- Bookmark name length limit: 40-character cap enforced while typing, with a live `n / 40` character counter in the save dialog that turns red at the limit

### Changed
- Save (BookmarkAdd) FAB is now a floating button: bottom-right in portrait (above the action bar), bottom-left in landscape
- Settings screen is now scrollable — no content is cut off in landscape or on small screens
- Reduced top-bar top padding by 90 % for a tighter, less padded header
- Slider tick labels in the GPS frequency setting are pixel-aligned to their corresponding tick marks

### Fixed
- Migrated `kotlinOptions { jvmTarget }` to `kotlin { compilerOptions { } }` (deprecated AGP 9.0 DSL)
- Replaced `android { }` block with `configure<ApplicationExtension> { }` to silence AGP 10.0 removal warning
- Explicit `File` type annotation on `keystorePropsFile` to resolve platform-type nullability warning

---

## [1.3] — 2026-05-04

### Added
- Quick-save FAB (BookmarkAdd icon) above the bottom bar divider for one-tap location saving
- Inline GPS-acquiring hint row (spinner + label) between the divider and the action bar, so users know why Map, Share and Save are unavailable while a fix is being obtained

### Changed
- Unified bottom bar button tint: the Saved/Bookmark icon no longer turns green when pins exist, matching the Map and Share icon colour for consistency

---

## [1.2] — 2026-04-27

### Added
- Undo snackbar for saved location deletion: deleting a saved location shows a 4-second undo snackbar instead of deleting immediately; tapping Undo restores the item, and if the snackbar times out the deletion is committed to storage
- Bottom bar icon labels: each button (Saved, Map, Share) now shows a text label below its icon; labels can be toggled off in Settings › Display and the preference is persisted across sessions

### Changed
- Version code bumped to 3

---

## [1.1] — 2026-04-27

### Added
- Share location feature: tapping the Share button (GPS-locked only) generates a 1080×860 image — a 3×3 OSM tile grid centred on the user's location with elevation, location name, coordinates and accuracy overlaid in the app's dark/light colour scheme; the image and an OpenStreetMap deep link are shared via the Android native share sheet
- Version name and code displayed at the bottom of the Settings screen

### Fixed
- Locale bug in OSM URL formatting (now uses `Locale.US` so decimal points are always dots)
- Map-section padding bug (tile grid now always fills the card width)

### Changed
- Updated app icon and signing configuration
- Version code bumped to 2

---

## [1.0] — 2026-04-04

### Added
- OSMDroid map screen: full-screen OpenStreetMap tiles, current position (ocean marker) and saved pins (summit marker), custom zoom and back controls styled to match the app theme
- Map button in bottom bar, enabled only when GPS is locked
- Reworked elevation display: full-width text layout, circles removed when GPS is locked
- Saved locations screen: shows saved pins with elevation and timestamp, save button when GPS is locked, swipe-to-delete
- City/country tagline via reverse geocoding — appears under the PeakPing title as soon as a GPS fix arrives
- Live satellite count shown during acquisition (not only after lock)
- `SavedPin` data class with JSON serialisation to `SharedPreferences`
- Bookmark icon turns accent colour when pins exist; brief "Saved" confirmation shown on the main screen

### Changed
- Removed GPS FAB and pulse animation from bottom bar
- Removed "ELEVATION · GPS / GPS + BARO" subtitle from top bar
- Removed History icon from bottom bar; spacer added to keep FAB centred
- Added divider above bottom bar matching the Details section style

---

## [0.3] — 2026-04-03

### Added
- Unit tests for formatting logic and GPS state
- README with build scripts documentation

### Changed
- Polished UI with brand colours and GPS lifecycle fixes

---

## [0.2] — 2026-04-03

### Added
- Location accuracy display
- Theme switching (dark/light)
- Unit selection (metric/imperial)
- EGM96 geoid altitude correction

---

## [0.1] — 2026-04-03

### Added
- Initial release of PeakPing
- Real-time GPS elevation and accuracy display
- Jetpack Compose UI with Material3
- MVVM architecture (ViewModel + StateFlow)
- Google Play Services Fused Location Provider (1-second updates, high-accuracy priority)
- Runtime location permission handling
- Dark theme enabled by default