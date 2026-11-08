package nl.marcel.peakping

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UpdateInterval(val ms: Long, val label: String) {
    ONE_SEC(1_000L, "1 second"),
    TWO_SEC(2_000L, "2 seconds"),
    FIVE_SEC(5_000L, "5 seconds"),
    TEN_SEC(10_000L, "10 seconds"),
}

/**
 * Process-wide single source of truth for live position/elevation data.
 *
 * Owns the GPS listener, GNSS satellite callback, barometer fusion, reverse
 * geocoding and update-interval throttling. Both the main UI ([ElevationViewModel])
 * and the floating overlay ([FloatingWindowService]) read [gpsState], so they always
 * show the same value at the same moment.
 *
 * Sensors run while at least one client holds it via [acquire]; the last [release]
 * stops them. All calls must be made on the main thread.
 */
@SuppressLint("StaticFieldLeak") // holds the application context only
object LocationRepository {

    private const val GEOCODE_INTERVAL_MS = 30_000L
    private const val PREF_UPDATE_INTERVAL = "update_interval"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var geocoder: Geocoder
    private var pressureSensor: Sensor? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _gpsState = MutableStateFlow(GpsState.Empty)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    private val _updateInterval = MutableStateFlow(UpdateInterval.ONE_SEC)
    val updateInterval: StateFlow<UpdateInterval> = _updateInterval.asStateFlow()

