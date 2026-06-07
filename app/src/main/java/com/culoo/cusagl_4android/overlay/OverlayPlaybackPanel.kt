package com.culoo.cusagl_4android.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.core.PlaybackSnapshot
import com.culoo.cusagl_4android.core.PlaybackState

@Composable
fun OverlayPlaybackPanel(
    modifier: Modifier = Modifier,
    snapshot: PlaybackSnapshot,
    onDrag: (Float, Float) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val dragModifier = if (snapshot.canMoveOverlay) {
        Modifier.pointerInput(snapshot.state) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x, dragAmount.y)
            }
        }
    } else {
        Modifier
    }
    val panelAlpha = if (snapshot.state == PlaybackState.PLAYING) 0.5f else 0.94f

    Surface(
        modifier = modifier
            .then(dragModifier)
            .alpha(panelAlpha),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = snapshot.currentTrackName ?: "未选择曲谱",
                style = MaterialTheme.typography.labelLarge
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (snapshot.state == PlaybackState.PLAYING) {
                        Button(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("暂停")
                        }
                    } else {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("开始")
                        }
                    }
                    TextButton(
                        onClick = onStop,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("停止")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = onPrevious,
                        enabled = snapshot.canPrevious,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("上一首")
                    }
                    TextButton(
                        onClick = onNext,
                        enabled = snapshot.canNext,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("下一首")
                    }
                }
                Text(
                    text = stateLabel(snapshot.state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun stateLabel(state: PlaybackState): String = when (state) {
    PlaybackState.IDLE -> "等待开始"
    PlaybackState.PLAYING -> "正在演奏"
    PlaybackState.PAUSED -> "已暂停"
    PlaybackState.STOPPED -> "已停止"
}
