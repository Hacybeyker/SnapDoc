package com.hacybeyker.scaffoldingandroidcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.hacybeyker.scaffoldingandroidcompose.core.ui.theme.ScaffoldingAndroidComposeTheme
import com.hacybeyker.scaffoldingandroidcompose.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScaffoldingAndroidComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(modifier = Modifier.padding(paddingValues = innerPadding))
                }
            }
        }
    }
}
