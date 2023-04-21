package com.example.streamify.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val isRecording = remember { mutableStateOf(false) }

    // Request camera and storage permissions
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                isRecording.value = !isRecording.value
            } else {
                // Show rationale for permission request
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(cameraProviderFuture, isRecording.value)

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            onClick = {
                if (isRecording.value) {
                    // Stop recording
                    isRecording.value = false
                } else {
                    // Request permissions
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            }
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = if (isRecording.value) "Stop recording" else "Start recording"
            )
        }
    }
}

@Composable
fun CameraPreview(
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    isRecording: Boolean
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProvider = remember(cameraProviderFuture) {
        mutableStateOf<ProcessCameraProvider?>(null)
    }
    LaunchedEffect(cameraProviderFuture) {
        cameraProvider.value = cameraProviderFuture.get()
    }
    val cameraSelector = remember {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }
    val videoCapture = remember {
        VideoCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    ) {
        cameraProvider.value?.let {
            val camera = it.bindToLifecycle(
                LocalLifecycleOwner.current,
                cameraSelector,
                Preview.Builder().build()
                    .also { preview -> preview.setSurfaceProvider(previewView.surfaceProvider) },
                videoCapture
            )

            if (isRecording) {
                val file = File(context.externalMediaDirs.first(), "video.mp4")
                videoCapture.startRecording(
                    file, Executors.newSingleThreadExecutor(),
                    object : VideoCapture.OnVideoSavedCallback {
                        override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                            // Video saved successfully
                        }

                        override fun onError(
                            videoCaptureError: Int,
                            message: String,
                            cause: Throwable?
                        ) {
                            // Handle errors during recording
                        }
                    }
                ) // end of startRecording()
            } else {
                videoCapture.stopRecording()
            }
        }
    }
}



