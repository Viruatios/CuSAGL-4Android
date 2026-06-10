package com.culoo.cusagl_4android.main.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.asString
import com.culoo.cusagl_4android.main.ManualScoreDraft
import com.culoo.cusagl_4android.main.ScoreEntry

@Composable
fun ScoreManagementScreen(
    entries: List<ScoreEntry>,
    message: UiText?,
    modifier: Modifier = Modifier,
    onImportScore: () -> Unit,
    onStartCreate: () -> Unit,
    onDeleteScore: (String) -> Unit,
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
            title = stringResource(R.string.score_management_title),
            subtitle = stringResource(R.string.score_management_subtitle)
        )
        if (message != null) {
            MessageText(message.asString())
        }

        SectionCard(stringResource(R.string.score_section_source)) {
            Button(
                onClick = onImportScore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_import_json))
            }
            OutlinedButton(
                onClick = onStartCreate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_create_score))
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.score_stored_title), style = MaterialTheme.typography.titleMedium)

        if (entries.isEmpty()) {
            MessageText(stringResource(R.string.score_empty))
        } else {
            entries.forEach { entry ->
                ScoreEntryCard(
                    entry = entry,
                    onDelete = { onDeleteScore(entry.storageName) }
                )
            }
        }

        OutlinedButton(
            onClick = onBackHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_back_home))
        }
    }
}

@Composable
fun ManualScoreCreateScreen(
    message: UiText?,
    draft: ManualScoreDraft,
    modifier: Modifier = Modifier,
    onDraftChange: (ManualScoreDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle(
            title = stringResource(R.string.manual_score_title),
            subtitle = stringResource(R.string.manual_score_subtitle)
        )
        if (message != null) {
            MessageText(message.asString())
        }
        ManualScoreForm(
            draft = draft,
            onDraftChange = onDraftChange,
            onSave = onSave,
            onCancel = onCancel
        )
    }
}

@Composable
private fun ManualScoreForm(
    draft: ManualScoreDraft,
    onDraftChange: (ManualScoreDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.manual_score_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.field_score_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.bpm,
                onValueChange = { onDraftChange(draft.copy(bpm = it)) },
                label = { Text(stringResource(R.string.field_bpm)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.timeSignature,
                onValueChange = { onDraftChange(draft.copy(timeSignature = it)) },
                label = { Text(stringResource(R.string.field_time_signature)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.author,
                onValueChange = { onDraftChange(draft.copy(author = it)) },
                label = { Text(stringResource(R.string.field_author)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.instrument,
                onValueChange = { onDraftChange(draft.copy(instrument = it)) },
                label = { Text(stringResource(R.string.field_instrument)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onDraftChange(draft.copy(description = it)) },
                label = { Text(stringResource(R.string.field_description)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.composer,
                onValueChange = { onDraftChange(draft.copy(composer = it)) },
                label = { Text(stringResource(R.string.field_composer)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.arranger,
                onValueChange = { onDraftChange(draft.copy(arranger = it)) },
                label = { Text(stringResource(R.string.field_arranger)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onDraftChange(draft.copy(notes = it)) },
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_save))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun ScoreEntryCard(
    entry: ScoreEntry,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.score_file_name, entry.storageName),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (entry.hasCache) {
                    stringResource(R.string.score_cache_ready)
                } else {
                    stringResource(R.string.score_cache_missing)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}
