package com.culoo.cusagl_4android.main

object MainConstants {
    // App-private playback config file. Changing this requires a migration.
    const val PLAYBACK_CONFIG_FILE_NAME = "playback_config.json"
    const val USER_PREFERENCES_NAME = "user_preferences"
    const val PREPARE_PLAYBACK_WARNING_SUPPRESSED_KEY = "suppress_prepare_playback_warning"

    const val REPOSITORY_URL = "https://github.com/Viruatios/CuSAGL-4Android"
    const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/Viruatios/CuSAGL-4Android/releases/latest"
    const val RELEASE_APK_ASSET_NAME = "app-release.apk"
    const val DEBUG_APK_ASSET_NAME = "app-debug.apk"
    const val UPDATE_DIR_NAME = "updates"
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 30_000
    const val DEFAULT_IMPORT_FILE_LABEL = "import-file"
    const val NO_SELECTED_SCORE_LABEL = "no-selected-score"
    const val NETWORK_ERROR_LABEL = "network-error"
    const val UPDATE_CACHE_WRITE_FAILED_LABEL = "update-cache-write-failed"
}
