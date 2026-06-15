package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.ScoreParser
import com.culoo.cusagl_4android.core.ScoreStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ScoreManagementControllerTest {
    @Test
    fun manualScoreDraft_defaultsToEmptyFields() {
        val draft = ManualScoreDraft()

        assertEquals("", draft.name)
        assertEquals("", draft.author)
        assertEquals("", draft.instrument)
        assertEquals("", draft.description)
        assertEquals("", draft.bpm)
        assertEquals("", draft.timeSignature)
        assertEquals("", draft.composer)
        assertEquals("", draft.arranger)
        assertEquals("", draft.notes)
    }

    @Test
    fun listScores_emptyDirectory_returnsEmptyList() {
        val tempDir = Files.createTempDirectory("cusagl-score-empty").toFile()

        val entries = ScoreManagementController.listScores(tempDir, DefaultLogger)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun importScoreText_validJson_writesNextNumberedFile() {
        val tempDir = Files.createTempDirectory("cusagl-score-import").toFile()

        val result = ScoreManagementController.importScoreText(
            filesDir = tempDir,
            sourceFileName = "song.json",
            text = validScoreJson("Test Song"),
            overwriteConfirmed = false,
            logger = DefaultLogger
        )

        assertTrue(result is ScoreSaveResult.Success)
        assertTrue(ScoreStorage.scoreFile(tempDir, "0001.Test Song").exists())
    }

    @Test
    fun importScoreText_invalidInputs_doNotWriteFiles() {
        val tempDir = Files.createTempDirectory("cusagl-score-invalid").toFile()
        val invalidTexts = listOf(
            """{"name":"","bpm":120,"time_signature":"4/4","notes":"A /"}""",
            """{"name":"Bad","bpm":0,"time_signature":"4/4","notes":"A /"}""",
            """{"name":"Bad","bpm":120,"time_signature":"3/x","notes":"A /"}""",
            """{"name":"Bad","bpm":120,"time_signature":"4/4","notes":""}""",
            """not json"""
        )

        invalidTexts.forEachIndexed { index, text ->
            val result = ScoreManagementController.importScoreText(
                filesDir = tempDir,
                sourceFileName = "bad$index.json",
                text = text,
                overwriteConfirmed = false,
                logger = DefaultLogger
            )

            assertTrue(result is ScoreSaveResult.Failure)
        }
        assertTrue(ScoreManagementController.listScores(tempDir, DefaultLogger).isEmpty())
    }

    @Test
    fun importScoreText_invalidJsonReportsReadableFieldMessage() {
        val tempDir = Files.createTempDirectory("cusagl-score-invalid-readable").toFile()

        val result = ScoreManagementController.importScoreText(
            filesDir = tempDir,
            sourceFileName = "broken.json",
            text = """{"name":"","bpm":120,"time_signature":"4/4","notes":"A /"}""",
            overwriteConfirmed = false,
            logger = DefaultLogger
        )

        val message = (result as ScoreSaveResult.Failure).message
        assertEquals(R.string.error_score_json_score_name_empty, message.resId)
        assertTrue(ScoreManagementController.listScores(tempDir, DefaultLogger).isEmpty())
    }

    @Test
    fun importScoreText_invalidSyntaxStillReportsLineAndColumn() {
        val tempDir = Files.createTempDirectory("cusagl-score-invalid-json").toFile()

        val result = ScoreManagementController.importScoreText(
            filesDir = tempDir,
            sourceFileName = "broken.json",
            text = "{\n  \"name\": \"Broken\",\n",
            overwriteConfirmed = false,
            logger = DefaultLogger
        )

        val message = (result as ScoreSaveResult.Failure).message
        assertEquals(R.string.error_score_json_invalid_syntax, message.resId)
        assertTrue((message.args[0] as Int) > 0)
        assertTrue((message.args[1] as Int) > 0)
        assertTrue(ScoreManagementController.listScores(tempDir, DefaultLogger).isEmpty())
    }

    @Test
    fun fieldForValidationMessage_mapsReadableErrorsToManualFields() {
        val cases = listOf(
            R.string.error_score_json_score_name_empty to ManualScoreField.NAME,
            R.string.error_score_name_invalid_file_name to ManualScoreField.NAME,
            R.string.error_bpm_positive_integer to ManualScoreField.BPM,
            R.string.error_score_json_time_signature_invalid to ManualScoreField.TIME_SIGNATURE,
            R.string.error_score_json_notes_empty to ManualScoreField.NOTES,
            R.string.error_score_json_notes_unparseable to ManualScoreField.NOTES
        )

        cases.forEach { (resId, field) ->
            assertEquals(
                field,
                ScoreManagementController.fieldForValidationMessage(com.culoo.cusagl_4android.UiText.resource(resId))
            )
        }
        assertEquals(
            null,
            ScoreManagementController.fieldForValidationMessage(
                com.culoo.cusagl_4android.UiText.resource(R.string.error_score_json_invalid_syntax, 1, 1)
            )
        )
    }

    @Test
    fun saveManualScore_validDraft_writesParsableJson() {
        val tempDir = Files.createTempDirectory("cusagl-score-manual").toFile()
        val draft = ManualScoreDraft(
            name = "Manual Song",
            bpm = "120",
            timeSignature = "4/4",
            notes = "A B /"
        )

        val result = ScoreManagementController.saveManualScore(
            filesDir = tempDir,
            draft = draft,
            overwriteConfirmed = false,
            logger = DefaultLogger
        )

        assertTrue(result is ScoreSaveResult.Success)
        assertTrue(ScoreStorage.scoreFile(tempDir, "0001.Manual Song").exists())
        assertEquals(
            "Manual Song",
            ScoreParser.loadScoreByName(tempDir, "0001.Manual Song", DefaultLogger)?.name
        )
    }

    @Test
    fun saveManualScore_duplicateNeedsConfirmation_thenOverwritesAndDeletesCache() {
        val tempDir = Files.createTempDirectory("cusagl-score-overwrite").toFile()
        val initial = ManualScoreDraft(name = "Same", bpm = "120", timeSignature = "4/4", notes = "A /")
        val replacement = ManualScoreDraft(name = "Same", bpm = "90", timeSignature = "3/4", notes = "B /")

        val first = ScoreManagementController.saveManualScore(tempDir, initial, false, DefaultLogger)
        assertTrue(first is ScoreSaveResult.Success)
        File(ScoreStorage.cacheDir(tempDir), "0001.Same.json").also {
            it.parentFile!!.mkdirs()
            it.writeText("""{"stale":true}""")
        }

        val duplicate = ScoreManagementController.saveManualScore(tempDir, replacement, false, DefaultLogger)
        assertTrue(duplicate is ScoreSaveResult.NeedsOverwrite)
        assertTrue(ScoreStorage.cacheFile(tempDir, "0001.Same").exists())

        val overwrite = ScoreManagementController.saveManualScore(tempDir, replacement, true, DefaultLogger)

        assertTrue(overwrite is ScoreSaveResult.Success)
        assertEquals("0001.Same", (overwrite as ScoreSaveResult.Success).storageName)
        assertFalse(ScoreStorage.cacheFile(tempDir, "0001.Same").exists())
        assertEquals(90, ScoreParser.loadScoreByName(tempDir, "0001.Same", DefaultLogger)?.bpm)
    }

    @Test
    fun deleteScore_removesScoreAndCache() {
        val tempDir = Files.createTempDirectory("cusagl-score-delete").toFile()
        ScoreManagementController.saveManualScore(
            tempDir,
            ManualScoreDraft(name = "Delete Me", bpm = "120", timeSignature = "4/4", notes = "A /"),
            false,
            DefaultLogger
        )
        ScoreStorage.cacheDir(tempDir).mkdirs()
        ScoreStorage.cacheFile(tempDir, "0001.Delete Me").writeText("""{"stale":true}""")

        val result = ScoreManagementController.deleteScore(tempDir, "0001.Delete Me")

        assertTrue(result is ScoreDeleteResult.Success)
        assertFalse(ScoreStorage.scoreFile(tempDir, "0001.Delete Me").exists())
        assertFalse(ScoreStorage.cacheFile(tempDir, "0001.Delete Me").exists())
    }

    @Test
    fun listScores_returnsNumericOrder() {
        val tempDir = Files.createTempDirectory("cusagl-score-order").toFile()
        val scoreDir = ScoreStorage.scoreDir(tempDir)
        scoreDir.mkdirs()
        File(scoreDir, "0002.Second.json").writeText(validScoreJson("Second"))
        File(scoreDir, "0001.First.json").writeText(validScoreJson("First"))

        val entries = ScoreManagementController.listScores(tempDir, DefaultLogger)

        assertEquals(listOf("0001.First", "0002.Second"), entries.map { it.storageName })
    }

    @Test
    fun listScores_withCorruptCache_doesNotMarkCacheReady() {
        val tempDir = Files.createTempDirectory("cusagl-score-cache-corrupt").toFile()
        val scoreDir = ScoreStorage.scoreDir(tempDir)
        scoreDir.mkdirs()
        File(scoreDir, "0001.First.json").writeText(validScoreJson("First"))
        ScoreStorage.cacheDir(tempDir).mkdirs()
        ScoreStorage.cacheFile(tempDir, "0001.First").writeText("""{"stale":true}""")

        val entries = ScoreManagementController.listScores(tempDir, DefaultLogger)

        assertEquals(listOf("0001.First"), entries.map { it.storageName })
        assertFalse(entries.first().hasCache)
    }

    private fun validScoreJson(name: String): String {
        return """
            {
              "name": "$name",
              "author": "Tester",
              "instrument": "风物之诗琴",
              "description": "无",
              "type": "keyboard",
              "bpm": 120,
              "time_signature": "4/4",
              "composer": "HoYo-Mix",
              "arranger": "HoYo-Mix",
              "notes": "A B /"
            }
        """.trimIndent()
    }
}
