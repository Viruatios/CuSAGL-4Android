package com.culoo.cusagl_4android

import android.graphics.PointF
import android.graphics.Path
import android.accessibilityservice.GestureDescription
import kotlin.math.max

/* 运行时时间轴扫描播放器
    * 读取曲谱解析 JS 脚本提交的按键指令时间轴数组，然后通过模拟触控执行对应的按键操作。
    * 在Kotlin层原生实现该功能，以保证更高的性能和更低的延迟，满足对实时性的严格要求。
 */
class TimelinePlayer {

    companion object {
        // 基准分辨率
        private const val W_BASE = 1920f
        private const val H_BASE = 1080f

        // 1920x1080 分辨率下的基准琴键坐标表
        private val BASE_COORDINATES = mapOf(
            'Q' to PointF(455f, 670f),
            'W' to PointF(625f, 670f),
            'E' to PointF(790f, 670f),
            'R' to PointF(960f, 670f),
            'T' to PointF(1125f, 670f),
            'Y' to PointF(1295f, 670f),
            'U' to PointF(1460f, 670f),
            'A' to PointF(455f, 805f),
            'S' to PointF(625f, 805f),
            'D' to PointF(790f, 805f),
            'F' to PointF(960f, 805f),
            'G' to PointF(1125f, 805f),
            'H' to PointF(1295f, 805f),
            'J' to PointF(1460f, 805f),
            'Z' to PointF(455f, 940f),
            'X' to PointF(625f, 940f),
            'C' to PointF(790f, 940f),
            'V' to PointF(960f, 940f),
            'B' to PointF(1125f, 940f),
            'N' to PointF(1295f, 940f),
            'M' to PointF(1460f, 940f)
        )
    }

    /**
     * 在屏幕上执行确切的点击手势
     */
    fun clickAt(x: Float, y: Float, duration: Long) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        CuSAGLAccessibilityService.instance?.dispatchGesture(gesture, null, null)
    }

    /**
     * 根据设备实际分辨率，将 1920x1080 基准下的原神“风物之诗琴”UI坐标映射为实际屏幕坐标。
     * 排版规则为“X 轴居中，Y 轴底部对齐，取宽高差异更大的一边缩放”。
     *
     * @param targetWidth 目标设备的屏幕宽度
     * @param targetHeight 目标设备的屏幕高度
     * @param keyName 琴键名称，例如 'Q', 'W', 'A' 等
     * @return 映射到当前屏幕的实际像素坐标 PointF
     */
    fun getMappedCoordinate(targetWidth: Int, targetHeight: Int, keyName: Char): PointF {
        val basePoint = BASE_COORDINATES[keyName.uppercaseChar()] ?: return PointF(0f, 0f)
        val xBase = basePoint.x
        val yBase = basePoint.y

        // 计算缩放比：取宽和高差异更大的一边
        val scale = max(targetWidth / W_BASE, targetHeight / H_BASE)

        // X轴转换 (居中对齐)
        val xTarget = xBase * scale + (targetWidth - W_BASE * scale) / 2f

        // Y轴转换 (底部对齐)
        val yTarget = yBase * scale + (targetHeight - H_BASE * scale)

        return PointF(xTarget, yTarget)
    }
}