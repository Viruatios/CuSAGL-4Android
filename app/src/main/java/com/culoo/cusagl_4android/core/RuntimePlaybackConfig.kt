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
    val startTimeEpochMs: Long = CoreConstants.DEFAULT_START_TIME_EPOCH_MS,
    val queueIntervalMs: Long = CoreConstants.DEFAULT_QUEUE_INTERVAL_MS,
    val repeatTimes: Int = CoreConstants.DEFAULT_REPEAT_TIMES,
    val repeatIntervalMs: Long = CoreConstants.DEFAULT_REPEAT_INTERVAL_MS,
    val spinThresholdMs: Long = CoreConstants.DEFAULT_SPIN_THRESHOLD_MS,
    val finalGapMultiplier: Int = CoreConstants.DEFAULT_FINAL_GAP_MULTIPLIER,
    val startWaitSafetyMarginMs: Long = CoreConstants.DEFAULT_START_WAIT_SAFETY_MARGIN_MS,
    val startWaitPollMs: Long = CoreConstants.DEFAULT_START_WAIT_POLL_MS
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
}

