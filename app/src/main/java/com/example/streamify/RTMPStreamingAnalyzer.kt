package com.example.streamify

import android.media.MediaCodec
import android.media.MediaFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pedro.rtmp.rtmp.RtmpClient
import java.nio.ByteBuffer

class RtmpStreamingAnalyzer(private val rtmpClient: RtmpClient) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        if (rtmpClient.isStreaming) {
            imageProxyToH264(image).let { bytes ->
                val presentationTimeUs = image.imageInfo.timestamp / 1000
                val bufferInfo = MediaCodec.BufferInfo()
                bufferInfo.presentationTimeUs = presentationTimeUs
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME

                rtmpClient.sendVideo(ByteBuffer.wrap(bytes), bufferInfo)
            }
        }
    }
}


fun imageProxyToH264(image: ImageProxy): ByteArray {
    // Get the image planes
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    // Get the image dimensions and format
    val width = image.width
    val height = image.height
    val format = image.format

    // Create a MediaCodec encoder for H.264
    val mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000)
    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, format)
    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
    val encoder = MediaCodec.createEncoderByType("video/avc")
    encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    encoder.start()

    // Feed the image planes to the encoder
    val inputBuffers = encoder.inputBuffers
    val outputBuffers = encoder.outputBuffers
    var inputBufferIndex = 0
    var outputBufferIndex = 0
    var presentationTimeUs = image.imageInfo.timestamp
    while (true) {
        inputBufferIndex = encoder.dequeueInputBuffer(-1)
        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            val yuv420 = ByteArray(yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining())
            yBuffer.get(yuv420, 0, yBuffer.remaining())
            uBuffer.get(yuv420, yBuffer.remaining(), uBuffer.remaining())
            vBuffer.get(yuv420, yBuffer.remaining() + uBuffer.remaining(), vBuffer.remaining())
            inputBuffer.put(yuv420)
            encoder.queueInputBuffer(inputBufferIndex, 0, yuv420.size, presentationTimeUs, 0)
            presentationTimeUs += 1000000 / 30
        }

        val bufferInfo = MediaCodec.BufferInfo()
        outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
        if (outputBufferIndex >= 0) {
            val outputBuffer = outputBuffers[outputBufferIndex]
            val data = ByteArray(bufferInfo.size)
            outputBuffer.get(data)
            encoder.releaseOutputBuffer(outputBufferIndex, false)
            if (data.isNotEmpty()) {
                return data
            }
        }
    }
}

