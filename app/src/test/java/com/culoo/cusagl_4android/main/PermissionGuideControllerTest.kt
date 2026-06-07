package com.culoo.cusagl_4android.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGuideControllerTest {
    @Test
    fun permissionItems_reportsBothMissingPermissions() {
        val state = readyState(
            hasOverlayPermission = false,
            hasAccessibility = false
        )

        val items = PermissionGuideController.permissionItems(state)

        assertEquals(
            listOf(PermissionGuideAction.OVERLAY, PermissionGuideAction.ACCESSIBILITY),
            items.map { it.action }
        )
    }

    @Test
    fun permissionItems_reportsOnlyOverlayMissing() {
        val state = readyState(
            hasOverlayPermission = false,
            hasAccessibility = true
        )

        val items = PermissionGuideController.permissionItems(state)

        assertEquals(listOf(PermissionGuideAction.OVERLAY), items.map { it.action })
    }

    @Test
    fun permissionItems_reportsOnlyAccessibilityMissing() {
        val state = readyState(
            hasOverlayPermission = true,
            hasAccessibility = false
        )

        val items = PermissionGuideController.permissionItems(state)

        assertEquals(listOf(PermissionGuideAction.ACCESSIBILITY), items.map { it.action })
    }

    @Test
    fun permissionItems_isEmptyWhenPermissionsAreReady() {
        val state = readyState()

        assertTrue(PermissionGuideController.permissionItems(state).isEmpty())
    }

    @Test
    fun preparationBlockers_explainMissingScoreOrConfigCacheAndLoading() {
        val noScore = MainScreenState(
            firstScoreName = null,
            hasPlaybackRequest = false,
            isCacheReady = false,
            hasOverlayPermission = true,
            hasAccessibility = true
        )
        val noConfig = MainScreenState(
            firstScoreName = "0001.test",
            hasPlaybackRequest = false,
            isCacheReady = false,
            hasOverlayPermission = true,
            hasAccessibility = true
        )
        val cacheMissing = readyState(isCacheReady = false)
        val loading = readyState(isLoading = true)

        assertTrue(PermissionGuideController.preparationBlockers(noScore).contains("请先在曲谱管理中导入或新建曲谱。"))
        assertTrue(PermissionGuideController.preparationBlockers(noConfig).contains("请先保存一组有效的播放配置。"))
        assertEquals(listOf("请先预加载当前配置队列的缓存。"), PermissionGuideController.preparationBlockers(cacheMissing))
        assertTrue(PermissionGuideController.preparationBlockers(loading).contains("正在预加载曲谱，请稍候。"))
    }

    @Test
    fun preparationBlockers_areConsistentWithCanPreparePlayback() {
        val ready = readyState()
        val missingOverlay = readyState(hasOverlayPermission = false)

        assertTrue(ready.canPreparePlayback)
        assertTrue(PermissionGuideController.preparationBlockers(ready).isEmpty())
        assertFalse(missingOverlay.canPreparePlayback)
        assertTrue(PermissionGuideController.preparationBlockers(missingOverlay).isNotEmpty())
    }

    @Test
    fun permissionDialog_onlyShowsOnHomeWhenMissingPermissionAndNotDismissed() {
        val missingPermission = readyState(hasAccessibility = false)

        assertTrue(
            PermissionGuideController.shouldShowPermissionDialog(
                state = missingPermission,
                dismissedInCurrentForeground = false
            )
        )
        assertFalse(
            PermissionGuideController.shouldShowPermissionDialog(
                state = missingPermission,
                dismissedInCurrentForeground = true
            )
        )
        assertFalse(
            PermissionGuideController.shouldShowPermissionDialog(
                state = missingPermission.copy(page = MainPage.PLAYBACK_CONFIG),
                dismissedInCurrentForeground = false
            )
        )
        assertFalse(
            PermissionGuideController.shouldShowPermissionDialog(
                state = readyState(),
                dismissedInCurrentForeground = false
            )
        )
    }

    private fun readyState(
        page: MainPage = MainPage.HOME,
        firstScoreName: String? = "0001.test",
        isCacheReady: Boolean = true,
        isLoading: Boolean = false,
        hasOverlayPermission: Boolean = true,
        hasAccessibility: Boolean = true,
        hasPlaybackRequest: Boolean = true
    ): MainScreenState {
        return MainScreenState(
            page = page,
            firstScoreName = firstScoreName,
            isCacheReady = isCacheReady,
            isLoading = isLoading,
            hasOverlayPermission = hasOverlayPermission,
            hasAccessibility = hasAccessibility,
            playbackQueueSize = 1,
            hasPlaybackRequest = hasPlaybackRequest
        )
    }
}
