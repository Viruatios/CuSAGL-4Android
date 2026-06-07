package com.culoo.cusagl_4android

import android.os.Bundle
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainScreenController
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.ManualScoreDraft
import com.culoo.cusagl_4android.main.PlaybackConfigApplyResult
import com.culoo.cusagl_4android.main.PlaybackConfigController
import com.culoo.cusagl_4android.main.PlaybackConfigDraft
import com.culoo.cusagl_4android.main.PlaybackConfigMode
import com.culoo.cusagl_4android.main.PreloadResult
import com.culoo.cusagl_4android.main.ScoreDeleteResult
import com.culoo.cusagl_4android.main.ScoreEntry
import com.culoo.cusagl_4android.main.ScoreManagementController
import com.culoo.cusagl_4android.main.ScoreSaveResult
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
    private var isCreatingScore by mutableStateOf(false)
    private var manualDraft by mutableStateOf(ManualScoreDraft())
    private var pendingSave by mutableStateOf<PendingScoreSave?>(null)
    private var playbackDraft by mutableStateOf(PlaybackConfigDraft())
    private var playbackConfigMessage by mutableStateOf<String?>(null)
    private var playbackRequest by mutableStateOf<PlaybackSessionRequest?>(null)

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
                BackHandler(enabled = screenState.page != MainPage.HOME) {
                    screenState = screenState.copy(page = MainPage.HOME)
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
                        onBackHome = {
                            screenState = screenState.copy(page = MainPage.HOME)
                        },
                        onGrantOverlay = {
                            startActivity(OverlayPermission.settingsIntent(this))
                        },
                        scoreEntries = scoreEntries,
                        scoreManagementMessage = scoreManagementMessage,
                        isCreatingScore = isCreatingScore,
                        manualDraft = manualDraft,
                        pendingSave = pendingSave,
                        onImportScore = {
                            openScoreDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        onStartCreateScore = {
                            isCreatingScore = true
                            scoreManagementMessage = null
                        },
                        onCancelCreateScore = {
                            isCreatingScore = false
                            manualDraft = ManualScoreDraft()
                        },
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
        refreshState()
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
                isCreatingScore = false
                manualDraft = ManualScoreDraft()
                refreshAfterScoreManagementChange()
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

@Composable
private fun MainScreen(
    state: MainScreenState,
    modifier: Modifier = Modifier,
    onOpenScoreManagement: () -> Unit,
    onOpenPlaybackConfig: () -> Unit,
    onBackHome: () -> Unit,
    onGrantOverlay: () -> Unit,
    scoreEntries: List<ScoreEntry>,
    scoreManagementMessage: String?,
    isCreatingScore: Boolean,
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

    when (state.page) {
        MainPage.HOME -> MainHomeScreen(
            state = state,
            modifier = modifier,
            onOpenScoreManagement = onOpenScoreManagement,
            onOpenPlaybackConfig = onOpenPlaybackConfig,
            onGrantOverlay = onGrantOverlay,
            onPreload = onPreload,
            onStartOverlay = onStartOverlay
        )
        MainPage.SCORE_MANAGEMENT -> ScoreManagementScreen(
            entries = scoreEntries,
            message = scoreManagementMessage,
            isCreating = isCreatingScore,
            draft = manualDraft,
            modifier = modifier,
            onImportScore = onImportScore,
            onStartCreate = onStartCreateScore,
            onCancelCreate = onCancelCreateScore,
            onDraftChange = onManualDraftChange,
            onSaveManualScore = onSaveManualScore,
            onDeleteScore = onDeleteScore,
            onBackHome = onBackHome
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
    }
}

@Composable
private fun ScoreManagementScreen(
    entries: List<ScoreEntry>,
    message: String?,
    isCreating: Boolean,
    draft: ManualScoreDraft,
    modifier: Modifier = Modifier,
    onImportScore: () -> Unit,
    onStartCreate: () -> Unit,
    onCancelCreate: () -> Unit,
    onDraftChange: (ManualScoreDraft) -> Unit,
    onSaveManualScore: () -> Unit,
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
        Text("曲谱管理")
        if (message != null) {
            Text(message)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onImportScore) {
                Text("导入 JSON")
            }
            OutlinedButton(onClick = onStartCreate) {
                Text("新建曲谱")
            }
        }

        if (isCreating) {
            ManualScoreForm(
                draft = draft,
                onDraftChange = onDraftChange,
                onSave = onSaveManualScore,
                onCancel = onCancelCreate
            )
        }

        HorizontalDivider()

        if (entries.isEmpty()) {
            Text("没有已存储的曲谱。")
        } else {
            entries.forEach { entry ->
                ScoreEntryCard(
                    entry = entry,
                    onDelete = { onDeleteScore(entry.storageName) }
                )
            }
        }

        Button(onClick = onBackHome) {
            Text("返回主页面")
        }
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
            Text("新建曲谱")
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSave) {
                    Text("保存")
                }
                OutlinedButton(onClick = onCancel) {
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
            Text(entry.title)
            Text("文件：${entry.storageName}.json")
            Text(if (entry.hasCache) "缓存：已生成" else "缓存：未生成")
            OutlinedButton(onClick = onDelete) {
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
        Text("播放配置")
        if (message != null) {
            Text(message)
        }
        Text("当前可用曲谱：${entries.size} 首")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("播放模式")
                PlaybackConfigMode.allModes.forEach { mode ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text("曲谱")
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadioButton(
                                selected = draft.selectedScoreName == entry.storageName,
                                onClick = { onDraftChange(draft.copy(selectedScoreName = entry.storageName)) }
                            )
                            Column {
                                Text(entry.title)
                                Text(entry.storageName)
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
                Text("定时与间隔")
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
                Text("调试模式")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onApply) {
                Text("保存并应用")
            }
            OutlinedButton(onClick = onBackHome) {
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
    onGrantOverlay: () -> Unit,
    onPreload: () -> Unit,
    onStartOverlay: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CuSAGL 主页面")
        Text("当前曲谱：${state.firstScoreName ?: "没有可用曲谱"}")
        Text("播放配置：${state.playbackConfigSummary}")
        Text("配置队列：${state.playbackQueueSize} 首")
        Text(if (state.isCacheReady) "配置队列缓存：已预加载" else "配置队列缓存：未预加载")
        Text(if (state.hasOverlayPermission) "悬浮窗权限：已授予" else "悬浮窗权限：未授予")
        Text(if (state.hasAccessibility) "无障碍服务：已连接" else "无障碍服务：未连接")
        if (state.errorMessage != null) {
            Text("错误：${state.errorMessage}")
        }

        HorizontalDivider()

        OutlinedButton(onClick = onOpenScoreManagement) {
            Text("曲谱管理")
        }
        OutlinedButton(onClick = onOpenPlaybackConfig) {
            Text("自定义播放配置")
        }
        if (!state.hasOverlayPermission) {
            Button(onClick = onGrantOverlay) {
                Text("授予悬浮窗权限")
            }
        }
        Button(onClick = onPreload, enabled = state.canPreload) {
            Text(if (state.isLoading) "正在预加载" else "预加载曲谱")
        }
        Button(onClick = onStartOverlay, enabled = state.canPreparePlayback) {
            Text("准备演奏")
        }
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
