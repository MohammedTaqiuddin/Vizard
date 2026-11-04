package com.example.algovisualizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.algovisualizer.core.navigation.NavGraph
import com.example.algovisualizer.core.theme.AlgoVisualizerTheme
import com.example.algovisualizer.core.ui.glass.GlassBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Glass needs the content to run under the system bars, not beside them.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AlgoVisualizerTheme {
                // One ambient layer for the whole app: screens stay translucent on top of it,
                // so the aurora never restarts when you navigate.
                GlassBackground(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
