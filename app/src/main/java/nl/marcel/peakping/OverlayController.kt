package nl.marcel.peakping

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlayController {

    fun requestPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    fun toggle(context: Context) {
        when {
            FloatingWindowService.isRunning -> FloatingWindowService.stop(context)
            Settings.canDrawOverlays(context) -> FloatingWindowService.start(context)
            else -> context.startActivity(requestPermissionIntent(context))
        }
    }
}
