package nl.marcel.peakping

data class GpsState(
    val elevation: Double,
    val lat: Double,
    val lon: Double,
    val accuracyM: Float,
    val satellites: Int,
    val locked: Boolean
) {
    companion object {
        val Empty = GpsState(
            elevation = 0.0,
            lat = 0.0,
            lon = 0.0,
            accuracyM = 0f,
            satellites = 0,
            locked = false
        )
    }
}