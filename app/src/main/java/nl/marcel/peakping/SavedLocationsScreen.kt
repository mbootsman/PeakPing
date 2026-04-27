package nl.marcel.peakping

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val timestampFmt = SimpleDateFormat("d MMM yyyy  HH:mm", Locale.getDefault())

@Composable
fun SavedLocationsScreen(
    pins: List<SavedPin>,
    gpsState: GpsState,
    unitSystem: UnitSystem,
    colors: AppColors,
    onSaveWithName: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var showSaveDialog by remember { mutableStateOf(false) }
    var renamePin by remember { mutableStateOf<SavedPin?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedPin?>(null) }

    // Commit the delete after 4 seconds unless Undo is tapped
    LaunchedEffect(pendingDelete) {
        val toDelete = pendingDelete ?: return@LaunchedEffect
        delay(4000L)
        onDelete(toDelete.id)
        pendingDelete = null
    }

    if (showSaveDialog) {
        val defaultName = gpsState.locationName.ifEmpty {
            "${formatLat(gpsState.lat)}  ${formatLon(gpsState.lon)}"
        }
        NameDialog(
            title = "Save location",
            initialName = defaultName,
            confirmLabel = "Save",
            colors = colors,
            onConfirm = { name ->
                onSaveWithName(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    renamePin?.let { pin ->
        NameDialog(
            title = "Rename",
            initialName = pin.label,
            confirmLabel = "Rename",
            colors = colors,
            onConfirm = { name ->
                onRename(pin.id, name)
                renamePin = null
            },
            onDismiss = { renamePin = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Text(
                text = "Saved Locations",
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
        }

        HorizontalDivider(color = AccentGreen.copy(alpha = 0.18f))

        // ── Save current location button ──────────────────────────────────────
        if (gpsState.locked) {
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                val elev = if (unitSystem == UnitSystem.METRIC)
                    "${elevationM(gpsState.elevation)} m"
                else
                    "${elevationFt(gpsState.elevation)} ft"
                val location = gpsState.locationName.ifEmpty {
                    "${formatLat(gpsState.lat)}  ${formatLon(gpsState.lon)}"
                }
                Text(
                    text = "Save  ·  $elev  ·  $location",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            HorizontalDivider(color = AccentGreen.copy(alpha = 0.18f))
        }

        // ── Saved pins list ───────────────────────────────────────────────────
        if (pins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved locations yet.\nGet a GPS fix and tap Save.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = colors.dimText,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn {
                items(
                    pins.filter { it.id != pendingDelete?.id }.sortedByDescending { it.savedAt },
                    key = { it.id }
                ) { pin ->
                    PinRow(
                        pin = pin,
                        unitSystem = unitSystem,
                        colors = colors,
                        onRenameClick = { renamePin = pin },
                        onDelete = {
                            // Commit any already-pending delete before starting a new one
                            pendingDelete?.let { prev -> onDelete(prev.id) }
                            pendingDelete = pin
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = AccentGreen.copy(alpha = 0.10f)
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    } // end Column

    pendingDelete?.let { deleted ->
        Snackbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            containerColor = Color(0xFF1A2A3A),
            contentColor = Color.White,
            action = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Undo", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Text("\"${deleted.label}\" deleted")
        }
    }

    } // end Box
}

@Composable
private fun PinRow(
    pin: SavedPin,
    unitSystem: UnitSystem,
    colors: AppColors,
    onRenameClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pin.label,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            val elev = if (unitSystem == UnitSystem.METRIC)
                "${elevationM(pin.elevationM)} m"
            else
                "${elevationFt(pin.elevationM)} ft"
            Text(
                text = "$elev  ·  ${timestampFmt.format(Date(pin.savedAt))}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = colors.dimText
            )
            if (pin.locationName.isNotEmpty() && pin.locationName != pin.label) {
                Text(
                    text = pin.locationName,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    color = colors.dimAccent
                )
            }
        }
        IconButton(onClick = onRenameClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename",
                tint = colors.dimText
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = colors.dimText
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    colors: AppColors,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var textValue by remember {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(0, initialName.length)))
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        titleContentColor = colors.text,
        title = {
            Text(title, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = colors.dimText,
                    focusedTextColor = colors.text,
                    unfocusedTextColor = colors.text,
                    cursorColor = AccentGreen,
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (textValue.text.isNotBlank()) onConfirm(textValue.text.trim()) },
            ) {
                Text(confirmLabel, color = AccentGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.dimText)
            }
        }
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}