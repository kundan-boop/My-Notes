package com.example.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.util.ActiveFormats
import com.example.util.FontSizePreset
import com.example.util.TextAlignmentPreset

class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : EditText(context, attrs, defStyleAttr) {

    var onSelectionChangedListener: ((start: Int, end: Int) -> Unit)? = null
    var onContentHtmlChanged: ((html: String) -> Unit)? = null

    // Pending formatting for toggle-then-type
    var pendingBold = false
    var pendingItalic = false
    var pendingUnderline = false
    var pendingStrikethrough = false
    var pendingTextColor: Int? = null
    var pendingHighlightColor: Int? = null
    var pendingFontSize: Float? = null

    private var isFormatting = false

    init {
        background = null
        gravity = Gravity.TOP or Gravity.START
        setPadding(0, 0, 0, 120)
        textSize = 16f
        includeFontPadding = false

        addTextChangedListener(object : TextWatcher {
            private var startPos = 0
            private var countAdded = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                startPos = start
                countAdded = after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return

                try {
                    if (countAdded > 0) {
                        val safeStart = startPos.coerceIn(0, s.length)
                        val safeEnd = (startPos + countAdded).coerceIn(safeStart, s.length)
                        if (safeStart < safeEnd) {
                            applyPendingStyles(s, safeStart, safeEnd)

                            val insertedText = s.subSequence(safeStart, safeEnd).toString()
                            if (insertedText.contains('\n')) {
                                val newlineRel = insertedText.indexOf('\n')
                                val newlineIndex = (safeStart + newlineRel).coerceIn(0, s.length)

                                if (newlineIndex > 0) {
                                    val searchBefore = newlineIndex - 1
                                    val prevNewline = if (searchBefore > 0) s.lastIndexOf('\n', searchBefore - 1) else -1
                                    val prevLineStart = if (prevNewline == -1) 0 else (prevNewline + 1).coerceIn(0, newlineIndex)

                                    if (prevLineStart <= newlineIndex && prevLineStart >= 0 && newlineIndex <= s.length) {
                                        val prevLineText = s.subSequence(prevLineStart, newlineIndex).toString().trimEnd()

                                        val bulletMatch = Regex("^([•▪■✦◆▸➢○\\-*▲★➜➔➝▣❏◀◄❖⮞☽✣✹⤢⇨◈✾❂✓➞❐❑]|->|=>)\\s*").find(prevLineText)
                                        if (bulletMatch != null) {
                                            val bulletSymbol = bulletMatch.groupValues[1]
                                            val contentWithoutBullet = prevLineText.removePrefix(bulletSymbol).trim()
                                            if (contentWithoutBullet.isEmpty()) {
                                                isFormatting = true
                                                val delEnd = (newlineIndex + 1).coerceIn(prevLineStart, s.length)
                                                s.delete(prevLineStart, delEnd)
                                                isFormatting = false
                                                setSelection(prevLineStart.coerceIn(0, s.length))
                                            } else {
                                                val nextBullet = "$bulletSymbol "
                                                isFormatting = true
                                                val insPos = (newlineIndex + 1).coerceIn(0, s.length)
                                                s.insert(insPos, nextBullet)
                                                isFormatting = false
                                                setSelection((insPos + nextBullet.length).coerceIn(0, s.length))
                                            }
                                        } else {
                                            val numberedMatch = Regex("^(\\d+)\\.\\s*").find(prevLineText)
                                            val alphaMatch = Regex("^([a-z])\\.\\s*").find(prevLineText)
                                            if (numberedMatch != null) {
                                                val numStr = numberedMatch.groupValues[1]
                                                val contentWithoutNum = prevLineText.removePrefix("$numStr.").trim()
                                                if (contentWithoutNum.isEmpty()) {
                                                    isFormatting = true
                                                    val delEnd = (newlineIndex + 1).coerceIn(prevLineStart, s.length)
                                                    s.delete(prevLineStart, delEnd)
                                                    isFormatting = false
                                                    setSelection(prevLineStart.coerceIn(0, s.length))
                                                } else {
                                                    val nextNum = (numStr.toLongOrNull() ?: 1L) + 1
                                                    val nextNumStr = "$nextNum. "
                                                    isFormatting = true
                                                    val insPos = (newlineIndex + 1).coerceIn(0, s.length)
                                                    s.insert(insPos, nextNumStr)
                                                    isFormatting = false
                                                    setSelection((insPos + nextNumStr.length).coerceIn(0, s.length))
                                                }
                                            } else if (alphaMatch != null) {
                                                val charStr = alphaMatch.groupValues[1]
                                                val contentWithoutAlpha = prevLineText.removePrefix("$charStr.").trim()
                                                if (contentWithoutAlpha.isEmpty()) {
                                                    isFormatting = true
                                                    val delEnd = (newlineIndex + 1).coerceIn(prevLineStart, s.length)
                                                    s.delete(prevLineStart, delEnd)
                                                    isFormatting = false
                                                    setSelection(prevLineStart.coerceIn(0, s.length))
                                                } else {
                                                    val nextChar = if (charStr[0] < 'z') (charStr[0] + 1) else 'z'
                                                    val nextStr = "$nextChar. "
                                                    isFormatting = true
                                                    val insPos = (newlineIndex + 1).coerceIn(0, s.length)
                                                    s.insert(insPos, nextStr)
                                                    isFormatting = false
                                                    setSelection((insPos + nextStr.length).coerceIn(0, s.length))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NativeRichTextEditor", "Error processing text change", e)
                } finally {
                    isFormatting = false
                }

                notifyHtmlChanged()
            }
        })
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }

    private fun applyPendingStyles(editable: Editable, start: Int, end: Int) {
        if (start >= end || end > editable.length) return
        isFormatting = true
        try {
            if (pendingBold) {
                editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (pendingItalic) {
                editable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (pendingUnderline) {
                editable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (pendingStrikethrough) {
                editable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            pendingTextColor?.let { color ->
                editable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            pendingHighlightColor?.let { color ->
                editable.setSpan(BackgroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            pendingFontSize?.let { size ->
                editable.setSpan(RelativeSizeSpan(size), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } finally {
            isFormatting = false
        }
    }

    fun notifyHtmlChanged() {
        val s = text ?: return
        val html = try {
            HtmlCompat.toHtml(s, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                .trim()
                .removePrefix("<div>").removeSuffix("</div>")
                .removePrefix("<p dir=\"ltr\">").removeSuffix("</p>")
                .trim()
        } catch (e: Exception) {
            s.toString()
        }
        onContentHtmlChanged?.invoke(html)
    }

    fun setHtml(html: String) {
        if (html.isBlank()) {
            if (!text.isNullOrEmpty()) {
                setText("")
            }
            return
        }
        isFormatting = true
        try {
            val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            val builder = SpannableStringBuilder(spanned)
            // Trim extra trailing newlines added by HtmlCompat
            while (builder.endsWith("\n")) {
                builder.delete(builder.length - 1, builder.length)
            }
            setText(builder)
            setSelection(builder.length)
        } catch (e: Exception) {
            setText(html)
        } finally {
            isFormatting = false
        }
    }

    fun toggleBold() {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val boldSpans = s.getSpans(selStart, selEnd, StyleSpan::class.java).filter { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
            if (boldSpans.isNotEmpty()) {
                boldSpans.forEach { s.removeSpan(it) }
            } else {
                s.setSpan(StyleSpan(Typeface.BOLD), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            notifyHtmlChanged()
        } else {
            pendingBold = !pendingBold
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun toggleItalic() {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, StyleSpan::class.java).filter { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
            if (spans.isNotEmpty()) {
                spans.forEach { s.removeSpan(it) }
            } else {
                s.setSpan(StyleSpan(Typeface.ITALIC), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            notifyHtmlChanged()
        } else {
            pendingItalic = !pendingItalic
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun toggleUnderline() {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, UnderlineSpan::class.java)
            if (spans.isNotEmpty()) {
                spans.forEach { s.removeSpan(it) }
            } else {
                s.setSpan(UnderlineSpan(), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            notifyHtmlChanged()
        } else {
            pendingUnderline = !pendingUnderline
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun toggleStrikethrough() {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, StrikethroughSpan::class.java)
            if (spans.isNotEmpty()) {
                spans.forEach { s.removeSpan(it) }
            } else {
                s.setSpan(StrikethroughSpan(), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            notifyHtmlChanged()
        } else {
            pendingStrikethrough = !pendingStrikethrough
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun setHighlight(hexColor: String) {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return
        val colorInt = android.graphics.Color.parseColor(hexColor)

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, BackgroundColorSpan::class.java)
            spans.forEach { s.removeSpan(it) }
            s.setSpan(BackgroundColorSpan(colorInt), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            notifyHtmlChanged()
        } else {
            pendingHighlightColor = colorInt
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun setTextColorHex(hexColor: String) {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return
        val colorInt = android.graphics.Color.parseColor(hexColor)

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, ForegroundColorSpan::class.java)
            spans.forEach { s.removeSpan(it) }
            s.setSpan(ForegroundColorSpan(colorInt), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            notifyHtmlChanged()
        } else {
            pendingTextColor = colorInt
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun setFontSizePreset(preset: FontSizePreset) {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return
        val relativeSize = when (preset) {
            FontSizePreset.SMALL -> 0.8f
            FontSizePreset.NORMAL -> 1.0f
            FontSizePreset.MEDIUM -> 1.15f
            FontSizePreset.LARGE -> 1.25f
            FontSizePreset.EXTRA_LARGE -> 1.5f
            FontSizePreset.ELEPHANT -> 2.0f
        }

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val spans = s.getSpans(selStart, selEnd, RelativeSizeSpan::class.java)
            spans.forEach { s.removeSpan(it) }
            if (preset != FontSizePreset.NORMAL) {
                s.setSpan(RelativeSizeSpan(relativeSize), selStart, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            notifyHtmlChanged()
        } else {
            pendingFontSize = if (preset == FontSizePreset.NORMAL) null else relativeSize
        }
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun setAlignmentPreset(preset: TextAlignmentPreset) {
        val selStart = selectionStart
        val selEnd = selectionEnd
        val s = text ?: return

        val align = when (preset) {
            TextAlignmentPreset.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignmentPreset.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            TextAlignmentPreset.LEFT -> Layout.Alignment.ALIGN_NORMAL
        }

        val spans = s.getSpans(selStart, selEnd, AlignmentSpan.Standard::class.java)
        spans.forEach { s.removeSpan(it) }
        s.setSpan(AlignmentSpan.Standard(align), selStart, selEnd, Spanned.SPAN_PARAGRAPH)
        notifyHtmlChanged()
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun insertBulletList(marker: String = "•") {
        val s = text ?: return
        val selStart = selectionStart.coerceIn(0, s.length)
        val lineStart = if (selStart <= 0) 0 else {
            val searchPos = (selStart - 1).coerceIn(0, (s.length - 1).coerceAtLeast(0))
            val prevNl = s.lastIndexOf('\n', searchPos)
            if (prevNl == -1) 0 else (prevNl + 1).coerceIn(0, s.length)
        }
        val bulletStr = "$marker "
        s.insert(lineStart, bulletStr)
        setSelection((selStart + bulletStr.length).coerceIn(0, s.length))
        notifyHtmlChanged()
    }

    fun insertNumberedList(prefix: String = "1.") {
        val s = text ?: return
        val selStart = selectionStart.coerceIn(0, s.length)
        val lineStart = if (selStart <= 0) 0 else {
            val searchPos = (selStart - 1).coerceIn(0, (s.length - 1).coerceAtLeast(0))
            val prevNl = s.lastIndexOf('\n', searchPos)
            if (prevNl == -1) 0 else (prevNl + 1).coerceIn(0, s.length)
        }
        val numStr = "$prefix "
        s.insert(lineStart, numStr)
        setSelection((selStart + numStr.length).coerceIn(0, s.length))
        notifyHtmlChanged()
    }

    fun clearFormatting() {
        val s = text ?: return
        val selStart = selectionStart.coerceIn(0, s.length)
        val selEnd = selectionEnd.coerceIn(0, s.length)

        if (selStart != selEnd && selStart >= 0 && selEnd <= s.length) {
            val start = minOf(selStart, selEnd)
            val end = maxOf(selStart, selEnd)
            val allSpans = s.getSpans(start, end, Any::class.java)
            allSpans.forEach { span ->
                if (span is StyleSpan || span is UnderlineSpan || span is StrikethroughSpan ||
                    span is ForegroundColorSpan || span is BackgroundColorSpan ||
                    span is RelativeSizeSpan || span is AlignmentSpan) {
                    s.removeSpan(span)
                }
            }
            notifyHtmlChanged()
        }
        pendingBold = false
        pendingItalic = false
        pendingUnderline = false
        pendingStrikethrough = false
        pendingTextColor = null
        pendingHighlightColor = null
        pendingFontSize = null
        onSelectionChanged(selectionStart, selectionEnd)
    }

    fun insertTextAtCursor(textToInsert: String) {
        val s = text ?: return
        val selStart = selectionStart.coerceIn(0, s.length)
        val selEnd = selectionEnd.coerceIn(selStart, s.length)
        s.replace(selStart, selEnd, textToInsert)
        setSelection((selStart + textToInsert.length).coerceIn(0, s.length))
        notifyHtmlChanged()
    }
}

class NativeRichTextEditorState(initialHtml: String = "") {
    var html by mutableStateOf(initialHtml)
        internal set

    var activeFormats by mutableStateOf(ActiveFormats())
        internal set

    var canUndo by mutableStateOf(false)
        internal set

    var canRedo by mutableStateOf(false)
        internal set

    private var history = mutableListOf<String>()
    private var historyIndex = -1
    private var isNavigatingHistory = false

    internal var editTextRef: RichEditText? = null

    fun pushHistory(newHtml: String) {
        if (isNavigatingHistory) return
        if (historyIndex >= 0 && historyIndex < history.size && history[historyIndex] == newHtml) return

        if (historyIndex < history.size - 1) {
            history = history.subList(0, historyIndex + 1).toMutableList()
        }
        history.add(newHtml)
        if (history.size > 50) history.removeAt(0)
        historyIndex = history.size - 1
        updateUndoRedo()
    }

    private fun updateUndoRedo() {
        canUndo = historyIndex > 0
        canRedo = historyIndex < history.size - 1
    }

    fun undo() {
        if (canUndo) {
            isNavigatingHistory = true
            historyIndex--
            val prev = history[historyIndex]
            html = prev
            editTextRef?.setHtml(prev)
            updateUndoRedo()
            isNavigatingHistory = false
        }
    }

    fun redo() {
        if (canRedo) {
            isNavigatingHistory = true
            historyIndex++
            val next = history[historyIndex]
            html = next
            editTextRef?.setHtml(next)
            updateUndoRedo()
            isNavigatingHistory = false
        }
    }

    fun toggleBold() = editTextRef?.toggleBold()
    fun toggleItalic() = editTextRef?.toggleItalic()
    fun toggleUnderline() = editTextRef?.toggleUnderline()
    fun toggleStrikethrough() = editTextRef?.toggleStrikethrough()
    fun setFontSize(preset: FontSizePreset) = editTextRef?.setFontSizePreset(preset)
    fun setAlignment(alignment: TextAlignmentPreset) = editTextRef?.setAlignmentPreset(alignment)
    fun setHighlight(hex: String) = editTextRef?.setHighlight(hex)
    fun setTextColor(hex: String) = editTextRef?.setTextColorHex(hex)
    fun toggleBulletList(marker: String = "•") = editTextRef?.insertBulletList(marker)
    fun toggleNumberedList(prefix: String = "1.") = editTextRef?.insertNumberedList(prefix)
    fun clearFormatting() = editTextRef?.clearFormatting()
    fun insertText(text: String) = editTextRef?.insertTextAtCursor(text)

    fun setHtmlContent(newHtml: String) {
        if (html != newHtml) {
            html = newHtml
            editTextRef?.setHtml(newHtml)
            pushHistory(newHtml)
        }
    }
}

@Composable
fun rememberNativeRichTextEditorState(initialHtml: String = ""): NativeRichTextEditorState {
    return remember { NativeRichTextEditorState(initialHtml) }
}

@Composable
fun NativeRichTextEditor(
    state: NativeRichTextEditorState,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    placeholder: String = "Note content..."
) {
    val textColorArgb = textColor.toArgb()
    val hintColorArgb = textColor.copy(alpha = 0.4f).toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        factory = { ctx ->
            RichEditText(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                hint = placeholder
                setHintTextColor(hintColorArgb)
                setTextColor(textColorArgb)

                onContentHtmlChanged = { newHtml ->
                    state.html = newHtml
                    state.pushHistory(newHtml)
                }

                onSelectionChangedListener = { start, end ->
                    val s = text
                    if (s != null) {
                        val isBold = if (start != end && start >= 0 && end <= s.length) {
                            s.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
                        } else pendingBold

                        val isItalic = if (start != end && start >= 0 && end <= s.length) {
                            s.getSpans(start, end, StyleSpan::class.java).any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
                        } else pendingItalic

                        val isUnderline = if (start != end && start >= 0 && end <= s.length) {
                            s.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
                        } else pendingUnderline

                        val isStrike = if (start != end && start >= 0 && end <= s.length) {
                            s.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()
                        } else pendingStrikethrough

                        state.activeFormats = ActiveFormats(
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrike
                        )
                    }
                }

                state.editTextRef = this
                setHtml(state.html)
            }
        },
        update = { editText ->
            state.editTextRef = editText
            editText.setTextColor(textColorArgb)
            editText.setHintTextColor(hintColorArgb)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            state.editTextRef = null
        }
    }
}
