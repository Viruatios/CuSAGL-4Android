package com.culoo.cusagl_4android.core

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
    private val logger: Logger = DefaultLogger
) {
    @Volatile
    private var state: PlaybackState = PlaybackState.IDLE
    @Volatile
    private var stopRequested: Boolean = false

    private var config: PlaybackConfig = PlaybackConfig()
    private var queue: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var playbackThread: Thread? = null

    fun updateConfig(newConfig: PlaybackConfig) {
        config = newConfig.normalized()
    }

    fun updateQueue(newQueue: List<String>) {
        val normalized = newQueue.filter { it.isNotBlank() }.distinct()
        queue = normalized
        if (currentIndex >= normalized.size) {
            currentIndex = 0
        }
    }

    fun getState(): PlaybackState = state

    fun start() {
        if (state == PlaybackState.PLAYING) return
        if (queue.isEmpty()) {
            logger.w(LogTags.FILE_MISSING, "Playback queue is empty")
            state = PlaybackState.STOPPED
            return
        }

        stopRequested = false
        state = PlaybackState.PLAYING
        playbackThread = Thread { runPlayback() }.apply { isDaemon = true; start() }
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
    }

    fun releaseAllTouches() {
        touchInjector.releaseAll(KeyLayout.allKeys)
    }

    private fun pauseInternal(targetState: PlaybackState, resetQueueIndex: Boolean) {
        stopRequested = true
        playbackThread?.interrupt()
        playbackThread = null
        releaseAllTouches()
        if (resetQueueIndex) {
            currentIndex = 0
        }
        state = targetState
    }

    private fun runPlayback() {
        val snapshotConfig = config.normalized()
        val snapshotQueue = queue

        if (!waitUntilStart(snapshotConfig)) {
            return
        }

        val isQueueMode = snapshotConfig.isQueueMode()
        val isRepeatMode = snapshotConfig.isRepeatMode()
        val alwaysRepeat = isRepeatMode && snapshotConfig.repeatTimes == 0
        var remainRounds = if (isRepeatMode) max(1, snapshotConfig.repeatTimes) else 1

        var index = currentIndex.coerceIn(0, snapshotQueue.lastIndex)
        do {
            val roundStartIndex = if (isQueueMode) index else currentIndex
            index = roundStartIndex

            while (index <= snapshotQueue.lastIndex) {
                if (stopRequested) {
                    return
                }
                currentIndex = index
                val musicName = snapshotQueue[index]
                val played = playTrack(musicName, snapshotConfig)
                if (!played && stopRequested) {
                    return
                }

                if (isQueueMode && snapshotConfig.queueIntervalMs > 0 && index < snapshotQueue.lastIndex) {
                    if (!sleepWithInterrupt(snapshotConfig.queueIntervalMs)) return
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
                if (!sleepWithInterrupt(snapshotConfig.repeatIntervalMs)) return
            }

            index = if (isQueueMode) 0 else currentIndex
        } while (isRepeatMode && (alwaysRepeat || remainRounds > 0))

        currentIndex = 0
        if (!stopRequested) {
            state = PlaybackState.STOPPED
        }
    }

    private fun playTrack(musicName: String, config: PlaybackConfig): Boolean {
        val cache = cacheProvider.loadCache(musicName)
        if (cache == null) {
            logger.w(LogTags.CACHE_INVALID, "Cache unavailable for track: $musicName")
            return false
        }

        val playStartTime = timeSource.nowMs()
        for (event in cache.mergedTimeline) {
            if (!waitUntilEvent(playStartTime + event.timeMs, config)) {
                return false
            }
            if (stopRequested) return false

            if (event.action == ActionType.DOWN) {
                touchInjector.keyDownAll(event.keys)
            } else {
                touchInjector.keyUpAll(event.keys)
            }
        }

        val finalRemain = playStartTime + cache.expectedDurationMs +
            (cache.gapMs * config.finalGapMultiplier).toLong() - timeSource.nowMs()
        if (finalRemain > 0) {
            if (!sleepWithInterrupt(finalRemain)) return false
        }
        return true
    }

    private fun waitUntilEvent(targetTimeMs: Long, config: PlaybackConfig): Boolean {
        val threshold = config.spinThresholdMs
        val remain = targetTimeMs - timeSource.nowMs()
        if (remain > threshold) {
            if (!sleepWithInterrupt(remain - threshold)) return false
        }
        while (!stopRequested && timeSource.nowMs() < targetTimeMs) {
            // spin-wait for precise alignment
        }
        return !stopRequested
    }

    private fun waitUntilStart(config: PlaybackConfig): Boolean {
        val targetEpochMs = config.startTimeEpochMs
        if (targetEpochMs <= 0) return true
        val nowEpoch = System.currentTimeMillis()
        if (nowEpoch >= targetEpochMs) return true

        val remain = targetEpochMs - nowEpoch
        val margin = config.startWaitSafetyMarginMs
        if (remain > margin) {
            if (!sleepWithInterrupt(remain - margin)) return false
        }
        while (!stopRequested && System.currentTimeMillis() < targetEpochMs) {
            if (!sleepWithInterrupt(config.startWaitPollMs)) return false
        }
        return !stopRequested
    }

    private fun sleepWithInterrupt(durationMs: Long): Boolean {
        if (durationMs <= 0) return true
        return try {
            sleeper.sleepMs(durationMs)
            !stopRequested
        } catch (ex: Exception) {
            false
        }
    }
}

