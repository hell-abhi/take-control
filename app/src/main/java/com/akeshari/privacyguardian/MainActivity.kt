package com.akeshari.privacyguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.akeshari.privacyguardian.ui.navigation.PrivacyGuardianNavHost
import com.akeshari.privacyguardian.ui.theme.PrivacyGuardianTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrivacyGuardianTheme {
                PrivacyGuardianNavHost()
            }
        }
    }
}
