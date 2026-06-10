package com.culoo.cusagl_4android.main.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.asString
import com.culoo.cusagl_4android.main.AboutUiState

@Composable
fun AboutScreen(
    state: AboutUiState,
    modifier: Modifier = Modifier,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onBackHome: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageTitle(
            title = stringResource(R.string.about_title),
            subtitle = stringResource(R.string.about_subtitle)
        )
        if (state.message != null) {
            MessageText(state.message.asString())
        }
        if (state.errorMessage != null) {
            ErrorText(state.errorMessage.asString())
        }

        SectionCard(stringResource(R.string.about_section_app_info)) {
            Text(stringResource(R.string.about_disclaimer))
            Text(stringResource(R.string.about_current_version, state.currentVersion))
            Text(
                stringResource(R.string.about_repository, state.repositoryUrl),
                modifier = Modifier.clickable {
                    uriHandler.openUri(state.repositoryUrl)
                }
            )
        }

        SectionCard(stringResource(R.string.about_section_update)) {
            Button(
                onClick = onCheckUpdate,
                enabled = !state.isChecking && !state.isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isChecking) {
                        stringResource(R.string.action_checking_update)
                    } else {
                        stringResource(R.string.action_check_update)
                    }
                )
            }
            Button(
                onClick = onInstallUpdate,
                enabled = state.canInstallUpdate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isDownloading) {
                        stringResource(R.string.action_downloading_update)
                    } else {
                        stringResource(R.string.action_download_install_update)
                    }
                )
            }
        }

        if (state.latestTag != null) {
            SectionCard(stringResource(R.string.about_section_latest)) {
                Text(stringResource(R.string.about_latest_version, state.latestTag))
                if (state.releaseUrl != null) {
                    Text(
                        stringResource(R.string.about_release_url, state.releaseUrl),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(state.releaseUrl)
                        }
                    )
                }
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
