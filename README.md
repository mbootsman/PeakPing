# PeakPing

An Android app that displays real-time GPS elevation and accuracy data, with barometric pressure fusion for smooth inter-fix altitude readings.

## Features

- MSL (mean sea level) elevation via GPS, corrected using the EGM96 geoid model
- Barometric pressure fusion — baro sensor calibrated by GPS, provides smooth altitude between GPS fixes
- Horizontal and vertical accuracy, satellite count, and pressure readout
- Metric / imperial unit toggle
- Light / dark / system theme
- Battery-safe: GPS and baro listeners stop on pause, resume on foreground

## Requirements

- Android 13 (API 33) or higher
- Location permission (`ACCESS_FINE_LOCATION`)

## Build

```bash
./gradlew assembleDebug        # build APK
./gradlew installDebug         # build and install on connected device
```

## Scripts

### `scripts/generate_geoid.py`

Generates the binary geoid lookup table bundled with the app as `app/src/main/assets/egm96_1deg.bin`.

**Why it exists:** Android's built-in `mslAltitudeMeters` is only available on API 34+. On older devices the app needs to convert the raw WGS84 ellipsoidal altitude reported by GPS into an MSL altitude itself. It does this by subtracting the local geoid undulation — the height difference between the WGS84 ellipsoid and mean sea level at a given lat/lon.

**What the script does:**
1. Downloads the EGM96 5-arcminute geoid grid from GeographicLib (~10 MB tar.bz2)
2. Parses the PGM raster and resamples it to a 1° grid (181 × 361 points)
3. Encodes each undulation value as a big-endian signed Int16 in centimetres
4. Writes the result to `egm96_1deg.bin` (127 KB)

The generated file is committed to the repo so the script only needs to be re-run if the geoid data needs updating.

**Usage:**

```bash
python3 scripts/generate_geoid.py
git add app/src/main/assets/egm96_1deg.bin
```

Requires Python 3.6+ and no third-party dependencies.

## License

GPLv3 — see [LICENSE](LICENSE).
