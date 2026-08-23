package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import com.example.util.BiometricHelper
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import com.example.data.repository.SettingsRepository
import com.example.ui.theme.NoteColors
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.example.data.local.NoteEntity
import com.example.ui.components.ColorPicker
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String) -> Unit,
    onNewNoteClick: (type: String) -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val activeNotes by viewModel.activeNotes.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val unlockedNoteIds by viewModel.unlockedNoteIds.collectAsState()

    val settingsRepo = remember { SettingsRepository(context) }
    val masterNotesPassword by settingsRepo.protectedNotesPassword.collectAsState(initial = "1234")
    val appLockPin by settingsRepo.appLockPin.collectAsState(initial = "")
    val securityQuestion by settingsRepo.securityQuestion.collectAsState(initial = "What is your favorite book?")
    val securityAnswer by settingsRepo.securityAnswer.collectAsState(initial = "")

    var colorPickNoteId by remember { mutableStateOf<String?>(null) }
    var noteToUnlock by remember { mutableStateOf<NoteEntity?>(null) }
    var unlockPasswordInput by remember { mutableStateOf("") }
    var unlockErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSecurityRecovery by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryErrorMessage by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showColorFilterDialog by remember { mutableStateOf(false) }

    val pinnedNotes = remember(activeNotes) { activeNotes.filter { it.isPinned } }
    val otherNotes = remember(activeNotes) { activeNotes.filter { !it.isPinned } }

    fun handleNoteClick(note: NoteEntity) {
        if (note.isProtected && !unlockedNoteIds.contains(note.id)) {
            noteToUnlock = note
            unlockPasswordInput = ""
            unlockErrorMessage = null
        } else {
            onNoteClick(note.id)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "My Notes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("All Notes") },
                    selected = selectedTypeFilter == null && selectedTagFilter == null,
                    onClick = {
                        viewModel.setSelectedTypeFilter(null)
                        viewModel.setSelectedTagFilter(null)
                        viewModel.setSearchQuery("")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_all_notes")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Reminders") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToReminders()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_reminders")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Archive") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToArchive()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_archive")
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Trash") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTrash()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_trash")
                )

                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 28.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                ) {}
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                )

                allTags.forEach { tag ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        label = { Text("#${tag.name}") },
                        selected = selectedTagFilter == tag.name,
                        onClick = {
                            viewModel.setSelectedTagFilter(tag.name)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text("Backup & Restore") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToBackup()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_backup")
                )



                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_settings")
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Search bar row
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("menu_drawer_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                                }

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search your notes...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("search_notes_input"),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.toggleViewMode() },
                                    modifier = Modifier.testTag("toggle_view_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (viewMode == "grid") Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                        contentDescription = "Toggle View Mode"
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier.testTag("sort_menu_button")
                                    ) {
                                        Icon(Icons.Default.Sort, contentDescription = "Sort Notes")
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Date Modified") },
                                            onClick = {
                                                viewModel.setSortOrder("modified")
                                                showSortMenu = false
                                            },
                                            trailingIcon = if (sortOrder == "modified") { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Date Created") },
                                            onClick = {
                                                viewModel.setSortOrder("created")
                                                showSortMenu = false
                                            },
                                            trailingIcon = if (sortOrder == "created") { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Alphabetical") },
                                            onClick = {
                                                viewModel.setSortOrder("alphabetical")
                                                showSortMenu = false
                                            },
                                            trailingIcon = if (sortOrder == "alphabetical") { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                                        )
                                    }
                                }
                            }
                        }

                        // Filter chips row: All, Notes, Checklist, Tags, and dedicated Color Filter
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == null && selectedTagFilter == null && selectedColorFilter == null,
                                    onClick = {
                                        viewModel.setSelectedTypeFilter(null)
                                        viewModel.setSelectedTagFilter(null)
                                        viewModel.setSelectedColorFilter(null)
                                    },
                                    label = { Text("All") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == "text",
                                    onClick = { viewModel.setSelectedTypeFilter("text") },
                                    label = { Text("Notes") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == "checklist",
                                    onClick = { viewModel.setSelectedTypeFilter("checklist") },
                                    label = { Text("Checklist") },
                                    leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }

                            // Dedicated Color Filter Chip with Palette Picker
                            item {
                                if (selectedColorFilter == null) {
                                    FilterChip(
                                        selected = false,
                                        onClick = { showColorFilterDialog = true },
                                        label = { Text("Color") },
                                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.testTag("filter_chip_color_picker")
                                    )
                                } else {
                                    val activePreset = NoteColors.getPreset(selectedColorFilter!!)
                                    FilterChip(
                                        selected = true,
                                        onClick = { showColorFilterDialog = true },
                                        label = { Text(activePreset.name) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(activePreset.lightBg)
                                                    .border(1.dp, activePreset.lightBorder, CircleShape)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.setSelectedColorFilter(null) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear color filter", modifier = Modifier.size(14.dp))
                                            }
                                        },
                                        modifier = Modifier.testTag("filter_chip_color_active")
                                    )
                                }
                            }

                            allTags.forEach { tag ->
                                item {
                                    FilterChip(
                                        selected = selectedTagFilter == tag.name,
                                        onClick = { viewModel.setSelectedTagFilter(tag.name) },
                                        label = { Text("#${tag.name}") }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNewNoteClick("text") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_note")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Note")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Empty state
                    if (activeNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No notes yet" else "No notes found matching '$searchQuery'",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Tap + to create your first note, checklist, or voice memo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Notes Display List / Masonry Grid
                        if (viewMode == "grid") {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "PINNED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                    itemsIndexed(pinnedNotes, key = { _, note -> note.id }) { index, note ->
                                        NoteCard(
                                            note = note,
                                            isUnlocked = unlockedNoteIds.contains(note.id),
                                            onClick = { handleNoteClick(note) },
                                            onTogglePin = { viewModel.togglePin(note.id) },
                                            onToggleArchive = { viewModel.toggleArchive(note.id) },
                                            onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                            onColorPickRequest = { colorPickNoteId = note.id },
                                            onMovePinnedLeft = if (index > 0) { { viewModel.movePinnedNote(note.id, -1) } } else null,
                                            onMovePinnedRight = if (index < pinnedNotes.size - 1) { { viewModel.movePinnedNote(note.id, 1) } } else null
                                        )
                                    }
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                items(otherNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        isUnlocked = unlockedNoteIds.contains(note.id),
                                        onClick = { handleNoteClick(note) },
                                        onTogglePin = { viewModel.togglePin(note.id) },
                                        onToggleArchive = { viewModel.toggleArchive(note.id) },
                                        onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                        onColorPickRequest = { colorPickNoteId = note.id }
                                    )
                                }
                            }
                        } else {
                            // Single Column List
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "PINNED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                    itemsIndexed(pinnedNotes, key = { _, note -> note.id }) { index, note ->
                                        NoteCard(
                                            note = note,
                                            isUnlocked = unlockedNoteIds.contains(note.id),
                                            onClick = { handleNoteClick(note) },
                                            onTogglePin = { viewModel.togglePin(note.id) },
                                            onToggleArchive = { viewModel.toggleArchive(note.id) },
                                            onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                            onColorPickRequest = { colorPickNoteId = note.id },
                                            onMovePinnedLeft = if (index > 0) { { viewModel.movePinnedNote(note.id, -1) } } else null,
                                            onMovePinnedRight = if (index < pinnedNotes.size - 1) { { viewModel.movePinnedNote(note.id, 1) } } else null
                                        )
                                    }
                                    item {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                }

                                items(otherNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        isUnlocked = unlockedNoteIds.contains(note.id),
                                        onClick = { handleNoteClick(note) },
                                        onTogglePin = { viewModel.togglePin(note.id) },
                                        onToggleArchive = { viewModel.toggleArchive(note.id) },
                                        onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                        onColorPickRequest = { colorPickNoteId = note.id }
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Color Picker Dialog / Sheet
                if (colorPickNoteId != null) {
                    val note = activeNotes.find { it.id == colorPickNoteId }
                    if (note != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pick Color Label", style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { colorPickNoteId = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }
                                ColorPicker(
                                    selectedColorId = note.colorHex,
                                    onColorSelected = { newColor ->
                                        scope.launch {
                                            viewModel.repository.saveNote(note.copy(colorHex = newColor))
                                            colorPickNoteId = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Protected Note Unlock Password Dialog
    if (noteToUnlock != null) {
        val target = noteToUnlock!!
        val fragmentActivity = context as? androidx.fragment.app.FragmentActivity

        androidx.compose.runtime.LaunchedEffect(target.id) {
            if (fragmentActivity != null && BiometricHelper.isBiometricAvailable(context)) {
                BiometricHelper.showBiometricPrompt(
                    activity = fragmentActivity,
                    title = "Unlock Protected Note",
                    subtitle = "Use fingerprint or face unlock to open note",
                    onSuccess = {
                        viewModel.unlockNote(target.id)
                        val destId = target.id
                        noteToUnlock = null
                        onNoteClick(destId)
                    }
                )
            }
        }

        if (showSecurityRecovery) {
            AlertDialog(
                onDismissRequest = {
                    showSecurityRecovery = false
                    recoveryAnswerInput = ""
                    recoveryErrorMessage = null
                },
                icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Note Lock Recovery") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Answer your security question to unlock this note:")
                        Text(
                            text = securityQuestion.ifBlank { "What is your favorite book?" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = recoveryAnswerInput,
                            onValueChange = {
                                recoveryAnswerInput = it
                                recoveryErrorMessage = null
                            },
                            label = { Text("Your Answer") },
                            singleLine = true,
                            isError = recoveryErrorMessage != null,
                            supportingText = recoveryErrorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth().testTag("unlock_recovery_answer_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val expected = securityAnswer.trim()
                            val given = recoveryAnswerInput.trim()
                            val isMatch = expected.isBlank() || given.equals(expected, ignoreCase = true)
                            if (isMatch) {
                                viewModel.unlockNote(target.id)
                                val destId = target.id
                                showSecurityRecovery = false
                                recoveryAnswerInput = ""
                                noteToUnlock = null
                                onNoteClick(destId)
                            } else {
                                recoveryErrorMessage = "Incorrect answer"
                            }
                        },
                        modifier = Modifier.testTag("unlock_recovery_confirm_button")
                    ) {
                        Text("Unlock Note")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSecurityRecovery = false
                            recoveryAnswerInput = ""
                            recoveryErrorMessage = null
                        }
                    ) {
                        Text("Back")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { noteToUnlock = null },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Unlock Protected Note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter password or PIN to view this protected note.")
                        OutlinedTextField(
                            value = unlockPasswordInput,
                            onValueChange = {
                                unlockPasswordInput = it
                                unlockErrorMessage = null
                            },
                            label = { Text("Password / PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = unlockErrorMessage != null,
                            supportingText = unlockErrorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth().testTag("unlock_password_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val notePass = target.protectedPassword
                            val isValid = when {
                                !notePass.isNullOrBlank() -> unlockPasswordInput == notePass
                                masterNotesPassword.isNotBlank() -> unlockPasswordInput == masterNotesPassword
                                appLockPin.isNotBlank() -> unlockPasswordInput == appLockPin
                                else -> unlockPasswordInput.isNotBlank()
                            }

                            if (isValid) {
                                viewModel.unlockNote(target.id)
                                val destId = target.id
                                noteToUnlock = null
                                onNoteClick(destId)
                            } else {
                                unlockErrorMessage = "Incorrect password or PIN"
                            }
                        },
                        modifier = Modifier.testTag("unlock_confirm_button")
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = { showSecurityRecovery = true },
                            modifier = Modifier.testTag("unlock_forgot_password_button")
                        ) {
                            Text("Forgot?")
                        }
                        TextButton(onClick = { noteToUnlock = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }

    // Filter Notes by Color Dialog
    if (showColorFilterDialog) {
        AlertDialog(
            onDismissRequest = { showColorFilterDialog = false },
            icon = { Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Filter Notes by Color") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select a color label (Coral, Ruby, Peach, etc.) to show only matching notes:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ColorPicker(
                        selectedColorId = selectedColorFilter ?: "default",
                        onColorSelected = { colorId ->
                            if (colorId == "default") {
                                viewModel.setSelectedColorFilter(null)
                            } else {
                                viewModel.setSelectedColorFilter(colorId)
                            }
                            showColorFilterDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                if (selectedColorFilter != null) {
                    TextButton(
                        onClick = {
                            viewModel.setSelectedColorFilter(null)
                            showColorFilterDialog = false
                        }
                    ) {
                        Text("Clear Color Filter")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorFilterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

