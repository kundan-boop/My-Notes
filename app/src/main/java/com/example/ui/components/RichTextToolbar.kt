package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    onToggleChecklistMode: () -> Unit,
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onApplyHighlight: (hex: String) -> Unit,
    onApplyTextColor: (hex: String) -> Unit,
    onInsertBulletList: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onStartSpeechToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighlightMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

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
            // Checklist mode toggle
            IconButton(
                onClick = onToggleChecklistMode,
                modifier = Modifier.testTag("toolbar_checklist")
            ) {
                Icon(
                    Icons.Default.CheckBox,
                    contentDescription = "Checklist mode",
                    tint = if (isChecklistMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bold
            IconButton(
                onClick = onInsertBold,
                modifier = Modifier.testTag("toolbar_bold")
            ) {
                Icon(
                    Icons.Default.FormatBold,
                    contentDescription = "Format bold",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Italic
            IconButton(
                onClick = onInsertItalic,
                modifier = Modifier.testTag("toolbar_italic")
            ) {
                Icon(
                    Icons.Default.FormatItalic,
                    contentDescription = "Format italic",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Highlight Color Dropdown
            Box {
                IconButton(
                    onClick = { showHighlightMenu = true },
                    modifier = Modifier.testTag("toolbar_highlight")
                ) {
                    Icon(
                        Icons.Default.FormatColorFill,
                        contentDescription = "Text highlight color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
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
                    modifier = Modifier.testTag("toolbar_font_color")
                ) {
                    Icon(
                        Icons.Default.FormatColorText,
                        contentDescription = "Text font color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
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
                    modifier = Modifier.size(22.dp)
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
                    modifier = Modifier.size(22.dp)
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
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

