package nl.marcel.peakping

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    unitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit,
    showLabels: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    colors: AppColors,
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
                text = "Settings",
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
        }

        HorizontalDivider(color = AccentGreen.copy(alpha = 0.18f))

        // ── Theme section ─────────────────────────────────────────────────────
        Text(
            text = "THEME",
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = colors.dimAccent,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
        )

        SettingsOption("System default", ThemeMode.SYSTEM, themeMode, onThemeChange, colors)
        SettingsOption("Dark", ThemeMode.DARK, themeMode, onThemeChange, colors)
        SettingsOption("Light", ThemeMode.LIGHT, themeMode, onThemeChange, colors)

        // ── Units section ─────────────────────────────────────────────────────
        Text(
            text = "UNITS",
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = colors.dimAccent,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
        )

        SettingsOption("Metric (m)", UnitSystem.METRIC, unitSystem, onUnitSystemChange, colors)
        SettingsOption("Imperial (ft)", UnitSystem.IMPERIAL, unitSystem, onUnitSystemChange, colors)

        // ── Display section ───────────────────────────────────────────────────
        Text(
            text = "DISPLAY",
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = colors.dimAccent,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowLabelsChange(!showLabels) }
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show bottom bar labels",
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
                color = colors.text,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = showLabels,
                onCheckedChange = onShowLabelsChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentGreen,
                    checkedTrackColor = AccentGreen.copy(alpha = 0.4f),
                    uncheckedThumbColor = colors.dimText,
                    uncheckedTrackColor = colors.dimText.copy(alpha = 0.3f),
                )
            )
        }

        // ── About ─────────────────────────────────────────────────────────────
        val context = LocalContext.current
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = AccentGreen.copy(alpha = 0.18f)
        )
        Text(
            text = "Version ${packageInfo.versionName} (${packageInfo.longVersionCode})",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.dimText,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun <T> SettingsOption(
    label: String,
    value: T,
    current: T,
    onSelect: (T) -> Unit,
    colors: AppColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = current == value,
            onClick = { onSelect(value) },
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentGreen,
                unselectedColor = colors.dimText
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif,
            color = colors.text
        )
    }
}
