package com.culoo.cusagl_4android.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.asString
import com.culoo.cusagl_4android.main.PlaybackConfigDraft
import com.culoo.cusagl_4android.main.PlaybackConfigMode
import com.culoo.cusagl_4android.main.ScoreEntry

@Composable
fun PlaybackConfigScreen(
    entries: List<ScoreEntry>,
    draft: PlaybackConfigDraft,
    message: UiText?,
    modifier: Modifier = Modifier,
    onDraftChange: (PlaybackConfigDraft) -> Unit,
    onApply: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle(
            title = stringResource(R.string.playback_config_title),
            subtitle = stringResource(R.string.playback_config_subtitle)
        )
        if (message != null) {
            MessageText(message.asString())
        }
        Text(
            stringResource(R.string.playback_available_scores, entries.size),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.playback_section_mode), style = MaterialTheme.typography.titleMedium)
                PlaybackConfigMode.allModes.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = draft.mode == mode,
                            onClick = { onDraftChange(draft.copy(mode = mode)) }
                        )
                        Text(playbackModeLabel(mode))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.playback_section_score), style = MaterialTheme.typography.titleMedium)
                if (entries.isEmpty()) {
                    Text(stringResource(R.string.playback_no_scores))
                } else if (draft.mode.isQueueMode()) {
                    OutlinedTextField(
                        value = draft.queueText,
                        onValueChange = { onDraftChange(draft.copy(queueText = it)) },
                        label = { Text(stringResource(R.string.field_queue_indexes)) },
                        placeholder = { Text(stringResource(R.string.placeholder_queue_indexes)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = draft.selectedScoreName == entry.storageName,
                                onClick = { onDraftChange(draft.copy(selectedScoreName = entry.storageName)) }
                            )
                            Column {
                                Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    entry.storageName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.playback_section_timing), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = draft.startTimeText,
                    onValueChange = { onDraftChange(draft.copy(startTimeText = it)) },
                    label = { Text(stringResource(R.string.field_start_time)) },
                    placeholder = { Text(stringResource(R.string.placeholder_start_time)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.queueIntervalSeconds,
                    onValueChange = { onDraftChange(draft.copy(queueIntervalSeconds = it)) },
                    label = { Text(stringResource(R.string.field_queue_interval)) },
                    enabled = draft.mode.isQueueMode(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.repeatTimes,
                    onValueChange = { onDraftChange(draft.copy(repeatTimes = it)) },
                    label = { Text(stringResource(R.string.field_repeat_times)) },
                    enabled = draft.mode.isRepeatMode(),
                    placeholder = { Text(stringResource(R.string.placeholder_repeat_times)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.repeatIntervalSeconds,
                    onValueChange = { onDraftChange(draft.copy(repeatIntervalSeconds = it)) },
                    label = { Text(stringResource(R.string.field_repeat_interval)) },
                    enabled = draft.mode.isRepeatMode(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(
                    checked = draft.debugEnabled,
                    onCheckedChange = { onDraftChange(draft.copy(debugEnabled = it)) }
                )
                Column {
                    Text(stringResource(R.string.playback_debug_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.playback_debug_body),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save_apply))
            }
            OutlinedButton(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_back_home))
            }
        }
    }
}

@Composable
private fun playbackModeLabel(mode: PlaybackConfigMode): String {
    return when (mode) {
        PlaybackConfigMode.SINGLE_ONCE -> stringResource(PlaybackConfigMode.SINGLE_ONCE.labelResId)
        PlaybackConfigMode.SINGLE_REPEAT -> stringResource(PlaybackConfigMode.SINGLE_REPEAT.labelResId)
        PlaybackConfigMode.QUEUE_ONCE -> stringResource(PlaybackConfigMode.QUEUE_ONCE.labelResId)
        PlaybackConfigMode.QUEUE_REPEAT -> stringResource(PlaybackConfigMode.QUEUE_REPEAT.labelResId)
    }
}
