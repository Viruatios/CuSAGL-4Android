package com.culoo.cusagl_4android.overlay

import com.culoo.cusagl_4android.core.PlaybackState
import kotlin.math.max
import kotlin.math.roundToInt

data class OverlayPosition(val x: Int, val y: Int)

object OverlayPositionMapper {
    const val BASE_WIDTH_PX = 1920f
    const val BASE_HEIGHT_PX = 1080f
    const val INITIAL_CENTER_X_BASE = 960f
    const val INITIAL_TOP_BASE = 40f
    const val FIRST_KEY_ROW_Y_BASE = 670f
    const val KEY_SAFETY_MARGIN_BASE = 80f
    const val SAFE_BOTTOM_BASE = FIRST_KEY_ROW_Y_BASE - KEY_SAFETY_MARGIN_BASE

    fun scale(widthPx: Int, heightPx: Int): Float {
        return max(widthPx / BASE_WIDTH_PX, heightPx / BASE_HEIGHT_PX)
    }

    fun mapTopAlignedPoint(xBase: Float, yBase: Float, widthPx: Int, heightPx: Int): OverlayPosition {
        val scale = scale(widthPx, heightPx)
        val x = xBase * scale + (widthPx - BASE_WIDTH_PX * scale) / 2f
        val y = yBase * scale
        return OverlayPosition(x.roundToInt(), y.roundToInt())
    }

    fun initialPosition(
        widthPx: Int,
        heightPx: Int,
        overlayWidthPx: Int,
        overlayHeightPx: Int,
        topInsetPx: Int = 0
    ): OverlayPosition {
        val anchor = mapTopAlignedPoint(INITIAL_CENTER_X_BASE, INITIAL_TOP_BASE, widthPx, heightPx)
        return constrain(
            requestedX = anchor.x - overlayWidthPx / 2,
            requestedY = anchor.y,
            widthPx = widthPx,
            heightPx = heightPx,
            overlayWidthPx = overlayWidthPx,
            overlayHeightPx = overlayHeightPx,
            topInsetPx = topInsetPx
        )
    }

    fun constrain(
        requestedX: Int,
        requestedY: Int,
        widthPx: Int,
        heightPx: Int,
        overlayWidthPx: Int,
        overlayHeightPx: Int,
        topInsetPx: Int = 0
    ): OverlayPosition {
        val maxX = max(0, widthPx - overlayWidthPx)
        val safeBottomPx = safeBottomPx(widthPx, heightPx)
        val maxY = safeBottomPx - overlayHeightPx
        if (maxY < topInsetPx) {
            return OverlayPosition(
                x = ((widthPx - overlayWidthPx) / 2).coerceIn(0, maxX),
                y = topInsetPx
            )
        }
        return OverlayPosition(
            x = requestedX.coerceIn(0, maxX),
            y = requestedY.coerceIn(topInsetPx, maxY)
        )
    }

    fun fitsSafeArea(
        widthPx: Int,
        heightPx: Int,
        overlayHeightPx: Int,
        topInsetPx: Int = 0
    ): Boolean {
        return topInsetPx + overlayHeightPx <= safeBottomPx(widthPx, heightPx)
    }

    fun canMove(state: PlaybackState): Boolean = state != PlaybackState.PLAYING

    private fun safeBottomPx(widthPx: Int, heightPx: Int): Int {
        val scale = scale(widthPx, heightPx)
        val mappedFirstKeyY = FIRST_KEY_ROW_Y_BASE * scale + (heightPx - BASE_HEIGHT_PX * scale)
        return (mappedFirstKeyY - KEY_SAFETY_MARGIN_BASE * scale)
            .roundToInt()
            .coerceAtMost(heightPx)
    }
}
