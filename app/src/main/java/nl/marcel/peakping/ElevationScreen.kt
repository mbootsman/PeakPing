package nl.marcel.peakping

import android.Manifest
import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.rememberUpdatedState

private const val MAX_LABEL_LENGTH = 40

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ElevationRing(gpsState: GpsState, acquiringAlpha: Float, colors: AppColors, unitSystem: UnitSystem) {
    if (gpsState.locked) {
        val unit    = if (unitSystem == UnitSystem.METRIC) "m" else "ft"
        val primary = if (unitSystem == UnitSystem.METRIC) elevationM(gpsState.elevation)
                      else elevationFt(gpsState.elevation)
        val label   = "$primary $unit"

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val targetWidth = maxWidth * 0.75f
            var fontSize by remember { mutableStateOf(180.sp) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ELEVATION",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = colors.dimAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = fontSize,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    modifier = Modifier.width(targetWidth),
                    onTextLayout = { result ->
                        if (result.didOverflowWidth) fontSize = (fontSize.value * 0.875f).sp
                    }
                )
            }
        }
        return
    }

    val pulseTransition = rememberInfiniteTransition(label = "ringPulse")
    val cycle = 2400
    val dim = 0.12f
    val ringAlpha1 by pulseTransition.animateFloat(
        initialValue = dim, targetValue = dim,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = cycle
            dim   at 0
            1.00f at 400
            dim   at 800
            dim   at cycle
        }), label = "ra1"
    )
    val ringAlpha2 by pulseTransition.animateFloat(
        initialValue = dim, targetValue = dim,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = cycle
            dim   at 0
            dim   at 800
            0.70f at 1200
            dim   at 1600
            dim   at cycle
        }), label = "ra2"
    )
    val ringAlpha3 by pulseTransition.animateFloat(
        initialValue = dim, targetValue = dim,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = cycle
            dim   at 0
            dim   at 1600
            0.45f at 2000
            dim   at cycle
        }), label = "ra3"
    )

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
                style = Stroke(width = 2.dp.toPx()),
                alpha = ringAlpha1
            )
            drawCircle(
                color = AccentGreen,
                radius = maxR * 0.82f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
                alpha = ringAlpha2
            )
            drawCircle(
                color = AccentGreen,
                radius = maxR * 0.65f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
                alpha = ringAlpha3
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

@Composable
private fun CoordRow(label: String, value: String, colors: AppColors, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (compact) 3.dp else 7.dp),
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

@Composable
private fun BottomBarButton(
    label: String,
    showLabel: Boolean,
    colors: AppColors,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        content()
        if (showLabel) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = colors.dimText,
            )
        }
    }
}

@Composable
private fun TopBar(
    gpsState: GpsState,
    themeMode: ThemeMode,
    colors: AppColors,
    onThemeCycle: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
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
            if (gpsState.locationName.isNotEmpty()) {
                Text(
                    text = gpsState.locationName,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = colors.dimText
                )
            }
        }
        IconButton(onClick = onThemeCycle) {
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
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colors.dimText
            )
        }
    }
}

@Composable
private fun DetailSection(gpsState: GpsState, unitSystem: UnitSystem, colors: AppColors, compact: Boolean = false) {
    Text(
        text = "Details",
        fontSize = 16.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        color = colors.text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = if (compact) 4.dp else 12.dp, bottom = if (compact) 2.dp else 4.dp)
    )
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = AccentGreen.copy(alpha = 0.18f)
    )
    CoordRow(label = "LATITUDE",            value = if (gpsState.locked) formatLat(gpsState.lat) else "---", colors = colors, compact = compact)
    CoordRow(label = "LONGITUDE",           value = if (gpsState.locked) formatLon(gpsState.lon) else "---", colors = colors, compact = compact)
    CoordRow(label = "HORIZONTAL ACCURACY", value = if (gpsState.locked) formatAccuracy(gpsState.accuracyM, unitSystem) else "---", colors = colors, compact = compact)
    CoordRow(
        label = "VERTICAL ACCURACY",
        value = if (gpsState.locked && gpsState.verticalAccuracyM > 0f) formatAccuracy(gpsState.verticalAccuracyM, unitSystem) else "---",
        colors = colors,
        compact = compact
    )
    CoordRow(label = "SATELLITES", value = if (gpsState.satellites > 0) gpsState.satellites.toString() else "---", colors = colors, compact = compact)
    if (gpsState.pressureHpa > 0f) {
        CoordRow(label = "PRESSURE", value = formatPressure(gpsState.pressureHpa, unitSystem), colors = colors, compact = compact)
    }
}

