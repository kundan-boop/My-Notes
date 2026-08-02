package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.ChecklistItem
import com.example.data.local.Converters
import com.example.data.local.NoteEntity
import com.example.ui.components.ColorPicker
import com.example.ui.components.RichTextToolbar
import com.example.ui.components.TagPickerDialog
import com.example.ui.components.VoiceNotePlayerBar
import com.example.ui.theme.NoteColors
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.ImageUtils
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
    var content by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("text") } // "text", "checklist", "voice"
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var colorHex by remember { mutableStateOf("default") }
    var tagNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var attachmentPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var audioPath by remember { mutableStateOf<String?>(null) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }

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
                    isRecording = true
                }
            }
        } else {
            val existing = viewModel.repository.getNoteByIdSync(noteId)
            if (existing != null) {
                note = existing
                title = existing.title
                content = existing.content
                type = existing.type
                checklistItems = Converters.jsonToChecklist(existing.checklistJson)
                colorHex = existing.colorHex
                tagNames = Converters.jsonToStringList(existing.tagsJson)
                isPinned = existing.isPinned
                isArchived = existing.isArchived
                reminderAt = existing.reminderAt
                attachmentPaths = Converters.jsonToStringList(existing.attachmentsJson)
                audioPath = existing.audioPath
            }
        }
    }

    // Debounced Autosave on changes
    LaunchedEffect(title, content, checklistItems, colorHex, tagNames, isPinned, isArchived, reminderAt, attachmentPaths, audioPath, type) {
        val currentNote = note ?: return@LaunchedEffect
        delay(400) // 400ms debounce
        val updated = currentNote.copy(
            title = title,
            content = content,
            type = type,
            checklistJson = Converters.checklistToJson(checklistItems),
            colorHex = colorHex,
            tagsJson = Converters.stringListToJson(tagNames),
            isPinned = isPinned,
            isArchived = isArchived,
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("note_edit_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
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
                        onClick = {
                            val noteToTrash = note
                            if (noteToTrash != null) {
                                scope.launch {
                                    viewModel.moveToTrash(noteToTrash.id)
                                    onBack()
                                }
                            }
                        }
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
                    .padding(horizontal = 20.dp)
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
                if (tagNames.isNotEmpty() || reminderAt != null) {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
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
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.4f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
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

                // Attachments Carousel
                if (attachmentPaths.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachmentPaths) { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    Box(modifier = Modifier.size(120.dp, 120.dp)) {
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
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove image",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Recording Bar OR Voice Note Player Bar
                if (isRecording) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Recording voice note...",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val path = viewModel.audioRecorder.stopRecording()
                                        isRecording = false
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
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (!audioPath.isNullOrBlank()) {
                    item {
                        Spacer(Modifier.height(12.dp))
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
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Checklist Items",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    items(checklistItems, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
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
                                .padding(top = 8.dp)
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
                    // Regular Note Body Text
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = {
                                Text(
                                    "Note content...",
                                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.4f))
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_content_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // Bottom Actions Toolbar (Images, Voice, Reminder, Format)
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.testTag("add_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                if (type == "checklist") {
                                    type = "text"
                                } else {
                                    type = "checklist"
                                }
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
                                if (isRecording) {
                                    val path = viewModel.audioRecorder.stopRecording()
                                    isRecording = false
                                    if (path != null) {
                                        audioPath = path
                                        type = "voice"
                                    }
                                } else {
                                    val file = viewModel.audioRecorder.startRecording()
                                    if (file != null) {
                                        isRecording = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("record_voice_button")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Record Voice",
                                tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
                        text = "Autosaved",
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
    }
}
