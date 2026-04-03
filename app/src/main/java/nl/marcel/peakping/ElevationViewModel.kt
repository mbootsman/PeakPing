package nl.marcel.peakping

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

    private val locationManager =
        application.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

    private val locationListener = LocationListener { location: Location ->
        _gpsState.value = _gpsState.value.copy(
            elevation = location.altitude,
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
        _pulseEvent.tryEmit(Unit)

        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                _gpsState.value = _gpsState.value.copy(
                    elevation = location.altitude,
                    lat = location.latitude,
                    lon = location.longitude,
                    accuracyM = location.accuracy,
                    locked = true
                )
            }
        } catch (_: Exception) {
            // ignore
        }
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