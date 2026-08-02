package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.ChecklistItem
import com.example.data.local.Converters
import com.example.data.local.NoteEntity
import com.example.data.local.TagEntity
import com.example.data.repository.NotesRepository
import com.example.data.repository.SettingsRepository
import com.example.util.AudioPlayer
import com.example.util.AudioRecorder
import com.example.util.ImageUtils
import com.example.util.ReminderScheduler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = NotesRepository(db.noteDao(), db.tagDao())
    val settingsRepository = SettingsRepository(application)

    val audioRecorder = AudioRecorder(application)
    val audioPlayer = AudioPlayer(application)

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<String?>(null) // "text", "checklist", "voice"

    val viewMode: StateFlow<String> = settingsRepository.viewMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "grid"
    )

    val allTags: StateFlow<List<TagEntity>> = repository.allTags.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered Active Notes
    @OptIn(FlowPreview::class)
    val activeNotes: StateFlow<List<NoteEntity>> = combine(
        searchQuery.debounce(200),
        selectedTagFilter,
        selectedColorFilter,
        selectedTypeFilter
    ) { query, tagFilter, colorFilter, typeFilter ->
        QuadFilter(query, tagFilter, colorFilter, typeFilter)
    }.flatMapLatest { filter ->
        if (filter.query.isNotBlank()) {
            repository.searchNotes(filter.query)
        } else {
            repository.activeNotes
        }
    }.combine(
        combine(selectedTagFilter, selectedColorFilter, selectedTypeFilter) { t, c, ty -> Triple(t, c, ty) }
    ) { notes, filters ->
        notes.filter { note ->
            val matchesTag = filters.first == null || Converters.jsonToStringList(note.tagsJson).contains(filters.first)
            val matchesColor = filters.second == null || note.colorHex == filters.second
            val matchesType = filters.third == null || note.type == filters.third
            matchesTag && matchesColor && matchesType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trashedNotes: StateFlow<List<NoteEntity>> = repository.trashedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingReminders: StateFlow<List<NoteEntity>> = repository.upcomingReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Toast/Snackbar events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Voice recording status
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceRecordingDurationSec = MutableStateFlow(0)
    val voiceRecordingDurationSec: StateFlow<Int> = _voiceRecordingDurationSec.asStateFlow()

    init {
        // Auto purge old trashed notes on launch
        viewModelScope.launch {
            repository.purgeOldTrashedNotes()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedTagFilter(tag: String?) {
        selectedTagFilter.value = if (selectedTagFilter.value == tag) null else tag
    }

    fun setSelectedColorFilter(colorId: String?) {
        selectedColorFilter.value = if (selectedColorFilter.value == colorId) null else colorId
    }

    fun setSelectedTypeFilter(type: String?) {
        selectedTypeFilter.value = if (selectedTypeFilter.value == type) null else type
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val newMode = if (viewMode.value == "grid") "list" else "grid"
            settingsRepository.setViewMode(newMode)
        }
    }

    fun togglePin(noteId: String) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun toggleArchive(noteId: String) {
        viewModelScope.launch {
            repository.toggleArchive(noteId)
            _userMessage.emit("Note archive status toggled")
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
            _userMessage.emit("Note moved to Trash")
        }
    }

    fun restoreFromTrash(noteId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
            _userMessage.emit("Note restored from Trash")
        }
    }

    fun deletePermanently(noteId: String) {
        viewModelScope.launch {
            val note = repository.getNoteByIdSync(noteId)
            if (note != null) {
                // Delete attachments and voice files
                Converters.jsonToStringList(note.attachmentsJson).forEach { path ->
                    ImageUtils.deleteImageFile(path)
                }
                if (!note.audioPath.isNullOrBlank()) {
                    runCatching { java.io.File(note.audioPath).delete() }
                }
                repository.deletePermanently(noteId)
                _userMessage.emit("Note permanently deleted")
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _userMessage.emit("Trash emptied")
        }
    }

    fun createNewTag(name: String, colorHex: String = "#4F46E5") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertTag(TagEntity(name = name.trim(), colorHex = colorHex))
            _userMessage.emit("Tag '${name.trim()}' created")
        }
    }

    fun deleteTag(id: String) {
        viewModelScope.launch {
            repository.deleteTag(id)
        }
    }

    // Voice recording
    fun startVoiceRecording() {
        val file = audioRecorder.startRecording()
        if (file != null) {
            _isRecordingVoice.value = true
        }
    }

    fun stopVoiceRecordingAndCreateNote() {
        val path = audioRecorder.stopRecording()
        _isRecordingVoice.value = false
        if (path != null) {
            viewModelScope.launch {
                val newNote = NoteEntity(
                    title = "Voice Note ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                    type = "voice",
                    audioPath = path
                )
                repository.saveNote(newNote)
                _userMessage.emit("Voice note saved")
            }
        }
    }

    fun cancelVoiceRecording() {
        audioRecorder.cancelRecording()
        _isRecordingVoice.value = false
    }

    // Backup & Restore
    fun exportBackupJson(): String {
        var json = ""
        kotlinx.coroutines.runBlocking {
            val (notes, tags) = repository.getAllDataForBackup()
            json = BackupManager.exportBackupJson(getApplication(), notes, tags)
            settingsRepository.updateLastBackupTime(System.currentTimeMillis())
        }
        return json
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            val parsed = BackupManager.parseBackupJson(getApplication(), jsonString)
            if (parsed != null) {
                repository.restoreDataFromBackup(parsed.first, parsed.second)
                settingsRepository.updateLastBackupTime(System.currentTimeMillis())
                _userMessage.emit("Backup successfully restored! (${parsed.first.size} notes)")
            } else {
                _userMessage.emit("Failed to parse backup file")
            }
        }
    }

    fun setReminder(context: android.content.Context, note: NoteEntity, timeMs: Long?) {
        viewModelScope.launch {
            val updated = note.copy(reminderAt = timeMs)
            repository.saveNote(updated)
            if (timeMs != null && timeMs > System.currentTimeMillis()) {
                ReminderScheduler.scheduleReminder(context, note.id, note.title, note.content, timeMs)
                _userMessage.emit("Reminder set")
            } else {
                ReminderScheduler.cancelReminder(context, note.id)
                _userMessage.emit("Reminder removed")
            }
        }
    }
}

private data class QuadFilter<A, B, C, D>(val query: A, val tag: B, val color: C, val type: D)
