package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    notesViewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val fontSize by settingsViewModel.fontSize.collectAsState()
    val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
    val appLockPin by settingsViewModel.appLockPin.collectAsState()
    val lockType by settingsViewModel.lockType.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val userEmail by settingsViewModel.userEmail.collectAsState()
    val protectedNotesPassword by settingsViewModel.protectedNotesPassword.collectAsState()
    val securityQuestion by settingsViewModel.securityQuestion.collectAsState()
    val securityAnswer by settingsViewModel.securityAnswer.collectAsState()
    val viewMode by notesViewModel.viewMode.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showProtectedNotesPasswordDialog by remember { mutableStateOf(false) }
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var profileNameInput by remember(userName) { mutableStateOf(userName) }
    var profileEmailInput by remember(userEmail) { mutableStateOf(userEmail) }

    var notesPasswordInput by remember(protectedNotesPassword) { mutableStateOf(protectedNotesPassword) }
    var questionInput by remember(securityQuestion) { mutableStateOf(securityQuestion) }
    var answerInput by remember(securityAnswer) { mutableStateOf(securityAnswer) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Profile Card (Requirement 3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName.ifBlank { "My Notes User" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = userEmail.ifBlank { "Local offline profile" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = {
                            profileNameInput = userName
                            profileEmailInput = userEmail
                            showProfileDialog = true
                        },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 2. Security & Protection (Requirements 4 & 9)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Security & Privacy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(Modifier.height(14.dp))

                    // App Lock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock (PIN)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                if (appLockEnabled) "PIN protection active on launch" else "Require 4-digit PIN to open app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinSetupDialog = true
                                } else {
                                    settingsViewModel.setAppLock(false)
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (appLockEnabled) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                pinInput = ""
                                pinError = null
                                showPinSetupDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Change PIN")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Protected Notes Master Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protected Notes Password", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Password used to unlock individually protected notes (Default: 1234)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                notesPasswordInput = protectedNotesPassword
                                showProtectedNotesPasswordDialog = true
                            }
                        ) {
                            Text("Change")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Security Question
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Security Question", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                if (securityAnswer.isNotBlank()) "Configured for password recovery" else "Set up for password recovery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                questionInput = securityQuestion
                                answerInput = securityAnswer
                                showSecurityQuestionDialog = true
                            }
                        ) {
                            Text(if (securityAnswer.isNotBlank()) "Edit" else "Set Up")
                        }
                    }
                }
            }

            // 3. Theme Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Theme", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        "system" to "System Default",
                        "light" to "Light Theme",
                        "dark" to "Dark Theme"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { settingsViewModel.setThemeMode(mode) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // 4. Default View Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Default Layout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        "grid" to "Masonry Grid View",
                        "list" to "Single Column List View"
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = viewMode == mode,
                                onClick = { notesViewModel.toggleViewMode() }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // 5. Text Size Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Text Size", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        "small" to "Small Text",
                        "medium" to "Medium (Default)",
                        "large" to "Large Text"
                    ).forEach { (size, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = fontSize == size,
                                onClick = { settingsViewModel.setFontSize(size) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // 6. About App Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("About My Notes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Version 2.0.0 (Native Android & Offline-First)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Complete personal notes application with rich text formatting, voice-to-text, protected notes, checklists, tags, 7-day rolling backups, and Material 3 design.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Edit Profile Dialog
        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                title = { Text("Edit Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = profileNameInput,
                            onValueChange = { profileNameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = profileEmailInput,
                            onValueChange = { profileEmailInput = it },
                            label = { Text("Email (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            settingsViewModel.setUserProfile(profileNameInput.trim(), profileEmailInput.trim(), "")
                            showProfileDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // PIN Setup Dialog
        if (showPinSetupDialog) {
            AlertDialog(
                onDismissRequest = { showPinSetupDialog = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                title = { Text("Set 4-Digit Security PIN") },
                text = {
                    Column {
                        Text("Enter a 4-digit numeric PIN to protect your notes on app launch:")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    pinInput = it
                                    pinError = null
                                }
                            },
                            label = { Text("4-Digit PIN") },
                            singleLine = true,
                            isError = pinError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(pinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pinInput.length == 4) {
                                settingsViewModel.setAppLock(true, pinInput)
                                showPinSetupDialog = false
                                pinInput = ""
                            } else {
                                pinError = "PIN must be exactly 4 digits"
                            }
                        }
                    ) {
                        Text("Set PIN")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPinSetupDialog = false
                            pinInput = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Protected Notes Password Dialog
        if (showProtectedNotesPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showProtectedNotesPasswordDialog = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                title = { Text("Protected Notes Password") },
                text = {
                    Column {
                        Text("Set the master unlock password for all protected notes:")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = notesPasswordInput,
                            onValueChange = { notesPasswordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (notesPasswordInput.isNotBlank()) {
                                settingsViewModel.setProtectedNotesPassword(notesPasswordInput.trim())
                                showProtectedNotesPasswordDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProtectedNotesPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Security Question Dialog
        if (showSecurityQuestionDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityQuestionDialog = false },
                icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                title = { Text("Security Recovery Question") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Set a recovery question to reset your password if forgotten:")
                        OutlinedTextField(
                            value = questionInput,
                            onValueChange = { questionInput = it },
                            label = { Text("Security Question") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = { answerInput = it },
                            label = { Text("Answer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (questionInput.isNotBlank() && answerInput.isNotBlank()) {
                                settingsViewModel.setSecurityQuestionAndAnswer(questionInput.trim(), answerInput.trim())
                                showSecurityQuestionDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSecurityQuestionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

