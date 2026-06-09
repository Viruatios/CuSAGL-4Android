package com.culoo.cusagl_4android

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.culoo.cusagl_4android.accessibility.AccessibilityPermission
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.main.AboutController
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainScreenController
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.ManualScoreDraft
import com.culoo.cusagl_4android.main.PermissionGuideAction
import com.culoo.cusagl_4android.main.PermissionGuideController
import com.culoo.cusagl_4android.main.PlaybackConfigApplyResult
import com.culoo.cusagl_4android.main.PlaybackConfigController
import com.culoo.cusagl_4android.main.PlaybackConfigDraft
import com.culoo.cusagl_4android.main.PlaybackConfigMode
import com.culoo.cusagl_4android.main.PreloadResult
import com.culoo.cusagl_4android.main.ScoreDeleteResult
import com.culoo.cusagl_4android.main.ScoreEntry
import com.culoo.cusagl_4android.main.ScoreManagementController
import com.culoo.cusagl_4android.main.ScoreSaveResult
import com.culoo.cusagl_4android.main.UpdateCheckResult
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.OverlayPlaybackService
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var screenState by mutableStateOf(MainScreenState())
    private var scoreEntries by mutableStateOf<List<ScoreEntry>>(emptyList())
    private var scoreManagementMessage by mutableStateOf<String?>(null)
    private var manualDraft by mutableStateOf(ManualScoreDraft())
    private var pendingSave by mutableStateOf<PendingScoreSave?>(null)
    private var playbackDraft by mutableStateOf(PlaybackConfigDraft())
    private var playbackConfigMessage by mutableStateOf<String?>(null)
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
                        pendingSave = pendingSave,
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
                            scoreManagementMessage = "已取消覆盖。"
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
                message = "已返回应用。如需重新安装，请重新下载更新包。",
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
                    playbackConfigMessage = "已应用播放配置。"
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
                        message = "发现新版本：${result.release.tagName}",
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
                        message = "当前已是最新版本：${currentVersion}",
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
            message = "正在下载更新包...",
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
                    errorMessage = "下载更新失败：${ex.message ?: "未知错误"}"
                )
                return@launch
            }
            startApkInstall(apkFile)
        }
    }

    private fun startApkInstall(apkFile: java.io.File) {
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
                message = "更新包已下载，正在打开系统安装器。",
                errorMessage = null
            )
            startActivity(intent)
        } catch (ex: Exception) {
            updateInstallStarted = false
            AboutController.clearUpdateCache(cacheDir)
            aboutState = aboutState.copy(
                isDownloading = false,
                message = null,
                errorMessage = "无法打开系统安装器：${ex.message ?: "未知错误"}"
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
                scoreManagementMessage = "无法读取导入文件。"
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
                    scoreManagementMessage = "已删除曲谱：${result.storageName}"
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
                scoreManagementMessage = "已保存曲谱：${result.storageName}"
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

private data class AboutUiState(
    val currentVersion: String,
    val repositoryUrl: String = AboutController.REPOSITORY_URL,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val latestTag: String? = null,
    val releaseUrl: String? = null,
    val apkDownloadUrl: String? = null,
    val hasUpdate: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
) {
    val canInstallUpdate: Boolean
        get() = hasUpdate && apkDownloadUrl != null && !isChecking && !isDownloading
}

@Composable
private fun MainScreen(
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
    scoreManagementMessage: String?,
    manualDraft: ManualScoreDraft,
    pendingSave: PendingScoreSave?,
    playbackDraft: PlaybackConfigDraft,
    playbackConfigMessage: String?,
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
    if (pendingSave?.overwriteTitle != null) {
        AlertDialog(
            onDismissRequest = onDismissOverwrite,
            title = { Text("覆盖曲谱") },
            text = { Text("已存在同名曲谱「${pendingSave.overwriteTitle}」，是否覆盖？旧缓存会同步删除。") },
            confirmButton = {
                TextButton(onClick = onConfirmOverwrite) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOverwrite) {
                    Text("取消")
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
        MainPage.HOME -> MainHomeScreen(
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
        title = { Text("完成演奏前的权限设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CuSAGL 需要以下权限才能在游戏中显示控制面板并注入触控。")
                items.forEach { item ->
                    Text("${item.title}：${item.description}")
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (items.any { it.action == PermissionGuideAction.OVERLAY }) {
                    TextButton(onClick = onGrantOverlay) {
                        Text("去开启悬浮窗")
                    }
                }
                if (items.any { it.action == PermissionGuideAction.ACCESSIBILITY }) {
                    TextButton(onClick = onGrantAccessibility) {
                        Text("去开启无障碍")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PageTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageText(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorText(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ScoreManagementScreen(
    entries: List<ScoreEntry>,
    message: String?,
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
            title = "曲谱管理",
            subtitle = "导入 JSON 曲谱，或手动创建一份可预加载的曲谱。"
        )
        if (message != null) {
            MessageText(message)
        }

        SectionCard("曲谱来源") {
            Button(
                onClick = onImportScore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导入 JSON")
            }
            OutlinedButton(
                onClick = onStartCreate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("新建曲谱")
            }
        }

        HorizontalDivider()
        Text("已存储曲谱", style = MaterialTheme.typography.titleMedium)

        if (entries.isEmpty()) {
            MessageText("没有已存储的曲谱。")
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
            Text("返回主页面")
        }
    }
}

@Composable
private fun ManualScoreCreateScreen(
    message: String?,
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
            title = "新建曲谱",
            subtitle = "手动输入曲谱字段，保存前会进行严格校验。"
        )
        if (message != null) {
            MessageText(message)
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
            Text("新建曲谱", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it)) },
                label = { Text("曲名") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.bpm,
                onValueChange = { onDraftChange(draft.copy(bpm = it)) },
                label = { Text("BPM") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.timeSignature,
                onValueChange = { onDraftChange(draft.copy(timeSignature = it)) },
                label = { Text("拍号") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.author,
                onValueChange = { onDraftChange(draft.copy(author = it)) },
                label = { Text("作者") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.instrument,
                onValueChange = { onDraftChange(draft.copy(instrument = it)) },
                label = { Text("建议乐器") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onDraftChange(draft.copy(description = it)) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.composer,
                onValueChange = { onDraftChange(draft.copy(composer = it)) },
                label = { Text("作曲者") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.arranger,
                onValueChange = { onDraftChange(draft.copy(arranger = it)) },
                label = { Text("编曲者") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onDraftChange(draft.copy(notes = it)) },
                label = { Text("曲谱 notes") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消")
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Text("文件：${entry.storageName}.json", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (entry.hasCache) "缓存：已生成" else "缓存：未生成",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun PlaybackConfigScreen(
    entries: List<ScoreEntry>,
    draft: PlaybackConfigDraft,
    message: String?,
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
            title = "播放配置",
            subtitle = "设置曲谱、队列、定时启动和循环参数。"
        )
        if (message != null) {
            MessageText(message)
        }
        Text("当前可用曲谱：${entries.size} 首", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("播放模式", style = MaterialTheme.typography.titleMedium)
                PlaybackConfigMode.allModes.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = draft.mode == mode,
                            onClick = { onDraftChange(draft.copy(mode = mode)) }
                        )
                        Text(mode.label)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("曲谱", style = MaterialTheme.typography.titleMedium)
                if (entries.isEmpty()) {
                    Text("没有已存储的曲谱。请先进入曲谱管理导入或创建曲谱。")
                } else if (draft.mode.isQueueMode()) {
                    OutlinedTextField(
                        value = draft.queueText,
                        onValueChange = { onDraftChange(draft.copy(queueText = it)) },
                        label = { Text("队列序号") },
                        placeholder = { Text("例如：1 3 15；留空默认全部播放") },
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
                Text("定时与间隔", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = draft.startTimeText,
                    onValueChange = { onDraftChange(draft.copy(startTimeText = it)) },
                    label = { Text("定时启动时间") },
                    placeholder = { Text("HH:mm 或 HH:mm:ss；留空不启用") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.queueIntervalSeconds,
                    onValueChange = { onDraftChange(draft.copy(queueIntervalSeconds = it)) },
                    label = { Text("队列内间隔时间（秒）") },
                    enabled = draft.mode.isQueueMode(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.repeatTimes,
                    onValueChange = { onDraftChange(draft.copy(repeatTimes = it)) },
                    label = { Text("循环执行次数") },
                    enabled = draft.mode.isRepeatMode(),
                    placeholder = { Text("0 表示无限循环") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.repeatIntervalSeconds,
                    onValueChange = { onDraftChange(draft.copy(repeatIntervalSeconds = it)) },
                    label = { Text("循环间隔时间（秒）") },
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
                    Text("调试模式", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "当前仅保存配置，不改变日志输出。",
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
                Text("保存并应用")
            }
            OutlinedButton(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回主页面")
            }
        }
    }
}

@Composable
private fun MainHomeScreen(
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
                title = "CuSAGL",
                subtitle = "整理曲谱、预加载缓存，然后进入悬浮窗演奏。",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "关于" }
            ) {
                ThreeDotMenuIcon()
            }
        }
        if (state.errorMessage != null) {
            ErrorText("错误：${state.errorMessage}")
        }

        SectionCard("播放准备") {
            Text("当前曲谱：${state.firstScoreName ?: "没有可用曲谱"}")
            Text("播放配置：${state.playbackConfigSummary}")
            Text("配置队列：${state.playbackQueueSize} 首")
            Text(if (state.isCacheReady) "队列缓存：已预加载" else "队列缓存：未预加载")
        }

        SectionCard("权限状态") {
            if (permissionItems.isEmpty()) {
                Text("悬浮窗和无障碍服务均已就绪。")
            } else {
                permissionItems.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = item.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = when (item.action) {
                                PermissionGuideAction.OVERLAY -> onGrantOverlay
                                PermissionGuideAction.ACCESSIBILITY -> onGrantAccessibility
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item.buttonLabel)
                        }
                    }
                }
            }
        }

        SectionCard("主要操作") {
            OutlinedButton(
                onClick = onOpenScoreManagement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("曲谱管理")
            }
            OutlinedButton(
                onClick = onOpenPlaybackConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("自定义播放配置")
            }
            Button(
                onClick = onPreload,
                enabled = state.canPreload,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isLoading) "正在预加载" else "预加载曲谱")
            }
            Button(
                onClick = onStartOverlay,
                enabled = state.canPreparePlayback,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("准备演奏")
            }
        }

        if (blockers.isNotEmpty()) {
            SectionCard("准备演奏还需要") {
                blockers.forEach { reason ->
                    Text(reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(
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
            title = "关于SAGL",
            subtitle = "Simplified Automatic Genshin Lyre for Android"
        )
        if (state.message != null) {
            MessageText(state.message)
        }
        if (state.errorMessage != null) {
            ErrorText(state.errorMessage)
        }

        SectionCard("应用信息") {
            Text("注意：CuSAGL Mobile 是一个免费软件，不带任何担保。任何由于使用本软件造成的后果，应当由用户自行承担。")
            Text("当前版本：${state.currentVersion}")
            Text(
                "项目地址：${state.repositoryUrl}",
                modifier= Modifier.clickable() {
                    uriHandler.openUri(state.repositoryUrl)
                }
            )
        }

        SectionCard("检查更新") {
            Button(
                onClick = onCheckUpdate,
                enabled = !state.isChecking && !state.isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isChecking) "正在检查..." else "检查更新")
            }
            Button(
                onClick = onInstallUpdate,
                enabled = state.canInstallUpdate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isDownloading) "正在下载..." else "下载并安装更新")
            }
        }

        if(state.latestTag != null){
            SectionCard("检测到的最新版本") {
                Text("最新版本：${state.latestTag}")
                if (state.releaseUrl != null) {
                    Text(
                        "Release：${state.releaseUrl}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable() {
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
            Text("返回主页面")
        }
    }
}

@Composable
private fun ThreeDotMenuIcon(modifier: Modifier = Modifier) {
    val dotColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.size(24.dp)) {
        val radius = 2.5.dp.toPx()
        val centerY = size.height / 2f
        val gap = 7.dp.toPx()
        val centerX = size.width / 2f
        drawCircle(dotColor, radius, Offset(centerX - gap, centerY))
        drawCircle(dotColor, radius, Offset(centerX, centerY))
        drawCircle(dotColor, radius, Offset(centerX + gap, centerY))
    }
}

@Composable
private fun PlaceholderPage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onBackHome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title)
        Text(body)
        Button(onClick = onBackHome) {
            Text("返回主页面")
        }
    }
}
