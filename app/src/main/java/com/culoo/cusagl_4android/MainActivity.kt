package com.culoo.cusagl_4android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.accessibility.AccessibilityPermission
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.main.AboutController
import com.culoo.cusagl_4android.main.AboutUiState
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainScreenController
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.ManualScoreDraft
import com.culoo.cusagl_4android.main.PlaybackConfigApplyResult
import com.culoo.cusagl_4android.main.PlaybackConfigController
import com.culoo.cusagl_4android.main.PlaybackConfigDraft
import com.culoo.cusagl_4android.main.PreloadResult
import com.culoo.cusagl_4android.main.ScoreDeleteResult
import com.culoo.cusagl_4android.main.ScoreEntry
import com.culoo.cusagl_4android.main.ScoreManagementController
import com.culoo.cusagl_4android.main.ScoreSaveResult
import com.culoo.cusagl_4android.main.UpdateCheckResult
import com.culoo.cusagl_4android.main.ui.MainScreen
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.OverlayPlaybackService
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var screenState by mutableStateOf(MainScreenState())
    private var scoreEntries by mutableStateOf<List<ScoreEntry>>(emptyList())
    private var scoreManagementMessage by mutableStateOf<UiText?>(null)
    private var manualDraft by mutableStateOf(ManualScoreDraft())
    private var pendingSave by mutableStateOf<PendingScoreSave?>(null)
    private var playbackDraft by mutableStateOf(PlaybackConfigDraft())
    private var playbackConfigMessage by mutableStateOf<UiText?>(null)
    private var playbackRequest by mutableStateOf<PlaybackSessionRequest?>(null)
    private var permissionDialogDismissedInCurrentForeground by mutableStateOf(false)
    private var aboutState by mutableStateOf(AboutUiState(currentVersion = BuildConfig.VERSION_NAME))
    private var updateInstallStarted = false

    private val openScoreDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importScoreUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AboutController.clearUpdateCache(cacheDir)
        enableEdgeToEdge()
        setContent {
            CuSAGL4AndroidTheme {
                BackHandler(enabled = screenState.page != MainPage.HOME) {
                    if (screenState.page == MainPage.MANUAL_SCORE_CREATE) {
                        cancelManualScoreCreation()
                    } else {
                        screenState = screenState.copy(page = MainPage.HOME)
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        state = screenState,
                        modifier = Modifier.padding(innerPadding),
                        onOpenScoreManagement = {
                            screenState = screenState.copy(page = MainPage.SCORE_MANAGEMENT)
                            refreshScoreEntries()
                        },
                        onOpenPlaybackConfig = {
                            screenState = screenState.copy(page = MainPage.PLAYBACK_CONFIG)
                            refreshPlaybackConfig()
                        },
                        onOpenAbout = {
                            screenState = screenState.copy(page = MainPage.ABOUT)
                        },
                        onBackHome = {
                            screenState = screenState.copy(page = MainPage.HOME)
                        },
                        onGrantOverlay = {
                            permissionDialogDismissedInCurrentForeground = true
                            startActivity(OverlayPermission.settingsIntent(this))
                        },
                        onGrantAccessibility = {
                            permissionDialogDismissedInCurrentForeground = true
                            startActivity(AccessibilityPermission.settingsIntent())
                        },
                        permissionDialogDismissed = permissionDialogDismissedInCurrentForeground,
                        onDismissPermissionGuide = {
                            permissionDialogDismissedInCurrentForeground = true
                        },
                        scoreEntries = scoreEntries,
                        scoreManagementMessage = scoreManagementMessage,
                        manualDraft = manualDraft,
                        pendingOverwriteTitle = pendingSave?.overwriteTitle,
                        onImportScore = {
                            openScoreDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        onStartCreateScore = {
                            manualDraft = ManualScoreDraft()
                            pendingSave = null
                            scoreManagementMessage = null
                            screenState = screenState.copy(page = MainPage.MANUAL_SCORE_CREATE)
                        },
                        onCancelCreateScore = ::cancelManualScoreCreation,
                        onManualDraftChange = { manualDraft = it },
                        onSaveManualScore = ::saveManualScore,
                        onDeleteScore = ::deleteScore,
                        onConfirmOverwrite = ::confirmPendingOverwrite,
                        onDismissOverwrite = {
                            pendingSave = null
                            scoreManagementMessage = UiText.resource(R.string.message_overwrite_cancelled)
                        },
                        playbackDraft = playbackDraft,
                        playbackConfigMessage = playbackConfigMessage,
                        onPlaybackDraftChange = { playbackDraft = it },
                        onApplyPlaybackConfig = ::applyPlaybackConfig,
                        aboutState = aboutState,
                        onCheckUpdate = ::checkForUpdate,
                        onInstallUpdate = ::downloadAndInstallUpdate,
                        onPreload = ::preloadScore,
                        onStartOverlay = {
                            val request = playbackRequest ?: return@MainScreen
                            if (!screenState.canPreparePlayback) return@MainScreen
                            OverlayPlaybackService.start(this, request)
                        }
                    )
                }
            }
        }
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        if (updateInstallStarted) {
            AboutController.clearUpdateCache(cacheDir)
            updateInstallStarted = false
            aboutState = aboutState.copy(
                isDownloading = false,
                message = UiText.resource(R.string.message_returned_from_installer),
                errorMessage = null
            )
        }
        refreshState()
    }

    override fun onStop() {
        permissionDialogDismissedInCurrentForeground = false
        super.onStop()
    }

    private fun cancelManualScoreCreation() {
        manualDraft = ManualScoreDraft()
        pendingSave = null
        screenState = screenState.copy(page = MainPage.SCORE_MANAGEMENT)
    }

    private fun refreshState() {
        lifecycleScope.launch {
            val currentPage = screenState.page
            val appliedConfig = withContext(Dispatchers.IO) {
                PlaybackConfigController.loadApplied(filesDir)
            }
            val result = withContext(Dispatchers.IO) {
                MainScreenController.refresh(filesDir, appliedConfig.scoreNames)
            }
            playbackDraft = appliedConfig.draft
            playbackRequest = appliedConfig.request
            playbackConfigMessage = appliedConfig.message
            if (currentPage == MainPage.SCORE_MANAGEMENT || currentPage == MainPage.PLAYBACK_CONFIG) {
                scoreEntries = withContext(Dispatchers.IO) {
                    ScoreManagementController.listScores(filesDir)
                }
            }
            screenState = screenState.copy(
                page = currentPage,
                firstScoreName = result.firstScoreName,
                isCacheReady = result.isCacheReady,
                isLoading = false,
                errorMessage = null,
                hasOverlayPermission = OverlayPermission.canDraw(this@MainActivity),
                hasAccessibility = AccessibilityServiceBridge.isConnected(),
                playbackConfigSummary = appliedConfig.summary,
                playbackQueueSize = appliedConfig.scoreNames.size,
                hasPlaybackRequest = appliedConfig.request != null
            )
        }
    }

    private fun preloadScore() {
        val request = playbackRequest ?: return
        screenState = screenState.copy(isLoading = true, errorMessage = null)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PlaybackConfigController.preloadScores(filesDir, request.queue)
            }
            screenState = when (result) {
                is PreloadResult.Success -> screenState.copy(
                    isCacheReady = true,
                    isLoading = false,
                    errorMessage = null
                )
                is PreloadResult.Failure -> screenState.copy(
                    isCacheReady = false,
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    private fun refreshPlaybackConfig() {
        lifecycleScope.launch {
            val applied = withContext(Dispatchers.IO) {
                PlaybackConfigController.loadApplied(filesDir)
            }
            playbackDraft = applied.draft
            playbackRequest = applied.request
            playbackConfigMessage = applied.message
            scoreEntries = withContext(Dispatchers.IO) {
                ScoreManagementController.listScores(filesDir)
            }
            screenState = screenState.copy(
                playbackConfigSummary = applied.summary,
                playbackQueueSize = applied.scoreNames.size,
                hasPlaybackRequest = applied.request != null
            )
        }
    }

    private fun applyPlaybackConfig() {
        val draft = playbackDraft
        playbackConfigMessage = null
        lifecycleScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                PlaybackConfigController.applyAndSave(filesDir, draft)
            }) {
                is PlaybackConfigApplyResult.Success -> {
                    val applied = result.applied
                    playbackDraft = applied.draft
                    playbackRequest = applied.request
                    playbackConfigMessage = UiText.resource(R.string.message_playback_config_applied)
                    val refresh = withContext(Dispatchers.IO) {
                        MainScreenController.refresh(filesDir, applied.scoreNames)
                    }
                    screenState = screenState.copy(
                        firstScoreName = refresh.firstScoreName,
                        isCacheReady = refresh.isCacheReady,
                        errorMessage = null,
                        playbackConfigSummary = applied.summary,
                        playbackQueueSize = applied.scoreNames.size,
                        hasPlaybackRequest = applied.request != null
                    )
                }
                is PlaybackConfigApplyResult.Failure -> {
                    playbackConfigMessage = result.message
                }
            }
        }
    }

    private fun checkForUpdate() {
        val currentVersion = aboutState.currentVersion
        aboutState = aboutState.copy(
            isChecking = true,
            message = null,
            errorMessage = null
        )
        lifecycleScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                AboutController.fetchLatestRelease(currentVersion)
            }) {
                is UpdateCheckResult.UpdateAvailable -> {
                    aboutState = aboutState.copy(
                        isChecking = false,
                        latestTag = result.release.tagName,
                        releaseUrl = result.release.releaseUrl,
                        apkDownloadUrl = result.release.apkDownloadUrl,
                        hasUpdate = true,
                        message = UiText.resource(R.string.message_update_available, result.release.tagName),
                        errorMessage = null
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    aboutState = aboutState.copy(
                        isChecking = false,
                        latestTag = result.release.tagName,
                        releaseUrl = result.release.releaseUrl,
                        apkDownloadUrl = result.release.apkDownloadUrl,
                        hasUpdate = false,
                        message = UiText.resource(R.string.message_up_to_date, currentVersion),
                        errorMessage = null
                    )
                }
                is UpdateCheckResult.Failure -> {
                    aboutState = aboutState.copy(
                        isChecking = false,
                        hasUpdate = false,
                        message = null,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun downloadAndInstallUpdate() {
        val downloadUrl = aboutState.apkDownloadUrl ?: return
        aboutState = aboutState.copy(
            isDownloading = true,
            message = UiText.resource(R.string.message_update_downloading),
            errorMessage = null
        )
        lifecycleScope.launch {
            val apkFile = try {
                withContext(Dispatchers.IO) {
                    AboutController.downloadApk(downloadUrl, cacheDir)
                }
            } catch (ex: Exception) {
                aboutState = aboutState.copy(
                    isDownloading = false,
                    message = null,
                    errorMessage = UiText.resource(R.string.error_download_update_failed, ex.message ?: getString(R.string.error_unknown))
                )
                return@launch
            }
            startApkInstall(apkFile)
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
            updateInstallStarted = true
            aboutState = aboutState.copy(
                isDownloading = false,
                message = UiText.resource(R.string.message_installer_opening),
                errorMessage = null
            )
            startActivity(intent)
        } catch (ex: Exception) {
            updateInstallStarted = false
            AboutController.clearUpdateCache(cacheDir)
            aboutState = aboutState.copy(
                isDownloading = false,
                message = null,
                errorMessage = UiText.resource(R.string.error_open_installer_failed, ex.message ?: getString(R.string.error_unknown))
            )
        }
    }

    private fun refreshScoreEntries() {
        lifecycleScope.launch {
            scoreEntries = withContext(Dispatchers.IO) {
                ScoreManagementController.listScores(filesDir)
            }
        }
    }

    private fun importScoreUri(uri: Uri) {
        scoreManagementMessage = null
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }
            if (text == null) {
                scoreManagementMessage = UiText.resource(R.string.error_import_read_failed)
                return@launch
            }
            handleSaveResult(
                result = withContext(Dispatchers.IO) {
                    ScoreManagementController.importScoreText(
                        filesDir = filesDir,
                        sourceFileName = uri.lastPathSegment.orEmpty(),
                        text = text,
                        overwriteConfirmed = false
                    )
                },
                pending = PendingScoreSave.Import(uri.lastPathSegment.orEmpty(), text)
            )
        }
    }

    private fun saveManualScore() {
        val draft = manualDraft
        scoreManagementMessage = null
        lifecycleScope.launch {
            handleSaveResult(
                result = withContext(Dispatchers.IO) {
                    ScoreManagementController.saveManualScore(
                        filesDir = filesDir,
                        draft = draft,
                        overwriteConfirmed = false
                    )
                },
                pending = PendingScoreSave.Manual(draft)
            )
        }
    }

    private fun confirmPendingOverwrite() {
        val pending = pendingSave ?: return
        pendingSave = null
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                when (pending) {
                    is PendingScoreSave.Import -> ScoreManagementController.importScoreText(
                        filesDir = filesDir,
                        sourceFileName = pending.sourceFileName,
                        text = pending.text,
                        overwriteConfirmed = true
                    )
                    is PendingScoreSave.Manual -> ScoreManagementController.saveManualScore(
                        filesDir = filesDir,
                        draft = pending.draft,
                        overwriteConfirmed = true
                    )
                }
            }
            handleSaveResult(result, pending)
        }
    }

    private fun deleteScore(storageName: String) {
        scoreManagementMessage = null
        lifecycleScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                ScoreManagementController.deleteScore(filesDir, storageName)
            }) {
                is ScoreDeleteResult.Success -> {
                    scoreManagementMessage = UiText.resource(R.string.message_score_deleted, result.storageName)
                    refreshAfterScoreManagementChange()
                }
                is ScoreDeleteResult.Failure -> {
                    scoreManagementMessage = result.message
                }
            }
        }
    }

    private suspend fun handleSaveResult(result: ScoreSaveResult, pending: PendingScoreSave) {
        when (result) {
            is ScoreSaveResult.Success -> {
                scoreManagementMessage = UiText.resource(R.string.message_score_saved, result.storageName)
                pendingSave = null
                manualDraft = ManualScoreDraft()
                refreshAfterScoreManagementChange()
                screenState = screenState.copy(page = MainPage.SCORE_MANAGEMENT)
            }
            is ScoreSaveResult.NeedsOverwrite -> {
                pendingSave = pending.withOverwriteTitle(result.title)
                scoreManagementMessage = null
            }
            is ScoreSaveResult.Failure -> {
                scoreManagementMessage = result.message
            }
        }
    }

    private suspend fun refreshAfterScoreManagementChange() {
        scoreEntries = withContext(Dispatchers.IO) {
            ScoreManagementController.listScores(filesDir)
        }
        val result = withContext(Dispatchers.IO) {
            PlaybackConfigController.loadApplied(filesDir)
        }
        val refresh = withContext(Dispatchers.IO) {
            MainScreenController.refresh(filesDir, result.scoreNames)
        }
        playbackDraft = result.draft
        playbackRequest = result.request
        playbackConfigMessage = result.message
        screenState = screenState.copy(
            firstScoreName = refresh.firstScoreName,
            isCacheReady = refresh.isCacheReady,
            playbackConfigSummary = result.summary,
            playbackQueueSize = result.scoreNames.size,
            hasPlaybackRequest = result.request != null,
            errorMessage = null
        )
    }

    private sealed class PendingScoreSave(open val overwriteTitle: String? = null) {
        data class Import(
            val sourceFileName: String,
            val text: String,
            override val overwriteTitle: String? = null
        ) : PendingScoreSave(overwriteTitle)

        data class Manual(
            val draft: ManualScoreDraft,
            override val overwriteTitle: String? = null
        ) : PendingScoreSave(overwriteTitle)

        fun withOverwriteTitle(title: String): PendingScoreSave {
            return when (this) {
                is Import -> copy(overwriteTitle = title)
                is Manual -> copy(overwriteTitle = title)
            }
        }
    }
}
