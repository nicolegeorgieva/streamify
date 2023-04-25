package com.example.streamify


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtmp.utils.ConnectCheckerRtmp

private const val TWITCH_KEY = "YOUR_TWITCH_KEY"
private const val STREAMING_URL = "rtmp://live.twitch.tv/app/$TWITCH_KEY"

var streamingStarted by mutableStateOf(false)

val rtmpClient = RtmpClient(object : ConnectCheckerRtmp {
    override fun onAuthErrorRtmp() {
        Log.i("stream", "onAuthErrorRtmp")
        streamingStarted = false
    }

    override fun onAuthSuccessRtmp() {
        Log.i("stream", "onAuthSuccessRtmp")
    }

    override fun onConnectionFailedRtmp(reason: String) {
        Log.i("stream", "onConnectionFailedRtmp: $reason")
        streamingStarted = false
    }

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
        Log.i("stream", "onConnectionStartedRtmp: $rtmpUrl")
    }

    override fun onConnectionSuccessRtmp() {
        Log.i("stream", "onConnectionSuccessRtmp")
        streamingStarted = true
    }

    override fun onDisconnectRtmp() {
        Log.i("stream", "onDisconnectRtmp")
        streamingStarted = false
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
