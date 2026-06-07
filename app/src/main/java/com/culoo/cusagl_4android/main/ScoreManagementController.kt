package com.culoo.cusagl_4android.main

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
    val author: String = "指尖灬旋律丿",
    val instrument: String = "风物之诗琴",
    val description: String = "无",
    val bpm: String = "",
    val timeSignature: String = "4/4",
    val composer: String = "HoYo-Mix",
    val arranger: String = "HoYo-Mix",
    val notes: String = ""
)

sealed class ScoreSaveResult {
    data class Success(val storageName: String) : ScoreSaveResult()
    data class NeedsOverwrite(val existingStorageName: String, val title: String) : ScoreSaveResult()
    data class Failure(val message: String) : ScoreSaveResult()
}

sealed class ScoreDeleteResult {
    data class Success(val storageName: String) : ScoreDeleteResult()
    data class Failure(val message: String) : ScoreDeleteResult()
}

object ScoreManagementController {
    private val invalidFileNameChars = Regex("""[\\/:*?"<>|]""")

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
            return ScoreSaveResult.Failure("JSON 格式无效：${sourceFileName.ifBlank { "导入文件" }}")
        }
        val parseResult = ScoreParser.parseScoreTextStrict(json.toString(), logger)
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
            ?: return ScoreSaveResult.Failure("BPM 必须是正整数")
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
            return ScoreDeleteResult.Failure("曲谱不存在：$storageName")
        }

        val deleted = scoreFile.delete()
        if (!deleted) {
            return ScoreDeleteResult.Failure("删除曲谱失败：$storageName")
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
            ?: return ScoreSaveResult.Failure("曲名不能作为文件名")
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
            ScoreSaveResult.Failure("保存曲谱失败：${ex.message ?: storageName}")
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
