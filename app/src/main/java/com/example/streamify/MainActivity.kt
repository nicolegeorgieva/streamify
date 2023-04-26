package com.example.streamify

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.streamify.Route.VIDEO_PREVIEW_ARG
import com.example.streamify.capture.VideoCaptureScreen
import com.example.streamify.capture.createShareIntent
import com.example.streamify.preview.VideoPreviewScreen
import com.example.streamify.theme.StreamifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamifyTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Route.VIDEO
                    ) {
                        composable(Route.VIDEO) {
                            VideoCaptureScreen(navController = navController)
                        }

                        composable(Route.VIDEO_PREVIEW_FULL_ROUTE) {
                            val uri = it.arguments?.getString(VIDEO_PREVIEW_ARG) ?: ""
                            val context = LocalContext.current
                            VideoPreviewScreen(uri = uri, onShareClick = {
                                createShareIntent(context, Uri.parse(uri))
                            })
                        }
                    }
                }
            }
        }
    }
}

object Route {
    const val VIDEO = "video"
    const val VIDEO_PREVIEW = "video_preview"
    const val VIDEO_PREVIEW_ARG = "uri"
    const val VIDEO_PREVIEW_FULL_ROUTE = "$VIDEO_PREVIEW/{$VIDEO_PREVIEW_ARG}"
}
