package com.culoo.cusagl_4android.accessibility

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.KeyLayout
import com.culoo.cusagl_4android.core.LogTags
import com.culoo.cusagl_4android.core.Logger
import com.culoo.cusagl_4android.core.TouchInjector

class AccessibilityTouchInjector(
    private val serviceProvider: () -> LyreAccessibilityService?,
    private val coordinateMapper: TouchCoordinateMapper,
    private val logger: Logger = DefaultLogger
) : TouchInjector, AccessibilityServiceListener {

    private val lock = Any()
    private val activeStrokes = mutableMapOf<String, ActiveStroke>()

    init {
        AccessibilityServiceBridge.registerListener(this)
    }

    override fun keyDown(key: String) {
        keyDownAll(listOf(key))
    }

    override fun keyUp(key: String) {
        keyUpAll(listOf(key))
    }

    override fun keyDownAll(keys: List<String>) {
        val service = serviceProvider()
        if (service == null) {
            logger.w(LogTags.ACCESSIBILITY, "Service unavailable for keyDown")
            return
        }
        val mapped = coordinateMapper.getCoordinates()
        val builder = GestureDescription.Builder()
        var added = 0
        synchronized(lock) {
            for (key in keys) {
                if (activeStrokes.containsKey(key)) continue
                val point = mapped[key]
                if (point == null) {
                    logger.w(LogTags.ACCESSIBILITY, "Missing coordinate for key: $key")
                    continue
                }
                val path = Path().apply {
                    moveTo(point.x, point.y)
                    lineTo(point.x, point.y)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, HOLD_DURATION_MS, true)
                activeStrokes[key] = ActiveStroke(key, path, stroke)
                builder.addStroke(stroke)
                added++
            }
        }
        if (added == 0) return
        service.dispatchGestureSafe(builder.build())
    }

    override fun keyUpAll(keys: List<String>) {
        val endStrokes = mutableListOf<GestureDescription.StrokeDescription>()
        synchronized(lock) {
            for (key in keys) {
                val active = activeStrokes.remove(key) ?: continue
                endStrokes.add(active.stroke.continueStroke(active.path, 0, RELEASE_DURATION_MS, false))
            }
        }
        if (endStrokes.isEmpty()) return
        val service = serviceProvider()
        if (service == null) {
            logger.w(LogTags.ACCESSIBILITY, "Service unavailable for keyUp")
            return
        }
        val builder = GestureDescription.Builder()
        endStrokes.forEach { builder.addStroke(it) }
        service.dispatchGestureSafe(builder.build())
    }

    override fun onServiceAvailable(service: LyreAccessibilityService) {
        // No-op.
    }

    override fun onServiceUnavailable() {
        synchronized(lock) {
            activeStrokes.clear()
        }
    }

    fun close() {
        releaseAll(KeyLayout.allKeys)
        AccessibilityServiceBridge.unregisterListener(this)
        synchronized(lock) {
            activeStrokes.clear()
        }
    }

    private data class ActiveStroke(
        val key: String,
        val path: Path,
        val stroke: GestureDescription.StrokeDescription
    )

    companion object {
        const val HOLD_DURATION_MS = 10_000L
        const val RELEASE_DURATION_MS = 1L
    }
}

