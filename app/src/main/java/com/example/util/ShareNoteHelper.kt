package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.Converters
import com.example.data.local.NoteEntity
import java.io.File

object ShareNoteHelper {

    fun shareNote(context: Context, note: NoteEntity) {
        try {
            val title = note.title.ifBlank { "Untitled Note" }
            val sb = StringBuilder()
            sb.append(title).append("\n\n")

            if (note.type == "checklist") {
                val items = Converters.jsonToChecklist(note.checklistJson)
                items.forEach { item ->
                    val mark = if (item.isChecked) "[x]" else "[ ]"
                    sb.append("$mark ${item.text}\n")
                }
            } else {
                val plainText = RichTextRenderer.stripHtml(note.content)
                sb.append(plainText)
            }

            val tags = Converters.jsonToStringList(note.tagsJson)
            if (tags.isNotEmpty()) {
                sb.append("\n\nTags: ").append(tags.joinToString(", ") { "#$it" })
            }

            val shareText = sb.toString().trim()

            val attachmentPaths = Converters.jsonToStringList(note.attachmentsJson)
            val validImageFiles = attachmentPaths.map { File(it) }.filter { it.exists() && it.canRead() }

            val intent = if (validImageFiles.isNotEmpty()) {
                val imageUris = ArrayList<Uri>()
                validImageFiles.forEach { file ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    imageUris.add(uri)
                }

                if (imageUris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, imageUris[0])
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            }

            val chooser = Intent.createChooser(intent, "Share Note via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
