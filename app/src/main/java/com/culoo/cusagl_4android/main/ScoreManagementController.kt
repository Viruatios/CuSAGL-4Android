package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.Logger
import com.culoo.cusagl_4android.core.ScoreParseResult
import com.culoo.cusagl_4android.core.ScoreParser
import com.culoo.cusagl_4android.core.ScoreStorage
import org.json.JSONObject
import java.io.File

data class ScoreEntry(
    val storageName: String,
    val title: String,
    val hasCache: Boolean,
    val lastModifiedMs: Long
)

data class ManualScoreDraft(
    val name: String = "",
    val author: String = "",
    val instrument: String = "",
    val description: String = "",
    val bpm: String = "",
    val timeSignature: String = "",
    val composer: String = "",
    val arranger: String = "",
    val notes: String = ""
)

enum class ManualScoreField {
    NAME,
    BPM,
    TIME_SIGNATURE,
    NOTES
}

sealed class ScoreSaveResult {
    data class Success(val storageName: String) : ScoreSaveResult()
    data class NeedsOverwrite(val existingStorageName: String, val title: String) : ScoreSaveResult()
    data class Failure(val message: UiText) : ScoreSaveResult()
}

sealed class ScoreDeleteResult {
    data class Success(val storageName: String) : ScoreDeleteResult()
    data class Failure(val message: UiText) : ScoreDeleteResult()
}

object ScoreManagementController {
    private val invalidFileNameChars = Regex("""[\\/:*?"<>|]""")

    fun fieldForValidationMessage(message: UiText?): ManualScoreField? {
        return when (message?.resId) {
            R.string.error_score_json_score_name_empty,
            R.string.error_score_name_invalid_file_name -> ManualScoreField.NAME
            R.string.error_bpm_positive_integer -> ManualScoreField.BPM
            R.string.error_score_json_time_signature_invalid -> ManualScoreField.TIME_SIGNATURE
            R.string.error_score_json_notes_empty,
            R.string.error_score_json_notes_unparseable -> ManualScoreField.NOTES
            else -> null
        }
    }

    fun listScores(filesDir: File, logger: Logger = DefaultLogger): List<ScoreEntry> {
        val scoreNames = ScoreStorage.listAndNormalizeScores(filesDir, logger)
        ScoreStorage.cleanExpiredCaches(filesDir, scoreNames.toSet(), logger)
        return scoreNames.map { storageName ->
            val scoreFile = ScoreStorage.scoreFile(filesDir, storageName)
            ScoreEntry(
                storageName = storageName,
                title = titleFromStorageName(storageName),
                hasCache = ScoreStorage.cacheFile(filesDir, storageName).exists(),
                lastModifiedMs = scoreFile.lastModified()
            )
        }
    }

    fun importScoreText(
        filesDir: File,
        sourceFileName: String,
        text: String,
        overwriteConfirmed: Boolean,
        logger: Logger = DefaultLogger
    ): ScoreSaveResult {
        val json = try {
            JSONObject(text)
        } catch (ex: Exception) {
            return ScoreSaveResult.Failure(
                (ScoreParser.parseScoreTextStrict(
                    text = text,
                    logger = logger,
                    source = sourceFileName.ifBlank { MainConstants.DEFAULT_IMPORT_FILE_LABEL }
                ) as ScoreParseResult.Failure).message
            )
        }
        val parseResult = ScoreParser.parseScoreTextStrict(
            text = text,
            logger = logger,
            source = sourceFileName.ifBlank { MainConstants.DEFAULT_IMPORT_FILE_LABEL }
        )
        val score = when (parseResult) {
            is ScoreParseResult.Success -> parseResult.score
            is ScoreParseResult.Failure -> return ScoreSaveResult.Failure(parseResult.message)
        }
        return saveValidatedJson(filesDir, score.name, json.toString(2), overwriteConfirmed, logger)
    }

