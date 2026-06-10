package com.culoo.cusagl_4android.core

import com.culoo.cusagl_4android.R
import com.culoo.cusagl_4android.UiText
import org.json.JSONException
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

        return when (val result = parseScoreText(text, strict = false, logger = logger, source = scoreFile.absolutePath)) {
            is ScoreParseInternalResult.Success -> result.score
            is ScoreParseInternalResult.Failure -> null
        }
    }

    fun parseScoreTextStrict(
        text: String,
        logger: Logger = DefaultLogger,
        source: String = "score text"
    ): ScoreParseResult {
        return when (val result = parseScoreText(text, strict = true, logger = logger, source = source)) {
            is ScoreParseInternalResult.Success -> ScoreParseResult.Success(result.score)
            is ScoreParseInternalResult.Failure -> ScoreParseResult.Failure(result.message)
        }
    }

    private fun parseScoreText(
        text: String,
        strict: Boolean,
        logger: Logger,
        source: String
    ): ScoreParseInternalResult {
        val json = try {
            JSONObject(text)
        } catch (ex: JSONException) {
            logger.e(LogTags.PARSE_FAIL, "Invalid score JSON: $source", ex)
            val location = locationFromException(ex) ?: locationFromOffset(text, 0)
            return ScoreParseInternalResult.Failure(jsonSyntaxFailure(location))
        } catch (ex: Exception) {
            logger.e(LogTags.PARSE_FAIL, "Invalid score JSON: $source", ex)
            return ScoreParseInternalResult.Failure(jsonSyntaxFailure(locationFromOffset(text, 0)))
        }

        val notesText = if (json.has("notes")) json.optString("notes") else null
        if (notesText.isNullOrBlank()) {
            logger.e(LogTags.PARSE_FAIL, "Missing notes in score: $source")
            return ScoreParseInternalResult.Failure(UiText.resource(R.string.error_score_json_notes_empty))
        }

        val name = json.optString("name", CoreConstants.DEFAULT_SCORE_NAME)
        val bpm = parseBpm(json.opt("bpm"))
        val timeSignature = json.optString("time_signature", "4/4")
        if (strict) {
            if (name.isBlank()) {
                logger.e(LogTags.PARSE_FAIL, "Missing name in score: $source")
                return ScoreParseInternalResult.Failure(UiText.resource(R.string.error_score_json_score_name_empty))
            }
            if (!isPositiveBpm(json.opt("bpm"))) {
                logger.e(LogTags.PARSE_FAIL, "Invalid bpm in score: $source")
                return ScoreParseInternalResult.Failure(UiText.resource(R.string.error_bpm_positive_integer))
            }
            if (!isValidTimeSignature(timeSignature)) {
                logger.e(LogTags.PARSE_FAIL, "Invalid time signature in score: $source")
                return ScoreParseInternalResult.Failure(UiText.resource(R.string.error_score_json_time_signature_invalid))
            }
        }

        val notes = parseNotes(notesText)
        if (strict && notes.isEmpty()) {
            logger.e(LogTags.PARSE_FAIL, "Empty parsed notes in score: $source")
            return ScoreParseInternalResult.Failure(UiText.resource(R.string.error_score_json_notes_unparseable))
        }

        return ScoreParseInternalResult.Success(
            ScoreInfo(
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

    private fun validationFailure(source: String, field: String, location: TextLocation?): UiText {
        val locationText = location?.let { "line ${it.line}, column ${it.column}" } ?: "unknown position"
        return UiText.resource(R.string.error_score_json_validation_detail, source, field, locationText)
    }

    private fun jsonSyntaxFailure(location: TextLocation): UiText {
        return UiText.resource(R.string.error_score_json_invalid_syntax, location.line, location.column)
    }

    private fun locationFromException(ex: JSONException): TextLocation? {
        val message = ex.message ?: return null
        val line = Regex("line\\s+(\\d+)").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val character = Regex("character\\s+(\\d+)").find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (line != null && character != null) {
            TextLocation(line.coerceAtLeast(1), character.coerceAtLeast(1))
        } else {
            null
        }
    }

    private fun fieldLocation(text: String, field: String): TextLocation? {
        val match = Regex("\"${Regex.escape(field)}\"\\s*:").find(text) ?: return null
        return locationFromOffset(text, match.range.first)
    }

    private fun locationFromOffset(text: String, offset: Int): TextLocation {
        var line = 1
        var column = 1
        text.take(offset.coerceAtMost(text.length)).forEach { char ->
            if (char == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return TextLocation(line, column)
    }

    private data class TextLocation(
        val line: Int,
        val column: Int
    )

    private sealed class ScoreParseInternalResult {
        data class Success(val score: ScoreInfo) : ScoreParseInternalResult()
        data class Failure(val message: UiText) : ScoreParseInternalResult()
    }
}

sealed class ScoreParseResult {
    data class Success(val score: ScoreInfo) : ScoreParseResult()
    data class Failure(val message: UiText) : ScoreParseResult()
}

