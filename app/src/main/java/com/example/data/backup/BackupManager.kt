package com.example.data.backup

import android.content.Context
import android.util.Base64
import com.example.data.local.NoteEntity
import com.example.data.local.TagEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class BackupNoteDto(
    val id: String,
    val title: String,
    val content: String,
    val type: String,
    val checklistJson: String,
    val colorHex: String,
    val tagsJson: String,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isTrashed: Boolean,
    val trashedAt: Long?,
    val reminderAt: Long?,
    val attachmentsJson: String,
    val audioPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val imageBase64List: List<String> = emptyList()
)

data class BackupDataDto(
    val appVersion: String = "1.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<BackupNoteDto>,
    val tags: List<TagEntity>
)

object BackupManager {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupDataDto::class.java)

    fun exportBackupJson(context: Context, notes: List<NoteEntity>, tags: List<TagEntity>): String {
        val backupNotes = notes.map { note ->
            val attachmentPaths = com.example.data.local.Converters.jsonToStringList(note.attachmentsJson)
            val base64List = attachmentPaths.mapNotNull { path ->
                runCatching {
                    val file = File(path)
                    if (file.exists()) {
                        Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                    } else null
                }.getOrNull()
            }
            BackupNoteDto(
                id = note.id,
                title = note.title,
                content = note.content,
                type = note.type,
                checklistJson = note.checklistJson,
                colorHex = note.colorHex,
                tagsJson = note.tagsJson,
                isPinned = note.isPinned,
                isArchived = note.isArchived,
                isTrashed = note.isTrashed,
                trashedAt = note.trashedAt,
                reminderAt = note.reminderAt,
                attachmentsJson = note.attachmentsJson,
                audioPath = note.audioPath,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                imageBase64List = base64List
            )
        }
        val backupData = BackupDataDto(notes = backupNotes, tags = tags)
        return adapter.toJson(backupData)
    }

    fun parseBackupJson(context: Context, jsonString: String): Pair<List<NoteEntity>, List<TagEntity>>? {
        val backupData = runCatching { adapter.fromJson(jsonString) }.getOrNull() ?: return null
        val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }

        val restoredNotes = backupData.notes.map { dto ->
            val restoredPaths = mutableListOf<String>()
            dto.imageBase64List.forEachIndexed { index, base64Str ->
                runCatching {
                    val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
                    val imgFile = File(imagesDir, "restored_${dto.id}_$index.jpg")
                    imgFile.writeBytes(bytes)
                    restoredPaths.add(imgFile.absolutePath)
                }
            }
            val attachmentsJson = if (restoredPaths.isNotEmpty()) {
                com.example.data.local.Converters.stringListToJson(restoredPaths)
            } else dto.attachmentsJson

            NoteEntity(
                id = dto.id,
                title = dto.title,
                content = dto.content,
                type = dto.type,
                checklistJson = dto.checklistJson,
                colorHex = dto.colorHex,
                tagsJson = dto.tagsJson,
                isPinned = dto.isPinned,
                isArchived = dto.isArchived,
                isTrashed = dto.isTrashed,
                trashedAt = dto.trashedAt,
                reminderAt = dto.reminderAt,
                attachmentsJson = attachmentsJson,
                audioPath = dto.audioPath,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
        return Pair(restoredNotes, backupData.tags)
    }
}
