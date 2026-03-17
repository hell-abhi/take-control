package com.akeshari.privacyguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.akeshari.privacyguardian.ui.navigation.TakeControlNavHost
import com.akeshari.privacyguardian.ui.theme.TakeControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TakeControlTheme {
                TakeControlNavHost()
            }
        }
    }
}
