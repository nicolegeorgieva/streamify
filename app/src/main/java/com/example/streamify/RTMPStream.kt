package com.example.streamify


import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtmp.utils.ConnectCheckerRtmp

private const val STREAMING_URL = ""

val rtmpClient = RtmpClient(object : ConnectCheckerRtmp {
    override fun onAuthErrorRtmp() {
    }

    override fun onAuthSuccessRtmp() {
    }

    override fun onConnectionFailedRtmp(reason: String) {
    }

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
    }

    override fun onConnectionSuccessRtmp() {
    }

    override fun onDisconnectRtmp() {
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
    }
})

fun startStreaming() {
    rtmpClient.connect(STREAMING_URL)
}

fun stopStreaming() {
    rtmpClient.disconnect()
}
