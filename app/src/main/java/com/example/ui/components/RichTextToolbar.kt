package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RichTextToolbar(
    isChecklistMode: Boolean,
    onToggleChecklistMode: () -> Unit,
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertBulletList: () -> Unit,
    onInsertNumberedList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    }
}
