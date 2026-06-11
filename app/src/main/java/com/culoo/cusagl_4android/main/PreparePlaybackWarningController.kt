package com.culoo.cusagl_4android.main

import android.content.SharedPreferences

interface BooleanPreferenceStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

class SharedPreferencesBooleanStore(
    private val sharedPreferences: SharedPreferences
) : BooleanPreferenceStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}

object PreparePlaybackWarningController {
    fun isSuppressed(store: BooleanPreferenceStore): Boolean {
        return store.getBoolean(MainConstants.PREPARE_PLAYBACK_WARNING_SUPPRESSED_KEY, false)
    }

    fun setSuppressed(store: BooleanPreferenceStore, suppressed: Boolean) {
        store.putBoolean(MainConstants.PREPARE_PLAYBACK_WARNING_SUPPRESSED_KEY, suppressed)
    }

    fun shouldShowWarning(store: BooleanPreferenceStore): Boolean {
        return !isSuppressed(store)
    }
}
