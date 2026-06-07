package com.culoo.cusagl_4android.main

enum class PermissionGuideAction {
    OVERLAY,
    ACCESSIBILITY
}

data class PermissionGuideItem(
    val action: PermissionGuideAction,
    val title: String,
    val description: String,
    val buttonLabel: String
)

object PermissionGuideController {
    fun permissionItems(state: MainScreenState): List<PermissionGuideItem> {
        return buildList {
            if (!state.hasOverlayPermission) {
                add(
                    PermissionGuideItem(
                        action = PermissionGuideAction.OVERLAY,
                        title = "开启悬浮窗权限",
                        description = "用于在游戏上方显示演奏控制面板。",
                        buttonLabel = "去开启悬浮窗"
                    )
                )
            }
            if (!state.hasAccessibility) {
                add(
                    PermissionGuideItem(
                        action = PermissionGuideAction.ACCESSIBILITY,
                        title = "开启无障碍服务",
                        description = "用于把曲谱按键转换成屏幕触控注入。",
                        buttonLabel = "去开启无障碍"
                    )
                )
            }
        }
    }

    fun preparationBlockers(state: MainScreenState): List<String> {
        return buildList {
            if (state.isLoading) {
                add("正在预加载曲谱，请稍候。")
            }
            if (!state.hasPlaybackRequest) {
                if (state.firstScoreName == null) {
                    add("请先在曲谱管理中导入或新建曲谱。")
                } else {
                    add("请先保存一组有效的播放配置。")
                }
            }
            if (!state.isCacheReady) {
                add("请先预加载当前配置队列的缓存。")
            }
            if (!state.hasOverlayPermission) {
                add("请开启悬浮窗权限。")
            }
            if (!state.hasAccessibility) {
                add("请开启 CuSAGL 无障碍服务。")
            }
        }
    }

    fun shouldShowPermissionDialog(
        state: MainScreenState,
        dismissedInCurrentForeground: Boolean
    ): Boolean {
        return state.page == MainPage.HOME &&
            !dismissedInCurrentForeground &&
            permissionItems(state).isNotEmpty()
    }
}
