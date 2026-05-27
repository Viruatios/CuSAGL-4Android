package com.culoo.cusagl_4android.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ScoreStorage {
    private const val SCORE_DIR_NAME = "score_file" // Fixed name; keep in comment only.
    private const val CACHE_DIR_NAME = "cache" // Fixed name; keep in comment only.

    private val validNameRegex = Regex("^\\d{4}\\..*\\.json$")

    fun listAndNormalizeScores(filesDir: File, logger: Logger = DefaultLogger): List<String> {
        val scoreDir = scoreDir(filesDir)
        if (!scoreDir.exists()) {
            scoreDir.mkdirs()
        }

        val entries = scoreDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }.orEmpty()
        val usedNumbers = mutableSetOf<Int>()
        val finalList = mutableListOf<String>()

        for (entry in entries) {
            if (validNameRegex.matches(entry.name)) {
                usedNumbers.add(entry.name.substring(0, 4).toInt())
            }
        }

        for (entry in entries) {
            if (validNameRegex.matches(entry.name)) {
                finalList.add(entry.name.removeSuffix(".json"))
            } else {
                val baseName = entry.name.removeSuffix(".json")
                var newNum = 1
                while (usedNumbers.contains(newNum)) newNum++

                val newPrefix = newNum.toString().padStart(4, '0')
                val newFileName = "$newPrefix.$baseName.json"
                val newPath = File(entry.parentFile, newFileName)

                val renamed = entry.renameTo(newPath)
                if (!renamed) {
                    logger.w(LogTags.PARSE_FAIL, "Failed to rename score file: ${entry.name}")
                    continue
                }

                finalList.add("$newPrefix.$baseName")
                usedNumbers.add(newNum)
            }
        }

        return finalList.sortedBy { it.substring(0, 4).toIntOrNull() ?: Int.MAX_VALUE }
    }

    fun loadCache(filesDir: File, name: String, logger: Logger = DefaultLogger): CacheData? {
        val cacheFile = cacheFile(filesDir, name)
        if (!cacheFile.exists()) return null

        val text = try {
            cacheFile.readText()
        } catch (ex: Exception) {
            logger.w(LogTags.CACHE_INVALID, "Failed to read cache file: ${cacheFile.absolutePath}", ex)
            return null
        }

        val json = try {
            JSONObject(text)
        } catch (ex: Exception) {
            logger.w(LogTags.CACHE_INVALID, "Invalid cache JSON: ${cacheFile.absolutePath}", ex)
            return null
        }

        return try {
            CacheData(
                name = json.getString("name"),
                author = json.optString("author", "未知作者"),
                barCount = json.getInt("barCount"),
                eventBatchCount = json.getInt("eventBatchCount"),
                expectedDurationMs = json.getInt("expectedDuration"),
                createTimeMs = json.getLong("create_time"),
                gapMs = json.getDouble("gap"),
                mergedTimeline = parseMergedTimeline(json.getJSONArray("mergedTimeline"))
            )
        } catch (ex: Exception) {
            logger.w(LogTags.CACHE_INVALID, "Failed to parse cache fields: ${cacheFile.absolutePath}", ex)
            null
        }
    }

    fun saveCache(filesDir: File, name: String, cache: CacheData, logger: Logger = DefaultLogger) {
        val cacheDir = cacheDir(filesDir)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val cacheFile = cacheFile(filesDir, name)
        try {
            cacheFile.writeText(serializeCache(cache))
        } catch (ex: Exception) {
            logger.w(LogTags.CACHE_INVALID, "Failed to write cache file: ${cacheFile.absolutePath}", ex)
        }
    }

    fun cleanExpiredCaches(filesDir: File, scoreNames: Set<String>, logger: Logger = DefaultLogger) {
        val cacheDir = cacheDir(filesDir)
        if (!cacheDir.exists()) return

        val caches = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }.orEmpty()
        for (cacheFile in caches) {
            val musicName = cacheFile.name.removeSuffix(".json")
            val scoreFile = scoreFile(filesDir, musicName)

            if (!scoreNames.contains(musicName) || !scoreFile.exists()) {
                cacheFile.delete()
                logger.d(LogTags.FILE_MISSING, "Removed orphan cache: ${cacheFile.name}")
                continue
            }

            val scoreModified = scoreFile.lastModified()
            val cacheModified = cacheFile.lastModified()
            if (scoreModified > 0 && cacheModified < scoreModified) {
                cacheFile.delete()
                logger.d(LogTags.CACHE_INVALID, "Removed expired cache: ${cacheFile.name}")
            }
        }
    }

    fun buildCache(score: ScoreInfo): CacheData {
        val gapMs = calcGap(score.bpm, score.timeSignature)
        val prebake = TimelinePrebaker.prebakeTimeline(score.notes, gapMs)

        return CacheData(
            name = score.name,
            author = score.author,
            barCount = score.notes.size,
            eventBatchCount = prebake.mergedTimeline.size,
            expectedDurationMs = prebake.totalCalculatedTimeMs,
            createTimeMs = System.currentTimeMillis(),
            gapMs = gapMs,
            mergedTimeline = prebake.mergedTimeline
        )
    }

    fun scoreDir(filesDir: File): File = File(filesDir, SCORE_DIR_NAME)

    fun cacheDir(filesDir: File): File = File(filesDir, CACHE_DIR_NAME)

    fun scoreFile(filesDir: File, name: String): File = File(scoreDir(filesDir), "$name.json")

    fun cacheFile(filesDir: File, name: String): File = File(cacheDir(filesDir), "$name.json")

    private fun calcGap(bpm: Int, timeSignature: String): Double {
        var gapMultiplier = 1.0
        if (timeSignature.contains('/')) {
            val parts = timeSignature.split('/')
            val num = parts.getOrNull(0)?.toIntOrNull() ?: 4
            val den = parts.getOrNull(1)?.toIntOrNull() ?: 4
            gapMultiplier = if (den == 8 && num % 3 == 0) {
                1.5
            } else {
                4.0 / den.toDouble()
            }
        }
        return (60000.0 / bpm.toDouble()) * gapMultiplier
    }

    private fun parseMergedTimeline(array: JSONArray): List<MergedEvent> {
        val list = mutableListOf<MergedEvent>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val action = when (obj.getString("action")) {
                "down" -> ActionType.DOWN
                "up" -> ActionType.UP
                else -> ActionType.DOWN
            }
            val keysArray = obj.getJSONArray("keys")
            val keys = mutableListOf<String>()
            for (k in 0 until keysArray.length()) {
                keys.add(keysArray.getString(k))
            }
            list.add(MergedEvent(obj.getInt("time"), action, keys))
        }
        return list
    }

    private fun serializeCache(cache: CacheData): String {
        val json = JSONObject()
        json.put("name", cache.name)
        json.put("author", cache.author)
        json.put("barCount", cache.barCount)
        json.put("eventBatchCount", cache.eventBatchCount)
        json.put("expectedDuration", cache.expectedDurationMs)
        json.put("create_time", cache.createTimeMs)
        json.put("gap", cache.gapMs)

        val timelineArray = JSONArray()
        for (event in cache.mergedTimeline) {
            val obj = JSONObject()
            obj.put("time", event.timeMs)
            obj.put("action", if (event.action == ActionType.DOWN) "down" else "up")
            obj.put("keys", JSONArray(event.keys))
            timelineArray.put(obj)
        }
        json.put("mergedTimeline", timelineArray)
        return json.toString()
    }
}

