package com.example.streamify.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.common.util.concurrent.ListenableFuture

@Composable
fun CameraScreen() {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(LocalContext.current) }
    val playButtonEnabled = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(cameraProviderFuture)

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            onClick = { playButtonEnabled.value = !playButtonEnabled.value }
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start streaming and recording"
            )
        }
    }
}

@Composable
fun CameraPreview(cameraProviderFuture: ListenableFuture<ProcessCameraProvider>) {
    val previewView = remember { PreviewView(LocalContext.current) }
    val cameraProvider = remember { cameraProviderFuture.await() }
    val cameraSelector = remember {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) {
        val camera = cameraProvider.bindToLifecycle(
            LocalLifecycleOwner.current,
            cameraSelector,
            Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        )
    }
}