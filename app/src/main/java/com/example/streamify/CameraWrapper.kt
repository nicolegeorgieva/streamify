package com.example.streamify

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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

// Obtain the camera provider asynchronously
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener(
            {
                continuation.resume(future.get())
            },
            ContextCompat.getMainExecutor(this) // Get main executor from the context
        )
    }
}

// Create a Preview instance with the given PreviewView's surface provider
suspend fun Context.createPreview(previewView: PreviewView): Preview = Preview.Builder()
    .build()
    .apply { setSurfaceProvider(previewView.surfaceProvider) }

// Define a quality selector for the video recording
fun createQualitySelector(): QualitySelector = QualitySelector.from(
    Quality.FHD,
    FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
)

// Create a Recorder instance with the given executor and quality selector
fun createRecorder(mainExecutor: Executor, qualitySelector: QualitySelector): Recorder =
    Recorder.Builder()
        .setExecutor(mainExecutor)
        .setQualitySelector(qualitySelector)
        .build()

// Create a VideoCapture instance with the given Recorder instance as its output
fun createVideoCapture(recorder: Recorder): VideoCapture<Recorder> =
    VideoCapture.withOutput(recorder)

// Bind the camera provider to the lifecycle and use cases
suspend fun Context.bindUseCases(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    preview: Preview,
    videoCapture: VideoCapture<Recorder>,
    imageAnalysis: ImageAnalysis
) {
    val cameraProvider = getCameraProvider()
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        videoCapture,
        imageAnalysis,
    )
}

// Set up the VideoCapture use case with the given lifecycle owner, camera selector, and preview view
suspend fun Context.createVideoCaptureUseCase(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    previewView: PreviewView,
    analyzer: ImageAnalysis.Analyzer
): VideoCapture<Recorder> {
    val mainExecutor = ContextCompat.getMainExecutor(this) // Get main executor from the context
    val preview = createPreview(previewView)
    val qualitySelector = createQualitySelector()
    val recorder = createRecorder(mainExecutor, qualitySelector)
    val videoCapture = createVideoCapture(recorder)
    val imageAnalysis = createImageAnalysis(this, analyzer)


    bindUseCases(lifecycleOwner, cameraSelector, preview, videoCapture, imageAnalysis)

    return videoCapture
}

// Create a video file with the given filename format and output directory
fun createVideoFile(filenameFormat: String, outputDirectory: File): File = File(
    outputDirectory,
    SimpleDateFormat(filenameFormat, Locale.US).format(System.currentTimeMillis()) + ".mp4"
)

// Prepare the recording with the given context, video capture instance, and video file
fun prepareRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    videoFile: File
): PendingRecording = videoCapture.output
    .prepareRecording(context, FileOutputOptions.Builder(videoFile).build())

// Start the video recording with the given parameters and consumer
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


fun createImageAnalysis(
    context: Context,
    analyzer: ImageAnalysis.Analyzer
): ImageAnalysis =
    ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply { setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

