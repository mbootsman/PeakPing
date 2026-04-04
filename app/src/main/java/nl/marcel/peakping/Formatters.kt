package nl.marcel.peakping

import java.util.Locale

internal fun formatThousands(n: Int): String = String.format(Locale.US, "%,d", n)

internal fun elevationM(m: Double): String = m.toInt().toString()
internal fun elevationFt(m: Double): String = (m * 3.28084).toInt().toString()

internal fun formatLat(lat: Double): String {
    val dir = if (lat >= 0) "N" else "S"
    return "${"%.5f".format(kotlin.math.abs(lat))}° $dir"
}

internal fun formatLon(lon: Double): String {
    val dir = if (lon >= 0) "E" else "W"
    return "${"%.5f".format(kotlin.math.abs(lon))}° $dir"
}

internal fun formatAccuracy(m: Float, unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.METRIC) "${"%.1f".format(m)} m"
    else "${"%.1f".format(m * 3.28084f)} ft"

internal fun formatPressure(hpa: Float, unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.METRIC) "${"%.1f".format(hpa)} hPa"
    else "${"%.2f".format(hpa * 0.02953f)} inHg"
