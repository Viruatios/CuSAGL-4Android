package com.culoo.cusagl_4android.core

object CoreConstants {
    // Keep a short key-up gap to avoid swallowed repeated notes in game input.
    const val MIN_KEY_UP_GAP_MS = 25

    // App-private storage folder names. Changing these requires a migration.
    const val SCORE_DIR_NAME = "score_file"
    const val CACHE_DIR_NAME = "cache"

    const val DEFAULT_SPIN_THRESHOLD_MS = 5L
    const val DEFAULT_FINAL_GAP_MULTIPLIER = 8
    const val DEFAULT_START_WAIT_SAFETY_MARGIN_MS = 100L
    const val DEFAULT_START_WAIT_POLL_MS = 5L
    const val DEFAULT_QUEUE_INTERVAL_MS = 0L
    const val DEFAULT_REPEAT_TIMES = 0
    const val DEFAULT_REPEAT_INTERVAL_MS = 0L
    const val DEFAULT_START_TIME_EPOCH_MS = 0L

    const val DEFAULT_SCORE_NAME = "unknown-score"
    const val DEFAULT_SCORE_AUTHOR = "unknown-author"
    const val DEFAULT_SCORE_INSTRUMENT = "no-suggested-instrument"
    const val DEFAULT_SCORE_DESCRIPTION = "no-description"
    const val DEFAULT_SCORE_COMPOSER = "unknown-composer"
    const val DEFAULT_SCORE_ARRANGER = "unknown-arranger"
}
