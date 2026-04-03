package nl.marcel.peakping

data class GpsState(
    val elevation: Double,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val verticalAccuracyM: Float,
    val satellites: Int,
    val locked: Boolean,
    val pressureHpa: Float,    // 0f = barometer not available
    val baroFused: Boolean,    // true = elevation is GPS-calibrated baro
    val locationName: String,  // reverse-geocoded "City, Country" or ""
) {
    companion object {
        val Empty = GpsState(
            elevation = 0.0,
            lat = 0.0,
            lon = 0.0,
            accuracyM = 0f,
            verticalAccuracyM = 0f,
            satellites = 0,
            locked = false,
            pressureHpa = 0f,
            baroFused = false,
            locationName = "",
        )
    }
}