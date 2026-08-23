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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
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
    ColorOption("Lavender", "#E9D5FF", Color(0xFFE9D5FF)),
    ColorOption("Pink", "#FBCFE8", Color(0xFFFBCFE8)),
    ColorOption("Rose", "#FECDD3", Color(0xFFFECDD3)),
    ColorOption("Amber", "#FDE68A", Color(0xFFFDE68A)),
    ColorOption("Teal", "#CCFBF1", Color(0xFFCCFBF1)),
    ColorOption("Sky", "#E0F2FE", Color(0xFFE0F2FE)),
    ColorOption("Violet", "#EDE9FE", Color(0xFFEDE9FE))
)

val FONT_COLORS = listOf(
    ColorOption("Red", "#EF4444", Color(0xFFEF4444)),
    ColorOption("Blue", "#3B82F6", Color(0xFF3B82F6)),
    ColorOption("Green", "#10B981", Color(0xFF10B981)),
    ColorOption("Amber", "#F59E0B", Color(0xFFF59E0B)),
    ColorOption("Purple", "#8B5CF6", Color(0xFF8B5CF6)),
    ColorOption("Pink", "#EC4899", Color(0xFFEC4899)),
    ColorOption("Teal", "#14B8A6", Color(0xFF14B8A6)),
    ColorOption("Indigo", "#6366F1", Color(0xFF6366F1)),
    ColorOption("Orange", "#F97316", Color(0xFFF97316)),
    ColorOption("Cyan", "#06B6D4", Color(0xFF06B6D4)),
    ColorOption("Lime", "#84CC16", Color(0xFF84CC16))
)

@Composable
fun RichTextToolbar(
    isChecklistMode: Boolean,
    activeFormats: ActiveFormats = ActiveFormats(),
    onInsertImage: () -> Unit,
    onToggleChecklistMode: () -> Unit,
    onOpenReminder: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onApplyFontSize: (FontSizePreset) -> Unit,
    onApplyAlignment: (TextAlignmentPreset) -> Unit,
    onApplyHighlight: (hex: String) -> Unit,
    onApplyTextColor: (hex: String) -> Unit,
    onInsertBulletList: (marker: String) -> Unit = {},
    onInsertNumberedList: (prefix: String) -> Unit = {},
    onStartSpeechToText: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHighlightMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showBulletMenu by remember { mutableStateOf(false) }
    var showNumberedMenu by remember { mutableStateOf(false) }

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
            // 1. Checklist mode toggle
            ToolbarToggleIconButton(
                icon = Icons.Default.CheckBox,
                contentDescription = "Checklist mode",
                isActive = isChecklistMode,
                testTag = "toolbar_checklist",
                onClick = onToggleChecklistMode
            )

            ToolbarDivider()

            // 4. Bold
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Format bold",
                isActive = activeFormats.isBold,
                testTag = "toolbar_bold",
                onClick = onToggleBold
            )

            // 5. Italic
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Format italic",
                isActive = activeFormats.isItalic,
                testTag = "toolbar_italic",
                onClick = onToggleItalic
            )

            // 6. Underline
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Format underline",
                isActive = activeFormats.isUnderline,
                testTag = "toolbar_underline",
                onClick = onToggleUnderline
            )

            // 7. Strikethrough
            ToolbarToggleIconButton(
                icon = Icons.Default.FormatStrikethrough,
                contentDescription = "Format strikethrough",
                isActive = activeFormats.isStrikethrough,
                testTag = "toolbar_strikethrough",
                onClick = onToggleStrikethrough
            )

            // 8. Highlight Color Dropdown
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

            // 9. Text Color Dropdown
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

            // 10. Font Size Dropdown
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

            // 11. Alignment Dropdown
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

            // 12. Bullet List with Style Picker
            Box {
                IconButton(
                    onClick = { showBulletMenu = true },
                    modifier = Modifier.testTag("toolbar_bullet")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = "Bullet list styles",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showBulletMenu,
                    onDismissRequest = { showBulletMenu = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    Text(
                        text = "Bullet List Style",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("•", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Round ( • )") },
                        onClick = {
                            onInsertBulletList("•")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("✓", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Checkmark ( ✓ )") },
                        onClick = {
                            onInsertBulletList("✓")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("->", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Arrow with line ( -> )") },
                        onClick = {
                            onInsertBulletList("->")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("➔", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Right Arrow ( ➔ )") },
                        onClick = {
                            onInsertBulletList("➔")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("◄", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Left Arrow ( ◄ )") },
                        onClick = {
                            onInsertBulletList("◄")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("⇨", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Outlined Arrow ( ⇨ )") },
                        onClick = {
                            onInsertBulletList("⇨")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("⮞", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Chevron Arrow ( ⮞ )") },
                        onClick = {
                            onInsertBulletList("⮞")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("⤢", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("4-Way Arrows ( ⤢ )") },
                        onClick = {
                            onInsertBulletList("⤢")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("❏", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Unique 3D Box ( ❏ )") },
                        onClick = {
                            onInsertBulletList("❏")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("▣", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("3D Framed Box ( ▣ )") },
                        onClick = {
                            onInsertBulletList("▣")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("▪", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Square ( ▪ )") },
                        onClick = {
                            onInsertBulletList("▪")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("◆", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Solid Diamond ( ◆ )") },
                        onClick = {
                            onInsertBulletList("◆")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("◈", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Diamond Target ( ◈ )") },
                        onClick = {
                            onInsertBulletList("◈")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("✣", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Diamond Cluster ( ✣ )") },
                        onClick = {
                            onInsertBulletList("✣")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("❖", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Floral Cross ( ❖ )") },
                        onClick = {
                            onInsertBulletList("❖")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("✾", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Petal Flower ( ✾ )") },
                        onClick = {
                            onInsertBulletList("✾")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("✦", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Sparkle Star ( ✦ )") },
                        onClick = {
                            onInsertBulletList("✦")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("❂", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Radiant Sun ( ❂ )") },
                        onClick = {
                            onInsertBulletList("❂")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("✹", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Sunburst Star ( ✹ )") },
                        onClick = {
                            onInsertBulletList("✹")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("☽", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Crescent Moon ( ☽ )") },
                        onClick = {
                            onInsertBulletList("☽")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("○", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Open Circle ( ○ )") },
                        onClick = {
                            onInsertBulletList("○")
                            showBulletMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("▲", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Pyramid Type ( ▲ )") },
                        onClick = {
                            onInsertBulletList("▲")
                            showBulletMenu = false
                        }
                    )
                }
            }

            // 13. Numbered List with Style Picker
            Box {
                IconButton(
                    onClick = { showNumberedMenu = true },
                    modifier = Modifier.testTag("toolbar_numbered")
                ) {
                    Icon(
                        Icons.Default.FormatListNumbered,
                        contentDescription = "Numbered list styles",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showNumberedMenu,
                    onDismissRequest = { showNumberedMenu = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    Text(
                        text = "Numbered List Style",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("1.", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Numbers ( 1, 2, 3... )") },
                        onClick = {
                            onInsertNumberedList("1.")
                            showNumberedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("a.", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Letters ( a, b, c... )") },
                        onClick = {
                            onInsertNumberedList("a.")
                            showNumberedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Text("i.", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) },
                        text = { Text("Roman ( i, ii, iii... )") },
                        onClick = {
                            onInsertNumberedList("i.")
                            showNumberedMenu = false
                        }
                    )
                }
            }

            ToolbarDivider()

            // 14. Speech-to-Text Dictation
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
