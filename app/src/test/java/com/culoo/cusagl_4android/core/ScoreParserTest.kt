package com.culoo.cusagl_4android.core

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
}

