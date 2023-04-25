package com.example.streamify

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pedro.rtmp.rtmp.RtmpClient
import java.nio.ByteBuffer

class RtmpStreamingAnalyzer(private val rtmpClient: RtmpClient) : ImageAnalysis.Analyzer {

    private val encoder: MediaCodec

    init {
        encoder = configureEncoder(1280, 720)
        encoder.start()
    }

    private fun configureEncoder(width: Int, height: Int): MediaCodec {
        val codecName = "video/avc"
        val codec = MediaCodec.createEncoderByType(codecName)

        val format = MediaFormat.createVideoFormat(codecName, width, height)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4000000) // Adjust bitrate as needed
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30) // Adjust frame rate as needed
        format.setInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL,
            2
        ) // Adjust key frame interval as needed

        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return codec
    }

    override fun analyze(image: ImageProxy) {
        if (rtmpClient.isStreaming) {
            val inputIndex = encoder.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputIndex)
                inputBuffer?.clear()

                // Convert ImageProxy to ByteBuffer in YUV420 format
                val yuvBuffer = imageProxyToYUV420ByteBuffer(image)
                inputBuffer?.put(yuvBuffer)

                val presentationTimeUs = image.imageInfo.timestamp / 1000
                encoder.queueInputBuffer(
                    inputIndex,
                    0,
                    yuvBuffer.remaining(),
                    presentationTimeUs,
                    0
                )
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)

            while (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                outputBuffer?.position(bufferInfo.offset)
                outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)

                val dataSize = bufferInfo.size + 4
                val dataWithStartCode = ByteArray(dataSize)
                dataWithStartCode[0] = 0x00
                dataWithStartCode[1] = 0x00
                dataWithStartCode[2] = 0x00
                dataWithStartCode[3] = 0x01
                outputBuffer?.get(dataWithStartCode, 4, bufferInfo.size)
                rtmpClient.sendVideo(ByteBuffer.wrap(dataWithStartCode), bufferInfo)

                encoder.releaseOutputBuffer(outputIndex, false)
                outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            }
        }
        image.close()
    }


    private fun imageProxyToYUV420ByteBuffer(image: ImageProxy): ByteBuffer {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteBuffer.allocateDirect(ySize + uSize + vSize)

        yBuffer.get(nv21.array(), 0, ySize)

        val chromaRowStride = image.planes[1].rowStride
        val chromaPixelStride = image.planes[1].pixelStride

        var yPos = ySize
        for (i in 0 until image.height step 2) {
            for (j in 0 until chromaRowStride step chromaPixelStride * 2) {
                if (yPos + 1 < nv21.capacity() && i * chromaRowStride + j < uSize && i * chromaRowStride + j < vSize) {
                    nv21.put(yPos++, uBuffer.get(i * chromaRowStride + j))
                    nv21.put(yPos++, vBuffer.get(i * chromaRowStride + j))
                }
            }
        }

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        return nv21
    }

}

