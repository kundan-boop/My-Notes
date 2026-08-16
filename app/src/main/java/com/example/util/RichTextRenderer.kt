package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object RichTextRenderer {

    /**
     * Parses Markdown and HTML tags commonly produced by RichTextToolbar:
     * - **bold** or <b>bold</b>
     * - *italic* or <i>italic</i>
     * - ==highlight== or <mark style="background:#HEX">text</mark>
     * - <span style="color:#HEX">text</span>
     */
    fun parseRichText(content: String, defaultColor: Color = Color.Unspecified): AnnotatedString {
        if (content.isBlank()) return AnnotatedString("")

        // Fast path: plain text without rich markers
        if (!content.contains("*") && !content.contains("<") && !content.contains("==")) {
            return AnnotatedString(content)
        }

        return try {
            buildRichAnnotatedString(content, defaultColor)
        } catch (e: Exception) {
            AnnotatedString(content)
        }
    }

    private fun buildRichAnnotatedString(input: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            val len = input.length

            while (i < len) {
                // Check for bold **text**
                if (i + 1 < len && input[i] == '*' && input[i + 1] == '*') {
                    val end = input.indexOf("**", i + 2)
                    if (end != -1) {
                        val inner = input.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(inner)
                        }
                        i = end + 2
                        continue
                    }
                }

                // Check for italic *text*
                if (input[i] == '*' && (i == 0 || input[i - 1] != '*') && (i + 1 < len && input[i + 1] != '*')) {
                    val end = input.indexOf('*', i + 1)
                    if (end != -1 && (end + 1 >= len || input[end + 1] != '*')) {
                        val inner = input.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(inner)
                        }
                        i = end + 1
                        continue
                    }
                }

                // Check for highlight ==text==
                if (i + 1 < len && input[i] == '=' && input[i + 1] == '=') {
                    val end = input.indexOf("==", i + 2)
                    if (end != -1) {
                        val inner = input.substring(i + 2, end)
                        withStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF1E293B))) {
                            append(inner)
                        }
                        i = end + 2
                        continue
                    }
                }

                // Check for <mark style="background:#HEX">text</mark>
                if (input.startsWith("<mark", i)) {
                    val tagEnd = input.indexOf('>', i)
                    val markClose = input.indexOf("</mark>", tagEnd)
                    if (tagEnd != -1 && markClose != -1) {
                        val tag = input.substring(i, tagEnd)
                        val colorHex = extractColorHex(tag) ?: "#FEF08A"
                        val inner = input.substring(tagEnd + 1, markClose)
                        val bg = parseHexColor(colorHex, Color(0xFFFEF08A))
                        withStyle(SpanStyle(background = bg, color = Color(0xFF1E293B))) {
                            append(inner)
                        }
                        i = markClose + 7
                        continue
                    }
                }

                // Check for <span style="color:#HEX">text</span>
                if (input.startsWith("<span", i)) {
                    val tagEnd = input.indexOf('>', i)
                    val spanClose = input.indexOf("</span>", tagEnd)
                    if (tagEnd != -1 && spanClose != -1) {
                        val tag = input.substring(i, tagEnd)
                        val colorHex = extractColorHex(tag) ?: "#EF4444"
                        val inner = input.substring(tagEnd + 1, spanClose)
                        val fg = parseHexColor(colorHex, defaultColor)
                        withStyle(SpanStyle(color = fg)) {
                            append(inner)
                        }
                        i = spanClose + 7
                        continue
                    }
                }

                // Check for <b>text</b>
                if (input.startsWith("<b>", i)) {
                    val end = input.indexOf("</b>", i + 3)
                    if (end != -1) {
                        val inner = input.substring(i + 3, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(inner)
                        }
                        i = end + 4
                        continue
                    }
                }

                // Check for <i>text</i>
                if (input.startsWith("<i>", i)) {
                    val end = input.indexOf("</i>", i + 3)
                    if (end != -1) {
                        val inner = input.substring(i + 3, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(inner)
                        }
                        i = end + 4
                        continue
                    }
                }

                // Append regular character
                append(input[i])
                i++
            }
        }
    }

    private fun extractColorHex(tag: String): String? {
        val regex = Regex("#[A-Fa-f0-9]{6}")
        return regex.find(tag)?.value
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
