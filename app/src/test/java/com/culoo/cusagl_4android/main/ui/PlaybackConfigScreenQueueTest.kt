package com.culoo.cusagl_4android.main.ui

import com.culoo.cusagl_4android.main.ScoreEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackConfigScreenQueueTest {
    @Test
    fun deriveQueueSelection_emptyQueue_returnsEmptySelection() {
        val state = deriveQueueSelection(sampleEntries(), "")

        assertEquals(emptyList<String>(), state.selectedNames)
        assertEquals(emptyMap<String, Int>(), state.orderByName)
        assertEquals(
            mapOf(
                "0001.first" to 1,
                "0002.second" to 2,
                "0003.third" to 3
            ),
            state.indexByName
        )
    }

    @Test
    fun deriveQueueSelection_ignoresInvalidDuplicateAndOutOfRangeIndexes() {
        val state = deriveQueueSelection(sampleEntries(), "1 1 3 x -2 9")

        assertEquals(listOf("0001.first", "0003.third"), state.selectedNames)
        assertEquals(
            mapOf(
                "0001.first" to 1,
                "0003.third" to 2
            ),
            state.orderByName
        )
    }

    @Test
    fun queueTextFromSelected_preservesClickOrderAndOldIndexFormat() {
        val state = deriveQueueSelection(sampleEntries(), "")

        val queueText = queueTextFromSelected(
            selectedNames = listOf("0003.third", "0001.first"),
            indexByName = state.indexByName
        )

        assertEquals("3 1", queueText)
    }

    @Test
    fun queueTextFromSelected_skipsDeletedOrMissingScores() {
        val state = deriveQueueSelection(sampleEntries(), "")

        val queueText = queueTextFromSelected(
            selectedNames = listOf("0002.second", "9999.missing", "0001.first"),
            indexByName = state.indexByName
        )

        assertEquals("2 1", queueText)
    }

    private fun sampleEntries(): List<ScoreEntry> {
        return listOf(
            ScoreEntry("0001.first", "first", hasCache = false, lastModifiedMs = 0L),
            ScoreEntry("0002.second", "second", hasCache = false, lastModifiedMs = 0L),
            ScoreEntry("0003.third", "third", hasCache = false, lastModifiedMs = 0L)
        )
    }
}
