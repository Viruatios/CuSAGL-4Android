package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.Logger
import com.culoo.cusagl_4android.core.ScoreStorage
import java.io.File

data class MainRefreshSnapshot(
    val scoreNames: List<String>,
    val appliedConfig: AppliedPlaybackConfig,
    val mainRefresh: MainRefreshResult,
    val scoreEntries: List<ScoreEntry>? = null
)

object MainRefreshCoordinator {
    fun refresh(
        filesDir: File,
        includeScoreEntries: Boolean,
        logger: Logger = DefaultLogger
    ): MainRefreshSnapshot {
        val scoreNames = ScoreStorage.listAndNormalizeScores(filesDir, logger)
        ScoreStorage.cleanExpiredCaches(filesDir, scoreNames.toSet(), logger)
        val appliedConfig = PlaybackConfigController.loadAppliedWithScoreNames(filesDir, scoreNames)
        val mainRefresh = MainScreenController.refreshWithScoreNames(
            filesDir = filesDir,
            scoreNames = scoreNames,
            configuredQueue = appliedConfig.scoreNames,
            logger = logger
        )
        val scoreEntries = if (includeScoreEntries) {
            ScoreManagementController.buildEntries(filesDir, scoreNames, logger)
        } else {
            null
        }
        return MainRefreshSnapshot(
            scoreNames = scoreNames,
            appliedConfig = appliedConfig,
            mainRefresh = mainRefresh,
            scoreEntries = scoreEntries
        )
    }
}
