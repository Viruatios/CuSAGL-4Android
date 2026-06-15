package com.culoo.cusagl_4android.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.culoo.cusagl_4android.BuildConfig
import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MainUiState(
    val screenState: MainScreenState = MainScreenState(),
    val scoreEntries: List<ScoreEntry> = emptyList(),
    val scoreManagementMessage: UiText? = null,
    val manualDraft: ManualScoreDraft = ManualScoreDraft(),
    val pendingOverwriteTitle: String? = null,
    val playbackDraft: PlaybackConfigDraft = PlaybackConfigDraft(),
    val playbackConfigMessage: UiText? = null,
    val permissionDialogDismissedInCurrentForeground: Boolean = false,
    val showPreparePlaybackWarningDialog: Boolean = false,
    val aboutState: AboutUiState = AboutUiState(currentVersion = BuildConfig.VERSION_NAME)
)

sealed class MainUiEvent {
    data class ShowSnackbar(val message: UiText) : MainUiEvent()
    data class StartOverlayPlayback(val request: PlaybackSessionRequest) : MainUiEvent()
    data class OpenApkInstaller(val apkFile: File) : MainUiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<Application>()
    private val filesDir: File
        get() = app.filesDir
    private val cacheDir: File
        get() = app.cacheDir
    private val preparePlaybackWarningStore = SharedPreferencesBooleanStore(
        app.getSharedPreferences(MainConstants.USER_PREFERENCES_NAME, 0)
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    private var playbackRequest: PlaybackSessionRequest? = null
    private var pendingSave: PendingScoreSave? = null
    private var updateInstallStarted = false

    init {
        AboutController.clearUpdateCache(cacheDir)
    }

    fun onResume() {
        if (updateInstallStarted) {
            AboutController.clearUpdateCache(cacheDir)
            updateInstallStarted = false
            _uiState.update { state ->
                state.copy(
                    aboutState = state.aboutState.copy(
                        isDownloading = false,
                        message = UiText.resource(R.string.message_returned_from_installer),
                        errorMessage = null
                    )
                )
            }
        }
        refreshState()
    }

    fun onStop() {
        _uiState.update {
            it.copy(permissionDialogDismissedInCurrentForeground = false)
        }
    }

    fun openScoreManagement() {
        _uiState.update {
            it.copy(screenState = it.screenState.copy(page = MainPage.SCORE_MANAGEMENT))
        }
        refreshState(includeScoreEntries = true)
    }

    fun openPlaybackConfig() {
        _uiState.update {
            it.copy(screenState = it.screenState.copy(page = MainPage.PLAYBACK_CONFIG))
        }
        refreshState(includeScoreEntries = true)
    }

    fun openAbout() {
        _uiState.update {
            it.copy(screenState = it.screenState.copy(page = MainPage.ABOUT))
        }
    }

    fun backHome() {
        _uiState.update {
            it.copy(screenState = it.screenState.copy(page = MainPage.HOME))
        }
    }

    fun backPressed() {
        val page = _uiState.value.screenState.page
        if (page == MainPage.MANUAL_SCORE_CREATE) {
            cancelManualScoreCreation()
        } else if (page != MainPage.HOME) {
            backHome()
        }
    }

    fun dismissPermissionGuide() {
        _uiState.update {
            it.copy(permissionDialogDismissedInCurrentForeground = true)
        }
    }

    fun markPermissionGuideLeavingApp() {
        dismissPermissionGuide()
    }

    fun confirmPreparePlaybackWarning(doNotShowAgain: Boolean) {
        if (doNotShowAgain) {
            PreparePlaybackWarningController.setSuppressed(preparePlaybackWarningStore, true)
        }
        _uiState.update { it.copy(showPreparePlaybackWarningDialog = false) }
        requestStartOverlayPlayback()
    }

    fun dismissPreparePlaybackWarning() {
        _uiState.update { it.copy(showPreparePlaybackWarningDialog = false) }
    }

    fun startCreateScore() {
        pendingSave = null
        _uiState.update {
            it.copy(
                manualDraft = ManualScoreDraft(),
                scoreManagementMessage = null,
                pendingOverwriteTitle = null,
                screenState = it.screenState.copy(page = MainPage.MANUAL_SCORE_CREATE)
            )
        }
    }

    fun cancelManualScoreCreation() {
        pendingSave = null
        _uiState.update {
            it.copy(
                manualDraft = ManualScoreDraft(),
                pendingOverwriteTitle = null,
                screenState = it.screenState.copy(page = MainPage.SCORE_MANAGEMENT)
            )
        }
    }

    fun updateManualDraft(draft: ManualScoreDraft) {
        _uiState.update { it.copy(manualDraft = draft) }
    }

    fun updatePlaybackDraft(draft: PlaybackConfigDraft) {
        _uiState.update { it.copy(playbackDraft = draft) }
    }

    fun dismissOverwrite() {
        pendingSave = null
        _uiState.update {
            it.copy(
                pendingOverwriteTitle = null,
                scoreManagementMessage = UiText.resource(R.string.message_overwrite_cancelled)
            )
        }
    }

    fun importScoreText(sourceFileName: String, text: String) {
        _uiState.update { it.copy(scoreManagementMessage = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ScoreManagementController.importScoreText(
                    filesDir = filesDir,
                    sourceFileName = sourceFileName,
                    text = text,
                    overwriteConfirmed = false
                )
            }
            handleSaveResult(result, PendingScoreSave.Import(sourceFileName, text))
        }
    }

    fun importScoreReadFailed() {
        _uiState.update {
            it.copy(scoreManagementMessage = UiText.resource(R.string.error_import_read_failed))
        }
    }

    fun saveManualScore() {
        val draft = _uiState.value.manualDraft
        _uiState.update { it.copy(scoreManagementMessage = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ScoreManagementController.saveManualScore(
                    filesDir = filesDir,
                    draft = draft,
                    overwriteConfirmed = false
                )
            }
            handleSaveResult(result, PendingScoreSave.Manual(draft))
        }
    }

    fun confirmPendingOverwrite() {
        val pending = pendingSave ?: return
        pendingSave = null
        _uiState.update { it.copy(pendingOverwriteTitle = null) }
        viewModelScope.launch {
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

    fun deleteScore(storageName: String) {
        _uiState.update { it.copy(scoreManagementMessage = null) }
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                ScoreManagementController.deleteScore(filesDir, storageName)
            }) {
                is ScoreDeleteResult.Success -> {
                    _uiState.update {
                        it.copy(scoreManagementMessage = UiText.resource(R.string.message_score_deleted, result.storageName))
                    }
                    refreshState(includeScoreEntries = true)
                }
                is ScoreDeleteResult.Failure -> {
                    _uiState.update { it.copy(scoreManagementMessage = result.message) }
                }
            }
        }
    }

