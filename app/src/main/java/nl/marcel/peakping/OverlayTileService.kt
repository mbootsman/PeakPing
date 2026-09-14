package nl.marcel.peakping

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class OverlayTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        if (FloatingWindowService.isRunning) {
            FloatingWindowService.stop(this)
            refreshTileState()
        } else {
            // Starting a foreground-service-location service requires a foreground
            // (Activity) caller; TileService.onClick() alone isn't eligible, so hand
            // off to the same trampoline the app shortcut uses.
            launchTrampoline()
        }
    }

    private fun launchTrampoline() {
        val intent = Intent(this, OverlayShortcutActivity::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refreshTileState() {
        qsTile?.let {
            it.state = if (FloatingWindowService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.updateTile()
        }
    }
}
