package com.culoo.cusagl_4android.core

import com.culoo.cusagl_4android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ScoreParserTest {
    @Test
    fun parseNotes_basicLine() {
        val notes = "A B /"
        val bars = ScoreParser.parseNotes(notes)

        assertEquals(1, bars.size)
        val bar = bars.first()
        assertEquals(2, bar.beats)
        assertEquals(2, bar.units.size)
        assertTrue(bar.units[0] is UnitNote.Single)
        assertTrue(bar.units[1] is UnitNote.Single)
    }

    @Test
    fun listAndNormalizeScores_renamesInvalidFiles() {
        val tempDir = Files.createTempDirectory("cusagl-test").toFile()
        val scoreDir = ScoreStorage.scoreDir(tempDir)
        scoreDir.mkdirs()

        File(scoreDir, "foo.json").writeText("{}")
        File(scoreDir, "0002.bar.json").writeText("{}")

        val result = ScoreStorage.listAndNormalizeScores(tempDir, DefaultLogger)

        assertEquals(listOf("0001.foo", "0002.bar"), result)
        assertTrue(File(scoreDir, "0001.foo.json").exists())
        assertTrue(File(scoreDir, "0002.bar.json").exists())
    }

    @Test
    fun strictParse_invalidJson_reportsLineAndColumn() {
        val result = ScoreParser.parseScoreTextStrict(
            text = "{\n  \"name\": \"Bad\",\n",
            source = "bad.json"
        )

        val message = (result as ScoreParseResult.Failure).message
        assertEquals(R.string.error_score_json_validation_detail, message.resId)
        assertEquals("bad.json", message.args[0])
        assertEquals("JSON", message.args[1])
        assertTrue(message.args[2].toString().contains("line"))
        assertTrue(message.args[2].toString().contains("column"))
    }

    @Test
    fun strictParse_fieldFailures_reportFieldNamesAndLocations() {
        val cases = listOf(
            """{"name":"","bpm":120,"time_signature":"4/4","notes":"A /"}""" to "name",
            """{"name":"Bad","bpm":0,"time_signature":"4/4","notes":"A /"}""" to "bpm",
            """{"name":"Bad","bpm":120,"time_signature":"3/x","notes":"A /"}""" to "time_signature",
            """{"name":"Bad","bpm":120,"time_signature":"4/4","notes":""}""" to "notes"
        )

        cases.forEach { (text, field) ->
            val message = (ScoreParser.parseScoreTextStrict(text, source = "score text") as ScoreParseResult.Failure).message

            assertEquals(R.string.error_score_json_validation_detail, message.resId)
            assertEquals(field, message.args[1])
            assertTrue(message.args[2].toString().contains("line"))
            assertTrue(message.args[2].toString().contains("column"))
        }
    }
}

