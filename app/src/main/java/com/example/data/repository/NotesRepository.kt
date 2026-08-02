package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.TagDao
import com.example.data.local.TagEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class NotesRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val upcomingReminders: Flow<List<NoteEntity>> = noteDao.getUpcomingReminders()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdSync(id: String): NoteEntity? = noteDao.getNoteByIdSync(id)

    suspend fun saveNote(note: NoteEntity) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.insertNote(updatedNote)
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
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun purgeOldTrashedNotes() {
        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        noteDao.purgeOldTrashedNotes(thirtyDaysAgo)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    suspend fun insertTag(tag: TagEntity) {
        tagDao.insertTag(tag)
    }

    suspend fun deleteTag(id: String) {
        tagDao.deleteTagById(id)
    }

    suspend fun getAllDataForBackup(): Pair<List<NoteEntity>, List<TagEntity>> {
        val notes = noteDao.getAllNotesForBackup()
        val tags = tagDao.getAllTagsSync()
        return Pair(notes, tags)
    }

    suspend fun restoreDataFromBackup(notes: List<NoteEntity>, tags: List<TagEntity>) {
        noteDao.insertAllNotes(notes)
        tagDao.insertAllTags(tags)
    }
}
