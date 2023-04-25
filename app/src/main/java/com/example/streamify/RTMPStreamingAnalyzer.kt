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
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1200 * 1024)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)

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

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // Extract and send SPS and PPS
                    val csd = ByteArray(bufferInfo.size)
                    outputBuffer?.get(csd)
                    rtmpClient.sendVideo(ByteBuffer.wrap(csd), bufferInfo)
                } else {
                    // Send the encoded video frame
                    rtmpClient.sendVideo(outputBuffer!!, bufferInfo)
                }

                encoder.releaseOutputBuffer(outputIndex, false)
                outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
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

        val chromaRowStride = uBuffer.remaining() / image.height

        for (i in 0 until image.height step 2) {
            for (j in 0 until chromaRowStride step 2) {
                nv21.put(uBuffer.get(i * chromaRowStride + j))
                nv21.put(vBuffer.get(i * chromaRowStride + j))
            }
        }

        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        return nv21
    }

}
