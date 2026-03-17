package com.akeshari.takecontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.akeshari.takecontrol.ui.navigation.TakeControlNavHost
import com.akeshari.takecontrol.ui.theme.TakeControlTheme
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
