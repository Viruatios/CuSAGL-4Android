package com.culoo.cusagl_4android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.culoo.cusagl_4android.accessibility.AccessibilityServiceBridge
import com.culoo.cusagl_4android.core.PlaybackConfig
import com.culoo.cusagl_4android.core.ScoreStorage
import com.culoo.cusagl_4android.overlay.OverlayPermission
import com.culoo.cusagl_4android.overlay.OverlayPlaybackService
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import com.culoo.cusagl_4android.ui.theme.CuSAGL4AndroidTheme

class MainActivity : ComponentActivity() {
    private var screenState by mutableStateOf(DebugOverlayState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshState()
        setContent {
            CuSAGL4AndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugOverlayScreen(
                        state = screenState,
                        modifier = Modifier.padding(innerPadding),
                        onGrantOverlay = {
                            startActivity(OverlayPermission.settingsIntent(this))
                        },
                        onStartOverlay = {
                            val firstScore = screenState.firstScore ?: return@DebugOverlayScreen
                            OverlayPlaybackService.start(
                                this,
                                PlaybackSessionRequest(listOf(firstScore), PlaybackConfig())
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val firstScore = ScoreStorage.listAndNormalizeScores(filesDir).firstOrNull()
        screenState = DebugOverlayState(
            hasOverlayPermission = OverlayPermission.canDraw(this),
            hasAccessibility = AccessibilityServiceBridge.isConnected(),
            firstScore = firstScore
        )
    }
}

@Composable
private fun DebugOverlayScreen(
    state: DebugOverlayState,
    modifier: Modifier = Modifier,
    onGrantOverlay: () -> Unit,
    onStartOverlay: () -> Unit
) {
    val canStart = state.hasOverlayPermission && state.hasAccessibility && state.firstScore != null
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step4 悬浮窗临时验收入口")
        Text(if (state.hasOverlayPermission) "悬浮窗权限：已授予" else "悬浮窗权限：未授予")
        Text(if (state.hasAccessibility) "无障碍服务：已连接" else "无障碍服务：未连接")
        Text("测试曲谱：${state.firstScore ?: "没有可用曲谱"}")
        if (!state.hasOverlayPermission) {
            Button(onClick = onGrantOverlay) {
                Text("授予悬浮窗权限")
            }
        }
        Button(onClick = onStartOverlay, enabled = canStart) {
            Text("启动悬浮控制面板")
        }
    }
}

private data class DebugOverlayState(
    val hasOverlayPermission: Boolean = false,
    val hasAccessibility: Boolean = false,
    val firstScore: String? = null
)
