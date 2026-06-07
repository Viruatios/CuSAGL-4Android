package com.culoo.cusagl_4android.accessibility

import android.content.Intent
import android.provider.Settings

object AccessibilityPermission {
    fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
