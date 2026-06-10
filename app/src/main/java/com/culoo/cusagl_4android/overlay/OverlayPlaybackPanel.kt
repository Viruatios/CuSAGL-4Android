package com.culoo.cusagl_4android.overlay

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
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
    val isPlaying = snapshot.state == PlaybackState.PLAYING
    val panelAlpha = if (isPlaying) 0.5f else 0.94f
    val configuration = LocalConfiguration.current
    val useWideLandscapeLayout = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp >= OverlayConstants.WIDE_LANDSCAPE_MIN_WIDTH_DP
    val panelModifier = modifier
        .then(dragModifier)
        .alpha(panelAlpha)

    if (isPlaying) {
        Box(modifier = panelModifier) {
            OverlayPlaybackPanelContent(
                snapshot = snapshot,
                useWideLandscapeLayout = useWideLandscapeLayout,
                onStart = onStart,
                onPause = onPause,
                onStop = onStop,
                onPrevious = onPrevious,
                onNext = onNext
            )
        }
    } else {
        Surface(
            modifier = panelModifier,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            OverlayPlaybackPanelContent(
                snapshot = snapshot,
                useWideLandscapeLayout = useWideLandscapeLayout,
                onStart = onStart,
                onPause = onPause,
                onStop = onStop,
                onPrevious = onPrevious,
                onNext = onNext
            )
        }
    }
}

@Composable
private fun OverlayPlaybackPanelContent(
    snapshot: PlaybackSnapshot,
    useWideLandscapeLayout: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (useWideLandscapeLayout) {
        WideLandscapePanelContent(
            snapshot = snapshot,
            onStart = onStart,
            onPause = onPause,
            onStop = onStop,
            onPrevious = onPrevious,
            onNext = onNext
        )
    } else {
        CompactPanelContent(
            snapshot = snapshot,
            onStart = onStart,
            onPause = onPause,
            onStop = onStop,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

@Composable
private fun CompactPanelContent(
    snapshot: PlaybackSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TrackNameText(snapshot.currentTrackName)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StartPauseButton(
                    state = snapshot.state,
                    modifier = Modifier.weight(1f),
                    onStart = onStart,
                    onPause = onPause
                )
                StopButton(
                    modifier = Modifier.weight(1f),
                    onStop = onStop
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PreviousButton(
                    enabled = snapshot.canPrevious,
                    modifier = Modifier.weight(1f),
                    onPrevious = onPrevious
                )
                NextButton(
                    enabled = snapshot.canNext,
                    modifier = Modifier.weight(1f),
                    onNext = onNext
                )
            }
            StatusText(snapshot.state)
        }
    }
}

@Composable
private fun WideLandscapePanelContent(
    snapshot: PlaybackSnapshot,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.widthIn(min = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackNameText(
                trackName = snapshot.currentTrackName,
                modifier = Modifier.weight(1f)
            )
            StatusText(
                state = snapshot.state,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.widthIn(min = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            StartPauseButton(
                state = snapshot.state,
                modifier = Modifier.weight(1f),
                onStart = onStart,
                onPause = onPause
            )
            StopButton(
                modifier = Modifier.weight(1f),
                onStop = onStop
            )
            PreviousButton(
                enabled = snapshot.canPrevious,
                modifier = Modifier.weight(1f),
                onPrevious = onPrevious
            )
            NextButton(
                enabled = snapshot.canNext,
                modifier = Modifier.weight(1f),
                onNext = onNext
            )
        }
    }
}

@Composable
private fun TrackNameText(trackName: String?, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = trackName ?: stringResource(R.string.overlay_panel_no_score),
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
private fun StatusText(state: PlaybackState, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stateLabel(state),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StartPauseButton(
    state: PlaybackState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onPause: () -> Unit
) {
    if (state == PlaybackState.PLAYING) {
        Button(
            onClick = onPause,
            modifier = modifier.height(COMPACT_BUTTON_HEIGHT),
            contentPadding = PRIMARY_BUTTON_PADDING
        ) {
            Text(stringResource(R.string.overlay_action_pause), style = MaterialTheme.typography.labelMedium)
        }
    } else {
        Button(
            onClick = onStart,
            modifier = modifier.height(COMPACT_BUTTON_HEIGHT),
            contentPadding = PRIMARY_BUTTON_PADDING
        ) {
            Text(stringResource(R.string.overlay_action_start), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StopButton(modifier: Modifier = Modifier, onStop: () -> Unit) {
    TextButton(
        onClick = onStop,
        modifier = modifier.height(COMPACT_BUTTON_HEIGHT),
        contentPadding = SECONDARY_BUTTON_PADDING
    ) {
        Text(stringResource(R.string.overlay_action_exit), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PreviousButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit
) {
    TextButton(
        onClick = onPrevious,
        enabled = enabled,
        modifier = modifier.height(COMPACT_BUTTON_HEIGHT),
        contentPadding = SECONDARY_BUTTON_PADDING
    ) {
        Text(stringResource(R.string.overlay_action_previous), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NextButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onNext: () -> Unit
) {
    TextButton(
        onClick = onNext,
        enabled = enabled,
        modifier = modifier.height(COMPACT_BUTTON_HEIGHT),
        contentPadding = SECONDARY_BUTTON_PADDING
    ) {
        Text(stringResource(R.string.overlay_action_next), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun stateLabel(state: PlaybackState): String = when (state) {
    PlaybackState.IDLE -> stringResource(R.string.overlay_state_idle)
    PlaybackState.PLAYING -> stringResource(R.string.overlay_state_playing)
    PlaybackState.PAUSED -> stringResource(R.string.overlay_state_paused)
    PlaybackState.STOPPED -> stringResource(R.string.overlay_state_stopped)
}

private val COMPACT_BUTTON_HEIGHT = 28.dp
private val PRIMARY_BUTTON_PADDING = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
private val SECONDARY_BUTTON_PADDING = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
