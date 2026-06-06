package com.culoo.cusagl_4android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.core.PlaybackConfig
import com.culoo.cusagl_4android.main.MainPage
import com.culoo.cusagl_4android.main.MainScreenController
import com.culoo.cusagl_4android.main.MainScreenState
import com.culoo.cusagl_4android.main.PreloadResult
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.OverlayPlaybackService
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var screenState by mutableStateOf(MainScreenState())

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
                        },
                        onOpenPlaybackConfig = {
                            screenState = screenState.copy(page = MainPage.PLAYBACK_CONFIG)
                        },
                        onBackHome = {
                            screenState = screenState.copy(page = MainPage.HOME)
                        },
                        onGrantOverlay = {
                            startActivity(OverlayPermission.settingsIntent(this))
                        },
                        onPreload = ::preloadScore,
                        onStartOverlay = {
                            val firstScore = screenState.firstScoreName ?: return@MainScreen
                            if (!screenState.canPreparePlayback) return@MainScreen
                            OverlayPlaybackService.start(
                                this,
                                PlaybackSessionRequest(listOf(firstScore), PlaybackConfig())
                            )
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
            val result = withContext(Dispatchers.IO) {
                MainScreenController.refresh(filesDir)
            }
            screenState = screenState.copy(
                page = currentPage,
                firstScoreName = result.firstScoreName,
                isCacheReady = result.isCacheReady,
                isLoading = false,
                errorMessage = null,
                hasOverlayPermission = OverlayPermission.canDraw(this@MainActivity),
                hasAccessibility = AccessibilityServiceBridge.isConnected()
            )
        }
    }

    private fun preloadScore() {
        val scoreName = screenState.firstScoreName ?: return
        screenState = screenState.copy(isLoading = true, errorMessage = null)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                MainScreenController.preloadFirstScore(filesDir, scoreName)
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
}

@Composable
private fun MainScreen(
    state: MainScreenState,
    modifier: Modifier = Modifier,
    onOpenScoreManagement: () -> Unit,
    onOpenPlaybackConfig: () -> Unit,
    onBackHome: () -> Unit,
    onGrantOverlay: () -> Unit,
    onPreload: () -> Unit,
    onStartOverlay: () -> Unit
) {
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
        MainPage.SCORE_MANAGEMENT -> PlaceholderPage(
            title = "曲谱管理",
            body = "曲谱导入、删除和手动创建会在 Step6 实现。",
            modifier = modifier,
            onBackHome = onBackHome
        )
        MainPage.PLAYBACK_CONFIG -> PlaceholderPage(
            title = "播放配置",
            body = "自定义播放参数会在 Step7 实现。当前使用默认配置。",
            modifier = modifier,
            onBackHome = onBackHome
        )
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
        Text(if (state.isCacheReady) "曲谱缓存：已预加载" else "曲谱缓存：未预加载")
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
