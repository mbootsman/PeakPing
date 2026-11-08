package nl.marcel.peakping

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ElevationViewModel(application: Application) : AndroidViewModel(application) {

    init {
        LocationRepository.init(application)
    }

    /** Live position/elevation, shared with the floating overlay via [LocationRepository]. */
    val gpsState: StateFlow<GpsState> = LocationRepository.gpsState

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

    private val _showLabels = MutableStateFlow(prefs.getBoolean("show_labels", true))
    val showLabels: StateFlow<Boolean> = _showLabels.asStateFlow()

    fun setShowLabels(show: Boolean) {
        _showLabels.value = show
        prefs.edit().putBoolean("show_labels", show).apply()
    }

    private val _floatingWindowEnabled = MutableStateFlow(FloatingWindowService.isRunning)
    val floatingWindowEnabled: StateFlow<Boolean> = _floatingWindowEnabled.asStateFlow()

    fun setFloatingWindowEnabled(enabled: Boolean) {
        _floatingWindowEnabled.value = enabled
    }

    fun syncFloatingWindowState() {
        _floatingWindowEnabled.value = FloatingWindowService.isRunning
    }

    val updateInterval: StateFlow<UpdateInterval> = LocationRepository.updateInterval

    fun setUpdateInterval(interval: UpdateInterval) = LocationRepository.setUpdateInterval(interval)

    val hasBarometer: Boolean get() = LocationRepository.hasBarometer

    // ── Saved pins ────────────────────────────────────────────────────────────

    private val _savedPins = MutableStateFlow(
        pinsFromJson(prefs.getString("saved_pins", "[]") ?: "[]")
    )
    val savedPins: StateFlow<List<SavedPin>> = _savedPins.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    fun saveCurrentLocation(label: String) {
        val state = gpsState.value
        if (!state.locked) return
        val now = System.currentTimeMillis()
        val pin = SavedPin(
            id           = now,
            label        = label.ifBlank { state.locationName.ifEmpty { formatLat(state.lat) } },
            lat          = state.lat,
            lon          = state.lon,
            elevationM   = state.elevation,
            locationName = state.locationName,
            savedAt      = now,
        )
        val updated = _savedPins.value + pin
        _savedPins.value = updated
        prefs.edit().putString("saved_pins", updated.toJson()).apply()
        _saveEvent.tryEmit(Unit)
    }

    fun renamePin(id: Long, newLabel: String) {
        val updated = _savedPins.value.map { if (it.id == id) it.copy(label = newLabel) else it }
        _savedPins.value = updated
        prefs.edit().putString("saved_pins", updated.toJson()).apply()
    }

    fun deletePin(id: Long) {
        val updated = _savedPins.value.filter { it.id != id }
        _savedPins.value = updated
        prefs.edit().putString("saved_pins", updated.toJson()).apply()
    }

    /** Merges imported pins, skipping any whose id already exists. Returns count of newly added pins. */
    fun importPins(imported: List<SavedPin>): Int {
        val existingIds = _savedPins.value.map { it.id }.toSet()
        val newPins = imported.filter { it.id !in existingIds }
        if (newPins.isEmpty()) return 0
        val updated = _savedPins.value + newPins
        _savedPins.value = updated
        prefs.edit().putString("saved_pins", updated.toJson()).apply()
        return newPins.size
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    private var updatesStarted = false

    fun startUpdates() {
        if (updatesStarted) return
        updatesStarted = LocationRepository.acquire()
    }

    fun stopUpdates() {
        if (!updatesStarted) return
        LocationRepository.release()
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
