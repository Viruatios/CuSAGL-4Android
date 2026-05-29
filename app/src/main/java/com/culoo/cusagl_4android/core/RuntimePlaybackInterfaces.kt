package com.culoo.cusagl_4android.core

import android.os.SystemClock
import java.io.File

interface TimeSource {
    fun nowMs(): Long
}

class SystemClockTimeSource : TimeSource {
    override fun nowMs(): Long = SystemClock.uptimeMillis()
}

interface Sleeper {
    fun sleepMs(durationMs: Long)
}

class ThreadSleeper : Sleeper {
    override fun sleepMs(durationMs: Long) {
        if (durationMs <= 0) return
        try {
            Thread.sleep(durationMs)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

interface TouchInjector {
    fun keyDown(key: String)
    fun keyUp(key: String)

    fun releaseAll(keys: List<String>) {
        keys.forEach { keyUp(it) }
    }
}

interface CacheProvider {
    fun loadCache(name: String): CacheData?
}

class ScoreCacheProvider(
    private val filesDir: File,
    private val logger: Logger = DefaultLogger
) : CacheProvider {
    override fun loadCache(name: String): CacheData? {
        val cached = ScoreStorage.loadCache(filesDir, name, logger)
        if (cached != null) return cached

        val score = ScoreParser.loadScoreByName(filesDir, name, logger) ?: return null
        val newCache = ScoreStorage.buildCache(score)
        ScoreStorage.saveCache(filesDir, name, newCache, logger)
        return newCache
    }
}
