package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.ScoreParser
import com.culoo.cusagl_4android.core.ScoreStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MainScreenControllerTest {
    @Test
    fun refresh_withoutScores_disablesPreloadAndPrepare() {
        val tempDir = Files.createTempDirectory("cusagl-main-empty").toFile()

        val result = MainScreenController.refresh(tempDir, logger = DefaultLogger)
        val state = MainScreenState(
            firstScoreName = result.firstScoreName,
            isCacheReady = result.isCacheReady,
            hasOverlayPermission = true,
            hasAccessibility = true
        )

        assertEquals(null, result.firstScoreName)
        assertFalse(result.isCacheReady)
        assertFalse(state.canPreload)
        assertFalse(state.canPreparePlayback)
    }

    @Test
    fun preloadFirstScore_withoutCache_writesCacheAndEnablesPrepare() {
        val tempDir = Files.createTempDirectory("cusagl-main-preload").toFile()
        writeScore(tempDir, "0001.test")

        val preload = MainScreenController.preloadFirstScore(tempDir, "0001.test", DefaultLogger)
        val refresh = MainScreenController.refresh(tempDir, logger = DefaultLogger)
        val state = MainScreenState(
            firstScoreName = refresh.firstScoreName,
            isCacheReady = refresh.isCacheReady,
            hasOverlayPermission = true,
            hasAccessibility = true,
            hasPlaybackRequest = true
        )

        assertTrue(preload is PreloadResult.Success)
        assertTrue(ScoreStorage.cacheFile(tempDir, "0001.test").exists())
        assertTrue(refresh.isCacheReady)
        assertTrue(state.canPreparePlayback)
    }

    @Test
    fun refresh_configuredQueueRequiresAllCachesReady() {
        val tempDir = Files.createTempDirectory("cusagl-main-queue-cache").toFile()
        writeScore(tempDir, "0001.first")
        writeScore(tempDir, "0002.second")
        val first = ScoreParser.loadScoreByName(tempDir, "0001.first", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0001.first", ScoreStorage.buildCache(first), DefaultLogger)

        val partial = MainScreenController.refresh(
            tempDir,
            configuredQueue = listOf("0001.first", "0002.second"),
            logger = DefaultLogger
        )
        val second = ScoreParser.loadScoreByName(tempDir, "0002.second", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0002.second", ScoreStorage.buildCache(second), DefaultLogger)
        val complete = MainScreenController.refresh(
            tempDir,
            configuredQueue = listOf("0001.first", "0002.second"),
            logger = DefaultLogger
        )

        assertFalse(partial.isCacheReady)
        assertTrue(complete.isCacheReady)
    }

    @Test
    fun preloadConfiguredQueue_writesCacheForEachScore() {
        val tempDir = Files.createTempDirectory("cusagl-main-queue-preload").toFile()
        writeScore(tempDir, "0001.first")
        writeScore(tempDir, "0002.second")

        val result = PlaybackConfigController.preloadScores(
            tempDir,
            listOf("0001.first", "0002.second"),
            DefaultLogger
        )

        assertTrue(result is PreloadResult.Success)
        assertTrue(ScoreStorage.cacheFile(tempDir, "0001.first").exists())
        assertTrue(ScoreStorage.cacheFile(tempDir, "0002.second").exists())
    }

    @Test
    fun refresh_withExistingCache_marksReady() {
        val tempDir = Files.createTempDirectory("cusagl-main-cache").toFile()
        writeScore(tempDir, "0001.test")
        val score = ScoreParser.loadScoreByName(tempDir, "0001.test", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0001.test", ScoreStorage.buildCache(score), DefaultLogger)

        val result = MainScreenController.refresh(tempDir, logger = DefaultLogger)

        assertEquals("0001.test", result.firstScoreName)
        assertTrue(result.isCacheReady)
    }

    @Test
    fun preloadFirstScore_withInvalidScore_returnsFailure() {
        val tempDir = Files.createTempDirectory("cusagl-main-invalid").toFile()
        val scoreDir = ScoreStorage.scoreDir(tempDir)
        scoreDir.mkdirs()
        File(scoreDir, "0001.bad.json").writeText("""{"name":"Bad"}""")

        val result = MainScreenController.preloadFirstScore(tempDir, "0001.bad", DefaultLogger)

        assertTrue(result is PreloadResult.Failure)
        assertFalse(ScoreStorage.cacheFile(tempDir, "0001.bad").exists())
    }

    @Test
    fun refresh_withStaleCache_removesCacheAndMarksNotReady() {
        val tempDir = Files.createTempDirectory("cusagl-main-stale").toFile()
        writeScore(tempDir, "0001.test")
        val score = ScoreParser.loadScoreByName(tempDir, "0001.test", DefaultLogger)!!
        val cacheFile = ScoreStorage.cacheFile(tempDir, "0001.test")
        ScoreStorage.saveCache(tempDir, "0001.test", ScoreStorage.buildCache(score), DefaultLogger)

        val scoreFile = ScoreStorage.scoreFile(tempDir, "0001.test")
        cacheFile.setLastModified(1_000L)
        scoreFile.setLastModified(2_000L)

        val result = MainScreenController.refresh(tempDir, logger = DefaultLogger)

        assertEquals("0001.test", result.firstScoreName)
        assertFalse(cacheFile.exists())
        assertFalse(result.isCacheReady)
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
