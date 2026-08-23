package com.example.data.backup

import android.content.Context
import android.util.Base64
import com.example.data.local.NoteEntity
import com.example.data.local.TagEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isProtected: Boolean = false,
    val protectedPassword: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val imageBase64List: List<String> = emptyList()
)

data class BackupDataDto(
    val appVersion: String = "2.0",
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<BackupNoteDto>,
    val tags: List<TagEntity>
)

data class BackupSlotInfo(
    val slotNumber: Int, // 1..7
    val fileName: String, // "Day-1.json"
    val dayName: String,
    val exists: Boolean,
    val lastModified: Long,
    val sizeBytes: Long,
    val isLatest: Boolean = false
)

object BackupManager {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupDataDto::class.java)

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getSlotDayName(slot: Int): String {
        return "Slot #$slot"
    }

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
                isProtected = note.isProtected,
                protectedPassword = note.protectedPassword,
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
                isProtected = dto.isProtected,
                protectedPassword = dto.protectedPassword,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt
            )
        }
        return Pair(restoredNotes, backupData.tags)
    }

    // 7-day rolling Google Drive / Local Cloud sync directory
    fun getRotatingBackupDir(context: Context): File {
        val dir = File(context.filesDir, "google_drive_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBackupSlotsInfo(context: Context, latestSlot: Int = 0): List<BackupSlotInfo> {
        val dir = getRotatingBackupDir(context)
        return (1..7).map { slot ->
            val file = File(dir, "Day-$slot.json")
            val exists = file.exists()
            val lastModified = if (exists) file.lastModified() else 0L
            val dateLabel = if (exists) {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                " (${sdf.format(Date(lastModified))})"
            } else ""
            BackupSlotInfo(
                slotNumber = slot,
                fileName = "Day-$slot.json",
                dayName = "Slot #$slot$dateLabel",
                exists = exists,
                lastModified = lastModified,
                sizeBytes = if (exists) file.length() else 0L,
                isLatest = (slot == latestSlot && exists)
            )
        }
    }

    /**
     * Executes 7-day rolling backup:
     * - If called multiple times on the same calendar day, overwrites the same slot
     * - On a new calendar day, advances slot (1 -> 2 -> ... -> 7 -> 1) maintaining at most 7 files
     */
    fun performRollingBackup(
        context: Context,
        notes: List<NoteEntity>,
        tags: List<TagEntity>,
        lastBackupDateStr: String,
        lastSlot: Int
    ): Triple<Int, String, String> {
        val todayStr = getTodayDateString()
        val targetSlot = if (lastBackupDateStr == todayStr && lastSlot in 1..7) {
            // Same day: Overwrite existing day slot
            lastSlot
        } else {
            // New calendar day: Advance slot (1..7 rolling window)
            if (lastSlot in 1..6) lastSlot + 1 else 1
        }

        val dir = getRotatingBackupDir(context)
        val file = File(dir, "Day-$targetSlot.json")
        val json = exportBackupJson(context, notes, tags)
        file.writeText(json)

        return Triple(targetSlot, todayStr, file.absolutePath)
    }

    fun writeRotatingBackupSlot(
        context: Context,
        notes: List<NoteEntity>,
        tags: List<TagEntity>,
        targetSlot: Int
    ): Pair<Int, String> {
        val slot = if (targetSlot in 1..7) targetSlot else 1
        val dir = getRotatingBackupDir(context)
        val file = File(dir, "Day-$slot.json")
        val json = exportBackupJson(context, notes, tags)
        file.writeText(json)
        return Pair(slot, file.absolutePath)
    }

    fun readRotatingBackupSlot(context: Context, slot: Int): String? {
        val dir = getRotatingBackupDir(context)
        val file = File(dir, "Day-$slot.json")
        return if (file.exists()) file.readText() else null
    }
}
