package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupManager
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    notesViewModel: NotesViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lastBackupTime by settingsViewModel.lastBackupTime.collectAsState()
    val lastRotatingSlot by settingsViewModel.lastRotatingSlot.collectAsState()
    val driveFolderName by settingsViewModel.driveFolderName.collectAsState()
    val driveFolderSelected by settingsViewModel.driveFolderSelected.collectAsState()
    val isDataDirty by settingsViewModel.isDataDirty.collectAsState()

    var slotsInfo by remember { mutableStateOf(BackupManager.getBackupSlotsInfo(context, lastRotatingSlot)) }
    var slotToRestore by remember { mutableStateOf<Int?>(null) }
    var showDriveFolderDialog by remember { mutableStateOf(!driveFolderSelected) }
    var customFolderNameInput by remember(driveFolderName) { mutableStateOf(driveFolderName) }

    fun refreshSlots() {
        slotsInfo = BackupManager.getBackupSlotsInfo(context, lastRotatingSlot)
    }

    LaunchedEffect(lastBackupTime, lastRotatingSlot) {
        refreshSlots()
    }

    // Save Document / Folder picker launcher
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val jsonString = notesViewModel.exportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                Toast.makeText(context, "Backup successfully saved to selected folder!", Toast.LENGTH_LONG).show()
                settingsViewModel.updateLastBackupTime(System.currentTimeMillis())
                settingsViewModel.setDataDirty(false)
                refreshSlots()
            }.onFailure {
                Toast.makeText(context, "Failed to save backup: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Import file launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    notesViewModel.importBackupJson(json)
                    refreshSlots()
                }
            }.onFailure {
                Toast.makeText(context, "Error importing file: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folder Selection Notice Banner if not configured
            if (!driveFolderSelected) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Google Drive Folder Required",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Please select a Google Drive folder to enable automatic close/background backups.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { showDriveFolderDialog = true },
                                modifier = Modifier.testTag("prompt_select_drive_folder_button")
                            ) {
                                Text("Select")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                // Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "7-Day Rolling Backup Window",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (lastBackupTime > 0) {
                                        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                                        "Last Backup: ${sdf.format(Date(lastBackupTime))}"
                                    } else {
                                        "No backup created yet"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last Written Slot:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (lastRotatingSlot in 1..7) "Slot #$lastRotatingSlot (Day-$lastRotatingSlot.json)" else "None",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "App Close Backup:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isDataDirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(8.dp)
                                        ) {}
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isDataDirty) "Changes pending backup" else "Up to date",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isDataDirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Google Drive Folder Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Google Drive Folder", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            FilledTonalButton(
                                onClick = {
                                    customFolderNameInput = driveFolderName
                                    showDriveFolderDialog = true
                                },
                                modifier = Modifier.testTag("backup_choose_drive_folder_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Change")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Target: $driveFolderName",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "All close-triggered backups automatically overwrite that same day's file or advance to the next rolling slot in this Drive folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // 7-Day Rotating Backup Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "7 Daily Rotating Slots",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            FilledTonalButton(
                                onClick = {
                                    notesViewModel.runDailyRotatingBackup()
                                    refreshSlots()
                                },
                                modifier = Modifier.testTag("trigger_backup_now_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Backup Now")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Maintains at most 7 daily backup files (Day-1.json to Day-7.json). Multiple app closes on the same day overwrite the same day's file, while Day 8 overwrites the oldest slot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        slotsInfo.forEach { slot ->
                            val isLatest = slot.slotNumber == lastRotatingSlot && slot.exists
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isLatest) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (slot.exists) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isLatest) MaterialTheme.colorScheme.primary else if (slot.exists) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Day-${slot.slotNumber}.json",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (isLatest) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Text(
                                                            "LATEST",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (slot.exists) {
                                                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                                    "${sdf.format(Date(slot.lastModified))} • ${(slot.sizeBytes / 1024).coerceAtLeast(1)} KB"
                                                } else {
                                                    "Empty slot"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    if (slot.exists) {
                                        TextButton(
                                            onClick = { slotToRestore = slot.slotNumber },
                                            modifier = Modifier.testTag("restore_slot_${slot.slotNumber}_button")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export Backup File Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Export to File", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Generate a portable JSON file containing all notes, tags, checklists, and compressed image attachments to save or share.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val fileName = "MyNotes_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                                    saveDocumentLauncher.launch(fileName)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_downloads_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save to Local")
                            }

                            OutlinedButton(
                                onClick = {
                                    val jsonString = notesViewModel.exportBackupJson()
                                    try {
                                        val backupFile = java.io.File(context.cacheDir, "MyNotes_Backup_${System.currentTimeMillis()}.json")
                                        backupFile.writeText(jsonString)
                                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            backupFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, fileUri)
                                            putExtra(Intent.EXTRA_SUBJECT, backupFile.name)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share or Save Notes Backup"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error sharing backup: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                    refreshSlots()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share JSON")
                            }
                        }
                    }
                }
            }

            // Restore Backup File Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Restore from File", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Import a previously saved My Notes JSON file to restore your notes, tags, and media.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch("*/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Select JSON File")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Google Drive Folder Picker Dialog
    if (showDriveFolderDialog) {
        val suggestedFolders = listOf(
            "My Drive / MyNotes_Backups /",
            "My Drive / Daily Backups /",
            "My Drive / Personal Notes /",
            "My Drive / Backups / NotesApp /"
        )

        AlertDialog(
            onDismissRequest = { showDriveFolderDialog = false },
            icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Select Google Drive Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Select or enter the Google Drive folder for rotating daily backups (Day-1.json through Day-7.json):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    suggestedFolders.forEach { folder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (customFolderNameInput == folder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            onClick = { customFolderNameInput = folder }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (customFolderNameInput == folder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    folder,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (customFolderNameInput == folder) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customFolderNameInput,
                        onValueChange = { customFolderNameInput = it },
                        label = { Text("Google Drive Folder Path") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_custom_folder_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customFolderNameInput.isNotBlank()) {
                            settingsViewModel.setDriveFolderName(customFolderNameInput.trim())
                            showDriveFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("backup_save_drive_folder_button")
                ) {
                    Text("Select Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (slotToRestore != null) {
        val slotNum = slotToRestore!!
        AlertDialog(
            onDismissRequest = { slotToRestore = null },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore Slot $slotNum?") },
            text = {
                Text("This will restore the notes and tags saved in Day-$slotNum.json. Any conflicting note will be safely merged.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        notesViewModel.restoreFromRotatingSlot(slotNum)
                        slotToRestore = null
                        refreshSlots()
                    }
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { slotToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

