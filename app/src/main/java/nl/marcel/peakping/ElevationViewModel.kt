package nl.marcel.peakping

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
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
        _gpsState.value = _gpsState.value.copy(
            elevation = altitude,
            lat = location.latitude,
            lon = location.longitude,
            accuracyM = location.accuracy,
            locked = true
        )
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val usedCount = (0 until status.satelliteCount).count { status.usedInFix(it) }
            _gpsState.value = _gpsState.value.copy(satellites = usedCount)
        }
    }

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
            updatesStarted = true
        } catch (_: Exception) {
            // GPS provider unavailable on this device
        }
    }

    fun ping() {
        // Trigger the pulse animation; the GPS listener keeps position data fresh
        _pulseEvent.tryEmit(Unit)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            locationManager.removeUpdates(locationListener)
            locationManager.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: Exception) {
            // ignore
        }
    }
}
