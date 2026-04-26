package nl.marcel.peakping

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

private const val CARD_W = 1080
private const val CARD_H = 860
private const val MAP_H  = 400
private const val TILE_PX = 256
private const val GRID    = 3
private const val MAP_ZOOM = 15

fun osmUrl(lat: Double, lon: Double) =
    String.format(
        java.util.Locale.US,
        "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=15/%.6f/%.6f",
        lat, lon, lat, lon
    )

suspend fun buildShareBitmap(
    context: Context,
    state: GpsState,
    isDark: Boolean,
    unitSystem: UnitSystem,
): Bitmap = withContext(Dispatchers.IO) {

    // ── Tile math ─────────────────────────────────────────────────────────────
    val (tx, ty) = latLonToTile(state.lat, state.lon, MAP_ZOOM)
    val (offsetX, offsetY) = latLonToPixelOffset(state.lat, state.lon, MAP_ZOOM, tx, ty)
    val half = GRID / 2

    // ── Fetch tiles in parallel, then stitch sequentially ─────────────────────
    val stitchedW = GRID * TILE_PX
    val stitchedH = GRID * TILE_PX
    val stitched = Bitmap.createBitmap(stitchedW, stitchedH, Bitmap.Config.ARGB_8888)
    val stitchCanvas = Canvas(stitched)
    stitchCanvas.drawColor(0xFF1A2A3A.toInt())

    val tileResults = coroutineScope {
        (0 until GRID).flatMap { col ->
            (0 until GRID).map { row ->
                async { (col to row) to fetchTile(MAP_ZOOM, tx - half + col, ty - half + row) }
            }
        }.map { it.await() }
    }
    for ((pos, tile) in tileResults) {
        tile?.let {
            stitchCanvas.drawBitmap(it, (pos.first * TILE_PX).toFloat(), (pos.second * TILE_PX).toFloat(), null)
            it.recycle()
        }
    }

    // ── Build card ────────────────────────────────────────────────────────────
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor     = if (isDark) 0xFF080E14.toInt() else 0xFFF0F6FF.toInt()
    val textColor   = if (isDark) Color.WHITE        else 0xFF0C447C.toInt()
    val dimColor    = if (isDark) Color.argb(140, 255, 255, 255) else Color.argb(140, 12, 68, 124)
    val accentColor = 0xFF00AAB3.toInt()

    canvas.drawColor(bgColor)

    // ── Map section ───────────────────────────────────────────────────────────
    val s = CARD_W.toFloat() / stitchedW.toFloat()
    val userPixelX = (half * TILE_PX + offsetX).toFloat()
    val userPixelY = (half * TILE_PX + offsetY).toFloat()
    // bx=0: scale fills width exactly so there's never a gap on either side.
    // by centers the user vertically in the map strip.
    val bx = 0f
    val by = ((MAP_H / 2f) / s - userPixelY).coerceIn(MAP_H / s - stitchedH.toFloat(), 0f)

    val mapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    canvas.save()
    canvas.clipRect(0f, 0f, CARD_W.toFloat(), MAP_H.toFloat())
    canvas.scale(s, s)
    canvas.drawBitmap(stitched, bx, by, mapPaint)
    canvas.restore()
    stitched.recycle()

    // Subtle dark overlay in dark mode so map doesn't clash
    if (isDark) {
        val overlay = Paint().apply { color = Color.argb(55, 8, 14, 20) }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), MAP_H.toFloat(), overlay)
    }

    // Marker at the user's actual position in the rendered tile grid
    val mx = (bx + userPixelX) * s
    val my = (by + userPixelY) * s
    val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    markerPaint.color = Color.argb(70, 0, 170, 179)
    canvas.drawCircle(mx, my, 36f, markerPaint)
    markerPaint.color = accentColor
    canvas.drawCircle(mx, my, 16f, markerPaint)
    markerPaint.color = Color.WHITE
    canvas.drawCircle(mx, my, 7f, markerPaint)

    // ── Accent divider ────────────────────────────────────────────────────────
    val divPaint = Paint().apply {
        color = accentColor; alpha = 55; strokeWidth = 1.8f
    }
    canvas.drawLine(36f, MAP_H.toFloat(), (CARD_W - 36f), MAP_H.toFloat(), divPaint)

    // ── Data section ──────────────────────────────────────────────────────────
    val elevVal  = if (unitSystem == UnitSystem.IMPERIAL) state.elevation * 3.28084 else state.elevation
    val elevUnit = if (unitSystem == UnitSystem.IMPERIAL) "ft" else "m"
    val elevStr  = "%.0f %s".format(elevVal, elevUnit)

    // "ELEVATION" label
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor; alpha = 170
        textSize = 26f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
    }
    canvas.drawText("ELEVATION", CARD_W / 2f, MAP_H + 48f, labelPaint)

    // Large elevation value
    val elevPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 148f
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(elevStr, CARD_W / 2f, MAP_H + 195f, elevPaint)

    // Location name
    if (state.locationName.isNotEmpty()) {
        val locPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; alpha = 200
            textSize = 34f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(state.locationName, CARD_W / 2f, MAP_H + 280f, locPaint)
    }

    // Thin divider before coordinates
    canvas.drawLine(72f, (MAP_H + 320f), (CARD_W - 72f), (MAP_H + 320f), divPaint)

    // Coordinates
    val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dimColor
        textSize = 28f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("${formatLat(state.lat)}   ${formatLon(state.lon)}", CARD_W / 2f, MAP_H + 360f, coordPaint)

    // Accuracy + satellites
    val accStr = "±${state.accuracyM.toInt()} m  ·  ${state.satellites} satellites"
    canvas.drawText(accStr, CARD_W / 2f, MAP_H + 400f, coordPaint)

    // ── Footer ────────────────────────────────────────────────────────────────
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDark) Color.argb(55, 255, 255, 255) else Color.argb(75, 12, 68, 124)
        textSize = 22f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("PeakPing  ·  © OpenStreetMap contributors", CARD_W / 2f, CARD_H - 24f, footerPaint)

    bitmap
}

