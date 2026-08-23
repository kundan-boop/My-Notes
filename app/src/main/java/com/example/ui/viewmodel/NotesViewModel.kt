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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val settingsRepository = SettingsRepository(application)
    val repository = NotesRepository(db.noteDao(), db.tagDao(), settingsRepository)

    val isDataDirty: StateFlow<Boolean> = settingsRepository.isDataDirty.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val driveFolderSelected: StateFlow<Boolean> = settingsRepository.driveFolderSelected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val lastBackupDateString: StateFlow<String> = settingsRepository.lastBackupDateString.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val audioRecorder = AudioRecorder(application)
    val audioPlayer = AudioPlayer(application)

    // Filters & Sorting
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<String?>(null) // "text", "checklist", "voice"
    val sortOrder = MutableStateFlow("modified") // "modified", "created", "alphabetical"

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

    val pinnedOrder: StateFlow<List<List<String>>> = settingsRepository.pinnedOrder.map { listOf(it) }.stateIn(
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
        selectedTypeFilter,
        sortOrder
    ) { query, tagFilter, colorFilter, typeFilter, sort ->
        PentaFilter(query, tagFilter, colorFilter, typeFilter, sort)
    }.flatMapLatest { filter ->
        if (filter.query.isNotBlank()) {
            repository.searchNotes(filter.query)
        } else {
            repository.activeNotes
        }
    }.combine(
        combine(selectedTagFilter, selectedColorFilter, selectedTypeFilter, sortOrder, settingsRepository.pinnedOrder) { t, c, ty, s, po ->
            ActiveNoteFilters(t, c, ty, s, po)
        }
    ) { notes, filters ->
        val filtered = notes.filter { note ->
            val matchesTag = filters.tag == null || Converters.jsonToStringList(note.tagsJson).contains(filters.tag)
            val matchesColor = filters.color == null || note.colorHex == filters.color
            val matchesType = filters.type == null || note.type == filters.type
            matchesTag && matchesColor && matchesType
        }

        val pinnedIds = filters.pinnedOrder

        val pinned = filtered.filter { it.isPinned }.sortedWith { a, b ->
            val idxA = pinnedIds.indexOf(a.id)
            val idxB = pinnedIds.indexOf(b.id)
            val posA = if (idxA != -1) idxA else Int.MAX_VALUE
            val posB = if (idxB != -1) idxB else Int.MAX_VALUE
            if (posA != posB) posA.compareTo(posB) else b.updatedAt.compareTo(a.updatedAt)
        }

        val others = filtered.filter { !it.isPinned }.let { unpinnedList ->
            when (filters.sort) {
                "created" -> unpinnedList.sortedByDescending { it.createdAt }
                "alphabetical" -> unpinnedList.sortedBy { it.title.lowercase() }
                else -> unpinnedList.sortedByDescending { it.updatedAt }
            }
        }

        pinned + others
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

    // Protected Notes session unlocked IDs
    val unlockedNoteIds = MutableStateFlow<Set<String>>(emptySet())

    fun unlockProtectedNote(noteId: String) {
        unlockedNoteIds.value = unlockedNoteIds.value + noteId
    }

    fun unlockNote(noteId: String) {
        unlockedNoteIds.value = unlockedNoteIds.value + noteId
    }

    fun isNoteUnlocked(noteId: String): Boolean {
        return unlockedNoteIds.value.contains(noteId)
    }

    // Voice recording status
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceRecordingDurationSec = MutableStateFlow(0)
    val voiceRecordingDurationSec: StateFlow<Int> = _voiceRecordingDurationSec.asStateFlow()

    init {
        // Auto purge old trashed notes on launch
        viewModelScope.launch {
            repository.purgeOldTrashedNotes()
            checkAndRunAutoDailyBackup()
        }
    }

    fun checkAndRunAutoDailyBackup() {
        viewModelScope.launch {
            val autoEnabled = settingsRepository.autoBackupEnabled.stateIn(viewModelScope).value
            if (!autoEnabled) return@launch

            val lastDateStr = settingsRepository.lastBackupDateString.stateIn(viewModelScope).value
            val todayDateStr = BackupManager.getTodayDateString()
            val isDataDirty = settingsRepository.isDataDirty.stateIn(viewModelScope).value || SettingsRepository.isDirtyInMemory

            // If a new calendar day has started or data is dirty, perform rolling backup
            if (lastDateStr != todayDateStr || isDataDirty) {
                performRollingBackupInternal()
            }
        }
    }

    suspend fun performRollingBackupInternal(): Int {
        val (notes, tags) = repository.getAllDataForBackup()
        val lastDateStr = settingsRepository.lastBackupDateString.stateIn(viewModelScope).value
        val lastSlot = settingsRepository.lastRotatingSlot.stateIn(viewModelScope).value

        val (slot, todayStr, path) = BackupManager.performRollingBackup(
            context = getApplication(),
            notes = notes,
            tags = tags,
            lastBackupDateStr = lastDateStr,
            lastSlot = lastSlot
        )
        val now = System.currentTimeMillis()
        settingsRepository.setLastRotatingSlot(slot)
        settingsRepository.setLastBackupDateString(todayStr)
        settingsRepository.updateLastBackupTime(now)
        settingsRepository.setDataDirty(false)
        SettingsRepository.isDirtyInMemory = false
        return slot
    }

    fun performAutoBackupIfDirty() {
        if (SettingsRepository.isDirtyInMemory || isDataDirty.value) {
            viewModelScope.launch {
                performRollingBackupInternal()
            }
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
            val note = activeNotes.value.find { it.id == noteId }
            val currentOrder = settingsRepository.pinnedOrder.first().toMutableList()
            if (note != null && !note.isPinned) {
                // Pinning: Append to end (right side) of pinned list
                if (!currentOrder.contains(noteId)) {
                    currentOrder.add(noteId)
                    settingsRepository.setPinnedOrder(currentOrder)
                }
            } else if (note != null && note.isPinned) {
                // Unpinning: Remove from pinned list
                currentOrder.remove(noteId)
                settingsRepository.setPinnedOrder(currentOrder)
            }
            repository.togglePin(noteId)
        }
    }

    fun movePinnedNote(noteId: String, delta: Int) {
        viewModelScope.launch {
            val pinnedList = activeNotes.value.filter { it.isPinned }.map { it.id }.toMutableList()
            val index = pinnedList.indexOf(noteId)
            if (index != -1) {
                val newIndex = index + delta
                if (newIndex in 0 until pinnedList.size) {
                    val item = pinnedList.removeAt(index)
                    pinnedList.add(newIndex, item)
                    settingsRepository.setPinnedOrder(pinnedList)
                }
            }
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
            ReminderScheduler.cancelReminder(getApplication(), noteId)
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
                ReminderScheduler.cancelReminder(getApplication(), noteId)
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

    fun runDailyRotatingBackup(targetSlot: Int? = null) {
        viewModelScope.launch {
            if (targetSlot != null) {
                val (notes, tags) = repository.getAllDataForBackup()
                val (slot, path) = BackupManager.writeRotatingBackupSlot(getApplication(), notes, tags, targetSlot)
                settingsRepository.setLastRotatingSlot(slot)
                val now = System.currentTimeMillis()
                settingsRepository.updateLastBackupTime(now)
                settingsRepository.setLastBackupDateString(BackupManager.getTodayDateString())
                settingsRepository.setDataDirty(false)
                SettingsRepository.isDirtyInMemory = false
                _userMessage.emit("Backup saved to Day-$slot.json in Google Drive folder")
            } else {
                val slot = performRollingBackupInternal()
                _userMessage.emit("Backup saved to Day-$slot.json in Google Drive folder")
            }
        }
    }

    fun restoreFromRotatingSlot(slot: Int) {
        viewModelScope.launch {
            val json = BackupManager.readRotatingBackupSlot(getApplication(), slot)
            if (!json.isNullOrBlank()) {
                val parsed = BackupManager.parseBackupJson(getApplication(), json)
                if (parsed != null) {
                    repository.restoreDataFromBackup(parsed.first, parsed.second)
                    _userMessage.emit("Restored backup from Day-$slot.json (${parsed.first.size} notes)")
                } else {
                    _userMessage.emit("Failed to parse Day-$slot.json")
                }
            } else {
                _userMessage.emit("No backup found in Day-$slot.json")
            }
        }
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

    fun setSortOrder(order: String) {
        sortOrder.value = order
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

private data class ActiveNoteFilters(
    val tag: String?,
    val color: String?,
    val type: String?,
    val sort: String,
    val pinnedOrder: List<String>
)
private data class PentaFilter<A, B, C, D, E>(val query: A, val tag: B, val color: C, val type: D, val sort: E)
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class QuadFilter<A, B, C, D>(val query: A, val tag: B, val color: C, val type: D)