    fun applyPlaybackConfig() {
        val draft = _uiState.value.playbackDraft
        _uiState.update { it.copy(playbackConfigMessage = null) }
        viewModelScope.launch {
            val scoreNames = withContext(Dispatchers.IO) { listAndCleanScoreNames() }
            when (val result = withContext(Dispatchers.IO) {
                PlaybackConfigController.applyAndSaveWithScoreNames(filesDir, draft, scoreNames)
            }) {
                is PlaybackConfigApplyResult.Success -> {
                    val applied = result.applied
                    val refresh = withContext(Dispatchers.IO) {
                        MainScreenController.refreshWithScoreNames(filesDir, scoreNames, applied.scoreNames)
                    }
                    playbackRequest = applied.request
                    _uiState.update { state ->
                        state.copy(
                            playbackDraft = applied.draft,
                            playbackConfigMessage = null,
                            screenState = state.screenState.copy(
                                page = MainPage.HOME,
                                firstScoreName = refresh.firstScoreName,
                                isCacheReady = refresh.isCacheReady,
                                errorMessage = null,
                                playbackConfigSummary = applied.summary,
                                playbackQueueSize = applied.scoreNames.size,
                                hasPlaybackRequest = applied.request != null
                            )
                        )
                    }
                    _events.emit(MainUiEvent.ShowSnackbar(UiText.resource(R.string.message_playback_config_saved)))
                }
                is PlaybackConfigApplyResult.Failure -> {
                    _uiState.update { it.copy(playbackConfigMessage = result.message) }
                }
            }
        }
    }

