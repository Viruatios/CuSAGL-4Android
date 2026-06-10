package com.culoo.cusagl_4android.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.asString
import com.culoo.cusagl_4android.main.AboutUiState
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.ManualScoreDraft
import com.culoo.cusagl_4android.main.PermissionGuideAction
import com.culoo.cusagl_4android.main.PermissionGuideController
import com.culoo.cusagl_4android.main.PlaybackConfigDraft
import com.culoo.cusagl_4android.main.ScoreEntry

@Composable
fun MainScreen(
    state: MainScreenState,
    modifier: Modifier = Modifier,
    onOpenScoreManagement: () -> Unit,
    onOpenPlaybackConfig: () -> Unit,
    onOpenAbout: () -> Unit,
    onBackHome: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    permissionDialogDismissed: Boolean,
    onDismissPermissionGuide: () -> Unit,
    scoreEntries: List<ScoreEntry>,
    scoreManagementMessage: UiText?,
    manualDraft: ManualScoreDraft,
    pendingOverwriteTitle: String?,
    playbackDraft: PlaybackConfigDraft,
    playbackConfigMessage: UiText?,
    onImportScore: () -> Unit,
    onStartCreateScore: () -> Unit,
    onCancelCreateScore: () -> Unit,
    onManualDraftChange: (ManualScoreDraft) -> Unit,
    onSaveManualScore: () -> Unit,
    onDeleteScore: (String) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onDismissOverwrite: () -> Unit,
    onPlaybackDraftChange: (PlaybackConfigDraft) -> Unit,
    onApplyPlaybackConfig: () -> Unit,
    aboutState: AboutUiState,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onPreload: () -> Unit,
    onStartOverlay: () -> Unit
) {
    if (pendingOverwriteTitle != null) {
        AlertDialog(
            onDismissRequest = onDismissOverwrite,
            title = { Text(stringResource(R.string.dialog_overwrite_title)) },
            text = { Text(stringResource(R.string.dialog_overwrite_body, pendingOverwriteTitle)) },
            confirmButton = {
                TextButton(onClick = onConfirmOverwrite) {
                    Text(stringResource(R.string.action_overwrite))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOverwrite) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    if (PermissionGuideController.shouldShowPermissionDialog(state, permissionDialogDismissed)) {
        PermissionGuideDialog(
            state = state,
            onGrantOverlay = onGrantOverlay,
            onGrantAccessibility = onGrantAccessibility,
            onDismiss = onDismissPermissionGuide
        )
    }

    when (state.page) {
        MainPage.HOME -> HomeScreen(
            state = state,
            modifier = modifier,
            onOpenScoreManagement = onOpenScoreManagement,
            onOpenPlaybackConfig = onOpenPlaybackConfig,
            onOpenAbout = onOpenAbout,
            onGrantOverlay = onGrantOverlay,
            onGrantAccessibility = onGrantAccessibility,
            onPreload = onPreload,
            onStartOverlay = onStartOverlay
        )
        MainPage.SCORE_MANAGEMENT -> ScoreManagementScreen(
            entries = scoreEntries,
            message = scoreManagementMessage,
            modifier = modifier,
            onImportScore = onImportScore,
            onStartCreate = onStartCreateScore,
            onDeleteScore = onDeleteScore,
            onBackHome = onBackHome
        )
        MainPage.MANUAL_SCORE_CREATE -> ManualScoreCreateScreen(
            message = scoreManagementMessage,
            draft = manualDraft,
            modifier = modifier,
            onDraftChange = onManualDraftChange,
            onSave = onSaveManualScore,
            onCancel = onCancelCreateScore
        )
        MainPage.PLAYBACK_CONFIG -> PlaybackConfigScreen(
            entries = scoreEntries,
            draft = playbackDraft,
            message = playbackConfigMessage,
            modifier = modifier,
            onDraftChange = onPlaybackDraftChange,
            onApply = onApplyPlaybackConfig,
            onBackHome = onBackHome
        )
        MainPage.ABOUT -> AboutScreen(
            state = aboutState,
            modifier = modifier,
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onBackHome = onBackHome
        )
    }
}

@Composable
private fun PermissionGuideDialog(
    state: MainScreenState,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onDismiss: () -> Unit
) {
    val items = PermissionGuideController.permissionItems(state)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_permission_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.dialog_permission_body))
                items.forEach { item ->
                    Text(stringResource(R.string.dialog_permission_item, item.title.asString(), item.description.asString()))
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (items.any { it.action == PermissionGuideAction.OVERLAY }) {
                    TextButton(onClick = onGrantOverlay) {
                        Text(itemButtonLabel(items, PermissionGuideAction.OVERLAY))
                    }
                }
                if (items.any { it.action == PermissionGuideAction.ACCESSIBILITY }) {
                    TextButton(onClick = onGrantAccessibility) {
                        Text(itemButtonLabel(items, PermissionGuideAction.ACCESSIBILITY))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_later))
            }
        }
    )
}

@Composable
private fun itemButtonLabel(items: List<com.culoo.cusagl_4android.main.PermissionGuideItem>, action: PermissionGuideAction): String {
    return items.first { it.action == action }.buttonLabel.asString()
}
