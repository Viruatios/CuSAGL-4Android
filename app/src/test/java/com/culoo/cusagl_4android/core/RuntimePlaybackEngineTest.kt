package com.culoo.cusagl_4android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun pause_cancelsFutureEventsAndReleasesTouches() {
        val sleeper = BlockingSleeper()
        val injector = RecordingTouchInjector(targetAction = "down:Q")
        val engine = RuntimePlaybackEngine(
            cacheProvider = StaticCacheProvider(
                cacheOf(
                    MergedEvent(0, ActionType.DOWN, listOf("Q")),
                    MergedEvent(100, ActionType.DOWN, listOf("W"))
                )
            ),
            touchInjector = injector,
            timeSource = FakeTimeSource(),
            sleeper = sleeper
        )
        engine.updateConfig(PlaybackConfig(spinThresholdMs = 0))
        engine.updateQueue(listOf("0001.first"))

        engine.start()
        assertTrue(injector.awaitTarget())
        assertTrue(sleeper.awaitSleep())

        engine.pause()
        sleeper.release()

        assertTrue(waitUntil { engine.getSnapshot().state == PlaybackState.PAUSED })
        assertFalse(injector.actions.contains("down:W"))
        assertTrue(injector.actions.contains("up:Q"))
    }

    @Test
    fun stop_resetsQueueIndexAndReleasesTouches() {
        val injector = RecordingTouchInjector()
        val engine = RuntimePlaybackEngine(
            cacheProvider = EmptyCacheProvider,
            touchInjector = injector
        )
        engine.updateConfig(PlaybackConfig(playType = PlayType.QUEUE_ONCE))
        engine.updateQueue(listOf("0001.first", "0002.second"))
        engine.next()
        injector.clear()

        engine.stop()

        val snapshot = engine.getSnapshot()
        assertEquals(PlaybackState.STOPPED, snapshot.state)
        assertEquals(0, snapshot.currentIndex)
        assertEquals("0001.first", snapshot.currentTrackName)
        assertTrue(injector.actions.contains("up:Q"))
        assertTrue(injector.actions.contains("up:M"))
    }

    @Test
    fun repeatedStart_doesNotCreateParallelPlaybackJobs() {
        val sleeper = BlockingSleeper()
        val cacheProvider = CountingCacheProvider(
            cacheOf(MergedEvent(100, ActionType.DOWN, listOf("Q")))
        )
        val injector = RecordingTouchInjector()
        val engine = RuntimePlaybackEngine(
            cacheProvider = cacheProvider,
            touchInjector = injector,
            timeSource = FakeTimeSource(),
            sleeper = sleeper
        )
        engine.updateConfig(PlaybackConfig(spinThresholdMs = 0))
        engine.updateQueue(listOf("0001.first"))

        engine.start()
        assertTrue(sleeper.awaitSleep())
        engine.start()
        sleeper.release()

        assertTrue(waitUntil { engine.getSnapshot().state == PlaybackState.STOPPED })
        assertEquals(1, cacheProvider.loadCount.get())
        assertEquals(1, injector.actions.count { it == "down:Q" })
    }

    @Test
    fun zeroSpinThreshold_usesSleepOnlyAndPreservesEventOrder() {
        val sleeper = AutoAdvanceSleeper()
        val injector = RecordingTouchInjector()
        val engine = RuntimePlaybackEngine(
            cacheProvider = StaticCacheProvider(
                cacheOf(
                    MergedEvent(10, ActionType.DOWN, listOf("Q")),
                    MergedEvent(20, ActionType.UP, listOf("Q"))
                )
            ),
            touchInjector = injector,
            timeSource = sleeper.timeSource,
            sleeper = sleeper
        )
        engine.updateConfig(PlaybackConfig(spinThresholdMs = 0))
        engine.updateQueue(listOf("0001.first"))

        engine.start()

        assertTrue(waitUntil { engine.getSnapshot().state == PlaybackState.STOPPED })
        assertEquals(listOf("down:Q", "up:Q"), injector.actions.filter { it.endsWith(":Q") })
        assertEquals(listOf(10L, 10L), sleeper.sleepDurations.take(2))
    }

    private object EmptyCacheProvider : CacheProvider {
        override fun loadCache(name: String): CacheData? = null
    }

    private class StaticCacheProvider(private val cache: CacheData) : CacheProvider {
        override fun loadCache(name: String): CacheData = cache
    }

    private class CountingCacheProvider(private val cache: CacheData) : CacheProvider {
        val loadCount = AtomicInteger(0)

        override fun loadCache(name: String): CacheData {
            loadCount.incrementAndGet()
            return cache
        }
    }

    private class RecordingTouchInjector(
        private val targetAction: String? = null
    ) : TouchInjector {
        val actions: MutableList<String> = Collections.synchronizedList(mutableListOf())
        private val targetLatch = CountDownLatch(if (targetAction == null) 0 else 1)

        override fun keyDown(key: String) {
            record("down:$key")
        }

        override fun keyUp(key: String) {
            record("up:$key")
        }

        fun awaitTarget(): Boolean = targetLatch.await(1, TimeUnit.SECONDS)

        fun clear() {
            actions.clear()
        }

        private fun record(action: String) {
            actions.add(action)
            if (action == targetAction) {
                targetLatch.countDown()
            }
        }
    }

    private class FakeTimeSource : TimeSource {
        private var currentMs: Long = 0

        override fun nowMs(): Long = synchronized(this) {
            currentMs
        }

        fun advanceBy(durationMs: Long) {
            synchronized(this) {
                currentMs += durationMs
            }
        }
    }

    private class AutoAdvanceSleeper : Sleeper {
        val timeSource = FakeTimeSource()
        val sleepDurations: MutableList<Long> = Collections.synchronizedList(mutableListOf())

        override suspend fun sleepMs(durationMs: Long) {
            sleepDurations.add(durationMs)
            timeSource.advanceBy(durationMs)
        }
    }

    private class BlockingSleeper : Sleeper {
        private val sleepStarted = CountDownLatch(1)
        private val releaseSleep = CountDownLatch(1)

        override suspend fun sleepMs(durationMs: Long) {
            sleepStarted.countDown()
            releaseSleep.await(1, TimeUnit.SECONDS)
        }

        fun awaitSleep(): Boolean = sleepStarted.await(1, TimeUnit.SECONDS)

        fun release() {
            releaseSleep.countDown()
        }
    }

    private fun cacheOf(vararg events: MergedEvent): CacheData {
        return CacheData(
            name = "test",
            author = "tester",
            barCount = 1,
            eventBatchCount = events.size,
            expectedDurationMs = events.maxOfOrNull { it.timeMs } ?: 0,
            createTimeMs = 0L,
            gapMs = CoreConstants.MIN_KEY_UP_GAP_MS.toDouble(),
            mergedTimeline = events.toList()
        )
    }

    private fun waitUntil(predicate: () -> Boolean): Boolean {
        repeat(50) {
            if (predicate()) return true
            Thread.sleep(20)
        }
        return predicate()
    }
}
