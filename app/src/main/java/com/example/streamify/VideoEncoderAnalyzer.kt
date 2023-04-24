package com.example.streamify

import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

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
                setInteger(MediaFormat.KEY_BIT_RATE, 2500 * 1000) // 2.5 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2) // 2 second
            }

        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec?.start()

        isCodecConfigured = true
    }

    private fun getYUV420Planar(image: Image): ByteBuffer {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val yuvBytes = ByteArray(ySize + uSize + vSize)

        yBuffer.get(yuvBytes, 0, ySize)

        val chromaSize = uSize + vSize
        val chromaWidth = image.width / 2
        val chromaHeight = image.height / 2

        for (j in 0 until chromaHeight) {
            for (i in 0 until chromaWidth) {
                yuvBytes[ySize + j * chromaWidth + i] = uBuffer.get()
                uBuffer.position(uBuffer.position() + uPlane.pixelStride - 1)

                yuvBytes[ySize + chromaSize / 2 + j * chromaWidth + i] = vBuffer.get()
                vBuffer.position(vBuffer.position() + vPlane.pixelStride - 1)
            }
            if (j < chromaHeight - 1) {
                uBuffer.position(
                    uBuffer.position() -
                            chromaWidth * (uPlane.pixelStride - 1) + uPlane.rowStride -
                            chromaWidth * uPlane.pixelStride
                )
                vBuffer.position(
                    vBuffer.position() -
                            chromaWidth * (vPlane.pixelStride - 1) + vPlane.rowStride -
                            chromaWidth * vPlane.pixelStride
                )
            }
        }

        return ByteBuffer.wrap(yuvBytes)
    }

    private fun encodeFrame(input: ByteBuffer) {
        val bufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: -1
        Log.i("ddq", "bufferIndex: $bufferIndex")

        if (bufferIndex >= 0) {
            val inputBuffer = mediaCodec?.getInputBuffer(bufferIndex)
            inputBuffer?.clear()

            val bytesToCopy = minOf(inputBuffer?.remaining() ?: 0, input.remaining())
            if (bytesToCopy > 0) {
                val tempBuffer = ByteArray(bytesToCopy)
                input.get(tempBuffer)
                inputBuffer?.put(tempBuffer)
            }

            mediaCodec?.queueInputBuffer(
                bufferIndex,
                0,
                bytesToCopy,
                System.nanoTime() / 1000, // presentationTimeUs
                0
            )
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1

        Log.i("ddq", "outputBufferIndex: $outputBufferIndex")

        while (outputBufferIndex >= 0) {
            val encodedData = mediaCodec?.getOutputBuffer(outputBufferIndex)

            Log.i("ddq", "Encoded data: $encodedData")
            // Send the encodedData to the RTMP server using RtmpClient
            if (encodedData != null && bufferInfo.size > 0) {
                val frameData = ByteArray(bufferInfo.size)
                encodedData.get(frameData)
                rtmpClient.sendVideo(ByteBuffer.wrap(frameData), bufferInfo)
            }

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

