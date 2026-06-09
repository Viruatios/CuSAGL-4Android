package com.culoo.cusagl_4android.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AboutControllerTest {
    @Test
    fun compareVersions_usesSemanticNumberOrder() {
        assertTrue(AboutController.compareVersions("v1.0.10", "v1.0.2")!! > 0)
        assertEquals(0, AboutController.compareVersions("v1.0.1", "1.0.1"))
        assertTrue(AboutController.compareVersions("1.2", "1.2.1")!! < 0)
    }

    @Test
    fun checkReleaseJson_newerVersionReportsUpdate() {
        val result = AboutController.checkReleaseJson(
            currentVersion = "v1.0.1",
            jsonText = releaseJson(tag = "v1.0.2")
        )

        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val release = (result as UpdateCheckResult.UpdateAvailable).release
        assertEquals("v1.0.2", release.tagName)
        assertEquals("https://example.com/app-debug.apk", release.apkDownloadUrl)
    }

    @Test
    fun checkReleaseJson_sameOrOlderVersionReportsUpToDate() {
        val same = AboutController.checkReleaseJson(
            currentVersion = "v1.0.1",
            jsonText = releaseJson(tag = "v1.0.1")
        )
        val older = AboutController.checkReleaseJson(
            currentVersion = "v1.0.2",
            jsonText = releaseJson(tag = "v1.0.1")
        )

        assertTrue(same is UpdateCheckResult.UpToDate)
        assertTrue(older is UpdateCheckResult.UpToDate)
    }

    @Test
    fun checkReleaseJson_missingApkAssetReportsFailure() {
        val result = AboutController.checkReleaseJson(
            currentVersion = "v1.0.1",
            jsonText = """
                {
                  "tag_name": "v1.0.2",
                  "html_url": "https://github.com/Viruatios/CuSAGL-4Android/releases/tag/v1.0.2",
                  "assets": [
                    {
                      "name": "source.zip",
                      "browser_download_url": "https://example.com/source.zip"
                    }
                  ]
                }
            """.trimIndent()
        )

        assertTrue(result is UpdateCheckResult.Failure)
    }

    @Test
    fun checkReleaseJson_invalidTagReportsFailure() {
        val result = AboutController.checkReleaseJson(
            currentVersion = "v1.0.1",
            jsonText = releaseJson(tag = "latest")
        )

        assertTrue(result is UpdateCheckResult.Failure)
    }

    @Test
    fun updateCachePathsAndCleanupUsePrivateUpdatesDirectory() {
        val cacheDir = Files.createTempDirectory("cusagl-about-cache").toFile()
        val updateDir = AboutController.updateDir(cacheDir)
        val apkFile = AboutController.apkFile(cacheDir)
        val tempFile = AboutController.tempApkFile(cacheDir)
        updateDir.mkdirs()
        apkFile.writeText("old apk")
        tempFile.writeText("partial")

        assertEquals("updates", updateDir.name)
        assertEquals("app-debug.apk", apkFile.name)
        assertEquals("app-debug.apk.part", tempFile.name)

        AboutController.clearUpdateCache(cacheDir)

        assertFalse(updateDir.exists())
    }

    private fun releaseJson(tag: String): String {
        return """
            {
              "tag_name": "$tag",
              "html_url": "https://github.com/Viruatios/CuSAGL-4Android/releases/tag/$tag",
              "assets": [
                {
                  "name": "app-debug.apk",
                  "browser_download_url": "https://example.com/app-debug.apk"
                }
              ]
            }
        """.trimIndent()
    }
}
