package com.culoo.cusagl_4android.core

data class PointF2(val x: Float, val y: Float)

object KeyLayout {
    val baseCoordinates: Map<String, PointF2> = mapOf(
        "Q" to PointF2(455f, 670f),
        "W" to PointF2(625f, 670f),
        "E" to PointF2(790f, 670f),
        "R" to PointF2(960f, 670f),
        "T" to PointF2(1125f, 670f),
        "Y" to PointF2(1295f, 670f),
        "U" to PointF2(1460f, 670f),
        "A" to PointF2(455f, 805f),
        "S" to PointF2(625f, 805f),
        "D" to PointF2(790f, 805f),
        "F" to PointF2(960f, 805f),
        "G" to PointF2(1125f, 805f),
        "H" to PointF2(1295f, 805f),
        "J" to PointF2(1460f, 805f),
        "Z" to PointF2(455f, 940f),
        "X" to PointF2(625f, 940f),
        "C" to PointF2(790f, 940f),
        "V" to PointF2(960f, 940f),
        "B" to PointF2(1125f, 940f),
        "N" to PointF2(1295f, 940f),
        "M" to PointF2(1460f, 940f)
    )

    val allKeys: List<String> = baseCoordinates.keys.sorted()
}

