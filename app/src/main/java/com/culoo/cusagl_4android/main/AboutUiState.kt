package com.culoo.cusagl_4android.main

import com.culoo.cusagl_4android.UiText

data class AboutUiState(
    val currentVersion: String,
    val repositoryUrl: String = AboutController.REPOSITORY_URL,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val latestTag: String? = null,
    val releaseUrl: String? = null,
    val apkDownloadUrl: String? = null,
    val apkAssetName: String? = null,
    val hasUpdate: Boolean = false,
    val message: UiText? = null,
    val errorMessage: UiText? = null
) {
    val canInstallUpdate: Boolean
        get() = hasUpdate && apkDownloadUrl != null && apkAssetName != null && !isChecking && !isDownloading
}
