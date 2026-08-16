package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class FontSizePreset(val label: String, val tag: String, val spSize: Float) {
    SMALL("Small", "small", 12f),
    NORMAL("Normal", "normal", 16f),
    LARGE("Large", "large", 20f),
    EXTRA_LARGE("X-Large", "x-large", 24f);

    companion object {
        fun fromTag(tag: String?): FontSizePreset {
            return when (tag?.lowercase()) {
                "small", "12px", "12sp" -> SMALL
                "large", "20px", "20sp" -> LARGE
                "x-large", "extra-large", "24px", "24sp" -> EXTRA_LARGE
                else -> NORMAL
            }
        }
    }
}

enum class TextAlignmentPreset(val label: String, val tag: String) {
    LEFT("Left", "left"),
    CENTER("Center", "center"),
    RIGHT("Right", "right");

    companion object {
        fun fromTag(tag: String?): TextAlignmentPreset {
            return when (tag?.lowercase()) {
                "center" -> CENTER
                "right" -> RIGHT
                else -> LEFT
            }
        }
    }
}

data class ActiveFormats(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val highlightColor: String? = null,
    val textColor: String? = null,
    val fontSize: FontSizePreset = FontSizePreset.NORMAL,
    val alignment: TextAlignmentPreset = TextAlignmentPreset.LEFT
)

object RichTextHelper {

    /**
     * Strips all rich text tags (HTML & Markdown) from the input string.
     */
    fun stripAllFormatting(input: String): String {
        var clean = input
        // Remove HTML tags
        clean = clean.replace(Regex("<[^>]+>"), "")
        // Remove Markdown bold/italic/strike/highlight
        clean = clean.replace("**", "")
        clean = clean.replace(Regex("(?<!\\*)\\*(?!\\*)"), "")
        clean = clean.replace("==", "")
        clean = clean.replace("~~", "")
        return clean
    }

    /**
     * Clears all formatting from the current selection in TextFieldValue.
     * If no selection is active, strips formatting across the whole text.
     */
    fun clearFormatting(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val sel = current.selection
        if (sel.start != sel.end) {
            val minPos = minOf(sel.start, sel.end)
            val maxPos = maxOf(sel.start, sel.end)
            val selected = text.substring(minPos, maxPos)
            val stripped = stripAllFormatting(selected)
            val newText = text.replaceRange(minPos, maxPos, stripped)
            return TextFieldValue(
                text = newText,
                selection = TextRange(minPos + stripped.length)
            )
        } else {
            val stripped = stripAllFormatting(text)
            return TextFieldValue(
                text = stripped,
                selection = TextRange(minOf(sel.start, stripped.length))
            )
        }
    }

    /**
     * Wraps selection with open/close tags or inserts placeholder tag.
     */
    fun applyFormatting(current: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = current.text
        val sel = current.selection
        if (sel.start != sel.end) {
            val minPos = minOf(sel.start, sel.end)
            val maxPos = maxOf(sel.start, sel.end)
            val selectedText = text.substring(minPos, maxPos)
            val newText = text.replaceRange(minPos, maxPos, "$prefix$selectedText$suffix")
            val newEnd = minPos + prefix.length + selectedText.length + suffix.length
            return TextFieldValue(
                text = newText,
                selection = TextRange(newEnd)
            )
        } else {
            val insert = "$prefix$suffix"
            val newText = text.replaceRange(sel.start, sel.end, insert)
            return TextFieldValue(
                text = newText,
                selection = TextRange(sel.start + prefix.length)
            )
        }
    }

    /**
     * Toggles a tag (e.g. <b>...</b>, <u>...</u>, <s>...</s>) on the selection.
     * If already wrapped, unwraps it; otherwise wraps.
     */
    fun toggleTag(current: TextFieldValue, openTag: String, closeTag: String): TextFieldValue {
        val text = current.text
        val sel = current.selection
        if (sel.start != sel.end) {
            val minPos = minOf(sel.start, sel.end)
            val maxPos = maxOf(sel.start, sel.end)
            val selected = text.substring(minPos, maxPos)

            // If selection itself is already wrapped in openTag ... closeTag
            if (selected.startsWith(openTag, ignoreCase = true) && selected.endsWith(closeTag, ignoreCase = true)) {
                val unwrapped = selected.substring(openTag.length, selected.length - closeTag.length)
                val newText = text.replaceRange(minPos, maxPos, unwrapped)
                return TextFieldValue(text = newText, selection = TextRange(minPos + unwrapped.length))
            }

            // Check if bounds around selection have the tag
            val before = text.substring(0, minPos)
            val after = text.substring(maxPos)
            if (before.endsWith(openTag, ignoreCase = true) && after.startsWith(closeTag, ignoreCase = true)) {
                val newBefore = before.substring(0, before.length - openTag.length)
                val newAfter = after.substring(closeTag.length)
                val newText = newBefore + selected + newAfter
                return TextFieldValue(text = newText, selection = TextRange(newBefore.length + selected.length))
            }

            // Otherwise wrap
            val newText = text.replaceRange(minPos, maxPos, "$openTag$selected$closeTag")
            return TextFieldValue(
                text = newText,
                selection = TextRange(minPos + openTag.length + selected.length + closeTag.length)
            )
        } else {
            return applyFormatting(current, openTag, closeTag)
        }
    }

