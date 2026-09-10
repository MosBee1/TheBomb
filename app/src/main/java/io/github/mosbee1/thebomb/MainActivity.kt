package io.github.mosbee1.thebomb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mosbee1.thebomb.ui.MainViewModel
import io.github.mosbee1.thebomb.ui.navigation.JanitorScaffold
import io.github.mosbee1.thebomb.ui.onboarding.OnboardingScreen
import io.github.mosbee1.thebomb.ui.theme.TheBombTheme

/**
 * Single-activity app. First run shows the permissions onboarding (which
 * explains every permission BEFORE asking); afterwards the four-tab shell.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheBombTheme {
                val mainViewModel: MainViewModel = viewModel()
                val settings by mainViewModel.settings
                    .collectAsStateWithLifecycle(initialValue = null)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val current = settings
                    if (current == null) {
                        // One frame while Room seeds/loads: empty box, no flicker.
                        Box(Modifier.fillMaxSize())
                    } else if (!current.onboardingCompleted) {
                        OnboardingScreen(mainViewModel = mainViewModel)
                    } else {
                        JanitorScaffold(mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }
}
