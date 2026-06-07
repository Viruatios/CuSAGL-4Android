package com.culoo.cusagl_4android.overlay

import com.culoo.cusagl_4android.core.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPositionMapperTest {
    @Test
    fun topAlignedPoint_identityAtBaseResolution() {
        val mapped = OverlayPositionMapper.mapTopAlignedPoint(960f, 40f, 1920, 1080)

        assertEquals(960, mapped.x)
        assertEquals(40, mapped.y)
    }

    @Test
    fun initialPosition_isTopCenteredAtBaseResolution() {
        val position = OverlayPositionMapper.initialPosition(
            widthPx = 1920,
            heightPx = 1080,
            overlayWidthPx = 400,
            overlayHeightPx = 180
        )

        assertEquals(760, position.x)
        assertEquals(40, position.y)
    }

    @Test
    fun constrain_keepsOverlayAboveKeySafetyBoundary() {
        val position = OverlayPositionMapper.constrain(
            requestedX = 5000,
            requestedY = 5000,
            widthPx = 1920,
            heightPx = 1080,
            overlayWidthPx = 400,
            overlayHeightPx = 180
        )

        assertEquals(1520, position.x)
        assertEquals(410, position.y)
        assertTrue(position.y + 180 <= 590)
    }

    @Test
    fun wideResolution_usesSharedScaleAndTopAlignment() {
        val mapped = OverlayPositionMapper.mapTopAlignedPoint(960f, 40f, 2400, 1080)

        assertEquals(1200, mapped.x)
        assertEquals(50, mapped.y)
    }

    @Test
    fun wideResolution_safetyBoundaryStaysAboveBottomAlignedKeys() {
        val position = OverlayPositionMapper.constrain(
            requestedX = 0,
            requestedY = 5000,
            widthPx = 2400,
            heightPx = 1080,
            overlayWidthPx = 400,
            overlayHeightPx = 180
        )

        assertEquals(288, position.y)
        assertTrue(position.y + 180 <= 468)
    }

    @Test
    fun movement_isDisabledOnlyWhilePlaying() {
        assertTrue(OverlayPositionMapper.canMove(PlaybackState.IDLE))
        assertTrue(OverlayPositionMapper.canMove(PlaybackState.PAUSED))
        assertTrue(OverlayPositionMapper.canMove(PlaybackState.STOPPED))
        assertFalse(OverlayPositionMapper.canMove(PlaybackState.PLAYING))
    }

    @Test
    fun oversizedPanel_doesNotFitSafeArea() {
        assertFalse(
            OverlayPositionMapper.fitsSafeArea(
                widthPx = 1920,
                heightPx = 1080,
                overlayHeightPx = 600
            )
        )
    }

    @Test
    fun oversizedPanel_constrainFallsBackToVisibleTop() {
        val position = OverlayPositionMapper.constrain(
            requestedX = 5000,
            requestedY = 5000,
            widthPx = 1920,
            heightPx = 1080,
            overlayWidthPx = 400,
            overlayHeightPx = 600,
            topInsetPx = 24
        )

        assertEquals(760, position.x)
        assertEquals(0, position.y)
    }

    @Test
    fun constrain_ignoresTopInsetAsUpperBoundary() {
        val position = OverlayPositionMapper.constrain(
            requestedX = 100,
            requestedY = -50,
            widthPx = 1920,
            heightPx = 1080,
            overlayWidthPx = 400,
            overlayHeightPx = 180,
            topInsetPx = 80
        )

        assertEquals(100, position.x)
        assertEquals(0, position.y)
    }
}