    /**
     * Applies Font Size to selection.
     */
    fun applyFontSize(current: TextFieldValue, size: FontSizePreset): TextFieldValue {
        val sel = current.selection
        val text = current.text
        val openTag = if (size == FontSizePreset.NORMAL) "" else "<span style=\"font-size:${size.tag}\">"
        val closeTag = if (size == FontSizePreset.NORMAL) "" else "</span>"

        if (sel.start != sel.end) {
            val minPos = minOf(sel.start, sel.end)
            val maxPos = maxOf(sel.start, sel.end)
            var selected = text.substring(minPos, maxPos)
            // Strip any existing font-size tag from selected text
            selected = selected.replace(Regex("<span style=\"font-size:[^\"]+\">"), "")
            selected = selected.replace("<small>", "").replace("</small>", "")
            selected = selected.replace("<big>", "").replace("</big>", "")
            selected = selected.replace("<h3>", "").replace("</h3>", "")
            if (size == FontSizePreset.NORMAL) {
                val newText = text.replaceRange(minPos, maxPos, selected)
                return TextFieldValue(text = newText, selection = TextRange(minPos + selected.length))
            } else {
                val newText = text.replaceRange(minPos, maxPos, "$openTag$selected$closeTag")
                return TextFieldValue(text = newText, selection = TextRange(minPos + openTag.length + selected.length + closeTag.length))
            }
        } else {
            if (size == FontSizePreset.NORMAL) return current
            return applyFormatting(current, openTag, closeTag)
        }
    }

    /**
     * Applies Text Alignment to selection or current line.
     */
    fun applyAlignment(current: TextFieldValue, alignment: TextAlignmentPreset): TextFieldValue {
        val text = current.text
        val sel = current.selection
        val minPos = if (sel.start != sel.end) minOf(sel.start, sel.end) else sel.start
        val maxPos = if (sel.start != sel.end) maxOf(sel.start, sel.end) else sel.end

        // Find line start and line end
        var lineStart = text.lastIndexOf('\n', minPos - 1)
        lineStart = if (lineStart == -1) 0 else lineStart + 1

        var lineEnd = text.indexOf('\n', maxPos)
        lineEnd = if (lineEnd == -1) text.length else lineEnd

        var lineContent = text.substring(lineStart, lineEnd)

        // Remove existing align wrappers on this line
        lineContent = lineContent.replace(Regex("<div align=\"[^\"]+\">"), "")
        lineContent = lineContent.replace("</div>", "")
        lineContent = lineContent.replace("<center>", "").replace("</center>", "")

        val openTag = if (alignment == TextAlignmentPreset.LEFT) "" else "<div align=\"${alignment.tag}\">"
        val closeTag = if (alignment == TextAlignmentPreset.LEFT) "" else "</div>"

        val newLineContent = if (alignment == TextAlignmentPreset.LEFT) lineContent else "$openTag$lineContent$closeTag"
        val newText = text.replaceRange(lineStart, lineEnd, newLineContent)

        return TextFieldValue(
            text = newText,
            selection = TextRange(lineStart + newLineContent.length)
        )
    }

