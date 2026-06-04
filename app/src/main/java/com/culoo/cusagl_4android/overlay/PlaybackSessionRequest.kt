package com.culoo.cusagl_4android.overlay

import android.content.Intent
import com.culoo.cusagl_4android.core.PlayType
import com.culoo.cusagl_4android.core.PlaybackConfig

data class PlaybackSessionRequest(
    val queue: List<String>,
    val config: PlaybackConfig
) {
    fun writeTo(intent: Intent): Intent {
        return intent.apply {
            putStringArrayListExtra(EXTRA_QUEUE, ArrayList(queue))
            putExtra(EXTRA_PLAY_TYPE, config.playType.name)
            putExtra(EXTRA_START_TIME, config.startTimeEpochMs)
            putExtra(EXTRA_QUEUE_INTERVAL, config.queueIntervalMs)
            putExtra(EXTRA_REPEAT_TIMES, config.repeatTimes)
            putExtra(EXTRA_REPEAT_INTERVAL, config.repeatIntervalMs)
            putExtra(EXTRA_SPIN_THRESHOLD, config.spinThresholdMs)
            putExtra(EXTRA_FINAL_GAP_MULTIPLIER, config.finalGapMultiplier)
            putExtra(EXTRA_START_WAIT_MARGIN, config.startWaitSafetyMarginMs)
            putExtra(EXTRA_START_WAIT_POLL, config.startWaitPollMs)
        }
    }

    companion object {
        private const val EXTRA_QUEUE = "overlay.queue"
        private const val EXTRA_PLAY_TYPE = "overlay.play_type"
        private const val EXTRA_START_TIME = "overlay.start_time"
        private const val EXTRA_QUEUE_INTERVAL = "overlay.queue_interval"
        private const val EXTRA_REPEAT_TIMES = "overlay.repeat_times"
        private const val EXTRA_REPEAT_INTERVAL = "overlay.repeat_interval"
        private const val EXTRA_SPIN_THRESHOLD = "overlay.spin_threshold"
        private const val EXTRA_FINAL_GAP_MULTIPLIER = "overlay.final_gap_multiplier"
        private const val EXTRA_START_WAIT_MARGIN = "overlay.start_wait_margin"
        private const val EXTRA_START_WAIT_POLL = "overlay.start_wait_poll"

        fun from(intent: Intent): PlaybackSessionRequest? {
            val queue = intent.getStringArrayListExtra(EXTRA_QUEUE)?.filter { it.isNotBlank() }.orEmpty()
            if (queue.isEmpty()) return null
            val defaults = PlaybackConfig()
            val playType = intent.getStringExtra(EXTRA_PLAY_TYPE)
                ?.let { runCatching { PlayType.valueOf(it) }.getOrNull() }
                ?: defaults.playType
            return PlaybackSessionRequest(
                queue = queue,
                config = PlaybackConfig(
                    playType = playType,
                    startTimeEpochMs = intent.getLongExtra(EXTRA_START_TIME, defaults.startTimeEpochMs),
                    queueIntervalMs = intent.getLongExtra(EXTRA_QUEUE_INTERVAL, defaults.queueIntervalMs),
                    repeatTimes = intent.getIntExtra(EXTRA_REPEAT_TIMES, defaults.repeatTimes),
                    repeatIntervalMs = intent.getLongExtra(EXTRA_REPEAT_INTERVAL, defaults.repeatIntervalMs),
                    spinThresholdMs = intent.getLongExtra(EXTRA_SPIN_THRESHOLD, defaults.spinThresholdMs),
                    finalGapMultiplier = intent.getIntExtra(
                        EXTRA_FINAL_GAP_MULTIPLIER,
                        defaults.finalGapMultiplier
                    ),
                    startWaitSafetyMarginMs = intent.getLongExtra(
                        EXTRA_START_WAIT_MARGIN,
                        defaults.startWaitSafetyMarginMs
                    ),
                    startWaitPollMs = intent.getLongExtra(EXTRA_START_WAIT_POLL, defaults.startWaitPollMs)
                ).normalized()
            )
        }
    }
}
