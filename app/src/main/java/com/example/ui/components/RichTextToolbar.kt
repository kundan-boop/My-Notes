package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.ActiveFormats
import com.example.util.FontSizePreset
import com.example.util.TextAlignmentPreset

data class ColorOption(val name: String, val hex: String, val color: Color)

val HIGHLIGHT_COLORS = listOf(
    ColorOption("Yellow", "#FEF08A", Color(0xFFFEF08A)),
    ColorOption("Green", "#BBF7D0", Color(0xFFBBF7D0)),
    ColorOption("Cyan", "#BAE6FD", Color(0xFFBAE6FD)),
    ColorOption("Peach", "#FED7AA", Color(0xFFFED7AA)),
    ColorOption("Lavender", "#E9D5FF", Color(0xFFE9D5FF))
)

val FONT_COLORS = listOf(
    ColorOption("Red", "#EF4444", Color(0xFFEF4444)),
    ColorOption("Blue", "#3B82F6", Color(0xFF3B82F6)),
    ColorOption("Green", "#10B981", Color(0xFF10B981)),
    ColorOption("Amber", "#F59E0B", Color(0xFFF59E0B)),
    ColorOption("Purple", "#8B5CF6", Color(0xFF8B5CF6))
)

@Composable
fun RichTextToolbar(
    isChecklistMode: Boolean,
    activeFormats: ActiveFormats = ActiveFormats(),
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleChecklistMode: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onApplyFontSize: (FontSizePreset) -> Unit,
    onApplyAlignment: (TextAlignmentPreset) -> Unit,
    onApplyHighlight: (hex: String) -> Unit,
    onApplyTextColor: (hex: String) -> Unit,
    onInsertBulletList: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onClearFormatting: () -> Unit,
    onStartSpeechToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighlightMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("toolbar_undo")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Redo Button
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("toolbar_redo")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }

            ToolbarDivider()

            // Checklist mode toggle
            ToolbarToggleIconButton(
                icon = Icons.Default.CheckBox,
                contentDescription = "Checklist mode",
                isActive = isChecklistMode,
                testTag = "toolbar_checklist",
                onClick = onToggleChecklistMode
            )

            // Bold
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Format bold",
                isActive = activeFormats.isBold,
                testTag = "toolbar_bold",
                onClick = onToggleBold
            )

            // Italic
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Format italic",
                isActive = activeFormats.isItalic,
                testTag = "toolbar_italic",
                onClick = onToggleItalic
            )

            // Underline
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Format underline",
                isActive = activeFormats.isUnderline,
                testTag = "toolbar_underline",
                onClick = onToggleUnderline
            )

            // Strikethrough
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatStrikethrough,
                contentDescription = "Format strikethrough",
                isActive = activeFormats.isStrikethrough,
                testTag = "toolbar_strikethrough",
                onClick = onToggleStrikethrough
            )

            // Font Size Dropdown
            Box {
                IconButton(
                    onClick = { showSizeMenu = true },
                    modifier = Modifier
                        .testTag("toolbar_font_size")
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeFormats.fontSize != FontSizePreset.NORMAL) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = "Font size preset",
                        tint = if (activeFormats.fontSize != FontSizePreset.NORMAL) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSizeMenu,
                    onDismissRequest = { showSizeMenu = false }
                ) {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    FontSizePreset.values().forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.label,
                                    fontWeight = if (activeFormats.fontSize == preset) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeFormats.fontSize == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onApplyFontSize(preset)
                                showSizeMenu = false
                            }
                        )
                    }
                }
            }

            // Alignment Dropdown / Selector
            Box {
                val alignIcon = when (activeFormats.alignment) {
                    TextAlignmentPreset.CENTER -> Icons.Default.FormatAlignCenter
                    TextAlignmentPreset.RIGHT -> Icons.Default.FormatAlignRight
                    else -> Icons.Default.FormatAlignLeft
                }

                IconButton(
                    onClick = { showAlignMenu = true },
                    modifier = Modifier
                        .testTag("toolbar_alignment")
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeFormats.alignment != TextAlignmentPreset.LEFT) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        alignIcon,
                        contentDescription = "Text alignment",
                        tint = if (activeFormats.alignment != TextAlignmentPreset.LEFT) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showAlignMenu,
                    onDismissRequest = { showAlignMenu = false }
                ) {
                    Text(
                        text = "Alignment",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) },
                        text = { Text("Align Left") },
                        onClick = {
                            onApplyAlignment(TextAlignmentPreset.LEFT)
                            showAlignMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FormatAlignCenter, contentDescription = null) },
                        text = { Text("Align Center") },
                        onClick = {
                            onApplyAlignment(TextAlignmentPreset.CENTER)
                            showAlignMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FormatAlignRight, contentDescription = null) },
                        text = { Text("Align Right") },
                        onClick = {
                            onApplyAlignment(TextAlignmentPreset.RIGHT)
                            showAlignMenu = false
                        }
                    )
                }
            }

            ToolbarDivider()

            // Highlight Color Dropdown
            Box {
                IconButton(
                    onClick = { showHighlightMenu = true },
                    modifier = Modifier
                        .testTag("toolbar_highlight")
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeFormats.highlightColor != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        Icons.Default.FormatColorFill,
                        contentDescription = "Text highlight color",
                        tint = if (activeFormats.highlightColor != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showHighlightMenu,
                    onDismissRequest = { showHighlightMenu = false }
                ) {
                    Text(
                        text = "Highlight Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HIGHLIGHT_COLORS.forEach { option ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        onApplyHighlight(option.hex)
                                        showHighlightMenu = false
                                    }
                            )
                        }
                    }
                }
            }

            // Text Color Dropdown
            Box {
                IconButton(
                    onClick = { showColorMenu = true },
                    modifier = Modifier
                        .testTag("toolbar_font_color")
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeFormats.textColor != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        Icons.Default.FormatColorText,
                        contentDescription = "Text font color",
                        tint = if (activeFormats.textColor != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    Text(
                        text = "Text Color",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FONT_COLORS.forEach { option ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        onApplyTextColor(option.hex)
                                        showColorMenu = false
                                    }
                            )
                        }
                    }
                }
            }

            // Bullet List
            IconButton(
                onClick = onInsertBulletList,
                modifier = Modifier.testTag("toolbar_bullet")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = "Bullet list",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Numbered List
            IconButton(
                onClick = onInsertNumberedList,
                modifier = Modifier.testTag("toolbar_numbered")
            ) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            ToolbarDivider()

            // Clear Formatting
            IconButton(
                onClick = onClearFormatting,
                modifier = Modifier.testTag("toolbar_clear_format")
            ) {
                Icon(
                    Icons.Default.FormatClear,
                    contentDescription = "Clear formatting",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Speech-to-Text Dictation
            IconButton(
                onClick = onStartSpeechToText,
                modifier = Modifier.testTag("toolbar_speech_to_text")
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Dictate Speech to Text",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarToggleIconButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
