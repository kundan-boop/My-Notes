package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    val activeNotes by viewModel.activeNotes.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()

    var colorPickNoteId by remember { mutableStateOf<String?>(null) }

    val pinnedNotes = remember(activeNotes) { activeNotes.filter { it.isPinned } }
    val otherNotes = remember(activeNotes) { activeNotes.filter { !it.isPinned } }

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
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
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
                    color = MaterialTheme.colorScheme.surface
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
                            }
                        }

                        // Filter chips row
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == "checklist",
                                    onClick = { viewModel.setSelectedTypeFilter("checklist") },
                                    label = { Text("Checklists") },
                                    leadingIcon = { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedTypeFilter == "voice",
                                    onClick = { viewModel.setSelectedTypeFilter("voice") },
                                    label = { Text("Voice Notes") },
                                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
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
                    // Quick Note Expand Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNewNoteClick("text") }
                            .testTag("quick_note_bar"),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Take a note...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { onNewNoteClick("checklist") }) {
                                    Icon(Icons.Default.CheckBox, contentDescription = "New Checklist", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onNewNoteClick("voice") }) {
                                    Icon(Icons.Default.Mic, contentDescription = "New Voice Note", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "PINNED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(pinnedNotes, key = { it.id }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onTogglePin = { viewModel.togglePin(note.id) },
                                            onToggleArchive = { viewModel.toggleArchive(note.id) },
                                            onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                            onColorPickRequest = { colorPickNoteId = note.id }
                                        )
                                    }
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                }

                                items(otherNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "PINNED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(pinnedNotes, key = { it.id }) { note ->
                                        NoteCard(
                                            note = note,
                                            onClick = { onNoteClick(note.id) },
                                            onTogglePin = { viewModel.togglePin(note.id) },
                                            onToggleArchive = { viewModel.toggleArchive(note.id) },
                                            onMoveToTrash = { viewModel.moveToTrash(note.id) },
                                            onColorPickRequest = { colorPickNoteId = note.id }
                                        )
                                    }
                                    item {
                                        Text(
                                            text = "OTHERS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                }

                                items(otherNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = { onNoteClick(note.id) },
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
}
