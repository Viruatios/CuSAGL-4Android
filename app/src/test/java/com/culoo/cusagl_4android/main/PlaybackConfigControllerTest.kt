package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.PlayType
import com.culoo.cusagl_4android.core.ScoreStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Calendar

class PlaybackConfigControllerTest {
    @Test
    fun defaultConfig_withScores_usesSingleOnceAndFirstScore() {
        val scores = listOf("0001.first", "0002.second")

        val result = PlaybackConfigController.buildRequest(PlaybackConfigDraft(), scores)

        val success = result as PlaybackConfigApplyResult.Success
        assertEquals(PlayType.SINGLE_ONCE, success.applied.request!!.config.playType)
        assertEquals(listOf("0001.first"), success.applied.request.queue)
    }

    @Test
    fun modes_mapToPlayTypes() {
        val scores = listOf("0001.first", "0002.second")

        PlaybackConfigMode.allModes.forEach { mode ->
            val result = PlaybackConfigController.buildRequest(
                PlaybackConfigDraft(mode = mode),
                scores
            ) as PlaybackConfigApplyResult.Success

            assertEquals(mode.playType, result.applied.request!!.config.playType)
        }
    }

    @Test
    fun startTime_acceptsEmptyHourMinuteAndHourMinuteSecond() {
        val scores = listOf("0001.first")
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 7, 1, 2, 3)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expected = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 19)
            set(Calendar.SECOND, 10)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val empty = PlaybackConfigController.buildRequest(PlaybackConfigDraft(startTimeText = ""), scores, now)
            as PlaybackConfigApplyResult.Success
        val withSeconds = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(startTimeText = "19:19:10"),
            scores,
            now
        ) as PlaybackConfigApplyResult.Success
        val withoutSeconds = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(startTimeText = "19:19"),
            scores,
            now
        ) as PlaybackConfigApplyResult.Success

        assertEquals(0L, empty.applied.request!!.config.startTimeEpochMs)
        assertEquals(expected, withSeconds.applied.request!!.config.startTimeEpochMs)
        assertEquals(expected - 10_000L, withoutSeconds.applied.request!!.config.startTimeEpochMs)
    }

    @Test
    fun invalidStartTime_fails() {
        val result = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(startTimeText = "24:00:00"),
            listOf("0001.first")
        )

        assertTrue(result is PlaybackConfigApplyResult.Failure)
    }

    @Test
    fun nonNegativeNumericFields_defaultAndValidate() {
        val scores = listOf("0001.first", "0002.second")
        val empty = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_REPEAT),
            scores
        ) as PlaybackConfigApplyResult.Success
        val filled = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(
                mode = PlaybackConfigMode.QUEUE_REPEAT,
                queueIntervalSeconds = "2",
                repeatTimes = "3",
                repeatIntervalSeconds = "4"
            ),
            scores
        ) as PlaybackConfigApplyResult.Success
        val invalid = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_REPEAT, repeatTimes = "-1"),
            scores
        )

        assertEquals(0L, empty.applied.request!!.config.queueIntervalMs)
        assertEquals(0, empty.applied.request!!.config.repeatTimes)
        assertEquals(2_000L, filled.applied.request!!.config.queueIntervalMs)
        assertEquals(3, filled.applied.request!!.config.repeatTimes)
        assertEquals(4_000L, filled.applied.request!!.config.repeatIntervalMs)
        assertTrue(invalid is PlaybackConfigApplyResult.Failure)
    }

    @Test
    fun singleSelection_invalidFallsBackToFirstScore() {
        val result = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(selectedScoreName = "9999.missing"),
            listOf("0001.first", "0002.second")
        ) as PlaybackConfigApplyResult.Success

        assertEquals(listOf("0001.first"), result.applied.request!!.queue)
        assertEquals("0001.first", result.applied.draft.selectedScoreName)
    }

    @Test
    fun queueRules_matchPrefixesDeduplicateAndFallback() {
        val scores = listOf("0001.first", "0002.second", "0003.third")
        val empty = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_ONCE, queueText = ""),
            scores
        ) as PlaybackConfigApplyResult.Success
        val mixed = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_ONCE, queueText = "1 1 3 x -2"),
            scores
        ) as PlaybackConfigApplyResult.Success
        val fallback = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_ONCE, queueText = "9 x -2"),
            scores
        ) as PlaybackConfigApplyResult.Success

        assertEquals(scores, empty.applied.request!!.queue)
        assertEquals(listOf("0001.first", "0003.third"), mixed.applied.request!!.queue)
        assertEquals(scores, fallback.applied.request!!.queue)
    }

    @Test
    fun queueRules_keepCheckboxGeneratedOrderCompatibleWithOldTextFormat() {
        val scores = listOf("0001.first", "0002.second", "0003.third")

        val result = PlaybackConfigController.buildRequest(
            PlaybackConfigDraft(mode = PlaybackConfigMode.QUEUE_ONCE, queueText = "3 1"),
            scores
        ) as PlaybackConfigApplyResult.Success

        assertEquals(listOf("0003.third", "0001.first"), result.applied.request!!.queue)
    }

    @Test
    fun debugSwitch_isSavedAndRestored() {
        val tempDir = Files.createTempDirectory("cusagl-playback-config").toFile()
        writeScore(tempDir, "0001.first")
        val draft = PlaybackConfigDraft(debugEnabled = true)

        val saveResult = PlaybackConfigController.applyAndSave(tempDir, draft, DefaultLogger)
        val loaded = PlaybackConfigController.loadApplied(tempDir, DefaultLogger)

        assertTrue(saveResult is PlaybackConfigApplyResult.Success)
        assertTrue(loaded.draft.debugEnabled)
    }

    @Test
    fun deletedSelectedScore_fallsBackToAvailableScore() {
        val tempDir = Files.createTempDirectory("cusagl-playback-config-fallback").toFile()
        writeScore(tempDir, "0001.first")
        writeScore(tempDir, "0002.second")
        val draft = PlaybackConfigDraft(selectedScoreName = "0002.second")

        PlaybackConfigController.applyAndSave(tempDir, draft, DefaultLogger)
        ScoreManagementController.deleteScore(tempDir, "0002.second")
        val loaded = PlaybackConfigController.loadApplied(tempDir, DefaultLogger)

        assertEquals(listOf("0001.first"), loaded.request!!.queue)
        assertEquals("0001.first", loaded.draft.selectedScoreName)
    }

    private fun writeScore(tempDir: File, scoreName: String) {
        val scoreDir = ScoreStorage.scoreDir(tempDir)
        scoreDir.mkdirs()
        File(scoreDir, "$scoreName.json").writeText(
            """
            {
              "name": "Test Song",
              "author": "Tester",
              "type": "keyboard",
              "bpm": 120,
              "time_signature": "4/4",
              "notes": "A B /"
            }
            """.trimIndent()
        )
    }
}
