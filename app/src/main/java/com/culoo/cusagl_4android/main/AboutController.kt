package com.culoo.cusagl_4android.main

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val releaseUrl: String,
    val apkDownloadUrl: String
)

sealed class UpdateCheckResult {
    data class UpToDate(val release: ReleaseInfo) : UpdateCheckResult()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult()
    data class Failure(val message: String) : UpdateCheckResult()
}

object AboutController {
    const val REPOSITORY_URL = "https://github.com/Viruatios/CuSAGL-4Android"
    const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/Viruatios/CuSAGL-4Android/releases/latest"
    const val APK_ASSET_NAME = "apk-debug.apk"

    private const val UPDATE_DIR_NAME = "updates"
    private const val TEMP_APK_NAME = "$APK_ASSET_NAME.part"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000

    fun checkReleaseJson(currentVersion: String, jsonText: String): UpdateCheckResult {
        val release = parseRelease(jsonText)
            ?: return UpdateCheckResult.Failure("最新 Release 中没有找到 $APK_ASSET_NAME。")
        val comparison = compareVersions(release.tagName, currentVersion)
            ?: return UpdateCheckResult.Failure("无法比较版本号：${release.tagName} / $currentVersion")
        return if (comparison > 0) {
            UpdateCheckResult.UpdateAvailable(release)
        } else {
            UpdateCheckResult.UpToDate(release)
        }
    }

    fun fetchLatestRelease(currentVersion: String): UpdateCheckResult {
        return try {
            val connection = URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "CuSAGL-4Android")
            connection.inputStream.use { stream ->
                val jsonText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                checkReleaseJson(currentVersion, jsonText)
            }
        } catch (ex: Exception) {
            UpdateCheckResult.Failure("检查更新失败：${ex.message ?: "网络请求异常"}")
        }
    }

    fun compareVersions(left: String, right: String): Int? {
        val leftParts = parseVersion(left) ?: return null
        val rightParts = parseVersion(right) ?: return null
        val maxSize = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until maxSize) {
            val leftPart = leftParts.getOrElse(index) { 0 }
            val rightPart = rightParts.getOrElse(index) { 0 }
            if (leftPart != rightPart) return leftPart.compareTo(rightPart)
        }
        return 0
    }

    fun updateDir(cacheDir: File): File = File(cacheDir, UPDATE_DIR_NAME)

    fun apkFile(cacheDir: File): File = File(updateDir(cacheDir), APK_ASSET_NAME)

    fun tempApkFile(cacheDir: File): File = File(updateDir(cacheDir), TEMP_APK_NAME)

    fun clearUpdateCache(cacheDir: File) {
        updateDir(cacheDir).deleteRecursively()
    }

    fun downloadApk(downloadUrl: String, cacheDir: File): File {
        clearUpdateCache(cacheDir)
        val updateDir = updateDir(cacheDir)
        updateDir.mkdirs()
        val tempFile = tempApkFile(cacheDir)
        val apkFile = apkFile(cacheDir)
        return try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", "CuSAGL-4Android")
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (apkFile.exists()) apkFile.delete()
            if (!tempFile.renameTo(apkFile)) {
                throw IllegalStateException("无法写入安装包缓存。")
            }
            apkFile
        } catch (ex: Exception) {
            tempFile.delete()
            throw ex
        }
    }

    private fun parseRelease(jsonText: String): ReleaseInfo? {
        val json = try {
            JSONObject(jsonText)
        } catch (ex: Exception) {
            return null
        }
        val tagName = json.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
        val releaseUrl = json.optString("html_url").takeIf { it.isNotBlank() } ?: REPOSITORY_URL
        val assets = json.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("name") == APK_ASSET_NAME) {
                val downloadUrl = asset.optString("browser_download_url")
                if (downloadUrl.isNotBlank()) {
                    return ReleaseInfo(
                        tagName = tagName,
                        releaseUrl = releaseUrl,
                        apkDownloadUrl = downloadUrl
                    )
                }
            }
        }
        return null
    }

    private fun parseVersion(version: String): List<Int>? {
        val normalized = version.trim().removePrefix("v").removePrefix("V")
        if (normalized.isBlank()) return null
        val parts = normalized.split(".")
        if (parts.any { it.isBlank() || !it.all(Char::isDigit) }) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
