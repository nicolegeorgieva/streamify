package com.example.streamify

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class VideoEncoderAnalyzer : ImageAnalysis.Analyzer {

    private var mediaCodec: MediaCodec? = null
    private var isCodecConfigured = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (mediaCodec == null) {
            setupMediaCodec()
        }

        if (!isCodecConfigured) {
            configureMediaCodec(imageProxy)
        }

        val inputImage = imageProxy.image ?: return
        val inputBuffer = getYUV420Planar(inputImage)

        // Encode the frame using MediaCodec
        encodeFrame(inputBuffer)

        imageProxy.close()
    }

    private fun setupMediaCodec() {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        mediaCodec = MediaCodec.createEncoderByType(mimeType)
    }

    private fun configureMediaCodec(imageProxy: ImageProxy) {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format =
            MediaFormat.createVideoFormat(mimeType, imageProxy.width, imageProxy.height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 2000 * 1000) // 2Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second
            }

        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()

        isCodecConfigured = true
    }

    private fun getYUV420Planar(image: Image): ByteBuffer {
        // Convert YUV_420_888 to YUV_420P (I420)
        // Code implementation here
        TODO()
    }

    private fun encodeFrame(inputBuffer: ByteBuffer) {
        val bufferIndex = mediaCodec?.dequeueInputBuffer(1000) ?: -1
        if (bufferIndex >= 0) {
            val inputBuffer = mediaCodec?.getInputBuffer(bufferIndex)
            inputBuffer?.put(inputBuffer)

            if (inputBuffer != null) {
                mediaCodec?.queueInputBuffer(
                    bufferIndex,
                    0,
                    inputBuffer.remaining(),
                    System.nanoTime() / 1000, // presentationTimeUs
                    0
                )
            }
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1

        while (outputBufferIndex >= 0) {
            val encodedData = mediaCodec?.getOutputBuffer(outputBufferIndex)

            // Here, you need to send the encodedData to the RTMP server.
            // You can use a networking library like ExoPlayer or Netty for this purpose.

            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
        }
    }

    fun release() {
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null
        isCodecConfigured = false
    }
}

