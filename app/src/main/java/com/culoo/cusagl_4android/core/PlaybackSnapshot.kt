package com.culoo.cusagl_4android.core

data class PlaybackSnapshot(
    val state: PlaybackState = PlaybackState.IDLE,
    val currentTrackName: String? = null,
    val currentIndex: Int = 0,
    val queueSize: Int = 0,
    val canPrevious: Boolean = false,
    val canNext: Boolean = false,
    val lastError: String? = null
) {
    val canMoveOverlay: Boolean
        get() = state != PlaybackState.PLAYING
}

fun interface PlaybackSnapshotListener {
    fun onPlaybackSnapshotChanged(snapshot: PlaybackSnapshot)
}
