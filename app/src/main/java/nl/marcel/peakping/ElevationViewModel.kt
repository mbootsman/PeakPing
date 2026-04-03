package nl.marcel.peakping

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ElevationViewModel(application: Application) : AndroidViewModel(application) {

    private val _gpsState = MutableStateFlow(GpsState.Empty)
    val gpsState: StateFlow<GpsState> = _gpsState.asStateFlow()

    private val _pulseEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pulseEvent: SharedFlow<Unit> = _pulseEvent.asSharedFlow()

    private val prefs = application.getSharedPreferences("peakping_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _unitSystem = MutableStateFlow(
        UnitSystem.valueOf(prefs.getString("unit_system", UnitSystem.METRIC.name) ?: UnitSystem.METRIC.name)
    )
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    fun setUnitSystem(system: UnitSystem) {
        _unitSystem.value = system
        prefs.edit().putString("unit_system", system.name).apply()
    }

    init {
        GeoidModel.init(application)
    }

    // ── Location ──────────────────────────────────────────────────────────────

    private val locationManager =
        application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // GPS_PROVIDER delivers fixes from GPS hardware only — no WiFi/cell blending
    private val locationListener = LocationListener { location: Location ->
        // API 34+: use the OS's built-in EGM96 MSL altitude.
        // Older: subtract the EGM96 geoid undulation from the WGS84 ellipsoidal height.
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
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val usedCount = (0 until status.satelliteCount).count { status.usedInFix(it) }
            _gpsState.value = _gpsState.value.copy(satellites = usedCount)
        }
    }

    // ── Barometer ─────────────────────────────────────────────────────────────

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    /** Raw baro altitude relative to standard atmosphere (1013.25 hPa). Null until first reading. */
    private var baroAltStd: Double? = null

    /** Offset = GPS MSL alt − standard baro alt; calibrated each GPS fix. Null until first GPS fix. */
    private var baroOffset: Double? = null

    private val baroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val pressureHpa = event.values[0]
            val rawAlt = SensorManager.getAltitude(
                SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureHpa
            ).toDouble()
            baroAltStd = rawAlt

            val offset = baroOffset
            val state  = _gpsState.value
            if (offset != null && state.locked) {
                // GPS has calibrated us — use baro for smooth inter-fix altitude
                _gpsState.value = state.copy(
                    elevation  = rawAlt + offset,
                    pressureHpa = pressureHpa,
                    baroFused  = true,
                )
            } else {
                // No GPS calibration yet — just surface the pressure reading
                _gpsState.value = state.copy(pressureHpa = pressureHpa)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private var updatesStarted = false

    fun startUpdates() {
        if (updatesStarted) return
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
            locationManager.registerGnssStatusCallback(
                gnssCallback,
                Handler(Looper.getMainLooper())
            )
        } catch (_: Exception) {
            // GPS provider unavailable on this device
        }

        pressureSensor?.let {
            sensorManager.registerListener(
                baroListener,
                it,
                SensorManager.SENSOR_DELAY_UI,
                Handler(Looper.getMainLooper())
            )
        }

        updatesStarted = true
    }

    fun stopUpdates() {
        if (!updatesStarted) return
        try {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: Exception) {}
        try {
            sensorManager.unregisterListener(baroListener)
        } catch (_: Exception) {}
        updatesStarted = false
    }

    fun ping() {
        // Trigger the pulse animation; GPS + baro listeners keep data fresh
        _pulseEvent.tryEmit(Unit)
    }

    override fun onCleared() {
        super.onCleared()
        stopUpdates()
    }
}
