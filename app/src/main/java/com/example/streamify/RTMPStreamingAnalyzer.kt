package com.example.streamify

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaCodec
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pedro.rtmp.rtmp.RtmpClient
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class RtmpStreamingAnalyzer(private val rtmpClient: RtmpClient) : ImageAnalysis.Analyzer {

    private fun imageToByteArray(image: ImageProxy): ByteArray? {
        val nv21Buffer = yuv420ThreePlanesToNV21(
            image.planes[0].buffer,
            image.planes[1].buffer,
            image.planes[2].buffer,
            image.width,
            image.height
        )
        val yuvImage = YuvImage(nv21Buffer, ImageFormat.NV21, image.width, image.height, null)
        val outputStream = ByteArrayOutputStream()
        val rect = Rect(0, 0, yuvImage.width, yuvImage.height)
        yuvImage.compressToJpeg(rect, 70, outputStream)
        return outputStream.toByteArray()
    }

    private fun yuv420ThreePlanesToNV21(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer,
        width: Int,
        height: Int
    ): ByteArray {
        val nv21 = ByteArray(width * height * 3 / 2)

        yBuffer.get(nv21, 0, width * height)

        val chromaRowStride = uBuffer.remaining() / height

        for (i in 0 until height step 2) {
            for (j in 0 until chromaRowStride step 2) {
                nv21[width * height + i * chromaRowStride + j] =
                    uBuffer.get(i * chromaRowStride + j)
                nv21[width * height + i * chromaRowStride + j + 1] =
                    vBuffer.get(i * chromaRowStride + j)
            }
        }
        return nv21
    }

    override fun analyze(image: ImageProxy) {
        if (rtmpClient.isStreaming) {
            val frame = imageToByteArray(image)
            frame?.let {
                val bufferInfo = MediaCodec.BufferInfo()
                bufferInfo.presentationTimeUs = image.imageInfo.timestamp / 1000
                bufferInfo.flags = 0
                bufferInfo.size = it.size
                bufferInfo.offset = 0

                rtmpClient.sendVideo(ByteBuffer.wrap(it), bufferInfo)
            }
        }
        image.close()
    }
}
