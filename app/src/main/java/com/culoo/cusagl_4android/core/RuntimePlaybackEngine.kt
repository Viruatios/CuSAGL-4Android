package com.culoo.cusagl_4android.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

enum class PlaybackState {
    IDLE,
    PLAYING,
    PAUSED,
    STOPPED
}

class RuntimePlaybackEngine(
    private val cacheProvider: CacheProvider,
    private val touchInjector: TouchInjector,
    private val timeSource: TimeSource = SystemClockTimeSource(),
    private val sleeper: Sleeper = ThreadSleeper(),
    private val logger: Logger = DefaultLogger,
    private val playbackScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val lock = Any()
    private val listeners = linkedSetOf<PlaybackSnapshotListener>()

    @Volatile
    private var state: PlaybackState = PlaybackState.IDLE
    @Volatile
    private var stopRequested: Boolean = false
    @Volatile
    private var playbackGeneration: Long = 0

    private var config: PlaybackConfig = PlaybackConfig()
    private var queue: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var playbackJob: Job? = null
    @Volatile
    private var lastError: String? = null

    fun updateConfig(newConfig: PlaybackConfig) {
        synchronized(lock) {
            config = newConfig.normalized()
        }
        publishSnapshot()
    }

    fun updateQueue(newQueue: List<String>) {
        val normalized = newQueue.filter { it.isNotBlank() }.distinct()
        synchronized(lock) {
            queue = normalized
            if (currentIndex >= normalized.size) {
                currentIndex = 0
            }
            lastError = null
        }
        publishSnapshot()
    }

    fun getState(): PlaybackState = state

    fun getSnapshot(): PlaybackSnapshot = synchronized(lock) {
        buildSnapshot()
    }

    fun addSnapshotListener(listener: PlaybackSnapshotListener) {
        val snapshot = synchronized(lock) {
            listeners.add(listener)
            buildSnapshot()
        }
        listener.onPlaybackSnapshotChanged(snapshot)
    }

    fun removeSnapshotListener(listener: PlaybackSnapshotListener) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }

    fun start() {
        var shouldPublish = false
        synchronized(lock) {
            if (state == PlaybackState.PLAYING) return
            if (queue.isEmpty()) {
                logger.w(LogTags.FILE_MISSING, "Playback queue is empty")
                lastError = "Playback queue is empty"
                state = PlaybackState.STOPPED
                shouldPublish = true
            } else {
                stopRequested = false
                val generation = playbackGeneration + 1
                playbackGeneration = generation
                state = PlaybackState.PLAYING
                lastError = null
                playbackJob = playbackScope.launch {
                    runPlayback(generation)
                }
                shouldPublish = true
            }
        }
        if (shouldPublish) publishSnapshot()
    }

    fun close() {
        pauseInternal(PlaybackState.STOPPED, resetQueueIndex = true)
        playbackScope.cancel()
    }

    fun pause() {
        pauseInternal(PlaybackState.PAUSED, resetQueueIndex = false)
    }

    fun stop() {
        pauseInternal(PlaybackState.STOPPED, resetQueueIndex = true)
    }

    fun previous() {
        pauseInternal(PlaybackState.PAUSED, resetQueueIndex = false)
        if (queue.isEmpty()) return
        if (!config.isQueueMode()) return

        currentIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            if (config.playType == PlayType.QUEUE_REPEAT) queue.lastIndex else currentIndex
        }
        publishSnapshot()
    }

    fun next() {
        pauseInternal(PlaybackState.PAUSED, resetQueueIndex = false)
        if (queue.isEmpty()) return
        if (!config.isQueueMode()) return

        currentIndex = if (currentIndex < queue.lastIndex) {
            currentIndex + 1
        } else {
            if (config.playType == PlayType.QUEUE_REPEAT) 0 else currentIndex
        }
        publishSnapshot()
    }

    fun releaseAllTouches() {
        touchInjector.releaseAll(KeyLayout.allKeys)
    }

    private fun pauseInternal(targetState: PlaybackState, resetQueueIndex: Boolean) {
        stopRequested = true
        playbackGeneration++
        playbackJob?.cancel()
        playbackJob = null
        releaseAllTouches()
        synchronized(lock) {
            if (resetQueueIndex) {
                currentIndex = 0
            }
            state = targetState
        }
        publishSnapshot()
    }

    private suspend fun runPlayback(generation: Long) {
        val (snapshotConfig, snapshotQueue) = synchronized(lock) {
            config.normalized() to queue
        }

        if (!waitUntilStart(snapshotConfig, generation)) {
            return
        }

        val isQueueMode = snapshotConfig.isQueueMode()
        val isRepeatMode = snapshotConfig.isRepeatMode()
        val alwaysRepeat = isRepeatMode && snapshotConfig.repeatTimes == 0
        var remainRounds = if (isRepeatMode) max(1, snapshotConfig.repeatTimes) else 1

        var index = currentIndex.coerceIn(0, snapshotQueue.lastIndex)
        var successfulTracks = 0
        do {
            val roundStartIndex = if (isQueueMode) index else currentIndex
            index = roundStartIndex

            while (index <= snapshotQueue.lastIndex) {
                if (isCancelled(generation)) {
                    return
                }
                currentIndex = index
                publishSnapshot()
                val musicName = snapshotQueue[index]
                val played = playTrack(musicName, snapshotConfig, generation)
                if (played) {
                    successfulTracks++
                }
                if (!played && isCancelled(generation)) {
                    return
                }

                if (isQueueMode && snapshotConfig.queueIntervalMs > 0 && index < snapshotQueue.lastIndex) {
                    if (!waitForDuration(snapshotConfig.queueIntervalMs, generation)) return
                }

                if (!isQueueMode) {
                    break
                }
                index++
            }

            if (!alwaysRepeat) {
                remainRounds--
            }

            if (isRepeatMode && snapshotConfig.repeatIntervalMs > 0 && (alwaysRepeat || remainRounds > 0)) {
                if (!waitForDuration(snapshotConfig.repeatIntervalMs, generation)) return
            }

            index = if (isQueueMode) 0 else currentIndex
        } while (isRepeatMode && (alwaysRepeat || remainRounds > 0))

        currentIndex = 0
        if (!isCancelled(generation)) {
            synchronized(lock) {
                state = PlaybackState.STOPPED
                if (successfulTracks == 0 && lastError == null) {
                    lastError = "No tracks could be played"
                }
            }
            publishSnapshot()
        }
    }

    private suspend fun playTrack(musicName: String, config: PlaybackConfig, generation: Long): Boolean {
        val cache = cacheProvider.loadCache(musicName)
        if (isCancelled(generation)) return false
        if (cache == null) {
            val message = "Cache unavailable for track: $musicName"
            logger.w(LogTags.CACHE_INVALID, message)
            synchronized(lock) {
                lastError = message
            }
            publishSnapshot()
            return false
        }

        val playStartTime = timeSource.nowMs()
        for (event in cache.mergedTimeline) {
            if (!waitUntilEvent(playStartTime + event.timeMs, config, generation)) {
                return false
            }
            if (isCancelled(generation)) return false

            if (event.action == ActionType.DOWN) {
                touchInjector.keyDownAll(event.keys)
            } else {
                touchInjector.keyUpAll(event.keys)
            }
        }

        val finalRemain = playStartTime + cache.expectedDurationMs +
            (cache.gapMs * config.finalGapMultiplier).toLong() - timeSource.nowMs()
        if (finalRemain > 0) {
            if (!waitForDuration(finalRemain, generation)) return false
        }
        return true
    }

    private fun publishSnapshot() {
        val snapshot: PlaybackSnapshot
        val listenerSnapshot: List<PlaybackSnapshotListener>
        synchronized(lock) {
            snapshot = buildSnapshot()
            listenerSnapshot = listeners.toList()
        }
        listenerSnapshot.forEach { listener ->
            try {
                listener.onPlaybackSnapshotChanged(snapshot)
            } catch (ex: Exception) {
                logger.w(LogTags.PLAYBACK, "Playback listener failed", ex)
            }
        }
    }

    private fun buildSnapshot(): PlaybackSnapshot {
        val queueSize = queue.size
        val safeIndex = if (queueSize == 0) 0 else currentIndex.coerceIn(0, queue.lastIndex)
        val queueMode = config.isQueueMode()
        val wraps = config.playType == PlayType.QUEUE_REPEAT && queueSize > 1
        return PlaybackSnapshot(
            state = state,
            currentTrackName = queue.getOrNull(safeIndex),
            currentIndex = safeIndex,
            queueSize = queueSize,
            canPrevious = queueMode && queueSize > 1 && (safeIndex > 0 || wraps),
            canNext = queueMode && queueSize > 1 && (safeIndex < queue.lastIndex || wraps),
            lastError = lastError
        )
    }

    private suspend fun waitUntilEvent(targetTimeMs: Long, config: PlaybackConfig, generation: Long): Boolean {
        val threshold = config.spinThresholdMs
        val remain = targetTimeMs - timeSource.nowMs()
        if (threshold <= 0) {
            return waitForDuration(remain, generation)
        }

        if (remain > threshold) {
            if (!waitForDuration(remain - threshold, generation)) return false
        }
        while (playbackScope.isActive && !isCancelled(generation) && timeSource.nowMs() < targetTimeMs) {
            // spin-wait for precise alignment
        }
        return !isCancelled(generation)
    }

    private suspend fun waitUntilStart(config: PlaybackConfig, generation: Long): Boolean {
        val targetEpochMs = config.startTimeEpochMs
        if (targetEpochMs <= 0) return true
        val nowEpoch = System.currentTimeMillis()
        if (nowEpoch >= targetEpochMs) return true

        val remain = targetEpochMs - nowEpoch
        val margin = config.startWaitSafetyMarginMs
        if (remain > margin) {
            if (!waitForDuration(remain - margin, generation)) return false
        }
        while (playbackScope.isActive && !isCancelled(generation) && System.currentTimeMillis() < targetEpochMs) {
            if (!waitForDuration(config.startWaitPollMs, generation)) return false
        }
        return !isCancelled(generation)
    }

    private suspend fun waitForDuration(durationMs: Long, generation: Long): Boolean {
        if (durationMs <= 0) return !isCancelled(generation)
        return try {
            sleeper.sleepMs(durationMs)
            !isCancelled(generation)
        } catch (ex: CancellationException) {
            false
        } catch (ex: Exception) {
            false
        }
    }

    private fun isCancelled(generation: Long): Boolean {
        return stopRequested || playbackGeneration != generation
    }
}

