package com.example.streamify

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView

/**
 * A screen that shows a preview of a video.
 * @param uri The URI of the video to preview.
 */
@Composable
fun VideoPreviewScreen(
    uri: String,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }

    DisposableEffect(
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { context ->
                    StyledPlayerView(context).apply {
                        player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    ) {
        onDispose {
            exoPlayer.release()
        }
    }

    Spacer(modifier = Modifier.padding(top = 16.dp))

    IconButton(
        onClick = onShareClick
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_share_24),
            contentDescription = "",
            modifier = Modifier.size(52.dp),
            tint = Color.White
        )
    }

    Button(
        onClick = onShareClick,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(text = stringResource(id = R.string.share_video))
    }
}
