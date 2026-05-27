package com.culoo.cusagl_4android.core

import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

object ScoreParser {
    private val partRegex = Regex("\\([A-Za-z]+\\)|[A-Za-z]")

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

        val json = try {
            JSONObject(text)
        } catch (ex: Exception) {
            logger.e(LogTags.PARSE_FAIL, "Invalid score JSON: ${scoreFile.absolutePath}", ex)
            return null
        }

        val notesText = json.optString("notes", null)
        if (notesText.isNullOrBlank()) {
            logger.e(LogTags.PARSE_FAIL, "Missing notes in score: ${scoreFile.absolutePath}")
            return null
        }

        val notes = parseNotes(notesText)

        return ScoreInfo(
            name = json.optString("name", "未知曲名"),
            author = json.optString("author", "未知作者"),
            instrument = json.optString("instrument", "无建议乐器"),
            description = json.optString("description", "无描述"),
            type = "keyboard",
            bpm = parseBpm(json.opt("bpm")),
            timeSignature = json.optString("time_signature", "4/4"),
            composer = json.optString("composer", "未知作曲者"),
            arranger = json.optString("arranger", "未知编曲者"),
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

    private fun toValidKeys(text: String): List<String> {
        return text.uppercase().filter { it in 'A'..'Z' }.map { it.toString() }
    }
}

