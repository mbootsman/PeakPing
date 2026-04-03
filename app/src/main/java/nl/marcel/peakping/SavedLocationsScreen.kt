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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timestampFmt = SimpleDateFormat("d MMM yyyy  HH:mm", Locale.getDefault())

@Composable
fun SavedLocationsScreen(
    pins: List<SavedPin>,
    gpsState: GpsState,
    unitSystem: UnitSystem,
    colors: AppColors,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
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
                onClick = onSave,
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
                items(pins.sortedByDescending { it.savedAt }, key = { it.id }) { pin ->
                    PinRow(pin, unitSystem, colors, onDelete)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = AccentGreen.copy(alpha = 0.10f)
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun PinRow(
    pin: SavedPin,
    unitSystem: UnitSystem,
    colors: AppColors,
    onDelete: (Long) -> Unit,
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
        IconButton(onClick = { onDelete(pin.id) }) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = colors.dimText
            )
        }
    }
}
