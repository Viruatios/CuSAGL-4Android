package com.culoo.cusagl_4android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.culoo.cusagl_4android.core.DefaultLogger
import com.culoo.cusagl_4android.core.LogTags
import com.culoo.cusagl_4android.core.Logger

class LyreAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val logger: Logger = DefaultLogger

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceBridge.bind(this)
        logger.d(LogTags.ACCESSIBILITY, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: gesture injection only.
    }

    override fun onInterrupt() {
        // No-op.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityServiceBridge.unbind(this)
        logger.d(LogTags.ACCESSIBILITY, "Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityServiceBridge.unbind(this)
        logger.d(LogTags.ACCESSIBILITY, "Accessibility service destroyed")
        super.onDestroy()
    }

    fun dispatchGestureSafe(
        gesture: GestureDescription,
        callback: GestureResultCallback? = null
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val ok = dispatchGesture(gesture, callback, null)
            if (!ok) {
                logger.w(LogTags.ACCESSIBILITY, "dispatchGesture returned false")
            }
            return
        }
        mainHandler.post {
            val ok = dispatchGesture(gesture, callback, null)
            if (!ok) {
                logger.w(LogTags.ACCESSIBILITY, "dispatchGesture returned false")
            }
        }
    }
}

