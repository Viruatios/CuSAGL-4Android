package com.culoo.cusagl_4android

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class CuSAGLAccessibilityService : AccessibilityService() {

    companion object {
        var instance: CuSAGLAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only care about gestures, so this can be left empty to minimize overhead.
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}

