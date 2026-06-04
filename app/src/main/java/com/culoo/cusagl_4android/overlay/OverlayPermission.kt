package com.culoo.cusagl_4android.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OverlayPermission {
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun settingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }
}
