package com.example.streamify

import android.media.MediaCodec
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pedro.rtmp.rtmp.RtmpClient
import java.nio.ByteBuffer

class RtmpStreamingAnalyzer(private val rtmpClient: RtmpClient) : ImageAnalysis.Analyzer {

    private val TAG = "RtmpStreamingAnalyzer"

    override fun analyze(image: ImageProxy) {
        if (rtmpClient.isStreaming) {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val yuv420 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(yuv420, 0, ySize)
            uBuffer.get(yuv420, ySize, uSize)
            vBuffer.get(yuv420, ySize + uSize, vSize)

            val presentationTimeUs = image.imageInfo.timestamp / 1000
            val bufferInfo = MediaCodec.BufferInfo()
            bufferInfo.presentationTimeUs = presentationTimeUs
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME

            rtmpClient.sendVideo(ByteBuffer.wrap(yuv420), bufferInfo)
        }
        image.close()
    }

}
