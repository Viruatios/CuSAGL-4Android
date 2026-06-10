package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.Logger
import com.culoo.cusagl_4android.core.PlayType
import com.culoo.cusagl_4android.core.PlaybackConfig
import com.culoo.cusagl_4android.core.ScoreStorage
import com.culoo.cusagl_4android.overlay.PlaybackSessionRequest
import org.json.JSONObject
import java.io.File
import java.util.Calendar

enum class PlaybackConfigMode(
    val labelResId: Int,
    val playType: PlayType
) {
    SINGLE_ONCE(R.string.playback_mode_single_once, PlayType.SINGLE_ONCE),
    SINGLE_REPEAT(R.string.playback_mode_single_repeat, PlayType.SINGLE_REPEAT),
    QUEUE_ONCE(R.string.playback_mode_queue_once, PlayType.QUEUE_ONCE),
    QUEUE_REPEAT(R.string.playback_mode_queue_repeat, PlayType.QUEUE_REPEAT);

    fun isQueueMode(): Boolean = this == QUEUE_ONCE || this == QUEUE_REPEAT

    fun isRepeatMode(): Boolean = this == SINGLE_REPEAT || this == QUEUE_REPEAT

    companion object {
        val allModes: List<PlaybackConfigMode> = listOf(SINGLE_ONCE, SINGLE_REPEAT, QUEUE_ONCE, QUEUE_REPEAT)

        fun fromName(name: String?): PlaybackConfigMode {
            return allModes.firstOrNull { it.name == name } ?: SINGLE_ONCE
        }
    }
}

data class PlaybackConfigDraft(
    val mode: PlaybackConfigMode = PlaybackConfigMode.SINGLE_ONCE,
    val startTimeText: String = "",
    val selectedScoreName: String = "",
    val queueText: String = "",
    val queueIntervalSeconds: String = "",
    val repeatTimes: String = "",
    val repeatIntervalSeconds: String = "",
    val debugEnabled: Boolean = false
)

data class AppliedPlaybackConfig(
    val draft: PlaybackConfigDraft,
    val request: PlaybackSessionRequest?,
    val scoreNames: List<String>,
    val summary: UiText,
    val message: UiText? = null
)

sealed class PlaybackConfigApplyResult {
    data class Success(val applied: AppliedPlaybackConfig) : PlaybackConfigApplyResult()
    data class Failure(val message: UiText) : PlaybackConfigApplyResult()
}

object PlaybackConfigController {
    fun loadApplied(filesDir: File, logger: Logger = DefaultLogger): AppliedPlaybackConfig {
        val scoreNames = listScores(filesDir, logger)
        val draft = loadDraft(filesDir)
        return when (val result = buildApplied(draft, scoreNames, saveFallback = false, filesDir = filesDir)) {
            is PlaybackConfigApplyResult.Success -> result.applied
            is PlaybackConfigApplyResult.Failure -> {
                val fallback = PlaybackConfigDraft()
                (buildApplied(fallback, scoreNames, saveFallback = false, filesDir = filesDir)
                    as PlaybackConfigApplyResult.Success).applied.copy(message = result.message)
            }
        }
    }

    fun applyAndSave(
        filesDir: File,
        draft: PlaybackConfigDraft,
        logger: Logger = DefaultLogger
    ): PlaybackConfigApplyResult {
        val scoreNames = listScores(filesDir, logger)
        val result = buildApplied(draft, scoreNames, saveFallback = true, filesDir = filesDir)
        if (result is PlaybackConfigApplyResult.Success) {
            saveDraft(filesDir, result.applied.draft)
        }
        return result
    }

    fun buildRequest(
        draft: PlaybackConfigDraft,
        scoreNames: List<String>,
        nowMs: Long = System.currentTimeMillis()
    ): PlaybackConfigApplyResult {
        return buildApplied(draft, scoreNames, saveFallback = false, filesDir = null, nowMs = nowMs)
    }

