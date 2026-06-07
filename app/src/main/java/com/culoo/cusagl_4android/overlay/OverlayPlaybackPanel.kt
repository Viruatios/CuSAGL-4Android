package com.culoo.cusagl_4android.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    Surface(
        modifier = modifier.then(dragModifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = snapshot.currentTrackName ?: "未选择曲谱",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stateLabel(snapshot.state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            snapshot.lastError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (snapshot.state == PlaybackState.PLAYING) {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("暂停")
                        }
                    } else {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("开始")
                        }
                    }
                    TextButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("停止")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onPrevious,
                        enabled = snapshot.canPrevious,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("上一首")
                    }
                    TextButton(
                        onClick = onNext,
                        enabled = snapshot.canNext,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下一首")
                    }
                }
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