suspend fun shareLocation(
    context: Context,
    state: GpsState,
    isDark: Boolean,
    unitSystem: UnitSystem,
) {
    val bitmap = buildShareBitmap(context, state, isDark, unitSystem)

    val file = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "share").also { it.mkdirs() }
        File(cacheDir, "peakping_share.png").also { f ->
            f.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }
    }
    bitmap.recycle()

    val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val osmLink  = osmUrl(state.lat, state.lon)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TEXT, osmLink)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    withContext(Dispatchers.Main) {
        context.startActivity(Intent.createChooser(intent, null))
    }
}

private suspend fun fetchTile(zoom: Int, x: Int, y: Int): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val conn = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "PeakPing/1.0 Android")
        conn.connectTimeout = 6000
        conn.readTimeout    = 6000
        conn.connect()
        val bmp = BitmapFactory.decodeStream(conn.inputStream)
        conn.disconnect()
        bmp
    } catch (_: Exception) {
        null
    }
}

private fun latLonToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
    val n = 1 shl zoom
    val x = ((lon + 180.0) / 360.0 * n).toInt()
    val latRad = Math.toRadians(lat)
    val y = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
    return x to y
}

private fun latLonToPixelOffset(lat: Double, lon: Double, zoom: Int, tx: Int, ty: Int): Pair<Int, Int> {
    val n = 1 shl zoom
    val worldPx = (lon + 180.0) / 360.0 * n * TILE_PX
    val latRad  = Math.toRadians(lat)
    val worldPy = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n * TILE_PX
    val ox = (worldPx - tx * TILE_PX).toInt().coerceIn(0, TILE_PX - 1)
    val oy = (worldPy - ty * TILE_PX).toInt().coerceIn(0, TILE_PX - 1)
    return ox to oy
}
