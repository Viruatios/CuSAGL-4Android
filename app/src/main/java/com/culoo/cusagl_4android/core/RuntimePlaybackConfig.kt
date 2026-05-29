package com.culoo.cusagl_4android.core

import kotlin.math.max

enum class PlayType {
    SINGLE_ONCE,
    SINGLE_REPEAT,
    QUEUE_ONCE,
    QUEUE_REPEAT
}

data class PlaybackConfig(
    val playType: PlayType = PlayType.SINGLE_ONCE,
    val startTimeEpochMs: Long = DEFAULT_START_TIME_EPOCH_MS,
    val queueIntervalMs: Long = DEFAULT_QUEUE_INTERVAL_MS,
    val repeatTimes: Int = DEFAULT_REPEAT_TIMES,
    val repeatIntervalMs: Long = DEFAULT_REPEAT_INTERVAL_MS,
    val spinThresholdMs: Long = DEFAULT_SPIN_THRESHOLD_MS,
    val finalGapMultiplier: Int = DEFAULT_FINAL_GAP_MULTIPLIER,
    val startWaitSafetyMarginMs: Long = DEFAULT_START_WAIT_SAFETY_MARGIN_MS,
    val startWaitPollMs: Long = DEFAULT_START_WAIT_POLL_MS
) {
    fun isQueueMode(): Boolean = playType == PlayType.QUEUE_ONCE || playType == PlayType.QUEUE_REPEAT

    fun isRepeatMode(): Boolean = playType == PlayType.SINGLE_REPEAT || playType == PlayType.QUEUE_REPEAT

    fun normalized(): PlaybackConfig {
        return copy(
            startTimeEpochMs = max(0L, startTimeEpochMs),
            queueIntervalMs = max(0L, queueIntervalMs),
            repeatTimes = max(0, repeatTimes),
            repeatIntervalMs = max(0L, repeatIntervalMs),
            spinThresholdMs = max(0L, spinThresholdMs),
            finalGapMultiplier = max(1, finalGapMultiplier),
            startWaitSafetyMarginMs = max(0L, startWaitSafetyMarginMs),
            startWaitPollMs = max(1L, startWaitPollMs)
        )
    }

    companion object {
        const val DEFAULT_SPIN_THRESHOLD_MS = 5L
        const val DEFAULT_FINAL_GAP_MULTIPLIER = 8
        const val DEFAULT_START_WAIT_SAFETY_MARGIN_MS = 100L
        const val DEFAULT_START_WAIT_POLL_MS = 5L
        const val DEFAULT_QUEUE_INTERVAL_MS = 0L
        const val DEFAULT_REPEAT_TIMES = 0
        const val DEFAULT_REPEAT_INTERVAL_MS = 0L
        const val DEFAULT_START_TIME_EPOCH_MS = 0L
    }
}