    fun saveManualScore(
        filesDir: File,
        draft: ManualScoreDraft,
        overwriteConfirmed: Boolean,
        logger: Logger = DefaultLogger
    ): ScoreSaveResult {
        val bpm = draft.bpm.trim().toIntOrNull()
            ?: return ScoreSaveResult.Failure(UiText.resource(R.string.error_bpm_positive_integer))
        val json = JSONObject()
            .put("name", draft.name.trim())
            .put("author", draft.author.trim())
            .put("instrument", draft.instrument.trim())
            .put("description", draft.description.trim())
            .put("type", "keyboard")
            .put("bpm", bpm)
            .put("time_signature", draft.timeSignature.trim())
            .put("composer", draft.composer.trim())
            .put("arranger", draft.arranger.trim())
            .put("notes", draft.notes)

        val parseResult = ScoreParser.parseScoreTextStrict(json.toString(), logger)
        val score = when (parseResult) {
            is ScoreParseResult.Success -> parseResult.score
            is ScoreParseResult.Failure -> return ScoreSaveResult.Failure(parseResult.message)
        }
        return saveValidatedJson(filesDir, score.name, json.toString(2), overwriteConfirmed, logger)
    }

    fun deleteScore(filesDir: File, storageName: String): ScoreDeleteResult {
        val scoreFile = ScoreStorage.scoreFile(filesDir, storageName)
        if (!scoreFile.exists()) {
            return ScoreDeleteResult.Failure(UiText.resource(R.string.error_score_missing, storageName))
        }

        val deleted = scoreFile.delete()
        if (!deleted) {
            return ScoreDeleteResult.Failure(UiText.resource(R.string.error_score_delete_failed, storageName))
        }

        val cacheFile = ScoreStorage.cacheFile(filesDir, storageName)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        return ScoreDeleteResult.Success(storageName)
    }

    private fun saveValidatedJson(
        filesDir: File,
        rawTitle: String,
        jsonText: String,
        overwriteConfirmed: Boolean,
        logger: Logger
    ): ScoreSaveResult {
        val title = sanitizeTitle(rawTitle)
            ?: return ScoreSaveResult.Failure(UiText.resource(R.string.error_score_name_invalid_file_name))
        val existing = findExistingByTitle(filesDir, title, logger)

        if (existing != null && !overwriteConfirmed) {
            return ScoreSaveResult.NeedsOverwrite(existing, title)
        }

        val storageName = existing ?: nextStorageName(filesDir, title, logger)
        val scoreDir = ScoreStorage.scoreDir(filesDir)
        if (!scoreDir.exists()) scoreDir.mkdirs()

        return try {
            ScoreStorage.scoreFile(filesDir, storageName).writeText(jsonText)
            val cacheFile = ScoreStorage.cacheFile(filesDir, storageName)
            if (cacheFile.exists()) cacheFile.delete()
            ScoreSaveResult.Success(storageName)
        } catch (ex: Exception) {
            ScoreSaveResult.Failure(UiText.resource(R.string.error_score_save_failed, ex.message ?: storageName))
        }
    }

    private fun findExistingByTitle(filesDir: File, title: String, logger: Logger): String? {
        return ScoreStorage.listAndNormalizeScores(filesDir, logger)
            .firstOrNull { titleFromStorageName(it) == title }
    }

    private fun nextStorageName(filesDir: File, title: String, logger: Logger): String {
        val usedNumbers = ScoreStorage.listAndNormalizeScores(filesDir, logger)
            .mapNotNull { it.substringBefore('.').toIntOrNull() }
            .toSet()
        var newNum = 1
        while (usedNumbers.contains(newNum)) newNum++
        return "${newNum.toString().padStart(4, '0')}.$title"
    }

    private fun titleFromStorageName(storageName: String): String {
        return storageName.substringAfter('.', storageName)
    }

    private fun sanitizeTitle(rawTitle: String): String? {
        val title = rawTitle
            .replace(invalidFileNameChars, "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return title.ifBlank { null }
    }
}
