package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ShareAppHelper {

    const val APP_NAME = "My Notes"
    const val APP_VERSION = "2.0.0"
    const val DEVELOPER_NAME = "Kundansinh Khant"
    const val APP_TAGLINE = "A simple, secure notes app with rich formatting, checklists, voice notes, offline access, and 7-day Google Drive backups."

    const val DOWNLOAD_LINK = "https://ais-pre-5477vnh3ftauoratbxkang-468255947282.asia-southeast1.run.app"

    val SHARE_TEXT = """
My Notes — a simple, secure notes app. Create rich-text notes and checklists, lock notes with a password, back up automatically to Google Drive, and access your notes offline. Try it out!

How to use:
Download the APK, install it, and start taking notes — no account required.

Download APK:
$DOWNLOAD_LINK
    """.trimIndent()

    fun shareApp(context: Context, shareApkFileDirectly: Boolean = true) {
        try {
            var sharedFileUri: Uri? = null

            if (shareApkFileDirectly) {
                try {
                    val appInfo = context.applicationInfo
                    val originalApk = File(appInfo.sourceDir)
                    if (originalApk.exists() && originalApk.canRead()) {
                        val cacheApk = File(context.cacheDir, "MyNotes_v$APP_VERSION.apk")
                        if (!cacheApk.exists() || cacheApk.length() != originalApk.length()) {
                            FileInputStream(originalApk).use { input ->
                                FileOutputStream(cacheApk).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        sharedFileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            cacheApk
                        )
                    }
                } catch (e: Exception) {
                    // Fallback to text sharing if file extraction fails
                    sharedFileUri = null
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                if (sharedFileUri != null) {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, sharedFileUri)
                    putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
                    putExtra(Intent.EXTRA_SUBJECT, "Download $APP_NAME App")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, SHARE_TEXT)
                    putExtra(Intent.EXTRA_SUBJECT, "Download $APP_NAME App")
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Share $APP_NAME via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open share sheet: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
