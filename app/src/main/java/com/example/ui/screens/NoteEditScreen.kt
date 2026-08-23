package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.util.ShareNoteHelper
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
    var showChecklistSection by remember { mutableStateOf(false) }
    var selectedImageForDialog by remember { mutableStateOf<String?>(null) }
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun promptReminderPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
                            val updatedForReminder = curNote.copy(
                                title = title,
                                content = richEditorState.html,
                                type = type,
                                checklistJson = Converters.checklistToJson(checklistItems),
                                colorHex = colorHex,
                                tagsJson = Converters.stringListToJson(tagNames),
                                reminderAt = scheduledTime
                            )
                            viewModel.setReminder(context, updatedForReminder, scheduledTime)
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
            if (defaultType == "checklist") {
                showChecklistSection = true
            }
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
                val items = Converters.jsonToChecklist(existing.checklistJson)
                checklistItems = items
                if (items.isNotEmpty() || existing.type == "checklist") {
                    showChecklistSection = true
                }
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
                    // Undo Button
                    IconButton(
                        onClick = { richEditorState.undo() },
                        enabled = richEditorState.canUndo,
                        modifier = Modifier
                            .testTag("note_undo_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (richEditorState.canUndo) textColor else textColor.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = { richEditorState.redo() },
                        enabled = richEditorState.canRedo,
                        modifier = Modifier
                            .testTag("note_redo_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (richEditorState.canRedo) textColor else textColor.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lock / Protect Note toggle
                    IconButton(
                        onClick = { showProtectDialog = true },
                        modifier = Modifier
                            .testTag("note_protect_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isProtected) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = if (isProtected) "Protected Note" else "Protect Note",
                            tint = if (isProtected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pin toggle
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Overflow Menu for Color, Tags, Archive, Delete
                    var showTopMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Pick Color") },
                                onClick = {
                                    showTopMenu = false
                                    showColorPicker = !showColorPicker
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Manage Tags") },
                                onClick = {
                                    showTopMenu = false
                                    showTagPicker = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Insert Image") },
                                onClick = {
                                    showTopMenu = false
                                    imagePickerLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Set Reminder") },
                                onClick = {
                                    showTopMenu = false
                                    promptReminderPicker()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text("Share Note") },
                                onClick = {
                                    showTopMenu = false
                                    val currentNote = note ?: return@DropdownMenuItem
                                    val updatedForShare = currentNote.copy(
                                        title = title,
                                        content = richEditorState.html,
                                        type = type,
                                        checklistJson = Converters.checklistToJson(checklistItems),
                                        colorHex = colorHex,
                                        tagsJson = Converters.stringListToJson(tagNames),
                                        attachmentsJson = Converters.stringListToJson(attachmentPaths),
                                        audioPath = audioPath
                                    )
                                    ShareNoteHelper.shareNote(context, updatedForShare)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                text = { Text(if (isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showTopMenu = false
                                    isArchived = !isArchived
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showTopMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 20.dp)
            ) {
                RichTextToolbar(
                    isChecklistMode = showChecklistSection,
                    activeFormats = richEditorState.activeFormats,
                    onInsertImage = { imagePickerLauncher.launch("image/*") },
                    onToggleChecklistMode = { showChecklistSection = !showChecklistSection },
                    onOpenReminder = { promptReminderPicker() },
                    onToggleBold = { richEditorState.toggleBold() },
                    onToggleItalic = { richEditorState.toggleItalic() },
                    onToggleUnderline = { richEditorState.toggleUnderline() },
                    onToggleStrikethrough = { richEditorState.toggleStrikethrough() },
                    onApplyFontSize = { preset -> richEditorState.setFontSize(preset) },
                    onApplyAlignment = { alignment -> richEditorState.setAlignment(alignment) },
                    onApplyHighlight = { hex -> richEditorState.setHighlight(hex) },
                    onApplyTextColor = { hex -> richEditorState.setTextColor(hex) },
                    onInsertBulletList = { marker -> richEditorState.toggleBulletList(marker) },
                    onInsertNumberedList = { prefix -> richEditorState.toggleNumberedList(prefix) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rich_text_toolbar")
                )
            }
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

                // Tag Chips & Metadata Row (Compact Horizontal Scrollbar)
                if (tagNames.isNotEmpty() || reminderAt != null || isProtected) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            if (isProtected) {
                                Surface(
                                    onClick = { showProtectDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Protected",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            if (reminderAt != null) {
                                val sdf = SimpleDateFormat("EEE, MMM dd HH:mm", Locale.getDefault())
                                Surface(
                                    onClick = {
                                        val curNote = note
                                        if (curNote != null) {
                                            viewModel.setReminder(context, curNote, null)
                                            reminderAt = null
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = sdf.format(Date(reminderAt!!)),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove reminder",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            tagNames.forEach { tag ->
                                Surface(
                                    onClick = {
                                        tagNames = tagNames - tag
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
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



                // Attachments Carousel (with single image share & action dialog)
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
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp, 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedImageForDialog = path
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(file)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Attachment. Tap for options or share.",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Delete Image Button (Top Right)
                                        IconButton(
                                            onClick = {
                                                attachmentPaths = attachmentPaths - path
                                                ImageUtils.deleteImageFile(path)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove image",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Quick Single Image Share Button (Bottom Right)
                                        IconButton(
                                            onClick = {
                                                ImageUtils.shareSingleImage(context, path)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Share this image",
                                                tint = MaterialTheme.colorScheme.primary,
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

                // Regular Note Body Native Rich Text Editor (Always available for rich paragraphs, styling, bullet lists)
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

                // Interactive Checklist Section (Coexists with rich text body seamlessly)
                if (showChecklistSection || checklistItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val completedCount = checklistItems.count { it.isChecked }
                                val totalCount = checklistItems.size
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckBox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (totalCount > 0) "Checklist ($completedCount/$totalCount)" else "Checklist",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = textColor
                                    )
                                }

                                if (checklistItems.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            checklistItems = checklistItems.filter { !it.isChecked }
                                        }
                                    ) {
                                        Text("Clear Checked", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
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
                                placeholder = { Text("Add checklist item...", color = textColor.copy(alpha = 0.5f)) },
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
                }

                item { Spacer(Modifier.height(24.dp)) }
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
        // Image Detail & Share Dialog (Requirement 5b)
        selectedImageForDialog?.let { imagePath ->
            val imgFile = File(imagePath)
            AlertDialog(
                onDismissRequest = { selectedImageForDialog = null },
                title = { Text("Image Attachment") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (imgFile.exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imgFile)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Image preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Text(
                            "Share this individual image to WhatsApp or any other app:",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ImageUtils.shareSingleImage(context, imagePath)
                            selectedImageForDialog = null
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Image")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedImageForDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

