package nl.marcel.peakping

import org.json.JSONArray
import org.json.JSONObject

data class SavedPin(
    val id: Long,
    val label: String,
    val lat: Double,
    val lon: Double,
    val elevationM: Double,
    val locationName: String,
    val savedAt: Long,
)

internal fun List<SavedPin>.toJson(): String {
    val array = JSONArray()
    forEach { pin ->
        array.put(JSONObject().apply {
            put("id", pin.id)
            put("label", pin.label)
            put("lat", pin.lat)
            put("lon", pin.lon)
            put("elevationM", pin.elevationM)
            put("locationName", pin.locationName)
            put("savedAt", pin.savedAt)
        })
    }
    return array.toString()
}

internal fun pinsFromJson(json: String): List<SavedPin> = try {
    val array = JSONArray(json)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        SavedPin(
            id           = o.getLong("id"),
            label        = o.getString("label"),
            lat          = o.getDouble("lat"),
            lon          = o.getDouble("lon"),
            elevationM   = o.getDouble("elevationM"),
            locationName = o.getString("locationName"),
            savedAt      = o.getLong("savedAt"),
        )
    }
} catch (_: Exception) { emptyList() }
