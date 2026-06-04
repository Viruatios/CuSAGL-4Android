package com.culoo.cusagl_4android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePlaybackEngineTest {
    @Test
    fun snapshot_reportsQueueAndNavigationAvailability() {
        val engine = RuntimePlaybackEngine(
            cacheProvider = EmptyCacheProvider,
            touchInjector = RecordingTouchInjector()
        )
        engine.updateConfig(PlaybackConfig(playType = PlayType.QUEUE_ONCE))
        engine.updateQueue(listOf("0001.first", "0002.second"))

        val initial = engine.getSnapshot()
        assertEquals("0001.first", initial.currentTrackName)
        assertFalse(initial.canPrevious)
        assertTrue(initial.canNext)

        engine.next()
        val next = engine.getSnapshot()
        assertEquals(PlaybackState.PAUSED, next.state)
        assertEquals("0002.second", next.currentTrackName)
        assertTrue(next.canPrevious)
        assertFalse(next.canNext)
    }

    @Test
    fun snapshotListener_receivesStateAndQueueChanges() {
        val engine = RuntimePlaybackEngine(
            cacheProvider = EmptyCacheProvider,
            touchInjector = RecordingTouchInjector()
        )
        val snapshots = mutableListOf<PlaybackSnapshot>()
        engine.addSnapshotListener { snapshots.add(it) }

        engine.updateQueue(listOf("0001.first"))
        engine.pause()

        assertTrue(snapshots.size >= 3)
        assertEquals(PlaybackState.PAUSED, snapshots.last().state)
        assertEquals("0001.first", snapshots.last().currentTrackName)
    }

    @Test
    fun emptyQueue_startStopsAndReportsError() {
        val engine = RuntimePlaybackEngine(
            cacheProvider = EmptyCacheProvider,
            touchInjector = RecordingTouchInjector()
        )

        engine.start()

        val snapshot = engine.getSnapshot()
        assertEquals(PlaybackState.STOPPED, snapshot.state)
        assertEquals("Playback queue is empty", snapshot.lastError)
    }

    private object EmptyCacheProvider : CacheProvider {
        override fun loadCache(name: String): CacheData? = null
    }

    private class RecordingTouchInjector : TouchInjector {
        override fun keyDown(key: String) = Unit
        override fun keyUp(key: String) = Unit
    }
}
