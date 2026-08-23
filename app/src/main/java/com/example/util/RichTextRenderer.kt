package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat

object RichTextRenderer {

    /**
     * Strips all HTML tags and decodes HTML entities to return clean, readable plain text.
     */
    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        val decoded = decodeHtmlEntities(html)
        return try {
            HtmlCompat.fromHtml(decoded, HtmlCompat.FROM_HTML_MODE_COMPACT)
                .toString()
                .replace(Regex("\n{2,}"), "\n")
                .trim()
        } catch (e: Exception) {
            decoded.replace(Regex("<[^>]+>"), "").replace(Regex("\n{2,}"), "\n").trim()
        }
    }

    /**
     * Decodes common named HTML entities and all numeric decimal (&#8226;) and hex (&#x2022;) entities.
     */
    fun decodeHtmlEntities(text: String): String {
        if (!text.contains("&")) return text
        var decoded = text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&bull;", "•")
            .replace("&check;", "✓")
            .replace("&rarr;", "➔")
            .replace("&larr;", "◄")

        // Replace numeric decimal entities like &#8226; or &#10003;
        if (decoded.contains("&#")) {
            decoded = Regex("&#([0-9]+);").replace(decoded) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null && code in 1..0x10FFFF) {
                    try {
                        Character.toChars(code).concatToString()
                    } catch (e: Exception) {
                        match.value
                    }
                } else match.value
            }
            // Replace numeric hex entities like &#x2022;
            decoded = Regex("&#x([0-9a-fA-F]+);").replace(decoded) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                if (code != null && code in 1..0x10FFFF) {
                    try {
                        Character.toChars(code).concatToString()
                    } catch (e: Exception) {
                        match.value
                    }
                } else match.value
            }
        }
        return decoded
    }

    /**
     * Parses HTML / Markdown rich content into Jetpack Compose AnnotatedString for display
     * in NoteCard, detail previews, or search results.
     */
    fun parseRichText(content: String, defaultColor: Color = Color.Unspecified): AnnotatedString {
        if (content.isBlank()) return AnnotatedString("")

        val cleanContent = decodeHtmlEntities(content)

        // Fast path for simple plain text with no tags or entities
        if (!cleanContent.contains("<") && !cleanContent.contains("&")) {
            return AnnotatedString(cleanContent.trim())
        }

        return try {
            buildRichAnnotatedString(cleanContent, defaultColor)
        } catch (e: Exception) {
            AnnotatedString(stripHtml(cleanContent))
        }
    }

    private data class StyleState(
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false,
        val isStrikethrough: Boolean = false,
        val textColor: Color? = null,
        val backgroundColor: Color? = null,
        val fontSize: Float? = null
    ) {
        fun toSpanStyle(defaultColor: Color): SpanStyle {
            val decs = mutableListOf<TextDecoration>()
            if (isUnderline) decs.add(TextDecoration.Underline)
            if (isStrikethrough) decs.add(TextDecoration.LineThrough)
            val decoration = when {
                decs.size == 2 -> TextDecoration.combine(decs)
                decs.size == 1 -> decs[0]
                else -> TextDecoration.None
            }

            return SpanStyle(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = decoration,
                color = textColor ?: defaultColor,
                background = backgroundColor ?: Color.Transparent,
                fontSize = if (fontSize != null) fontSize.sp else androidx.compose.ui.unit.TextUnit.Unspecified
            )
        }
    }

    private fun buildRichAnnotatedString(input: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = input.length

            val styleStack = ArrayDeque<StyleState>()
            styleStack.addLast(StyleState())

            fun currentStyle(): StyleState = styleStack.last()

            fun ensureNewline() {
                if (length > 0 && this.toAnnotatedString().text.last() != '\n') {
                    append("\n")
                }
            }

            while (i < len) {
                // Check HTML tags
                if (input[i] == '<') {
                    val tagEnd = input.indexOf('>', i)
                    if (tagEnd != -1) {
                        val tagFull = input.substring(i, tagEnd + 1)
                        val tagLower = tagFull.lowercase()

                        // Handle line breaks
                        if (tagLower == "<br>" || tagLower == "<br/>" || tagLower == "<br />") {
                            ensureNewline()
                            i = tagEnd + 1
                            continue
                        }

                        // Handle opening list items
                        if (tagLower.startsWith("<li")) {
                            ensureNewline()
                            append("• ")
                            i = tagEnd + 1
                            continue
                        }

                        // Handle block element closing tags
                        if (tagLower.startsWith("</li") || tagLower.startsWith("</p") || tagLower.startsWith("</div") || tagLower.startsWith("</ul") || tagLower.startsWith("</ol")) {
                            ensureNewline()
                            if (styleStack.size > 1 && (tagLower.startsWith("</div") || tagLower.startsWith("</p"))) {
                                styleStack.removeLast()
                            }
                            i = tagEnd + 1
                            continue
                        }

                        // Opening block tags
                        if (tagLower.startsWith("<p") || tagLower.startsWith("<div") || tagLower.startsWith("<ul") || tagLower.startsWith("<ol")) {
                            ensureNewline()
                            styleStack.addLast(currentStyle())
                            i = tagEnd + 1
                            continue
                        }

                        // Closing inline tag
                        if (tagLower.startsWith("</")) {
                            if (styleStack.size > 1) {
                                styleStack.removeLast()
                            }
                            i = tagEnd + 1
                            continue
                        }

                        // Opening formatting tags
                        var newStyle = currentStyle()
                        var isRecognized = false

                        if (tagLower.startsWith("<b") || tagLower.startsWith("<strong")) {
                            newStyle = newStyle.copy(isBold = true)
                            isRecognized = true
                        } else if (tagLower.startsWith("<i") || tagLower.startsWith("<em")) {
                            newStyle = newStyle.copy(isItalic = true)
                            isRecognized = true
                        } else if (tagLower.startsWith("<u") || tagLower.startsWith("<ins")) {
                            newStyle = newStyle.copy(isUnderline = true)
                            isRecognized = true
                        } else if (tagLower.startsWith("<s") || tagLower.startsWith("<del") || tagLower.startsWith("<strike")) {
                            newStyle = newStyle.copy(isStrikethrough = true)
                            isRecognized = true
                        } else if (tagLower.startsWith("<mark")) {
                            val colorHex = extractColorHex(tagFull) ?: "#FEF08A"
                            newStyle = newStyle.copy(
                                backgroundColor = parseHexColor(colorHex, Color(0xFFFEF08A)),
                                textColor = Color(0xFF1E293B)
                            )
                            isRecognized = true
                        } else if (tagLower.startsWith("<font")) {
                            var fontStyle = newStyle
                            if (tagLower.contains("color=")) {
                                val colorHex = extractColorHex(tagFull)
                                if (colorHex != null) {
                                    fontStyle = fontStyle.copy(textColor = parseHexColor(colorHex, defaultColor))
                                }
                            }
                            if (tagLower.contains("size=")) {
                                val fontSize = extractFontSize(tagFull)
                                if (fontSize != null) {
                                    fontStyle = fontStyle.copy(fontSize = fontSize)
                                }
                            }
                            newStyle = fontStyle
                            isRecognized = true
                        } else if (tagLower.startsWith("<span")) {
                            var spanStyle = newStyle
                            if (tagLower.contains("color:")) {
                                val colorHex = extractColorHex(tagFull) ?: "#EF4444"
                                spanStyle = spanStyle.copy(textColor = parseHexColor(colorHex, defaultColor))
                            }
                            if (tagLower.contains("background")) {
                                val colorHex = extractColorHex(tagFull) ?: "#FEF08A"
                                spanStyle = spanStyle.copy(backgroundColor = parseHexColor(colorHex, Color(0xFFFEF08A)))
                            }
                            if (tagLower.contains("font-size:")) {
                                val fontSize = extractFontSize(tagFull)
                                if (fontSize != null) {
                                    spanStyle = spanStyle.copy(fontSize = fontSize)
                                }
                            }
                            newStyle = spanStyle
                            isRecognized = true
                        }

                        if (isRecognized) {
                            styleStack.addLast(newStyle)
                            i = tagEnd + 1
                            continue
                        } else {
                            // Skip any unhandled tag without displaying tag chars
                            i = tagEnd + 1
                            continue
                        }
                    }
                }

                // Append normal character with current style
                val char = input[i]
                val style = currentStyle()
                withStyle(style.toSpanStyle(defaultColor)) {
                    append(char)
                }
                i++
            }
        }
    }

    private fun extractColorHex(tag: String): String? {
        val regex = Regex("#[A-Fa-f0-9]{6}")
        return regex.find(tag)?.value
    }

    private fun extractFontSize(tag: String): Float? {
        return when {
            tag.contains("size=\"1\"", ignoreCase = true) || tag.contains("font-size:small", ignoreCase = true) -> 12f
            tag.contains("size=\"5\"", ignoreCase = true) || tag.contains("font-size:large", ignoreCase = true) -> 20f
            tag.contains("size=\"6\"", ignoreCase = true) || tag.contains("font-size:x-large", ignoreCase = true) -> 24f
            tag.contains("size=\"3\"", ignoreCase = true) || tag.contains("font-size:normal", ignoreCase = true) -> 16f
            else -> null
        }
    }

    private fun parseHexColor(hex: String, fallback: Color): Color {
        return try {
            val colorLong = java.lang.Long.parseLong(hex.removePrefix("#"), 16)
            Color((0xFF000000 or colorLong).toInt())
        } catch (e: Exception) {
            fallback
        }
    }
}
