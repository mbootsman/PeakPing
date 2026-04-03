package nl.marcel.peakping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.marcel.peakping.ui.theme.PeakPingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PeakPingTheme(darkTheme = true, dynamicColor = false) {
                val elevationViewModel: ElevationViewModel = viewModel()
                ElevationScreen(viewModel = elevationViewModel)
            }
        }
    }
}
