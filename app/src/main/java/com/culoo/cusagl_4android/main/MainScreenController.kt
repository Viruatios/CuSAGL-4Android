package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
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
    PLAYBACK_CONFIG,
    ABOUT
}

data class MainScreenState(
    val page: MainPage = MainPage.HOME,
    val firstScoreName: String? = null,
    val isCacheReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val hasOverlayPermission: Boolean = false,
    val hasAccessibility: Boolean = false,
    val playbackConfigSummary: UiText = UiText.resource(R.string.playback_summary_default),
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
    data class Success(val message: UiText) : PreloadResult()
    data class Failure(val message: UiText) : PreloadResult()
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
            ?: return PreloadResult.Failure(UiText.resource(R.string.error_score_parse_failed, scoreName))

        return try {
            val cache = ScoreStorage.buildCache(score)
            ScoreStorage.saveCache(filesDir, scoreName, cache, logger)
            if (ScoreStorage.loadCache(filesDir, scoreName, logger) == null) {
                PreloadResult.Failure(UiText.resource(R.string.error_cache_save_failed, scoreName))
            } else {
                PreloadResult.Success(UiText.resource(R.string.message_preload_single_success, scoreName))
            }
        } catch (ex: Exception) {
            logger.e(LogTags.CACHE_INVALID, "Failed to preload score: $scoreName", ex)
            PreloadResult.Failure(UiText.resource(R.string.error_preload_failed, ex.message ?: scoreName))
        }
    }
}
