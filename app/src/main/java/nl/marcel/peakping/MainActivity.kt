package nl.marcel.peakping

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.marcel.peakping.ui.theme.PeakPingTheme

class MainActivity : ComponentActivity() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            PeakPingTheme(darkTheme = true, dynamicColor = false) {
                val elevationViewModel: ElevationViewModel = viewModel()
                ElevationScreen(viewModel = elevationViewModel)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(
        onUpdate: (altitudeMeters: Double, accuracyMeters: Float) -> Unit,
        onStatus: (String) -> Unit
    ) {
        try {
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation
                    if (loc == null) {
                        onStatus("no location yet")
                        return
                    }
                    onUpdate(loc.altitude, loc.accuracy)
                    onStatus("receiving updates")
                }
            }

            locationCallback = callback
            onStatus("requesting updates")

            // Start continuous updates
            fusedClient.requestLocationUpdates(request, callback, mainLooper)

            // Also force a single "now" location (helps on emulators)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) onUpdate(loc.altitude, loc.accuracy)
                }
                .addOnFailureListener { e ->
                    onStatus("getCurrentLocation failed: ${e.message}")
                }

        } catch (e: Exception) {
            onStatus("error: ${e::class.simpleName}: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }
}