package com.lumify.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lumify.editor.camera.CameraCaptureScreen
import com.lumify.editor.ui.screens.EditorScreen
import com.lumify.editor.ui.screens.EditorViewModel
import com.lumify.editor.ui.screens.HomeScreen
import com.lumify.editor.ui.screens.SplashScreen
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.LumifyTheme

enum class AppDestination {
    SPLASH,
    HOME,
    EDITOR,
    CAMERA
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LumifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }

                    // Handle incoming image from other apps (SEND or VIEW intent)
                    LaunchedEffect(intent) {
                        handleIncomingIntent(intent) {
                            currentDestination = AppDestination.EDITOR
                        }
                    }

                    Crossfade(
                        targetState = currentDestination,
                        label = "appNavigation"
                    ) { dest ->
                        when (dest) {
                            AppDestination.SPLASH -> {
                                SplashScreen(
                                    onAnimationFinished = {
                                        currentDestination = AppDestination.HOME
                                    }
                                )
                            }

                            AppDestination.HOME -> {
                                HomeScreen(
                                    onImageSelected = { uri ->
                                        editorViewModel.loadImage(this@MainActivity, uri)
                                        currentDestination = AppDestination.EDITOR
                                    },
                                    onSampleSelected = { bmp, meta ->
                                        editorViewModel.setBitmapDirectly(bmp, meta)
                                        currentDestination = AppDestination.EDITOR
                                    },
                                    onLaunchCamera = {
                                        currentDestination = AppDestination.CAMERA
                                    }
                                )
                            }

                            AppDestination.EDITOR -> {
                                EditorScreen(
                                    viewModel = editorViewModel,
                                    onNavigateBack = {
                                        currentDestination = AppDestination.HOME
                                    }
                                )
                            }

                            AppDestination.CAMERA -> {
                                CameraCaptureScreen(
                                    onPhotoCaptured = { bmp, meta ->
                                        editorViewModel.setBitmapDirectly(bmp, meta)
                                        currentDestination = AppDestination.EDITOR
                                    },
                                    onClose = {
                                        currentDestination = AppDestination.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?, onImageLoaded: () -> Unit) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if ((Intent.ACTION_SEND == action || Intent.ACTION_VIEW == action) && type != null) {
            val uri: Uri? = if (Intent.ACTION_SEND == action) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            } else {
                intent.data
            }

            uri?.let {
                editorViewModel.loadImage(this, it)
                onImageLoaded()
            }
        }
    }
}
