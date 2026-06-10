package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText

enum class PermissionGuideAction {
    OVERLAY,
    ACCESSIBILITY
}

data class PermissionGuideItem(
    val action: PermissionGuideAction,
    val title: UiText,
    val description: UiText,
    val buttonLabel: UiText
)

object PermissionGuideController {
    fun permissionItems(state: MainScreenState): List<PermissionGuideItem> {
        return buildList {
            if (!state.hasOverlayPermission) {
                add(
                    PermissionGuideItem(
                        action = PermissionGuideAction.OVERLAY,
                        title = UiText.resource(R.string.permission_overlay_title),
                        description = UiText.resource(R.string.permission_overlay_description),
                        buttonLabel = UiText.resource(R.string.action_enable_overlay)
                    )
                )
            }
            if (!state.hasAccessibility) {
                add(
                    PermissionGuideItem(
                        action = PermissionGuideAction.ACCESSIBILITY,
                        title = UiText.resource(R.string.permission_accessibility_title),
                        description = UiText.resource(R.string.permission_accessibility_description),
                        buttonLabel = UiText.resource(R.string.action_enable_accessibility)
                    )
                )
            }
        }
    }

    fun preparationBlockers(state: MainScreenState): List<UiText> {
        return buildList {
            if (state.isLoading) {
                add(UiText.resource(R.string.blocker_loading))
            }
            if (!state.hasPlaybackRequest) {
                if (state.firstScoreName == null) {
                    add(UiText.resource(R.string.blocker_missing_score))
                } else {
                    add(UiText.resource(R.string.blocker_missing_config))
                }
            }
            if (!state.isCacheReady) {
                add(UiText.resource(R.string.blocker_missing_cache))
            }
            if (!state.hasOverlayPermission) {
                add(UiText.resource(R.string.blocker_missing_overlay))
            }
            if (!state.hasAccessibility) {
                add(UiText.resource(R.string.blocker_missing_accessibility))
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
