package com.culoo.cusagl_4android.core

data class ScoreInfo(
    val name: String,
    val author: String,
    val instrument: String,
    val description: String,
    val type: String,
    val bpm: Int,
    val timeSignature: String,
    val composer: String,
    val arranger: String,
    val notes: List<Bar>
)

data class Bar(
    val beats: Int,
    val units: List<UnitNote>
)

sealed class UnitNote(open val time: Double) {
    data class Rest(override val time: Double) : UnitNote(time)
    data class Single(val key: String, override val time: Double) : UnitNote(time)
    data class Chord(val keys: List<String>, override val time: Double) : UnitNote(time)
    data class Arpeggio(val groups: List<List<String>>, override val time: Double) : UnitNote(time)
}

enum class ActionType {
    DOWN,
    UP
}

data class TimelineEvent(
    val timeMs: Int,
    val action: ActionType,
    val key: String
)

data class MergedEvent(
    val timeMs: Int,
    val action: ActionType,
    val keys: List<String>
)

data class PrebakeResult(
    val mergedTimeline: List<MergedEvent>,
    val totalCalculatedTimeMs: Int
)


