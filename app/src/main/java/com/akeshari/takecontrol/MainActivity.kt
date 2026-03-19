package com.akeshari.takecontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.akeshari.takecontrol.ui.navigation.TakeControlNavHost
import com.akeshari.takecontrol.ui.onboarding.OnboardingScreen
import com.akeshari.takecontrol.ui.theme.TakeControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var showOnboarding by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("take_control_prefs", Context.MODE_PRIVATE)
        showOnboarding = !prefs.getBoolean("onboarding_completed", false)

        setContent {
            TakeControlTheme {
                if (showOnboarding) {
                    OnboardingScreen(onComplete = {
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        showOnboarding = false
                    })
                } else {
                    TakeControlNavHost()
                }
            }
        }
    }
}