    /** True if the device has a barometer (used to interpolate altitude between GPS fixes). */
    val hasBarometer: Boolean get() = pressureSensor != null

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("peakping_prefs", Context.MODE_PRIVATE)
        locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        geocoder = Geocoder(appContext)
        GeoidModel.init(appContext)
        _updateInterval.value = runCatching {
            UpdateInterval.valueOf(
                prefs.getString(PREF_UPDATE_INTERVAL, UpdateInterval.ONE_SEC.name)
                    ?: UpdateInterval.ONE_SEC.name
            )
        }.getOrDefault(UpdateInterval.ONE_SEC)
    }

    fun setUpdateInterval(interval: UpdateInterval) {
        if (_updateInterval.value == interval) return
        _updateInterval.value = interval
        prefs.edit().putString(PREF_UPDATE_INTERVAL, interval.name).apply()
        if (running) {
            // Re-request GPS at the new rate; throttles below read the value live.
            try {
                locationManager.removeUpdates(locationListener)
                requestGps()
            } catch (_: SecurityException) {}
        }
    }

    // ── Client reference counting ─────────────────────────────────────────────

    private var clients = 0
    private var running = false

    /**
     * Registers a client. Returns false (and registers nothing) if location
     * permission isn't granted, so the caller can retry later.
     */
    fun acquire(): Boolean {
        if (!hasLocationPermission()) return false
        clients++
        if (!running) start()
        return true
    }

    fun release() {
        if (clients == 0) return
        clients--
        if (clients == 0 && running) stop()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun start() {
        try {
            requestGps()
            locationManager.registerGnssStatusCallback(gnssCallback, mainHandler)
        } catch (_: Exception) {
            // GPS provider unavailable on this device / permission revoked
        }
        pressureSensor?.let {
            sensorManager.registerListener(baroListener, it, SensorManager.SENSOR_DELAY_UI, mainHandler)
        }
        running = true
    }

    private fun stop() {
        try {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: Exception) {}
        try {
            sensorManager.unregisterListener(baroListener)
        } catch (_: Exception) {}
        running = false
    }

    @SuppressLint("MissingPermission") // callers check hasLocationPermission()
    private fun requestGps() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            _updateInterval.value.ms,
            0f,
            locationListener,
            Looper.getMainLooper()
        )
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    // GPS_PROVIDER delivers fixes from GPS hardware only — no WiFi/cell blending
    private val locationListener = LocationListener { location: Location ->
        // API 34+: use the OS's built-in EGM96 MSL altitude.
        // Older: subtract the EGM96 geoid undulation from the WGS84 ellipsoidal height.
        if (!location.hasAltitude()) return@LocationListener

        val altitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            location.hasMslAltitude()
        ) {
            location.mslAltitudeMeters
        } else {
            location.altitude - GeoidModel.undulation(location.latitude, location.longitude)
        }

        // Calibrate the baro offset every GPS fix so baro tracks MSL between fixes
        val currentBaro = baroAltStd
        if (currentBaro != null) {
            baroOffset = altitude - currentBaro
        }

        _gpsState.value = _gpsState.value.copy(
            elevation = altitude,
            lat = location.latitude,
            lon = location.longitude,
            accuracyM = location.accuracy,
            verticalAccuracyM = if (location.hasVerticalAccuracy()) location.verticalAccuracyMeters else 0f,
            locked = true,
            baroFused = false,   // GPS just updated — baro will take over next sensor tick
        )
        lastBaroEmitMs = SystemClock.elapsedRealtime()

        updateLocationName(location.latitude, location.longitude)
    }

    // ── Satellites ────────────────────────────────────────────────────────────

    /** Last time (elapsedRealtime) satellite count was published. */
    private var lastSatEmitMs = 0L

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastSatEmitMs < _updateInterval.value.ms) return
            lastSatEmitMs = now
            val usedCount = (0 until status.satelliteCount).count { status.usedInFix(it) }
            _gpsState.value = _gpsState.value.copy(satellites = usedCount)
        }
    }

    // ── Barometer ─────────────────────────────────────────────────────────────

    /** Raw baro altitude relative to standard atmosphere (1013.25 hPa). Null until first reading. */
    private var baroAltStd: Double? = null

    /** Offset = GPS MSL alt − standard baro alt; calibrated each GPS fix. Null until first GPS fix. */
    private var baroOffset: Double? = null

    /**
     * Last time (elapsedRealtime) a baro-derived reading was published.
     * The sensor itself is sampled continuously (so calibration stays fresh), but
     * published updates are throttled to the user-selected update interval.
     */
    private var lastBaroEmitMs = 0L

    private val baroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val pressureHpa = event.values[0]
            val rawAlt = SensorManager.getAltitude(
                SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureHpa
            ).toDouble()
            baroAltStd = rawAlt

            val now = SystemClock.elapsedRealtime()
            if (now - lastBaroEmitMs < _updateInterval.value.ms) return
            lastBaroEmitMs = now

            val offset = baroOffset
            val state = _gpsState.value
            if (offset != null && state.locked) {
                // GPS has calibrated us — use baro for smooth inter-fix altitude
                _gpsState.value = state.copy(
                    elevation = rawAlt + offset,
                    pressureHpa = pressureHpa,
                    baroFused = true,
                )
            } else {
                // No GPS calibration yet — just surface the pressure reading
                _gpsState.value = state.copy(pressureHpa = pressureHpa)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    // ── Reverse geocoding ─────────────────────────────────────────────────────

    /** Last time (elapsedRealtime) a reverse-geocode lookup was started; null = never. */
    private var lastGeocodeMs: Long? = null

    private fun updateLocationName(lat: Double, lon: Double) {
        // Place names change slowly — don't hit the geocoder on every GPS fix.
        val now = SystemClock.elapsedRealtime()
        val last = lastGeocodeMs
        if (last != null && now - last < GEOCODE_INTERVAL_MS) return
        lastGeocodeMs = now

        geocoder.getFromLocation(lat, lon, 1) { addresses ->
            val a = addresses.firstOrNull() ?: return@getFromLocation
            val name = listOfNotNull(a.locality, a.countryName).joinToString(", ")
            if (name.isNotEmpty()) {
                // Callback runs on a binder thread; apply on main so it can't race
                // with (and be overwritten by) GPS/baro state updates.
                mainHandler.post {
                    _gpsState.value = _gpsState.value.copy(locationName = name)
                }
            }
        }
    }
}
