package com.culoo.cusagl_4android.main.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    QueueScoreSelector(
                        entries = entries,
                        queueText = draft.queueText,
                        onQueueTextChange = { onDraftChange(draft.copy(queueText = it)) }
                    )
                } else {
                    SingleScoreSelector(
                        entries = entries,
                        selectedScoreName = draft.selectedScoreName,
                        onSelectedScoreChange = { onDraftChange(draft.copy(selectedScoreName = it)) }
                    )
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
private fun SingleScoreSelector(
    entries: List<ScoreEntry>,
    selectedScoreName: String,
    onSelectedScoreChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = entries,
            key = { it.storageName }
        ) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = selectedScoreName == entry.storageName,
                    onClick = { onSelectedScoreChange(entry.storageName) }
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

@Composable
private fun QueueScoreSelector(
    entries: List<ScoreEntry>,
    queueText: String,
    onQueueTextChange: (String) -> Unit
) {
    val queueState = remember(entries, queueText) {
        deriveQueueSelection(entries, queueText)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.playback_queue_selector_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = entries,
                key = { it.storageName }
            ) { entry ->
                val order = queueState.orderByName[entry.storageName]
                QueueScoreRow(
                    entry = entry,
                    order = order,
                    onToggle = {
                        val next = if (order == null) {
                            queueState.selectedNames + entry.storageName
                        } else {
                            queueState.selectedNames - entry.storageName
                        }
                        onQueueTextChange(queueTextFromSelected(next, queueState.indexByName))
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueScoreRow(
    entry: ScoreEntry,
    order: Int?,
    onToggle: () -> Unit
) {
    val selected = order != null
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "queue-row-container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "queue-row-content"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() }
            )
            OrderBadge(order = order)
            Column {
                Text(entry.title, color = contentColor, style = MaterialTheme.typography.bodyLarge)
                Text(
                    entry.storageName,
                    color = if (selected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OrderBadge(order: Int?) {
    val badgeColor by animateColorAsState(
        targetValue = if (order == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
        label = "queue-order-badge"
    )
    val textColor by animateColorAsState(
        targetValue = if (order == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
        label = "queue-order-text"
    )
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clip(MaterialTheme.shapes.small),
        color = badgeColor,
        shape = MaterialTheme.shapes.small
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = order?.toString() ?: stringResource(R.string.playback_queue_unselected),
                color = textColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

internal data class QueueSelectionState(
    val selectedNames: List<String>,
    val orderByName: Map<String, Int>,
    val indexByName: Map<String, Int>
)

internal fun deriveQueueSelection(entries: List<ScoreEntry>, queueText: String): QueueSelectionState {
    val nameByPrefix = entries.associateBy { it.storageName.substringBefore('.') }
    val indexByName = entries.withIndex().associate { (index, entry) -> entry.storageName to index + 1 }
    val result = linkedSetOf<String>()
    queueText.trim().split(Regex("\\s+")).forEach { raw ->
        if (raw.isBlank()) return@forEach
        val index = raw.toIntOrNull()
        if (index == null || index <= 0) return@forEach
        val prefix = index.toString().padStart(4, '0')
        val matched = nameByPrefix[prefix]
        if (matched != null) result.add(matched.storageName)
    }
    val selectedNames = result.toList()
    return QueueSelectionState(
        selectedNames = selectedNames,
        orderByName = selectedNames.withIndex().associate { (index, name) -> name to index + 1 },
        indexByName = indexByName
    )
}

internal fun queueTextFromSelected(
    selectedNames: List<String>,
    indexByName: Map<String, Int>
): String {
    return selectedNames.mapNotNull { selected ->
        indexByName[selected]?.toString()
    }.joinToString(" ")
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
