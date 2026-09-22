package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.GameRunnerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val session by viewModel.activeSession.collectAsStateWithLifecycle()

                Crossfade(
                    targetState = session.isRunning,
                    label = "AppScreenCrossfade"
                ) { isRunning ->
                    if (isRunning) {
                        GameRunnerScreen(viewModel = viewModel)
                    } else {
                        HomeScreen(
                            viewModel = viewModel,
                            onLaunchGame = { game -> viewModel.launchGame(game) }
                        )
                    }
                }
            }
        }
    }
}

