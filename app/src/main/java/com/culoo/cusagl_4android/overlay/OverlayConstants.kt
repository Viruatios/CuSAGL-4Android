package com.culoo.cusagl_4android.overlay

object OverlayConstants {
    // Same 1920x1080 baseline as touch mapping; overlay Y is top-aligned.
    const val BASE_WIDTH_PX = 1920f
    const val BASE_HEIGHT_PX = 1080f
    const val INITIAL_CENTER_X_BASE = 960f
    const val INITIAL_TOP_BASE = 40f
    const val FIRST_KEY_ROW_Y_BASE = 670f
    const val KEY_SAFETY_MARGIN_BASE = 80f
    const val SAFE_BOTTOM_BASE = FIRST_KEY_ROW_Y_BASE - KEY_SAFETY_MARGIN_BASE

    const val WIDE_LANDSCAPE_MIN_WIDTH_DP = 600

    const val NOTIFICATION_CHANNEL_ID = "overlay_playback"
    const val NOTIFICATION_ID = 4104
    const val PERMISSION_CHECK_INTERVAL_MS = 1_000L
}