    /**
     * Detects all active formatting states at current cursor / selection.
     */
    fun detectActiveFormats(fullText: String, selection: TextRange): ActiveFormats {
        if (fullText.isEmpty()) return ActiveFormats()
        val pos = minOf(selection.start, fullText.length)

        val before = fullText.substring(0, pos)
        val after = fullText.substring(pos)

        val isBold = isTagOpen(before, after, "<b>", "</b>") ||
                isTagOpen(before, after, "<strong>", "</strong>") ||
                isMarkdownOpen(before, after, "**")

        val isItalic = isTagOpen(before, after, "<i>", "</i>") ||
                isTagOpen(before, after, "<em>", "</em>") ||
                isMarkdownSingleStarOpen(before, after)

        val isUnderline = isTagOpen(before, after, "<u>", "</u>") ||
                isTagOpen(before, after, "<ins>", "</ins>")

        val isStrikethrough = isTagOpen(before, after, "<s>", "</s>") ||
                isTagOpen(before, after, "<del>", "</del>") ||
                isTagOpen(before, after, "<strike>", "</strike>") ||
                isMarkdownOpen(before, after, "~~")

        val highlightColor = extractActiveStyleValue(before, after, "background:") ?:
                if (isMarkdownOpen(before, after, "==")) "#FEF08A" else null

        val textColor = extractActiveStyleValue(before, after, "color:")

        val fontSizeTag = extractActiveFontSize(before, after)
        val fontSize = FontSizePreset.fromTag(fontSizeTag)

        val alignmentTag = extractActiveAlignment(before, after)
        val alignment = TextAlignmentPreset.fromTag(alignmentTag)

        return ActiveFormats(
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            isStrikethrough = isStrikethrough,
            highlightColor = highlightColor,
            textColor = textColor,
            fontSize = fontSize,
            alignment = alignment
        )
    }

    private fun isTagOpen(before: String, after: String, open: String, close: String): Boolean {
        val lastOpen = before.lastIndexOf(open, ignoreCase = true)
        val lastClose = before.lastIndexOf(close, ignoreCase = true)
        if (lastOpen != -1 && lastOpen > lastClose) {
            val nextClose = after.indexOf(close, ignoreCase = true)
            return nextClose != -1
        }
        return false
    }

    private fun isMarkdownOpen(before: String, after: String, marker: String): Boolean {
        val countBefore = countOccurrences(before, marker)
        val countAfter = countOccurrences(after, marker)
        return (countBefore % 2 == 1) && (countAfter >= 1)
    }

    private fun isMarkdownSingleStarOpen(before: String, after: String): Boolean {
        // Strip bold markers first to avoid false positives
        val cleanBefore = before.replace("**", "")
        val cleanAfter = after.replace("**", "")
        val countBefore = cleanBefore.count { it == '*' }
        val countAfter = cleanAfter.count { it == '*' }
        return (countBefore % 2 == 1) && (countAfter >= 1)
    }

    private fun countOccurrences(str: String, sub: String): Int {
        var count = 0
        var idx = 0
        while (true) {
            val next = str.indexOf(sub, idx)
            if (next == -1) break
            count++
            idx = next + sub.length
        }
        return count
    }

    private fun extractActiveStyleValue(before: String, after: String, property: String): String? {
        val lastProp = before.lastIndexOf(property, ignoreCase = true)
        if (lastProp != -1) {
            val lastClose = before.lastIndexOf("</span>", ignoreCase = true).coerceAtLeast(before.lastIndexOf("</mark>", ignoreCase = true))
            if (lastProp > lastClose && (after.contains("</span>", ignoreCase = true) || after.contains("</mark>", ignoreCase = true))) {
                val hexRegex = Regex("#[A-Fa-f0-9]{6}")
                val snippet = before.substring(lastProp)
                return hexRegex.find(snippet)?.value
            }
        }
        return null
    }

    private fun extractActiveFontSize(before: String, after: String): String? {
        val lastSpan = before.lastIndexOf("font-size:", ignoreCase = true)
        if (lastSpan != -1) {
            val lastClose = before.lastIndexOf("</span>", ignoreCase = true)
            if (lastSpan > lastClose && after.contains("</span>", ignoreCase = true)) {
                val match = Regex("font-size:([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE).find(before.substring(lastSpan))
                if (match != null) return match.groupValues[1]
            }
        }
        if (isTagOpen(before, after, "<small>", "</small>")) return "small"
        if (isTagOpen(before, after, "<big>", "</big>")) return "large"
        if (isTagOpen(before, after, "<h3>", "</h3>")) return "x-large"
        return null
    }

    private fun extractActiveAlignment(before: String, after: String): String? {
        val lastDiv = before.lastIndexOf("<div align=\"", ignoreCase = true)
        if (lastDiv != -1) {
            val lastClose = before.lastIndexOf("</div>", ignoreCase = true)
            if (lastDiv > lastClose && after.contains("</div>", ignoreCase = true)) {
                val match = Regex("<div align=\"([a-zA-Z]+)\"", RegexOption.IGNORE_CASE).find(before.substring(lastDiv))
                if (match != null) return match.groupValues[1]
            }
        }
        if (isTagOpen(before, after, "<center>", "</center>")) return "center"
        return null
    }
}
