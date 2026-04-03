package nl.marcel.peakping

import android.Manifest
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.Canvas

// ── Formatting helpers ────────────────────────────────────────────────────────

private fun formatThousands(n: Int): String = String.format(Locale.US, "%,d", n)

private fun elevationM(m: Double): String = formatThousands(m.toInt())
private fun elevationFt(m: Double): String = formatThousands((m * 3.28084).toInt())

private fun formatLat(lat: Double): String {
    val dir = if (lat >= 0) "N" else "S"
    return "${"%.5f".format(kotlin.math.abs(lat))}° $dir"
}

private fun formatLon(lon: Double): String {
    val dir = if (lon >= 0) "E" else "W"
    return "${"%.5f".format(kotlin.math.abs(lon))}° $dir"
}

private fun formatAccuracy(m: Float, unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.METRIC) "${"%.1f".format(m)} m"
    else "${"%.1f".format(m * 3.28084f)} ft"

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ElevationRing(gpsState: GpsState, acquiringAlpha: Float, colors: AppColors, unitSystem: UnitSystem) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            drawCircle(
                color = AccentGreen,
                radius = maxR,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = AccentGreen.copy(alpha = 0.45f),
                radius = maxR * 0.82f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = AccentGreen.copy(alpha = 0.25f),
                radius = maxR * 0.65f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ELEVATION",
                fontSize = 9.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = colors.dimAccent
            )
            Spacer(modifier = Modifier.height(2.dp))

            if (gpsState.locked) {
                val primary  = if (unitSystem == UnitSystem.METRIC) elevationM(gpsState.elevation)
                               else elevationFt(gpsState.elevation)
                val subtitle = if (unitSystem == UnitSystem.METRIC)
                    "${elevationM(gpsState.elevation)} m  ·  ${elevationFt(gpsState.elevation)} ft"
                else
                    "${elevationFt(gpsState.elevation)} ft  ·  ${elevationM(gpsState.elevation)} m"

                Text(
                    text = primary,
                    fontSize = 52.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    color = colors.text,
                    lineHeight = 54.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = colors.dimText,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "ACQUIRING…",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AccentGreen,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(acquiringAlpha)
                )
            }
        }
    }
}

@Composable
private fun SignalBarsRow(satellites: Int, colors: AppColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 5 bars of increasing height
        val filledBars = minOf(satellites, 5)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height((8 + i * 5).dp)
                        .background(
                            color = if (i <= filledBars) AccentGreen else AccentGreen.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$satellites SATELLITES",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = colors.dimText
        )
    }
}

@Composable
private fun CoordRow(label: String, value: String, colors: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = colors.dimAccent
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = colors.text
        )
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ElevationScreen(viewModel: ElevationViewModel) {
    val gpsState by viewModel.gpsState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
    }
    val colors = if (isDark) DarkColors else LightColors

    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            onThemeChange = { viewModel.setThemeMode(it) },
            unitSystem = unitSystem,
            onUnitSystemChange = { viewModel.setUnitSystem(it) },
            colors = colors,
            onBack = { showSettings = false }
        )
        return
    }

    // Permission
    val fineLocation = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) {
        if (!fineLocation.status.isGranted) fineLocation.launchPermissionRequest()
    }
    LaunchedEffect(fineLocation.status.isGranted) {
        if (fineLocation.status.isGranted) viewModel.startUpdates()
    }

    // Pulse animation (one-shot on ping)
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        viewModel.pulseEvent.collect {
            pulseScale.snapTo(0.5f)
            pulseAlpha.snapTo(0.75f)
            launch {
                pulseScale.animateTo(2.6f, tween(700, easing = FastOutSlowInEasing))
            }
            pulseAlpha.animateTo(0f, tween(700))
        }
    }

    // Acquiring blink animation
    val infiniteTransition = rememberInfiniteTransition(label = "acquiring")
    val acquiringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "acquiringAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PeakPing",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = "ELEVATION VIA GPS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = colors.dimAccent
                    )
                }
                IconButton(onClick = {
                    val next = when (themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.DARK
                        ThemeMode.DARK   -> ThemeMode.LIGHT
                        ThemeMode.LIGHT  -> ThemeMode.SYSTEM
                    }
                    viewModel.setThemeMode(next)
                }) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            ThemeMode.DARK   -> Icons.Default.DarkMode
                            ThemeMode.LIGHT  -> Icons.Default.LightMode
                        },
                        contentDescription = "Cycle theme",
                        tint = colors.dimText
                    )
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colors.dimText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Elevation ring ────────────────────────────────────────────────
            ElevationRing(gpsState = gpsState, acquiringAlpha = acquiringAlpha, colors = colors, unitSystem = unitSystem)

            Spacer(modifier = Modifier.height(20.dp))

            // ── Signal bars ───────────────────────────────────────────────────
            SignalBarsRow(satellites = gpsState.satellites, colors = colors)

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = AccentGreen.copy(alpha = 0.18f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Coordinates panel ─────────────────────────────────────────────
            CoordRow(
                label = "LAT",
                value = if (gpsState.locked) formatLat(gpsState.lat) else "---",
                colors = colors
            )
            CoordRow(
                label = "LON",
                value = if (gpsState.locked) formatLon(gpsState.lon) else "---",
                colors = colors
            )
            CoordRow(
                label = "ACCURACY",
                value = if (gpsState.locked) formatAccuracy(gpsState.accuracyM, unitSystem) else "---",
                colors = colors
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom action bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Save",
                        tint = colors.dimText
                    )
                }

                // Ping FAB with pulse ring
                Box(contentAlignment = Alignment.Center) {
                    // Expanding pulse ring
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val r = 32.dp.toPx() * pulseScale.value
                        drawCircle(
                            color = AccentGreen,
                            radius = r,
                            alpha = pulseAlpha.value
                        )
                    }
                    // FAB button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                            .clickable { viewModel.ping() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Ping",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = colors.dimText
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Home bar pill ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .background(
                        color = colors.dimText.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}