    fun preloadScores(
        filesDir: File,
        scoreNames: List<String>,
        logger: Logger = DefaultLogger
    ): PreloadResult {
        val queue = scoreNames.filter { it.isNotBlank() }.distinct()
        if (queue.isEmpty()) return PreloadResult.Failure(UiText.resource(R.string.error_no_preloadable_scores))

        val failed = mutableListOf<String>()
        for (scoreName in queue) {
            when (MainScreenController.preloadFirstScore(filesDir, scoreName, logger)) {
                is PreloadResult.Success -> Unit
                is PreloadResult.Failure -> failed.add(scoreName)
            }
        }

        return if (failed.isEmpty()) {
            PreloadResult.Success(UiText.resource(R.string.message_preload_queue_success, queue.size))
        } else {
            PreloadResult.Failure(UiText.resource(R.string.error_preload_partial_failed, failed.joinToString(", ")))
        }
    }

    private fun buildApplied(
        draft: PlaybackConfigDraft,
        scoreNames: List<String>,
        saveFallback: Boolean,
        filesDir: File?,
        nowMs: Long = System.currentTimeMillis()
    ): PlaybackConfigApplyResult {
        if (scoreNames.isEmpty()) {
            return PlaybackConfigApplyResult.Success(
                AppliedPlaybackConfig(
                    draft = draft,
                    request = null,
                    scoreNames = emptyList(),
                    summary = UiText.resource(R.string.playback_summary_no_scores)
                )
            )
        }

        val startTime = parseStartTime(draft.startTimeText, nowMs)
            ?: return PlaybackConfigApplyResult.Failure(UiText.resource(R.string.error_start_time_format))
        val queueInterval = parseNonNegativeSeconds(draft.queueIntervalSeconds)
            ?: return PlaybackConfigApplyResult.Failure(UiText.resource(R.string.error_queue_interval_non_negative))
        val repeatTimes = parseNonNegativeInt(draft.repeatTimes)
            ?: return PlaybackConfigApplyResult.Failure(UiText.resource(R.string.error_repeat_times_non_negative))
        val repeatInterval = parseNonNegativeSeconds(draft.repeatIntervalSeconds)
            ?: return PlaybackConfigApplyResult.Failure(UiText.resource(R.string.error_repeat_interval_non_negative))

        val queue = if (draft.mode.isQueueMode()) {
            resolveQueue(draft.queueText, scoreNames)
        } else {
            listOf(resolveSingleScore(draft.selectedScoreName, scoreNames))
        }

        val resolvedDraft = if (draft.mode.isQueueMode()) {
            draft
        } else {
            draft.copy(selectedScoreName = queue.first())
        }

        val config = PlaybackConfig(
            playType = draft.mode.playType,
            startTimeEpochMs = startTime,
            queueIntervalMs = if (draft.mode.isQueueMode()) queueInterval else 0L,
            repeatTimes = if (draft.mode.isRepeatMode()) repeatTimes else 0,
            repeatIntervalMs = if (draft.mode.isRepeatMode()) repeatInterval else 0L
        ).normalized()
        val applied = AppliedPlaybackConfig(
            draft = resolvedDraft,
            request = PlaybackSessionRequest(queue, config),
            scoreNames = queue,
            summary = buildSummary(draft.mode, queue, startTime, draft.debugEnabled)
        )
        if (saveFallback && filesDir != null) {
            saveDraft(filesDir, resolvedDraft)
        }
        return PlaybackConfigApplyResult.Success(applied)
    }

    private fun listScores(filesDir: File, logger: Logger): List<String> {
        val scoreNames = ScoreStorage.listAndNormalizeScores(filesDir, logger)
        ScoreStorage.cleanExpiredCaches(filesDir, scoreNames.toSet(), logger)
        return scoreNames
    }

