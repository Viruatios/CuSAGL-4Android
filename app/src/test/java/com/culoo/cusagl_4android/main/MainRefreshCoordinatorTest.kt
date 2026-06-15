package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.ScoreParser
import com.culoo.cusagl_4android.core.ScoreStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MainRefreshCoordinatorTest {
    @Test
    fun refresh_forHomeDoesNotBuildScoreEntries() {
        val tempDir = Files.createTempDirectory("cusagl-refresh-home").toFile()
        writeScore(tempDir, "0001.first")

        val snapshot = MainRefreshCoordinator.refresh(
            filesDir = tempDir,
            includeScoreEntries = false,
            logger = DefaultLogger
        )

        assertEquals(listOf("0001.first"), snapshot.scoreNames)
        assertNull(snapshot.scoreEntries)
        assertEquals(listOf("0001.first"), snapshot.appliedConfig.scoreNames)
        assertFalse(snapshot.mainRefresh.isCacheReady)
    }

    @Test
    fun refresh_forScoreManagementBuildsEntriesFromSameScoreNames() {
        val tempDir = Files.createTempDirectory("cusagl-refresh-entries").toFile()
        writeScore(tempDir, "0001.first")
        writeScore(tempDir, "0002.second")
        val first = ScoreParser.loadScoreByName(tempDir, "0001.first", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0001.first", ScoreStorage.buildCache(first), DefaultLogger)

        val snapshot = MainRefreshCoordinator.refresh(
            filesDir = tempDir,
            includeScoreEntries = true,
            logger = DefaultLogger
        )

        val entries = snapshot.scoreEntries
        assertNotNull(entries)
        assertEquals(snapshot.scoreNames, entries!!.map { it.storageName })
        assertEquals(listOf("0001.first"), snapshot.appliedConfig.scoreNames)
        assertTrue(entries.first { it.storageName == "0001.first" }.hasCache)
        assertFalse(entries.first { it.storageName == "0002.second" }.hasCache)
    }

    @Test
    fun refresh_usesAppliedQueueForCacheReady() {
        val tempDir = Files.createTempDirectory("cusagl-refresh-queue").toFile()
        writeScore(tempDir, "0001.first")
        writeScore(tempDir, "0002.second")
        val draft = PlaybackConfigDraft(
            mode = PlaybackConfigMode.QUEUE_ONCE,
            queueText = "1 2"
        )
        PlaybackConfigController.applyAndSave(tempDir, draft, DefaultLogger)
        val first = ScoreParser.loadScoreByName(tempDir, "0001.first", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0001.first", ScoreStorage.buildCache(first), DefaultLogger)

        val partial = MainRefreshCoordinator.refresh(tempDir, includeScoreEntries = false, logger = DefaultLogger)
        val second = ScoreParser.loadScoreByName(tempDir, "0002.second", DefaultLogger)!!
        ScoreStorage.saveCache(tempDir, "0002.second", ScoreStorage.buildCache(second), DefaultLogger)
        val complete = MainRefreshCoordinator.refresh(tempDir, includeScoreEntries = false, logger = DefaultLogger)

        assertEquals(listOf("0001.first", "0002.second"), partial.appliedConfig.scoreNames)
        assertFalse(partial.mainRefresh.isCacheReady)
        assertTrue(complete.mainRefresh.isCacheReady)
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
