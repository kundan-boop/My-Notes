package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.ChecklistItem
import com.example.data.local.Converters
import com.example.data.local.NoteEntity
import com.example.ui.components.ColorPicker
import com.example.ui.components.NativeRichTextEditor
import com.example.ui.components.NativeRichTextEditorState
import com.example.ui.components.RichTextToolbar
import com.example.ui.components.TagPickerDialog
import com.example.ui.components.VoiceNotePlayerBar
import com.example.ui.components.rememberNativeRichTextEditorState
import com.example.ui.theme.NoteColors
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.ActiveFormats
import com.example.util.FontSizePreset
import com.example.util.ImageUtils
import com.example.util.RichTextHelper
import com.example.util.RichTextRenderer
import com.example.util.SpeechToTextManager
import com.example.util.TextAlignmentPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditScreen(
    noteId: String,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = isSystemInDarkTheme()

    val allTags by viewModel.allTags.collectAsState()

    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var title by remember { mutableStateOf("") }
    val richEditorState = rememberNativeRichTextEditorState("")
    var type by remember { mutableStateOf("text") } // "text", "checklist", "voice"
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var colorHex by remember { mutableStateOf("default") }
    var tagNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isProtected by remember { mutableStateOf(false) }
    var protectedPassword by remember { mutableStateOf<String?>(null) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var attachmentPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var audioPath by remember { mutableStateOf<String?>(null) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showProtectDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var isListeningSpeech by remember { mutableStateOf(false) }
    var speechToTextManager by remember { mutableStateOf<SpeechToTextManager?>(null) }

    // Initialize SpeechToTextManager
    DisposableEffect(Unit) {
        val manager = SpeechToTextManager(
            context = context,
            onResult = { recognizedText ->
                val separator = " "
                richEditorState.insertText(separator + recognizedText)
            },
            onError = { error ->
                Toast.makeText(context, "Voice dictation: $error", Toast.LENGTH_SHORT).show()
                isListeningSpeech = false
            },
            onListeningStateChange = { listening ->
                isListeningSpeech = listening
            }
        )
        speechToTextManager = manager
        onDispose {
            manager.destroy()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechToTextManager?.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for speech dictation", Toast.LENGTH_SHORT).show()
        }
    }

    fun startDictation() {
        if (isListeningSpeech) {
            speechToTextManager?.stopListening()
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                speechToTextManager?.startListening()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Load Note
    LaunchedEffect(noteId) {
        if (noteId == "new" || noteId.startsWith("new_")) {
            val defaultType = if (noteId.contains("checklist")) "checklist" else if (noteId.contains("voice")) "voice" else "text"
            val newNote = NoteEntity(
                id = UUID.randomUUID().toString(),
                type = defaultType
            )
            note = newNote
            type = defaultType
            if (defaultType == "voice") {
                val voiceFile = viewModel.audioRecorder.startRecording()
                if (voiceFile != null) {
                    isRecordingAudio = true
                }
            }
        } else {
            val existing = viewModel.repository.getNoteByIdSync(noteId)
            if (existing != null) {
                note = existing
                title = existing.title
                richEditorState.setHtmlContent(existing.content)
                type = existing.type
                checklistItems = Converters.jsonToChecklist(existing.checklistJson)
                colorHex = existing.colorHex
                tagNames = Converters.jsonToStringList(existing.tagsJson)
                isPinned = existing.isPinned
                isArchived = existing.isArchived
                isProtected = existing.isProtected
                protectedPassword = existing.protectedPassword
                reminderAt = existing.reminderAt
                attachmentPaths = Converters.jsonToStringList(existing.attachmentsJson)
                audioPath = existing.audioPath
            }
        }
    }

    // Function to check if the note is completely blank
    fun isNoteBlank(): Boolean {
        val plainText = RichTextRenderer.stripHtml(richEditorState.html)
        return title.isBlank() &&
                plainText.isBlank() &&
                checklistItems.isEmpty() &&
                attachmentPaths.isEmpty() &&
                audioPath.isNullOrBlank()
    }

    // Safe exit handler that prevents saving blank notes (Requirement 8)
    fun handleSafeExit() {
        val currentNote = note
        if (currentNote != null) {
            if (isNoteBlank()) {
                scope.launch {
                    viewModel.repository.deletePermanently(currentNote.id)
                    onBack()
                }
                return
            }
        }
        onBack()
    }

    // Debounced Autosave on changes (only if note has content)
    LaunchedEffect(
        title, richEditorState.html, checklistItems, colorHex, tagNames,
        isPinned, isArchived, isProtected, protectedPassword, reminderAt,
        attachmentPaths, audioPath, type
    ) {
        val currentNote = note ?: return@LaunchedEffect
        delay(400) // 400ms debounce
        if (isNoteBlank()) {
            // Do not persist empty blank drafts
            return@LaunchedEffect
        }
        val updated = currentNote.copy(
            title = title,
            content = richEditorState.html,
            type = type,
            checklistJson = Converters.checklistToJson(checklistItems),
            colorHex = colorHex,
            tagsJson = Converters.stringListToJson(tagNames),
            isPinned = isPinned,
            isArchived = isArchived,
            isProtected = isProtected,
            protectedPassword = protectedPassword,
            reminderAt = reminderAt,
            attachmentsJson = Converters.stringListToJson(attachmentPaths),
            audioPath = audioPath,
            updatedAt = System.currentTimeMillis()
        )
        note = updated
        viewModel.repository.saveNote(updated)
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageUtils.saveAndCompressImage(context, uri)
            if (savedPath != null) {
                attachmentPaths = attachmentPaths + savedPath
            }
        }
    }

    val preset = NoteColors.getPreset(colorHex)
    val pageBg = if (preset.id == "default") {
        MaterialTheme.colorScheme.background
    } else {
        if (darkTheme) preset.darkBg else preset.lightBg
    }

    val textColor = if (preset.id == "default") {
        MaterialTheme.colorScheme.onBackground
    } else {
        if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }

    Scaffold(
        containerColor = pageBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {},
                navigationIcon = {
                    IconButton(onClick = { handleSafeExit() }, modifier = Modifier.testTag("note_edit_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    // Lock / Protect Note toggle
                    IconButton(
                        onClick = { showProtectDialog = true },
                        modifier = Modifier.testTag("note_protect_button")
                    ) {
                        Icon(
                            imageVector = if (isProtected) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = if (isProtected) "Protected Note" else "Protect Note",
                            tint = if (isProtected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.Palette, contentDescription = "Pick Color", tint = textColor.copy(alpha = 0.7f))
                    }

                    IconButton(onClick = { showTagPicker = true }) {
                        Icon(Icons.Default.Tag, contentDescription = "Manage Tags", tint = textColor.copy(alpha = 0.7f))
                    }

                    IconButton(onClick = { isArchived = !isArchived }) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = if (isArchived) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("note_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Editor Scroll Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Color Picker Row Expandable
                if (showColorPicker) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            ColorPicker(
                                selectedColorId = colorHex,
                                onColorSelected = {
                                    colorHex = it
                                    showColorPicker = false
                                }
                            )
                        }
                    }
                }

                // Tag Chips Row
                if (tagNames.isNotEmpty() || reminderAt != null || isProtected) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            if (isProtected) {
                                FilterChip(
                                    selected = true,
                                    onClick = { showProtectDialog = true },
                                    label = { Text("Protected") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            if (reminderAt != null) {
                                val sdf = SimpleDateFormat("EEE, MMM dd HH:mm", Locale.getDefault())
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val curNote = note
                                        if (curNote != null) {
                                            viewModel.setReminder(context, curNote, null)
                                            reminderAt = null
                                        }
                                    },
                                    label = { Text(sdf.format(Date(reminderAt!!))) },
                                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove reminder") }
                                )
                            }

                            tagNames.forEach { tag ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        tagNames = tagNames - tag
                                    },
                                    label = { Text("#$tag") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove tag") }
                                )
                            }
                        }
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.4f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Rich Text Toolbar placed directly above note body (Requirement 1)
                if (type != "checklist") {
                    item {
                        RichTextToolbar(
                            isChecklistMode = (type == "checklist"),
                            activeFormats = richEditorState.activeFormats,
                            canUndo = richEditorState.canUndo,
                            canRedo = richEditorState.canRedo,
                            onUndo = { richEditorState.undo() },
                            onRedo = { richEditorState.redo() },
                            onToggleChecklistMode = { type = "checklist" },
                            onToggleBold = { richEditorState.toggleBold() },
                            onToggleItalic = { richEditorState.toggleItalic() },
                            onToggleUnderline = { richEditorState.toggleUnderline() },
                            onToggleStrikethrough = { richEditorState.toggleStrikethrough() },
                            onApplyFontSize = { preset -> richEditorState.setFontSize(preset) },
                            onApplyAlignment = { alignment -> richEditorState.setAlignment(alignment) },
                            onApplyHighlight = { hex -> richEditorState.setHighlight(hex) },
                            onApplyTextColor = { hex -> richEditorState.setTextColor(hex) },
                            onInsertBulletList = { richEditorState.toggleBulletList() },
                            onInsertNumberedList = { richEditorState.toggleNumberedList() },
                            onClearFormatting = { richEditorState.clearFormatting() },
                            onStartSpeechToText = { startDictation() },
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("rich_text_toolbar")
                        )
                    }
                }

                // Speech Dictation Active Banner
                if (isListeningSpeech) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Listening... Speak clearly to dictate note.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { speechToTextManager?.stopListening() }) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }

                // Attachments Carousel
                if (attachmentPaths.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachmentPaths) { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    Box(modifier = Modifier.size(100.dp, 100.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(file)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Attachment",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                        )

                                        IconButton(
                                            onClick = {
                                                attachmentPaths = attachmentPaths - path
                                                ImageUtils.deleteImageFile(path)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove image",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Recording Bar OR Voice Note Player Bar
                if (isRecordingAudio) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Recording audio...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val path = viewModel.audioRecorder.stopRecording()
                                        isRecordingAudio = false
                                        if (path != null) {
                                            audioPath = path
                                            type = "voice"
                                        }
                                    },
                                    modifier = Modifier.testTag("stop_recording_button")
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Stop Recording",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (!audioPath.isNullOrBlank()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        VoiceNotePlayerBar(
                            audioPath = audioPath!!,
                            audioPlayer = viewModel.audioPlayer,
                            onDeleteAudio = {
                                runCatching { File(audioPath!!).delete() }
                                audioPath = null
                                if (type == "voice") type = "text"
                            }
                        )
                    }
                }

                // Body Content OR Checklist Mode
                if (type == "checklist") {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Checklist Items",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor.copy(alpha = 0.7f)
                            )
                            TextButton(onClick = { type = "text" }) {
                                Text("Switch to Text")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    items(checklistItems, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    checklistItems = checklistItems.map {
                                        if (it.id == item.id) it.copy(isChecked = checked) else it
                                    }
                                }
                            )

                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { newTxt ->
                                    checklistItems = checklistItems.map {
                                        if (it.id == item.id) it.copy(text = newTxt) else it
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (item.isChecked) textColor.copy(alpha = 0.5f) else textColor
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )

                            IconButton(
                                onClick = {
                                    checklistItems = checklistItems.filter { it.id != item.id }
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Item", tint = textColor.copy(alpha = 0.6f))
                            }
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text("List item", color = textColor.copy(alpha = 0.5f)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_checklist_item_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                            if (newChecklistText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        checklistItems = checklistItems + ChecklistItem(
                                            id = UUID.randomUUID().toString(),
                                            text = newChecklistText.trim()
                                        )
                                        newChecklistText = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } else {
                    // Regular Note Body Native Rich Text Editor (100% Native Spannable Android Engine)
                    item {
                        Spacer(Modifier.height(4.dp))
                        NativeRichTextEditor(
                            state = richEditorState,
                            textColor = textColor,
                            placeholder = "Note content...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_content_input")
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // Bottom Actions Toolbar (Images, Voice, Reminder)
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag("add_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                type = if (type == "checklist") "text" else "checklist"
                            }
                        ) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = "Toggle Checklist",
                                tint = if (type == "checklist") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    val path = viewModel.audioRecorder.stopRecording()
                                    isRecordingAudio = false
                                    if (path != null) {
                                        audioPath = path
                                        type = "voice"
                                    }
                                } else {
                                    val file = viewModel.audioRecorder.startRecording()
                                    if (file != null) {
                                        isRecordingAudio = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("record_voice_button")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Record Voice",
                                tint = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                calendar.set(Calendar.MINUTE, minute)
                                                calendar.set(Calendar.SECOND, 0)
                                                val scheduledTime = calendar.timeInMillis
                                                reminderAt = scheduledTime
                                                val curNote = note
                                                if (curNote != null) {
                                                    viewModel.setReminder(context, curNote, scheduledTime)
                                                }
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.testTag("set_reminder_button")
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Set Reminder",
                                tint = if (reminderAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (isNoteBlank()) "Draft (unsaved)" else "Autosaved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Tag Picker Dialog
        if (showTagPicker) {
            TagPickerDialog(
                availableTags = allTags,
                selectedTagNames = tagNames,
                onTagToggled = { tag ->
                    tagNames = if (tagNames.contains(tag)) tagNames - tag else tagNames + tag
                },
                onCreateTag = { newTag ->
                    viewModel.createNewTag(newTag)
                    tagNames = tagNames + newTag
                },
                onDismiss = { showTagPicker = false }
            )
        }

        // Note Protection Dialog (Requirement 9)
        if (showProtectDialog) {
            var customPasswordInput by remember { mutableStateOf(protectedPassword ?: "") }
            var setCustomPassword by remember { mutableStateOf(!protectedPassword.isNullOrBlank()) }

            AlertDialog(
                onDismissRequest = { showProtectDialog = false },
                title = { Text(if (isProtected) "Note Protection Settings" else "Protect Note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Protect this note with password or biometrics so its contents stay private.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isProtected,
                                onCheckedChange = { isProtected = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Note Protection", fontWeight = FontWeight.SemiBold)
                        }

                        if (isProtected) {
                            OutlinedTextField(
                                value = customPasswordInput,
                                onValueChange = { customPasswordInput = it },
                                label = { Text("Specific Note Password (optional)") },
                                placeholder = { Text("Leave blank to use App Lock") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            protectedPassword = if (customPasswordInput.isNotBlank()) customPasswordInput else null
                            showProtectDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProtectDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Confirmation Dialog (Requirement 7)
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Move note to Trash?") },
                text = { Text("This note will be moved to the Trash. You can restore it anytime within 30 days.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            val noteToTrash = note
                            if (noteToTrash != null) {
                                scope.launch {
                                    viewModel.moveToTrash(noteToTrash.id)
                                    onBack()
                                }
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Text("Move to Trash", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