    fun preloadScore() {
        val request = playbackRequest ?: return
        _uiState.update {
            it.copy(screenState = it.screenState.copy(isLoading = true, errorMessage = null))
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                PlaybackConfigController.preloadScores(filesDir, request.queue)
            }
            _uiState.update { state ->
                state.copy(
                    screenState = when (result) {
                        is PreloadResult.Success -> state.screenState.copy(
                            isCacheReady = true,
                            isLoading = false,
                            errorMessage = null
                        )
                        is PreloadResult.Failure -> state.screenState.copy(
                            isCacheReady = false,
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                )
            }
        }
    }

    fun startOverlayRequested() {
        if (!_uiState.value.screenState.canPreparePlayback) return
        if (PreparePlaybackWarningController.shouldShowWarning(preparePlaybackWarningStore)) {
            _uiState.update { it.copy(showPreparePlaybackWarningDialog = true) }
        } else {
            requestStartOverlayPlayback()
        }
    }

    fun checkForUpdate() {
        val currentVersion = _uiState.value.aboutState.currentVersion
        _uiState.update {
            it.copy(
                aboutState = it.aboutState.copy(
                    isChecking = true,
                    message = null,
                    errorMessage = null
                )
            )
        }
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) {
                AboutController.fetchLatestRelease(currentVersion)
            }) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            aboutState = it.aboutState.copy(
                                isChecking = false,
                                latestTag = result.release.tagName,
                                releaseUrl = result.release.releaseUrl,
                                apkDownloadUrl = result.release.apkDownloadUrl,
                                apkAssetName = result.release.apkAssetName,
                                hasUpdate = true,
                                message = UiText.resource(R.string.message_update_available, result.release.tagName),
                                errorMessage = null
                            )
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            aboutState = it.aboutState.copy(
                                isChecking = false,
                                latestTag = result.release.tagName,
                                releaseUrl = result.release.releaseUrl,
                                apkDownloadUrl = result.release.apkDownloadUrl,
                                apkAssetName = result.release.apkAssetName,
                                hasUpdate = false,
                                message = UiText.resource(R.string.message_up_to_date, currentVersion),
                                errorMessage = null
                            )
                        )
                    }
                }
                is UpdateCheckResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            aboutState = it.aboutState.copy(
                                isChecking = false,
                                hasUpdate = false,
                                message = null,
                                errorMessage = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val aboutState = _uiState.value.aboutState
        val downloadUrl = aboutState.apkDownloadUrl ?: return
        val apkAssetName = aboutState.apkAssetName ?: return
        _uiState.update {
            it.copy(
                aboutState = it.aboutState.copy(
                    isDownloading = true,
                    message = UiText.resource(R.string.message_update_downloading),
                    errorMessage = null
                )
            )
        }
        viewModelScope.launch {
            val apkFile = try {
                withContext(Dispatchers.IO) {
                    AboutController.downloadApk(downloadUrl, cacheDir, apkAssetName)
                }
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        aboutState = it.aboutState.copy(
                            isDownloading = false,
                            message = null,
                            errorMessage = UiText.resource(R.string.error_download_update_failed, ex.message ?: app.getString(R.string.error_unknown))
                        )
                    )
                }
                return@launch
            }
            updateInstallStarted = true
            _uiState.update {
                it.copy(
                    aboutState = it.aboutState.copy(
                        isDownloading = false,
                        message = UiText.resource(R.string.message_installer_opening),
                        errorMessage = null
                    )
                )
            }
            _events.emit(MainUiEvent.OpenApkInstaller(apkFile))
        }
    }

    fun apkInstallOpenFailed(message: String?) {
        updateInstallStarted = false
        AboutController.clearUpdateCache(cacheDir)
        _uiState.update {
            it.copy(
                aboutState = it.aboutState.copy(
                    isDownloading = false,
                    message = null,
                    errorMessage = UiText.resource(R.string.error_open_installer_failed, message ?: app.getString(R.string.error_unknown))
                )
            )
        }
    }

    fun refreshState(includeScoreEntries: Boolean = shouldIncludeScoreEntries()) {
        viewModelScope.launch {
            val currentPage = _uiState.value.screenState.page
            val snapshot = withContext(Dispatchers.IO) {
                MainRefreshCoordinator.refresh(filesDir, includeScoreEntries)
            }
            playbackRequest = snapshot.appliedConfig.request
            _uiState.update { state ->
                state.copy(
                    scoreEntries = snapshot.scoreEntries ?: state.scoreEntries,
                    playbackDraft = snapshot.appliedConfig.draft,
                    playbackConfigMessage = snapshot.appliedConfig.message,
                    screenState = state.screenState.copy(
                        page = currentPage,
                        firstScoreName = snapshot.mainRefresh.firstScoreName,
                        isCacheReady = snapshot.mainRefresh.isCacheReady,
                        isLoading = false,
                        errorMessage = null,
                        hasOverlayPermission = OverlayPermission.canDraw(app),
                        hasAccessibility = AccessibilityServiceBridge.isConnected(),
                        playbackConfigSummary = snapshot.appliedConfig.summary,
                        playbackQueueSize = snapshot.appliedConfig.scoreNames.size,
                        hasPlaybackRequest = snapshot.appliedConfig.request != null
                    )
                )
            }
        }
    }

    private suspend fun handleSaveResult(result: ScoreSaveResult, pending: PendingScoreSave) {
        when (result) {
            is ScoreSaveResult.Success -> {
                pendingSave = null
                _uiState.update {
                    it.copy(
                        scoreManagementMessage = UiText.resource(R.string.message_score_saved, result.storageName),
                        manualDraft = ManualScoreDraft(),
                        pendingOverwriteTitle = null,
                        screenState = it.screenState.copy(page = MainPage.SCORE_MANAGEMENT)
                    )
                }
                refreshState(includeScoreEntries = true)
            }
            is ScoreSaveResult.NeedsOverwrite -> {
                pendingSave = pending.withOverwriteTitle(result.title)
                _uiState.update {
                    it.copy(
                        scoreManagementMessage = null,
                        pendingOverwriteTitle = result.title
                    )
                }
            }
            is ScoreSaveResult.Failure -> {
                _uiState.update { it.copy(scoreManagementMessage = result.message) }
            }
        }
    }

    private fun requestStartOverlayPlayback() {
        val request = playbackRequest ?: return
        if (!_uiState.value.screenState.canPreparePlayback) return
        viewModelScope.launch {
            _events.emit(MainUiEvent.StartOverlayPlayback(request))
        }
    }

    private fun shouldIncludeScoreEntries(): Boolean {
        val page = _uiState.value.screenState.page
        return page == MainPage.SCORE_MANAGEMENT || page == MainPage.PLAYBACK_CONFIG
    }

    private fun listAndCleanScoreNames(): List<String> {
        val scoreNames = com.culoo.cusagl_4android.core.ScoreStorage.listAndNormalizeScores(filesDir)
        com.culoo.cusagl_4android.core.ScoreStorage.cleanExpiredCaches(filesDir, scoreNames.toSet())
        return scoreNames
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
