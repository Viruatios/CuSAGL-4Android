package com.culoo.cusagl_4android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.culoo.cusagl_4android.accessibility.AccessibilityPermission
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainUiEvent
import com.culoo.cusagl_4android.main.MainViewModel
import com.culoo.cusagl_4android.main.ui.MainScreen
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.OverlayPlaybackService
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val openScoreDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importScoreUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CuSAGL4AndroidTheme {
                val uiState by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MainUiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(resolve(event.message))
                            }
                            is MainUiEvent.StartOverlayPlayback -> {
                                OverlayPlaybackService.start(this@MainActivity, event.request)
                            }
                            is MainUiEvent.OpenApkInstaller -> {
                                startApkInstall(event.apkFile)
                            }
                        }
                    }
                }

                BackHandler(enabled = uiState.screenState.page != MainPage.HOME) {
                    viewModel.backPressed()
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    MainScreen(
                        state = uiState.screenState,
                        modifier = Modifier.padding(innerPadding),
                        onOpenScoreManagement = viewModel::openScoreManagement,
                        onOpenPlaybackConfig = viewModel::openPlaybackConfig,
                        onOpenAbout = viewModel::openAbout,
                        onBackHome = viewModel::backHome,
                        onGrantOverlay = {
                            viewModel.markPermissionGuideLeavingApp()
                            startActivity(OverlayPermission.settingsIntent(this))
                        },
                        onGrantAccessibility = {
                            viewModel.markPermissionGuideLeavingApp()
                            startActivity(AccessibilityPermission.settingsIntent())
                        },
                        permissionDialogDismissed = uiState.permissionDialogDismissedInCurrentForeground,
                        onDismissPermissionGuide = viewModel::dismissPermissionGuide,
                        showPreparePlaybackWarningDialog = uiState.showPreparePlaybackWarningDialog,
                        onConfirmPreparePlaybackWarning = viewModel::confirmPreparePlaybackWarning,
                        onDismissPreparePlaybackWarning = viewModel::dismissPreparePlaybackWarning,
                        scoreEntries = uiState.scoreEntries,
                        scoreManagementMessage = uiState.scoreManagementMessage,
                        manualDraft = uiState.manualDraft,
                        pendingOverwriteTitle = uiState.pendingOverwriteTitle,
                        onImportScore = {
                            openScoreDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        onStartCreateScore = viewModel::startCreateScore,
                        onCancelCreateScore = viewModel::cancelManualScoreCreation,
                        onManualDraftChange = viewModel::updateManualDraft,
                        onSaveManualScore = viewModel::saveManualScore,
                        onDeleteScore = viewModel::deleteScore,
                        onConfirmOverwrite = viewModel::confirmPendingOverwrite,
                        onDismissOverwrite = viewModel::dismissOverwrite,
                        playbackDraft = uiState.playbackDraft,
                        playbackConfigMessage = uiState.playbackConfigMessage,
                        onPlaybackDraftChange = viewModel::updatePlaybackDraft,
                        onApplyPlaybackConfig = viewModel::applyPlaybackConfig,
                        aboutState = uiState.aboutState,
                        onCheckUpdate = viewModel::checkForUpdate,
                        onInstallUpdate = viewModel::downloadAndInstallUpdate,
                        onPreload = viewModel::preloadScore,
                        onStartOverlay = viewModel::startOverlayRequested
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    private fun importScoreUri(uri: Uri) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }
            if (text == null) {
                viewModel.importScoreReadFailed()
            } else {
                viewModel.importScoreText(uri.lastPathSegment.orEmpty(), text)
            }
        }
    }

    private fun startApkInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (ex: Exception) {
            viewModel.apkInstallOpenFailed(ex.message)
        }
    }
}
