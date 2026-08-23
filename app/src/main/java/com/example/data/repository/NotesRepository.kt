package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.TagDao
import com.example.data.local.TagEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class NotesRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val settingsRepository: SettingsRepository? = null
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val upcomingReminders: Flow<List<NoteEntity>> = noteDao.getUpcomingReminders()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    private suspend fun notifyDataDirty() {
        SettingsRepository.isDirtyInMemory = true
        settingsRepository?.setDataDirty(true)
    }

    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdSync(id: String): NoteEntity? = noteDao.getNoteByIdSync(id)

    /**
     * Creates or inserts a note with current timestamp
     */
    suspend fun createNote(note: NoteEntity) {
        val now = System.currentTimeMillis()
        val newNote = note.copy(
            createdAt = if (note.createdAt == 0L) now else note.createdAt,
            updatedAt = now
        )
        noteDao.insertNote(newNote)
        notifyDataDirty()
    }

    /**
     * Updates an existing note
     */
    suspend fun updateNote(note: NoteEntity) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
        notifyDataDirty()
    }

    /**
     * General save method (insert or replace)
     */
    suspend fun saveNote(note: NoteEntity) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.insertNote(updatedNote)
        notifyDataDirty()
    }

    suspend fun setNoteColor(id: String, colorHex: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(colorHex = colorHex))
    }

    suspend fun setNoteProtected(id: String, isProtected: Boolean, password: String?) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(isProtected = isProtected, protectedPassword = password))
    }

    suspend fun setNoteReminder(id: String, reminderAt: Long?) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(reminderAt = reminderAt))
    }

    suspend fun togglePin(id: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(isPinned = !note.isPinned))
    }

    suspend fun toggleArchive(id: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(isArchived = !note.isArchived, isPinned = false))
    }

    suspend fun moveToTrash(id: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(isTrashed = true, trashedAt = System.currentTimeMillis(), isPinned = false))
    }

    suspend fun restoreFromTrash(id: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        saveNote(note.copy(isTrashed = false, trashedAt = null))
    }

    suspend fun deletePermanently(id: String) {
        val note = noteDao.getNoteByIdSync(id) ?: return
        noteDao.deleteNote(note)
        notifyDataDirty()
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
        notifyDataDirty()
    }

    suspend fun purgeOldTrashedNotes() {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        noteDao.purgeOldTrashedNotes(thirtyDaysAgo)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun createTag(name: String, colorHex: String = "#4F46E5"): TagEntity {
        val tag = TagEntity(id = UUID.randomUUID().toString(), name = name.trim(), colorHex = colorHex)
        tagDao.insertTag(tag)
        notifyDataDirty()
        return tag
    }

    suspend fun insertTag(tag: TagEntity) {
        tagDao.insertTag(tag)
        notifyDataDirty()
    }

    suspend fun deleteTag(id: String) {
        tagDao.deleteTagById(id)
        notifyDataDirty()
    }

    suspend fun getAllDataForBackup(): Pair<List<NoteEntity>, List<TagEntity>> {
        val notes = noteDao.getAllNotesForBackup()
        val tags = tagDao.getAllTagsSync()
        return Pair(notes, tags)
    }

    suspend fun restoreDataFromBackup(notes: List<NoteEntity>, tags: List<TagEntity>) {
        noteDao.insertAllNotes(notes)
        tagDao.insertAllTags(tags)
        notifyDataDirty()
    }
}
