package com.example.streamify

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(this) // Use mainExecutor from the context
        )
    }
}

suspend fun Context.createPreview(previewView: PreviewView): Preview = Preview.Builder()
    .build()
    .apply { setSurfaceProvider(previewView.surfaceProvider) }

fun createQualitySelector(): QualitySelector = QualitySelector.from(
    Quality.FHD,
    FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
)

fun createRecorder(mainExecutor: Executor, qualitySelector: QualitySelector): Recorder =
    Recorder.Builder()
        .setExecutor(mainExecutor)
        .setQualitySelector(qualitySelector)
        .build()

fun createVideoCapture(recorder: Recorder): VideoCapture<Recorder> =
    VideoCapture.withOutput(recorder)

suspend fun Context.bindUseCases(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    preview: Preview,
    videoCapture: VideoCapture<Recorder>
) {
    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        videoCapture
    )
}

suspend fun Context.createVideoCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView
): VideoCapture<Recorder> {
    val mainExecutor = ContextCompat.getMainExecutor(this) // Use mainExecutor from the context
    val preview = createPreview(previewView)
    val qualitySelector = createQualitySelector()
    val recorder = createRecorder(mainExecutor, qualitySelector)
    val videoCapture = createVideoCapture(recorder)

    bindUseCases(lifecycleOwner, cameraSelector, preview, videoCapture)

    return videoCapture
}

fun createVideoFile(filenameFormat: String, outputDirectory: File): File = File(
    outputDirectory,
    SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4"
)

fun prepareRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    videoFile: File
): PendingRecording = videoCapture.output
    .prepareRecording(context, FileOutputOptions.Builder(videoFile).build())

@SuppressLint("MissingPermission")
fun startRecordingVideo(
    context: Context,
    filenameFormat: String,
    videoCapture: VideoCapture<Recorder>,
    outputDirectory: File,
    executor: Executor,
    audioEnabled: Boolean,
    consumer: Consumer<VideoRecordEvent>
): Recording {
    val videoFile = createVideoFile(filenameFormat, outputDirectory)
    val recording = prepareRecording(context, videoCapture, videoFile)

    return recording.apply { if (audioEnabled) withAudioEnabled() }
        .start(executor, consumer)
}