@Composable
private fun BottomActionSection(
    gpsState: GpsState,
    showLabels: Boolean,
    isSharing: Boolean,
    colors: AppColors,
    onSavedClick: () -> Unit,
    onMapClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = AccentGreen.copy(alpha = 0.18f)
    )

    if (!gpsState.locked) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = AccentGreen, strokeWidth = 1.5.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Acquiring GPS fix — map, share and save unavailable",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                color = colors.dimText,
                textAlign = TextAlign.Center
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarButton(label = "Saved", showLabel = showLabels, colors = colors) {
            IconButton(onClick = onSavedClick) {
                Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Saved locations", tint = colors.dimText)
            }
        }
        BottomBarButton(label = "Map", showLabel = showLabels, colors = colors) {
            IconButton(onClick = { if (gpsState.locked) onMapClick() }, enabled = gpsState.locked) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    tint = if (gpsState.locked) colors.dimText else colors.dimText.copy(alpha = 0.3f)
                )
            }
        }
        BottomBarButton(label = "Share", showLabel = showLabels, colors = colors) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = { if (gpsState.locked && !isSharing) onShareClick() },
                    enabled = gpsState.locked && !isSharing
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentGreen, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share location",
                            tint = if (gpsState.locked) colors.dimText else colors.dimText.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val controller = WindowInsetsControllerCompat(
                (view.context as Activity).window, view
            )
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    val savedPins by viewModel.savedPins.collectAsState()
    val showLabels by viewModel.showLabels.collectAsState()
    val updateInterval by viewModel.updateInterval.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showSaved    by remember { mutableStateOf(false) }
    var showMap      by remember { mutableStateOf(false) }
    var savedConfirmation by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── Floating window state (must be before early returns so the lifecycle
    //    observer fires even while the Settings sub-screen is visible) ─────────
    val floatingWindowEnabled by viewModel.floatingWindowEnabled.collectAsState()
    var pendingOverlayEnable by remember { mutableStateOf(false) }
    val lifecycleOwnerEarly = LocalLifecycleOwner.current
    val currentPending = rememberUpdatedState(pendingOverlayEnable)

    DisposableEffect(lifecycleOwnerEarly) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncFloatingWindowState()
                if (currentPending.value) {
                    pendingOverlayEnable = false
                    if (Settings.canDrawOverlays(context)) {
                        FloatingWindowService.start(context)
                        viewModel.setFloatingWindowEnabled(true)
                    }
                }
            }
        }
        lifecycleOwnerEarly.lifecycle.addObserver(observer)
        onDispose { lifecycleOwnerEarly.lifecycle.removeObserver(observer) }
    }

    val onFloatingWindowToggle: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (Settings.canDrawOverlays(context)) {
                FloatingWindowService.start(context)
                viewModel.setFloatingWindowEnabled(true)
            } else {
                pendingOverlayEnable = true
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                )
            }
        } else {
            FloatingWindowService.stop(context)
            viewModel.setFloatingWindowEnabled(false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collect {
            savedConfirmation = true
            kotlinx.coroutines.delay(2000)
            savedConfirmation = false
        }
    }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            onThemeChange = { viewModel.setThemeMode(it) },
            unitSystem = unitSystem,
            onUnitSystemChange = { viewModel.setUnitSystem(it) },
            showLabels = showLabels,
            onShowLabelsChange = { viewModel.setShowLabels(it) },
            updateInterval = updateInterval,
            onUpdateIntervalChange = { viewModel.setUpdateInterval(it) },
            floatingWindowEnabled = floatingWindowEnabled,
            onFloatingWindowToggle = onFloatingWindowToggle,
            colors = colors,
            onBack = { showSettings = false }
        )
        return
    }

    if (showSaved) {
        SavedLocationsScreen(
            pins = savedPins,
            gpsState = gpsState,
            unitSystem = unitSystem,
            colors = colors,
            onSaveWithName = { viewModel.saveCurrentLocation(it) },
            onRename = { id, name -> viewModel.renamePin(id, name) },
            onDelete = { viewModel.deletePin(it) },
            onBack = { showSaved = false }
        )
        return
    }

    if (showMap) {
        MapScreen(
            gpsState = gpsState,
            savedPins = savedPins,
            unitSystem = unitSystem,
            colors = colors,
            onBack = { showMap = false }
        )
        return
    }

    val fineLocation = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) {
        if (!fineLocation.status.isGranted) fineLocation.launchPermissionRequest()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, fineLocation.status.isGranted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (fineLocation.status.isGranted) viewModel.startUpdates()
                Lifecycle.Event.ON_PAUSE  -> viewModel.stopUpdates()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopUpdates()
        }
    }

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

    val onThemeCycle: () -> Unit = {
        viewModel.setThemeMode(when (themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK   -> ThemeMode.LIGHT
            ThemeMode.LIGHT  -> ThemeMode.SYSTEM
        })
    }
    val onShareClick: () -> Unit = {
        isSharing = true
        scope.launch {
            shareLocation(context, gpsState, isDark, unitSystem)
            isSharing = false
        }
    }
    val onSaveClick: () -> Unit = {
        saveLabel = gpsState.locationName
        showSaveDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // ── Floating save FAB (bottom-left) ───────────────────────────────────
        FloatingActionButton(
            onClick = { if (gpsState.locked) onSaveClick() },
            modifier = Modifier
                .align(if (isLandscape) Alignment.BottomStart else Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    start = if (isLandscape) 16.dp else 0.dp,
                    end = if (isLandscape) 0.dp else 16.dp,
                    bottom = if (isLandscape) 16.dp else 96.dp
                ),
            containerColor = if (gpsState.locked) AccentGreen else colors.dimText.copy(alpha = 0.3f),
            contentColor = if (gpsState.locked) Color.Black else colors.dimText.copy(alpha = 0.5f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
                focusedElevation = 2.dp,
                hoveredElevation = 2.dp
            )
        ) {
            Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Save location")
        }

        if (isLandscape) {
            // ── Landscape: ring left, details+actions right ───────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                TopBar(gpsState, themeMode, colors, onThemeCycle, { showSettings = true })

                Row(modifier = Modifier.weight(1f)) {
                    // Left: elevation ring
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ElevationRing(gpsState = gpsState, acquiringAlpha = acquiringAlpha, colors = colors, unitSystem = unitSystem)
                    }

                    VerticalDivider(color = AccentGreen.copy(alpha = 0.18f))

                    // Right: scrollable details + bottom actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DetailSection(gpsState = gpsState, unitSystem = unitSystem, colors = colors, compact = true)
                        }
                        BottomActionSection(
                            gpsState = gpsState,
                            showLabels = showLabels,
                            isSharing = isSharing,
                            colors = colors,
                            onSavedClick = { showSaved = true },
                            onMapClick = { showMap = true },
                            onShareClick = onShareClick
                        )
                    }
                }
            }
        } else {
            // ── Portrait: stacked layout ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(gpsState, themeMode, colors, onThemeCycle, { showSettings = true })

                Spacer(modifier = Modifier.height(12.dp))
                ElevationRing(gpsState = gpsState, acquiringAlpha = acquiringAlpha, colors = colors, unitSystem = unitSystem)
                Spacer(modifier = Modifier.height(20.dp))

                DetailSection(gpsState = gpsState, unitSystem = unitSystem, colors = colors)

                Spacer(modifier = Modifier.weight(1f))

                BottomActionSection(
                    gpsState = gpsState,
                    showLabels = showLabels,
                    isSharing = isSharing,
                    colors = colors,
                    onSavedClick = { showSaved = true },
                    onMapClick = { showMap = true },
                    onShareClick = onShareClick
                )
            }
        }
    }

    // ── Save location dialog ──────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveLabel = ""
            },
            title = { Text("Save location") },
            text = {
                OutlinedTextField(
                    value = saveLabel,
                    onValueChange = { if (it.length <= MAX_LABEL_LENGTH) saveLabel = it },
                    label = { Text("Name") },
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${saveLabel.length} / $MAX_LABEL_LENGTH",
                            color = if (saveLabel.length >= MAX_LABEL_LENGTH)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCurrentLocation(saveLabel)
                    showSaveDialog = false
                    saveLabel = ""
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    saveLabel = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
