package com.culoo.cusagl_4android.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TimelinePrebaker {
    private const val MIN_GAP_TIME_MS = 25

    fun prebakeTimeline(bars: List<Bar>, gapMs: Double): PrebakeResult {
        val keyState = mutableMapOf<String, Int>()
        val timeline = mutableListOf<TimelineEvent>()

        fun addKeyPulse(key: String, targetHalfTimeMs: Double, simTimeMs: Int) {
            val lastUp = keyState[key] ?: 0
            var actualDownTime = simTimeMs

            val timeSinceLastUp = actualDownTime - lastUp
            if (timeSinceLastUp < MIN_GAP_TIME_MS) {
                actualDownTime = lastUp + MIN_GAP_TIME_MS
            }

            var holdTime = min(MIN_GAP_TIME_MS.toDouble(), targetHalfTimeMs)
            holdTime = max(1.0, kotlin.math.round(holdTime))
            val actualUpTime = actualDownTime + holdTime.roundToInt()

            timeline.add(TimelineEvent(actualDownTime, ActionType.DOWN, key))
            timeline.add(TimelineEvent(actualUpTime, ActionType.UP, key))

            keyState[key] = actualUpTime
        }

        var currentSimTime = 0
        var totalCalculatedTime = 0

        for (bar in bars) {
            val barTime = bar.beats
            var elapsedBeat = 0.0

            for (unit in bar.units) {
                val unitStartTime = currentSimTime + (elapsedBeat * gapMs).roundToInt()
                when (unit) {
                    is UnitNote.Single -> {
                        val targetHalfTime = unit.time * gapMs * 0.5
                        addKeyPulse(unit.key, targetHalfTime, unitStartTime)
                    }

                    is UnitNote.Chord -> {
                        val targetHalfTime = unit.time * gapMs * 0.5
                        unit.keys.forEach { key -> addKeyPulse(key, targetHalfTime, unitStartTime) }
                    }

                    is UnitNote.Arpeggio -> {
                        val groups = unit.groups
                        val n = groups.size
                        if (n > 0) {
                            for (i in groups.indices) {
                                val noteStartTime = unitStartTime + ((i.toDouble() / n) * unit.time * gapMs).roundToInt()
                                val stepHalfTime = (unit.time * gapMs / n) * 0.5
                                groups[i].forEach { key -> addKeyPulse(key, stepHalfTime, noteStartTime) }
                            }
                        }
                    }

                    is UnitNote.Rest -> {
                        // Rest consumes time only.
                    }
                }
                elapsedBeat += unit.time
            }

            currentSimTime += (barTime * gapMs).roundToInt()
            totalCalculatedTime = currentSimTime
        }

        val mergedTimeline = mergeTimeline(timeline)
        return PrebakeResult(mergedTimeline, totalCalculatedTime)
    }

    private fun mergeTimeline(timeline: List<TimelineEvent>): List<MergedEvent> {
        val sorted = timeline.sortedWith(compareBy<TimelineEvent> { it.timeMs }.thenBy {
            if (it.action == ActionType.UP) 0 else 1
        })

        val merged = mutableListOf<MergedEvent>()
        for (event in sorted) {
            val last = merged.lastOrNull()
            if (last != null && last.timeMs == event.timeMs && last.action == event.action) {
                merged[merged.lastIndex] = last.copy(keys = last.keys + event.key)
            } else {
                merged.add(MergedEvent(event.timeMs, event.action, listOf(event.key)))
            }
        }
        return merged
    }
}

