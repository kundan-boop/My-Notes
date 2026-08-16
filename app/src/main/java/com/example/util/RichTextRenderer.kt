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
     * Strips all HTML tags and Markdown markers to return clean, readable plain text.
     */
    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        return try {
            HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]+>"), "").trim()
        }
    }

    /**
     * Parses HTML / Markdown rich content into Jetpack Compose AnnotatedString for display
     * in NoteCard, detail previews, or search results.
     */
    fun parseRichText(content: String, defaultColor: Color = Color.Unspecified): AnnotatedString {
        if (content.isBlank()) return AnnotatedString("")

        // Fast path for simple plain text
        if (!content.contains("<") && !content.contains("*") && !content.contains("~~") && !content.contains("==")) {
            return AnnotatedString(content)
        }

        return try {
            buildRichAnnotatedString(content, defaultColor)
        } catch (e: Exception) {
            AnnotatedString(stripHtml(content))
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

            while (i < len) {
                // Check HTML tags
                if (input[i] == '<') {
                    val tagEnd = input.indexOf('>', i)
                    if (tagEnd != -1) {
                        val tagFull = input.substring(i, tagEnd + 1)
                        val tagLower = tagFull.lowercase()

                        // Handle line breaks
                        if (tagLower == "<br>" || tagLower == "<br/>" || tagLower == "<br />") {
                            append("\n")
                            i = tagEnd + 1
                            continue
                        }

                        // Handle list items
                        if (tagLower == "<li>") {
                            append("• ")
                            i = tagEnd + 1
                            continue
                        }

                        if (tagLower == "</li>" || tagLower == "</ul>" || tagLower == "</ol>" || tagLower == "</div>" || tagLower == "</p>") {
                            if (tagLower != "</div>" && tagLower != "</p>") {
                                append("\n")
                            }
                            if (styleStack.size > 1 && (tagLower == "</div>" || tagLower == "</p>")) {
                                styleStack.removeLast()
                            }
                            i = tagEnd + 1
                            continue
                        }

                        // Closing tag
                        if (tagLower.startsWith("</")) {
                            if (styleStack.size > 1) {
                                styleStack.removeLast()
                            }
                            i = tagEnd + 1
                            continue
                        }

                        // Opening tags
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
                        } else if (tagLower.startsWith("<div") || tagLower.startsWith("<p")) {
                            isRecognized = true
                        } else if (tagLower.startsWith("<ul") || tagLower.startsWith("<ol")) {
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
