package com.tubers.blacklight

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tubers.blacklight.ui.navigation.UvGlowRoot
import com.tubers.blacklight.ui.state.StudioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels { StudioViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the system splash until saved settings are restored, so the first frame is correct.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !viewModel.uiState.value.loaded }

        // Dark, transparent system bars over the near-black UI.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent { UvGlowRoot(viewModel) }

        // Ad consent (Google UMP) once per launch, not on every rotation; ads start only if allowed.
        if (savedInstanceState == null) viewModel.gatherAdConsent(this)
    }
}
