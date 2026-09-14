package nl.marcel.peakping

import android.app.Activity
import android.os.Bundle

class OverlayShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OverlayController.toggle(this)
        finish()
    }
}
