package com.culoo.cusagl_4android.core

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import org.json.JSONObject
import java.io.File

object ScoreParser {
    private val partRegex = Regex("\\([A-Za-z]+\\)|[A-Za-z]")
    private val strictTimeSignatureRegex = Regex("^\\d+/\\d+$")

    fun loadScoreByName(filesDir: File, name: String, logger: Logger = DefaultLogger): ScoreInfo? {
        val scoreFile = ScoreStorage.scoreFile(filesDir, name)
        if (!scoreFile.exists()) {
            logger.e(LogTags.FILE_MISSING, "Score file missing: ${scoreFile.absolutePath}")
            return null
        }

        val text = try {
            scoreFile.readText()
        } catch (ex: Exception) {
            logger.e(LogTags.PARSE_FAIL, "Failed to read score file: ${scoreFile.absolutePath}", ex)
            return null
        }

        return parseScoreText(text, strict = false, logger = logger, source = scoreFile.absolutePath)
    }

    fun parseScoreTextStrict(text: String, logger: Logger = DefaultLogger): ScoreParseResult {
        return parseScoreText(text, strict = true, logger = logger, source = "score text")
            ?.let { ScoreParseResult.Success(it) }
            ?: ScoreParseResult.Failure(UiText.resource(R.string.error_score_json_validation_failed))
    }

    private fun parseScoreText(
        text: String,
        strict: Boolean,
        logger: Logger,
        source: String
    ): ScoreInfo? {
        val json = try {
            JSONObject(text)
        } catch (ex: Exception) {
            logger.e(LogTags.PARSE_FAIL, "Invalid score JSON: $source", ex)
            return null
        }

        val notesText = if (json.has("notes")) json.optString("notes") else null
        if (notesText.isNullOrBlank()) {
            logger.e(LogTags.PARSE_FAIL, "Missing notes in score: $source")
            return null
        }

        val name = json.optString("name", CoreConstants.DEFAULT_SCORE_NAME)
        val bpm = parseBpm(json.opt("bpm"))
        val timeSignature = json.optString("time_signature", "4/4")
        if (strict) {
            if (name.isBlank()) {
                logger.e(LogTags.PARSE_FAIL, "Missing name in score: $source")
                return null
            }
            if (!isPositiveBpm(json.opt("bpm"))) {
                logger.e(LogTags.PARSE_FAIL, "Invalid bpm in score: $source")
                return null
            }
            if (!isValidTimeSignature(timeSignature)) {
                logger.e(LogTags.PARSE_FAIL, "Invalid time signature in score: $source")
                return null
            }
        }

        val notes = parseNotes(notesText)
        if (strict && notes.isEmpty()) {
            logger.e(LogTags.PARSE_FAIL, "Empty parsed notes in score: $source")
            return null
        }

        return ScoreInfo(
            name = name,
            author = json.optString("author", CoreConstants.DEFAULT_SCORE_AUTHOR),
            instrument = json.optString("instrument", CoreConstants.DEFAULT_SCORE_INSTRUMENT),
            description = json.optString("description", CoreConstants.DEFAULT_SCORE_DESCRIPTION),
            type = "keyboard",
            bpm = bpm,
            timeSignature = timeSignature,
            composer = json.optString("composer", CoreConstants.DEFAULT_SCORE_COMPOSER),
            arranger = json.optString("arranger", CoreConstants.DEFAULT_SCORE_ARRANGER),
            notes = notes
        )
    }

    fun parseNotes(notes: String): List<Bar> {
        val result = mutableListOf<Bar>()
        val lines = notes.split('\n')

        for (line in lines) {
            var currentLine = line.replace("\r", "")
            if (currentLine.trim().isEmpty()) continue

            currentLine = currentLine.replace(Regex("/{2,}"), "/")

            val beats = currentLine.split('/')
            val barLength = beats.size
            val units = mutableListOf<UnitNote>()

            for ((beatIdx, beatStrRaw) in beats.withIndex()) {
                val beatStr = beatStrRaw.trimEnd()
                if (beatStr.isEmpty() && beatIdx == beats.lastIndex) {
                    continue
                }

                val rawUnits = beatStr.split(' ').map { if (it.isEmpty()) "@" else it }
                if (rawUnits.isEmpty()) continue

                val unitDuration = 1.0 / rawUnits.size.toDouble()
                for (unitStr in rawUnits) {
                    if (unitStr == "@") {
                        units.add(UnitNote.Rest(unitDuration))
                        continue
                    }

                    val parts = partRegex.findAll(unitStr).map { it.value }.toList()
                    if (parts.isEmpty()) {
                        units.add(UnitNote.Rest(unitDuration))
                        continue
                    }

                    if (parts.size == 1) {
                        val part = parts.first()
                        val keys = toValidKeys(part)
                        if (keys.isEmpty()) {
                            units.add(UnitNote.Rest(unitDuration))
                        } else if (part.startsWith('(')) {
                            units.add(UnitNote.Chord(keys, unitDuration))
                        } else {
                            units.add(UnitNote.Single(keys.first(), unitDuration))
                        }
                    } else {
                        val groups = parts.map { toValidKeys(it) }
                        units.add(UnitNote.Arpeggio(groups, unitDuration))
                    }
                }
            }

            if (units.isNotEmpty() || barLength > 0) {
                result.add(Bar(barLength, units))
            }
        }

        return result
    }

    private fun parseBpm(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 120
            else -> 120
        }
    }

    private fun isPositiveBpm(value: Any?): Boolean {
        return when (value) {
            is Number -> value.toInt() > 0
            is String -> value.trim().toIntOrNull()?.let { it > 0 } ?: false
            else -> false
        }
    }

    private fun isValidTimeSignature(value: String): Boolean {
        if (!strictTimeSignatureRegex.matches(value.trim())) return false
        val parts = value.split('/')
        val num = parts[0].toIntOrNull() ?: return false
        val den = parts[1].toIntOrNull() ?: return false
        return num > 0 && den > 0 && den and (den - 1) == 0
    }

    private fun toValidKeys(text: String): List<String> {
        return text.uppercase().filter { it in 'A'..'Z' }.map { it.toString() }
    }
}

sealed class ScoreParseResult {
    data class Success(val score: ScoreInfo) : ScoreParseResult()
    data class Failure(val message: UiText) : ScoreParseResult()
}

