package com.culoo.cusagl_4android.accessibility

import com.culoo.cusagl_4android.core.PointF2
import org.junit.Assert.assertEquals
import org.junit.Test

class TouchCoordinateMapperTest {
    @Test
    fun mapPoint_identityAtBaseResolution() {
        val base = PointF2(455f, 670f)
        val mapped = TouchCoordinateMapper.mapPoint(base, 1920, 1080)
        assertEquals(455f, mapped.x, 0.01f)
        assertEquals(670f, mapped.y, 0.01f)
    }

    @Test
    fun mapPoint_wideResolutionBottomAligned() {
        val base = PointF2(455f, 670f)
        val mapped = TouchCoordinateMapper.mapPoint(base, 2400, 1080)
        assertEquals(568.75f, mapped.x, 0.02f)
        assertEquals(567.5f, mapped.y, 0.02f)
    }
}

