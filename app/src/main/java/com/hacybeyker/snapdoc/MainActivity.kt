package com.hacybeyker.snapdoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnapDocTheme {
                // No Scaffold here on purpose. Every destination already owns one (or, for the camera,
                // handles insets itself), and wrapping them in a second one applied the system-bar
                // padding twice: doubled gutters everywhere, and a viewfinder letterboxed between two
                // black bars instead of filling the screen.
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
