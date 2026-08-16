package com.example

import com.example.util.FontSizePreset
import com.example.util.RichTextRenderer
import com.example.util.TextAlignmentPreset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun testStripHtml() {
        val html = "<p><b>Hello</b> <i>World</i> with <ul><li>item 1</li><li>item 2</li></ul></p>"
        val plain = RichTextRenderer.stripHtml(html)
        assertTrue(plain.contains("Hello World"))
        assertFalse(plain.contains("<b>"))
        assertFalse(plain.contains("</b>"))
        assertFalse(plain.contains("<ul>"))
    }

    @Test
    fun testRendererParsesRichTextWithoutCrashing() {
        val complex = "<b>Bold</b> <i>Italic</i> <u>Underline</u> <s>Strike</s> <mark style=\"background:#BBF7D0\">Highlight</mark> <span style=\"font-size:large\">Big</span> <div align=\"center\">Center</div>"
        val parsed = RichTextRenderer.parseRichText(complex)
        assertTrue(parsed.text.contains("Bold"))
        assertTrue(parsed.text.contains("Underline"))
        assertTrue(parsed.text.contains("Strike"))
        assertTrue(parsed.text.contains("Highlight"))
        assertFalse(parsed.text.contains("<mark"))
        assertFalse(parsed.text.contains("<span"))
    }

    @Test
    fun testRendererHandlesLists() {
        val listHtml = "<ul><li>First</li><li>Second</li></ul>"
        val parsed = RichTextRenderer.parseRichText(listHtml)
        assertTrue(parsed.text.contains("First"))
        assertTrue(parsed.text.contains("Second"))
        assertFalse(parsed.text.contains("<li>"))
    }
}
