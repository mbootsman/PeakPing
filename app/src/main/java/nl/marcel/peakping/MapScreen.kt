package nl.marcel.peakping

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow

@Composable
fun MapScreen(
    gpsState: GpsState,
    savedPins: List<SavedPin>,
    unitSystem: UnitSystem,
    colors: AppColors,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled   = false
            minZoomLevel = 3.0
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(gpsState.lat, gpsState.lon))
        }
    }

    fun refreshOverlays() {
        mapView.overlays.clear()

        savedPins.forEach { pin ->
            val elev = if (unitSystem == UnitSystem.METRIC)
                "${elevationM(pin.elevationM)} m"
            else
                "${elevationFt(pin.elevationM)} ft"
            val marker = Marker(mapView).apply {
                position  = GeoPoint(pin.lat, pin.lon)
                title     = pin.label
                snippet   = "$elev · ${pin.locationName}".trimEnd(' ', '·', ' ')
                icon      = PinDrawable(context, accentArgb = 0xFF00AAB3.toInt()) // summit
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                infoWindow = BasicInfoWindow(
                    org.osmdroid.library.R.layout.bonuspack_bubble, mapView
                )
            }
            mapView.overlays.add(marker)
        }

        val posMarker = Marker(mapView).apply {
            position  = GeoPoint(gpsState.lat, gpsState.lon)
            title     = "You are here"
            snippet   = if (unitSystem == UnitSystem.METRIC)
                "${elevationM(gpsState.elevation)} m"
            else
                "${elevationFt(gpsState.elevation)} ft"
            icon      = PinDrawable(context, accentArgb = 0xFF185FA5.toInt(), isPosition = true) // ocean
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = BasicInfoWindow(
                org.osmdroid.library.R.layout.bonuspack_bubble, mapView
            )
        }
        mapView.overlays.add(posMarker)

        mapView.invalidate()
    }

    refreshOverlays()

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // ── Bottom-left controls: zoom in / zoom out / back ───────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapControlButton(onClick = { mapView.controller.zoomIn() }, colors = colors) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = colors.text)
            }
            Spacer(modifier = Modifier.height(12.dp))
            MapControlButton(onClick = { mapView.controller.zoomOut() }, colors = colors) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = colors.text)
            }
            Spacer(modifier = Modifier.height(12.dp))
            MapControlButton(onClick = onBack, colors = colors) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
        }
    }
}

@Composable
private fun MapControlButton(onClick: () -> Unit, colors: AppColors, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.bg.copy(alpha = 0.85f))
    ) {
        content()
    }
}

/** Simple filled-circle drawable for map markers. */
private class PinDrawable(
    context: Context,
    private val accentArgb: Int,
    private val isPosition: Boolean = false,
) : android.graphics.drawable.Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentArgb
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val r  = minOf(bounds.width(), bounds.height()) / 2f - 4f
        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, strokePaint)
        if (!isPosition) {
            canvas.drawCircle(cx, cy, r * 0.35f, innerPaint)
        }
    }

    override fun setAlpha(alpha: Int) { fillPaint.alpha = alpha }
    override fun setColorFilter(cf: android.graphics.ColorFilter?) { fillPaint.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    override fun getIntrinsicWidth()  = 64
    override fun getIntrinsicHeight() = 64
}