    private fun loadDraft(filesDir: File): PlaybackConfigDraft {
        val file = configFile(filesDir)
        if (!file.exists()) return PlaybackConfigDraft()
        val json = try {
            JSONObject(file.readText())
        } catch (ex: Exception) {
            return PlaybackConfigDraft()
        }
        return PlaybackConfigDraft(
            mode = PlaybackConfigMode.fromName(json.optString("mode")),
            startTimeText = json.optString("startTimeText", ""),
            selectedScoreName = json.optString("selectedScoreName", ""),
            queueText = json.optString("queueText", ""),
            queueIntervalSeconds = json.optString("queueIntervalSeconds", ""),
            repeatTimes = json.optString("repeatTimes", ""),
            repeatIntervalSeconds = json.optString("repeatIntervalSeconds", ""),
            debugEnabled = json.optBoolean("debugEnabled", false)
        )
    }

    private fun saveDraft(filesDir: File, draft: PlaybackConfigDraft) {
        val json = JSONObject()
            .put("mode", draft.mode.name)
            .put("startTimeText", draft.startTimeText)
            .put("selectedScoreName", draft.selectedScoreName)
            .put("queueText", draft.queueText)
            .put("queueIntervalSeconds", draft.queueIntervalSeconds)
            .put("repeatTimes", draft.repeatTimes)
            .put("repeatIntervalSeconds", draft.repeatIntervalSeconds)
            .put("debugEnabled", draft.debugEnabled)
        configFile(filesDir).writeText(json.toString(2))
    }

    private fun configFile(filesDir: File): File = File(filesDir, MainConstants.PLAYBACK_CONFIG_FILE_NAME)

    private fun resolveSingleScore(selected: String, scoreNames: List<String>): String {
        return selected.takeIf { scoreNames.contains(it) } ?: scoreNames.first()
    }

    private fun resolveQueue(queueText: String, scoreNames: List<String>): List<String> {
        val text = queueText.trim()
        if (text.isEmpty()) return scoreNames

        val result = linkedSetOf<String>()
        text.split(Regex("\\s+")).forEach { raw ->
            val index = raw.toIntOrNull()
            if (index == null || index <= 0) return@forEach
            val prefix = index.toString().padStart(4, '0')
            val matched = scoreNames.firstOrNull { it.startsWith("$prefix.") }
            if (matched != null) result.add(matched)
        }
        return result.toList().ifEmpty { scoreNames }
    }

    private fun parseStartTime(text: String, nowMs: Long): Long? {
        val normalized = text.replace(Regex("[^0-9:]"), "").trim()
        if (normalized.isEmpty()) return 0L
        val parts = normalized.split(':')
        if (parts.size !in 2..3) return null
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        val hours = numbers[0]
        val minutes = numbers[1]
        val seconds = numbers.getOrElse(2) { 0 }
        if (hours !in 0..23 || minutes !in 0..59 || seconds !in 0..59) return null

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMs
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, seconds)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun parseNonNegativeSeconds(text: String): Long? {
        val value = parseNonNegativeInt(text) ?: return null
        return value * 1000L
    }

    private fun parseNonNegativeInt(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        val value = trimmed.toIntOrNull() ?: return null
        return value.takeIf { it >= 0 }
    }

    private fun buildSummary(
        mode: PlaybackConfigMode,
        queue: List<String>,
        startTimeEpochMs: Long,
        debugEnabled: Boolean
    ): UiText {
        val target = if (mode.isQueueMode()) {
            UiText.resource(R.string.playback_summary_queue_target, queue.size)
        } else {
            UiText.resource(R.string.playback_summary_single_target, queue.firstOrNull() ?: MainConstants.NO_SELECTED_SCORE_LABEL)
        }
        return UiText.resource(
            R.string.playback_summary_template,
            UiText.resource(mode.labelResId),
            target,
            if (startTimeEpochMs > 0) UiText.resource(R.string.playback_summary_timed) else UiText.resource(R.string.empty),
            if (debugEnabled) UiText.resource(R.string.playback_summary_debug) else UiText.resource(R.string.empty)
        )
    }
}
