package com.culoo.cusagl_4android.accessibility

import android.view.WindowManager
import com.culoo.cusagl_4android.core.KeyLayout
import com.culoo.cusagl_4android.core.PointF2
import kotlin.math.max

class TouchCoordinateMapper(private val windowManager: WindowManager) {
    data class ScreenSize(val widthPx: Int, val heightPx: Int)

    @Volatile
    private var cachedSize: ScreenSize? = null
    @Volatile
    private var cachedCoordinates: Map<String, PointF2> = emptyMap()

    fun getCoordinateForKey(key: String): PointF2? {
        return getCoordinates()[key]
    }

    fun getCoordinates(): Map<String, PointF2> {
        val size = getScreenSizePx()
        val lastSize = cachedSize
        if (lastSize == null || lastSize != size) {
            cachedSize = size
            cachedCoordinates = KeyLayout.baseCoordinates.mapValues {
                mapPoint(it.value, size.widthPx, size.heightPx)
            }
        }
        return cachedCoordinates
    }

    private fun getScreenSizePx(): ScreenSize {
        val bounds = windowManager.currentWindowMetrics.bounds
        return ScreenSize(bounds.width(), bounds.height())
    }

    companion object {
        const val BASE_WIDTH_PX = AccessibilityConstants.BASE_WIDTH_PX
        const val BASE_HEIGHT_PX = AccessibilityConstants.BASE_HEIGHT_PX

        fun mapPoint(base: PointF2, widthPx: Int, heightPx: Int): PointF2 {
            val scale = max(widthPx / BASE_WIDTH_PX, heightPx / BASE_HEIGHT_PX)
            val x = base.x * scale + (widthPx - BASE_WIDTH_PX * scale) / 2f
            val y = base.y * scale + (heightPx - BASE_HEIGHT_PX * scale)
            return PointF2(x, y)
        }
    }
}

