package com.culoo.cusagl_4android.core

data class CacheData(
    val name: String,
    val author: String,
    val barCount: Int,
    val eventBatchCount: Int,
    val expectedDurationMs: Int,
    val createTimeMs: Long,
    val gapMs: Double,
    val mergedTimeline: List<MergedEvent>
)

