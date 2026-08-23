package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import com.example.util.ShareNoteHelper
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.ChecklistItem
import com.example.data.local.Converters
import com.example.data.local.NoteEntity
import com.example.ui.theme.NoteColors
import com.example.util.RichTextRenderer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    isUnlocked: Boolean = false,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onMoveToTrash: () -> Unit,
    onColorPickRequest: () -> Unit,
    onMovePinnedLeft: (() -> Unit)? = null,
    onMovePinnedRight: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    val preset = NoteColors.getPreset(note.colorHex)
    val cardBg = if (preset.id == "default") {
        MaterialTheme.colorScheme.surface
    } else {
        if (darkTheme) preset.darkBg else preset.lightBg
    }

    val cardBorder = if (preset.id == "default") {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        if (darkTheme) preset.darkBorder else preset.lightBorder
    }

    val textColor = if (preset.id == "default") {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }

    val isMasked = note.isProtected && !isUnlocked

    val checklistItems = remember(note.checklistJson) {
        Converters.jsonToChecklist(note.checklistJson)
    }

    val tagNames = remember(note.tagsJson) {
        Converters.jsonToStringList(note.tagsJson)
    }

    val attachmentPaths = remember(note.attachmentsJson) {
        Converters.jsonToStringList(note.attachmentsJson)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Title + Protected / Pin / Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isProtected) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    val displayTitle = when {
                        isMasked -> "Protected Note"
                        note.title.isNotBlank() -> note.title
                        else -> "Untitled Note"
                    }

                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned && onMovePinnedLeft != null) {
                        IconButton(
                            onClick = onMovePinnedLeft,
                            modifier = Modifier.size(28.dp).testTag("move_pinned_left_${note.id}")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move Pinned Left",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (note.isPinned && onMovePinnedRight != null) {
                        IconButton(
                            onClick = onMovePinnedRight,
                            modifier = Modifier.size(28.dp).testTag("move_pinned_right_${note.id}")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move Pinned Right",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_note_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("note_menu_button_${note.id}")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                                onClick = {
                                    showMenu = false
                                    onTogglePin()
                                },
                                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showMenu = false
                                    onToggleArchive()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Note") },
                                onClick = {
                                    showMenu = false
                                    ShareNoteHelper.shareNote(context, note)
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Color Label") },
                                onClick = {
                                    showMenu = false
                                    onColorPickRequest()
                                },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash") },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (isMasked) {
                // Masked protected note preview
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Locked content • Tap to unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Image Preview Grid (Compact 56dp height)
                if (attachmentPaths.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        attachmentPaths.take(3).forEach { path ->
                            val file = File(path)
                            if (file.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Attachment preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }

                // Voice Note Indicator
                if (note.type == "voice" && !note.audioPath.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Voice Note",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Rich Text Content Preview (Max 3 lines for compact density)
                if (note.content.isNotBlank() && RichTextRenderer.stripHtml(note.content).isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = RichTextRenderer.parseRichText(note.content, textColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Checklist Preview Mode (Max 2 items for compact grid density)
                if (checklistItems.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        checklistItems.take(2).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (item.isChecked) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (item.isChecked) textColor.copy(alpha = 0.5f) else textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (checklistItems.size > 2) {
                            Text(
                                text = "+ ${checklistItems.size - 2} more items",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Tag Chips & Reminder Pill
            if (tagNames.isNotEmpty() || note.reminderAt != null) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.reminderAt != null) {
                        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = sdf.format(Date(note.reminderAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    tagNames.forEach { tagName ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "#$tagName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Move note to Trash?") },
            text = { Text("You can restore this note from the Trash within 30 days before it is permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onMoveToTrash()
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

