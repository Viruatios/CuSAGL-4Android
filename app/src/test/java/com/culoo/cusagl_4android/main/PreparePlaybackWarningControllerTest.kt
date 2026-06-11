package com.culoo.cusagl_4android.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparePlaybackWarningControllerTest {
    @Test
    fun warningShowsByDefault() {
        val store = FakeBooleanPreferenceStore()

        assertFalse(PreparePlaybackWarningController.isSuppressed(store))
        assertTrue(PreparePlaybackWarningController.shouldShowWarning(store))
    }

    @Test
    fun warningCanBeSuppressed() {
        val store = FakeBooleanPreferenceStore()

        PreparePlaybackWarningController.setSuppressed(store, true)

        assertTrue(PreparePlaybackWarningController.isSuppressed(store))
        assertFalse(PreparePlaybackWarningController.shouldShowWarning(store))
    }

    private class FakeBooleanPreferenceStore : BooleanPreferenceStore {
        private val values = mutableMapOf<String, Boolean>()

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            return values[key] ?: defaultValue
        }

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }
    }
}
