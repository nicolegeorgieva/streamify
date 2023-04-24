package com.example.streamify


import android.util.Log
import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtmp.utils.ConnectCheckerRtmp

private const val TWITCH_KEY = "YOUR_TWITCH_KEY"
private const val STREAMING_URL = "rtmp://live.twitch.tv/app/$TWITCH_KEY"

val rtmpClient = RtmpClient(object : ConnectCheckerRtmp {
    override fun onAuthErrorRtmp() {
        Log.i("stream", "onAuthErrorRtmp")
    }

    override fun onAuthSuccessRtmp() {
        Log.i("stream", "onAuthSuccessRtmp")
    }

    override fun onConnectionFailedRtmp(reason: String) {
        Log.i("stream", "onConnectionFailedRtmp: $reason")
    }

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
        Log.i("stream", "onConnectionStartedRtmp: $rtmpUrl")
    }

    override fun onConnectionSuccessRtmp() {
        Log.i("stream", "onConnectionSuccessRtmp")
    }

    override fun onDisconnectRtmp() {
        Log.i("stream", "onDisconnectRtmp")
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        Log.i("stream", "onNewBitrateRtmp: $bitrate")
    }
})

fun startStreaming() {
    rtmpClient.connect(STREAMING_URL)
}

fun stopStreaming() {
    rtmpClient.disconnect()
}
