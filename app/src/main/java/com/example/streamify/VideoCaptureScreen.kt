package com.example.streamify

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val analyzer = VideoEncoderAnalyzer()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoCaptureScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Handle permissions
    val permissionState = handlePermissions()

    // 2. Initialize states
    val states = initializeStates()

    // 3. Request permissions when the effect is launched
    requestPermissions(permissionState)

    // 4. Create video capture use case when the effect is launched
    createVideoCaptureUseCase(
        context,
        lifecycleOwner,
        states.previewView,
        states.videoCapture,
        states.cameraSelector
    )

    PermissionsRequired(
        multiplePermissionsState = permissionState,
        permissionsNotGrantedContent = {
            NotGrantedPermissionsScreen {
                permissionState.launchMultiplePermissionRequest()
            }
        },
        permissionsNotAvailableContent = { /* ... */ }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 5. Show the camera preview
            showCameraPreview(states.previewView)

            // 6. Handle recording button
            handleRecordingButton(context, navController, states, states.recordingStarted)

            // 7. Handle audio button
            handleAudioButton(states.audioEnabled, states.recordingStarted)

            // 8. Handle camera switch button
            handleCameraSwitchButton(context, lifecycleOwner, states, states.recordingStarted)
        }
    }
}

// 1. Handle permissions
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun handlePermissions(): MultiplePermissionsState {
    return rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )
}

// 2. Initialize states
@Composable
fun initializeStates(): States {
    val context = LocalContext.current
    return remember {
        States(
            recording = null,
            previewView = PreviewView(context),
            videoCapture = mutableStateOf(null),
            recordingStarted = mutableStateOf(false),
            audioEnabled = mutableStateOf(false),
            cameraSelector = mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
        )
    }
}

data class States(
    var recording: Recording?,
    val previewView: PreviewView,
    val videoCapture: MutableState<VideoCapture<Recorder>?>,
    val recordingStarted: MutableState<Boolean>,
    val audioEnabled: MutableState<Boolean>,
    val cameraSelector: MutableState<CameraSelector>
)

// 3. Request permissions when the effect is launched
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun requestPermissions(permissionState: MultiplePermissionsState) {
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }
}

// 4. Create video capture use case when the effect is launched
@Composable
fun createVideoCaptureUseCase(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    videoCapture: MutableState<VideoCapture<Recorder>?>,
    cameraSelector: MutableState<CameraSelector>
) {
    LaunchedEffect(previewView) {
        videoCapture.value = context.createVideoCaptureUseCase(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector.value,
            previewView = previewView,
            analyzer = analyzer,
        )
    }
}

// 5. Show the camera preview
@Composable
fun BoxScope.showCameraPreview(previewView: PreviewView) {
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

// 6. Handle recording button
@Composable
fun BoxScope.handleRecordingButton(
    context: Context,
    navController: NavController,
    states: States,
    recordingStarted: MutableState<Boolean>
) {
    IconButton(
        onClick = {
            if (!recordingStarted.value) {
                startRecording(context, navController, states)
            } else {
                stopRecording(states.recordingStarted, states.recording)
            }
        },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)
    ) {
        Icon(
            painter = painterResource(if (recordingStarted.value) R.drawable.ic_stop else R.drawable.ic_record),
            contentDescription = "",
            modifier = Modifier.size(64.dp)
        )
    }
}

fun startRecording(context: Context, navController: NavController, states: States) {
    states.videoCapture.value?.let { videoCapture ->
        states.recordingStarted.value = true
        val mediaDir = context.externalCacheDirs.firstOrNull()?.let {
            File(it, context.getString(R.string.app_name)).apply { mkdirs() }
        }

        states.recording = startRecordingVideo(
            context = context,
            filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS",
            videoCapture = videoCapture,
            outputDirectory = if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir,
            executor = context.mainExecutor,
            audioEnabled = states.audioEnabled.value
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                val uri = event.outputResults.outputUri
                if (uri != Uri.EMPTY) {
                    val uriEncoded = URLEncoder.encode(
                        uri.toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate("${Route.VIDEO_PREVIEW}/$uriEncoded")
                }
            }
        }
    }
}

fun stopRecording(recordingStarted: MutableState<Boolean>, recording: Recording?) {
    recordingStarted.value = false
    recording?.stop()
}

// 7. Handle audio button
@Composable
fun BoxScope.handleAudioButton(
    audioEnabled: MutableState<Boolean>,
    recordingStarted: MutableState<Boolean>
) {
    if (!recordingStarted.value) {
        IconButton(
            onClick = {
                audioEnabled.value = !audioEnabled.value
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                painter = painterResource(if (audioEnabled.value) R.drawable.ic_mic_on else R.drawable.ic_mic_off),
                contentDescription = "",
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

// 8. Handle camera switch button
@Composable
fun BoxScope.handleCameraSwitchButton(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    states: States,
    recordingStarted: MutableState<Boolean>
) {
    if (!recordingStarted.value) {
        IconButton(
            onClick = {
                lifecycleOwner.lifecycleScope.launch {
                    switchCamera(context, lifecycleOwner, states)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_switch_camera),
                contentDescription = "",
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

suspend fun switchCamera(context: Context, lifecycleOwner: LifecycleOwner, states: States) {
    states.cameraSelector.value =
        if (states.cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA

    states.videoCapture.value = context.createVideoCaptureUseCase(
        lifecycleOwner = lifecycleOwner,
        cameraSelector = states.cameraSelector.value,
        previewView = states.previewView,
        analyzer = analyzer,
    )
}

@Composable
fun NotGrantedPermissionsScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(android.R.drawable.stat_sys_warning),
            contentDescription = "Permissions Needed",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colors.error
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "We need access to your camera and microphone to record videos",
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRequestPermissions,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Ask for permissions",
                color = Color.White,
                style = MaterialTheme.typography.button
            )
        }
    }
}
