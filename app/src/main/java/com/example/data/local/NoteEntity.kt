package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val type: String = "text", // "text", "checklist", "voice"
    val checklistJson: String = "[]",
    val colorHex: String = "",
    val tagsJson: String = "[]",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val reminderAt: Long? = null,
    val attachmentsJson: String = "[]",
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
