package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.LogTags
import com.culoo.cusagl_4android.core.Logger
import com.culoo.cusagl_4android.core.ScoreParser
import com.culoo.cusagl_4android.core.ScoreStorage
import java.io.File

enum class MainPage {
    HOME,
    SCORE_MANAGEMENT,
    MANUAL_SCORE_CREATE,
    PLAYBACK_CONFIG
}

data class MainScreenState(
    val page: MainPage = MainPage.HOME,
    val firstScoreName: String? = null,
    val isCacheReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasOverlayPermission: Boolean = false,
    val hasAccessibility: Boolean = false,
    val playbackConfigSummary: String = "单曲单次执行：默认第一首曲谱",
    val playbackQueueSize: Int = 0,
    val hasPlaybackRequest: Boolean = false
) {
    val canPreload: Boolean
        get() = hasPlaybackRequest && !isLoading

    val canPreparePlayback: Boolean
        get() = hasPlaybackRequest &&
            isCacheReady &&
            hasOverlayPermission &&
            hasAccessibility &&
            !isLoading
}

data class MainRefreshResult(
    val firstScoreName: String?,
    val isCacheReady: Boolean
)

sealed class PreloadResult {
    data class Success(val scoreName: String) : PreloadResult()
    data class Failure(val message: String) : PreloadResult()
}

object MainScreenController {
    fun refresh(
        filesDir: File,
        configuredQueue: List<String>? = null,
        logger: Logger = DefaultLogger
    ): MainRefreshResult {
        val scoreNames = ScoreStorage.listAndNormalizeScores(filesDir, logger)
        ScoreStorage.cleanExpiredCaches(filesDir, scoreNames.toSet(), logger)
        val firstScore = scoreNames.firstOrNull()
        val queue = configuredQueue?.filter { scoreNames.contains(it) } ?: listOfNotNull(firstScore)
        return MainRefreshResult(
            firstScoreName = firstScore,
            isCacheReady = queue.isNotEmpty() && queue.all { ScoreStorage.loadCache(filesDir, it, logger) != null }
        )
    }

    fun preloadFirstScore(filesDir: File, scoreName: String, logger: Logger = DefaultLogger): PreloadResult {
        val score = ScoreParser.loadScoreByName(filesDir, scoreName, logger)
            ?: return PreloadResult.Failure("曲谱解析失败：$scoreName")

        return try {
            val cache = ScoreStorage.buildCache(score)
            ScoreStorage.saveCache(filesDir, scoreName, cache, logger)
            if (ScoreStorage.loadCache(filesDir, scoreName, logger) == null) {
                PreloadResult.Failure("缓存保存失败：$scoreName")
            } else {
                PreloadResult.Success(scoreName)
            }
        } catch (ex: Exception) {
            logger.e(LogTags.CACHE_INVALID, "Failed to preload score: $scoreName", ex)
            PreloadResult.Failure("预加载失败：${ex.message ?: scoreName}")
        }
    }
}
