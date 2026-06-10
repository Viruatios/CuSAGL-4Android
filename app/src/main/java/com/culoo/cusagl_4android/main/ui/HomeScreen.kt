package com.culoo.cusagl_4android.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.asString
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.PermissionGuideAction
import com.culoo.cusagl_4android.main.PermissionGuideController

@Composable
fun HomeScreen(
    state: MainScreenState,
    modifier: Modifier = Modifier,
    onOpenScoreManagement: () -> Unit,
    onOpenPlaybackConfig: () -> Unit,
    onOpenAbout: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onPreload: () -> Unit,
    onStartOverlay: () -> Unit
) {
    val permissionItems = PermissionGuideController.permissionItems(state)
    val blockers = PermissionGuideController.preparationBlockers(state)
    val aboutDescription = stringResource(R.string.content_description_about)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            PageTitle(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(R.string.home_subtitle),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = aboutDescription }
            ) {
                ThreeDotMenuIcon()
            }
        }
        if (state.errorMessage != null) {
            ErrorText(stringResource(R.string.error_prefix, state.errorMessage.asString()))
        }

        SectionCard(stringResource(R.string.home_section_playback_ready)) {
            Text(stringResource(R.string.home_current_score, state.firstScoreName ?: stringResource(R.string.home_no_score)))
            Text(stringResource(R.string.home_playback_config, state.playbackConfigSummary.asString()))
            Text(stringResource(R.string.home_queue_size, state.playbackQueueSize))
            Text(
                if (state.isCacheReady) {
                    stringResource(R.string.home_queue_cache_ready)
                } else {
                    stringResource(R.string.home_queue_cache_not_ready)
                }
            )
        }

        SectionCard(stringResource(R.string.home_section_permissions)) {
            if (permissionItems.isEmpty()) {
                Text(stringResource(R.string.home_permissions_ready))
            } else {
                permissionItems.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title.asString(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = item.description.asString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = when (item.action) {
                                PermissionGuideAction.OVERLAY -> onGrantOverlay
                                PermissionGuideAction.ACCESSIBILITY -> onGrantAccessibility
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item.buttonLabel.asString())
                        }
                    }
                }
            }
        }

        SectionCard(stringResource(R.string.home_section_actions)) {
            OutlinedButton(
                onClick = onOpenScoreManagement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_score_management))
            }
            OutlinedButton(
                onClick = onOpenPlaybackConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_playback_config))
            }
            Button(
                onClick = onPreload,
                enabled = state.canPreload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isLoading) {
                        stringResource(R.string.action_preloading)
                    } else {
                        stringResource(R.string.action_preload_score)
                    }
                )
            }
            Button(
                onClick = onStartOverlay,
                enabled = state.canPreparePlayback,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_prepare_playback))
            }
        }

        if (blockers.isNotEmpty()) {
            SectionCard(stringResource(R.string.home_section_prepare_needs)) {
                blockers.forEach { reason ->
                    Text(reason.asString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
