package com.culoo.cusagl_4android

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.provider.Settings

class FloatingWindowController(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    init {
        if (Settings.canDrawOverlays(context)) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
    }

    fun show() {
        if (windowManager == null || floatingView != null || !Settings.canDrawOverlays(context)) {
            return
        }

        // Layout creation
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0x88000000.toInt())
            setPadding(16, 16, 16, 16)
        }

        val playButton = Button(context).apply {
            text = "Play"
            setOnClickListener {
                // TODO: Trigger playback via JS -> Kotlin bridge / TimelinePlayer
            }
        }
        val stopButton = Button(context).apply {
            text = "Stop"
            setOnClickListener {
                // TODO: Stop timeline loop playing
            }
        }
        val closeButton = Button(context).apply {
            text = "Close"
            setOnClickListener {
                hide()
            }
        }

        container.addView(playButton)
        container.addView(stopButton)
        container.addView(closeButton)

        floatingView = container

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager?.addView(floatingView, layoutParams)
    }

    fun hide() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
}

