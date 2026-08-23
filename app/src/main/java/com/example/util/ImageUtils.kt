package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    fun saveAndCompressImage(context: Context, uri: Uri): String? {
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val maxDimension = 1200
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW = if (width > height) maxDimension else (maxDimension * ratio).toInt()
                val targetH = if (height > width) maxDimension else (maxDimension / ratio).toInt()
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
            val outputFile = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(outputFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()

            outputFile.absolutePath
        }.getOrNull()
    }

    fun deleteImageFile(filePath: String) {
        runCatching {
            val file = File(filePath)
            if (file.exists()) file.delete()
        }
    }

    fun shareSingleImage(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "Image file not found", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(shareIntent, "Share Image via").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error sharing image: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
