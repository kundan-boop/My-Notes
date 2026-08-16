package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.ActiveFormats
import com.example.util.FontSizePreset
import com.example.util.TextAlignmentPreset

class RichTextEditorState(initialHtml: String = "") {
    var html by mutableStateOf(initialHtml)
        internal set

    var activeFormats by mutableStateOf(ActiveFormats())
        internal set

    var canUndo by mutableStateOf(false)
        internal set

    var canRedo by mutableStateOf(false)
        internal set

    internal var webViewRef: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runJs(js: String) {
        mainHandler.post {
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    fun toggleBold() = runJs("window.editorApi && window.editorApi.toggleBold();")
    fun toggleItalic() = runJs("window.editorApi && window.editorApi.toggleItalic();")
    fun toggleUnderline() = runJs("window.editorApi && window.editorApi.toggleUnderline();")
    fun toggleStrikethrough() = runJs("window.editorApi && window.editorApi.toggleStrikethrough();")
    fun toggleBulletList() = runJs("window.editorApi && window.editorApi.toggleBulletList();")
    fun toggleNumberedList() = runJs("window.editorApi && window.editorApi.toggleNumberedList();")
    
    fun setFontSize(preset: FontSizePreset) {
        val sizeVal = when (preset) {
            FontSizePreset.SMALL -> "1"
            FontSizePreset.NORMAL -> "3"
            FontSizePreset.LARGE -> "5"
            FontSizePreset.EXTRA_LARGE -> "6"
        }
        runJs("window.editorApi && window.editorApi.setFontSize('$sizeVal');")
    }

    fun setAlignment(alignment: TextAlignmentPreset) {
        val alignCmd = when (alignment) {
            TextAlignmentPreset.CENTER -> "justifyCenter"
            TextAlignmentPreset.RIGHT -> "justifyRight"
            TextAlignmentPreset.LEFT -> "justifyLeft"
        }
        runJs("window.editorApi && window.editorApi.setAlignment('$alignCmd');")
    }

    fun setHighlight(hexColor: String) {
        runJs("window.editorApi && window.editorApi.setHighlight('$hexColor');")
    }

    fun setTextColor(hexColor: String) {
        runJs("window.editorApi && window.editorApi.setTextColor('$hexColor');")
    }

    fun clearFormatting() = runJs("window.editorApi && window.editorApi.clearFormatting();")
    fun undo() = runJs("window.editorApi && window.editorApi.undo();")
    fun redo() = runJs("window.editorApi && window.editorApi.redo();")

    fun insertText(text: String) {
        val escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
        runJs("window.editorApi && window.editorApi.insertText('$escaped');")
    }

    fun setHtmlContent(newHtml: String) {
        html = newHtml
        val escaped = newHtml.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "")
        runJs("window.editorApi && window.editorApi.setHtml('$escaped');")
    }

    fun updateColors(textColorHex: String, placeholderColorHex: String) {
        runJs("window.editorApi && window.editorApi.setThemeColors('$textColorHex', '$placeholderColorHex');")
    }
}

@Composable
fun rememberRichTextEditorState(initialHtml: String = ""): RichTextEditorState {
    return remember { RichTextEditorState(initialHtml) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichTextEditorWebView(
    state: RichTextEditorState,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    placeholderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    placeholder: String = "Note content..."
) {
    val context = LocalContext.current
    val textColorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
    val placeholderColorHex = String.format("#%06X", 0xFFFFFF and placeholderColor.toArgb())

    val jsBridge = remember(state) {
        object {
            @JavascriptInterface
            fun onContentChanged(newHtml: String) {
                Handler(Looper.getMainLooper()).post {
                    state.html = newHtml
                }
            }

            @JavascriptInterface
            fun onStateChanged(
                isBold: Boolean,
                isItalic: Boolean,
                isUnderline: Boolean,
                isStrike: Boolean,
                isBullet: Boolean,
                isNumbered: Boolean,
                canUndo: Boolean,
                canRedo: Boolean,
                align: String,
                fontSizeVal: String,
                fontColor: String,
                highlightColor: String
            ) {
                Handler(Looper.getMainLooper()).post {
                    val fontPreset = when (fontSizeVal) {
                        "1", "small" -> FontSizePreset.SMALL
                        "5", "large" -> FontSizePreset.LARGE
                        "6", "7", "x-large" -> FontSizePreset.EXTRA_LARGE
                        else -> FontSizePreset.NORMAL
                    }

                    val alignPreset = when (align.lowercase()) {
                        "center" -> TextAlignmentPreset.CENTER
                        "right" -> TextAlignmentPreset.RIGHT
                        else -> TextAlignmentPreset.LEFT
                    }

                    state.activeFormats = ActiveFormats(
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline,
                        isStrikethrough = isStrike,
                        highlightColor = if (highlightColor.isNotBlank() && highlightColor != "none") highlightColor else null,
                        textColor = if (fontColor.isNotBlank()) fontColor else null,
                        fontSize = fontPreset,
                        alignment = alignPreset
                    )
                    state.canUndo = canUndo
                    state.canRedo = canRedo
                }
            }
        }
    }

    LaunchedEffect(textColorHex, placeholderColorHex) {
        state.updateColors(textColorHex, placeholderColorHex)
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(0x00000000) // Transparent background
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = false
                    loadWithOverviewMode = true
                    defaultFontSize = 16
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        state.setHtmlContent(state.html)
                        state.updateColors(textColorHex, placeholderColorHex)
                    }
                }

                addJavascriptInterface(jsBridge, "AndroidBridge")
                state.webViewRef = this

                val htmlData = buildEditorHtml(placeholder, textColorHex, placeholderColorHex)
                loadDataWithBaseURL("https://localhost/", htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            state.webViewRef = webView
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            state.webViewRef?.destroy()
            state.webViewRef = null
        }
    }
}

private fun buildEditorHtml(
    placeholder: String,
    textColorHex: String,
    placeholderColorHex: String
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * {
                    box-sizing: border-box;
                    -webkit-tap-highlight-color: transparent;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    min-height: 100%;
                    background: transparent;
                    font-family: -apple-system, BlinkMacSystemFont, "Roboto", "Segoe UI", sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    color: $textColorHex;
                }
                #editor {
                    width: 100%;
                    min-height: 280px;
                    padding: 8px 4px 60px 4px;
                    outline: none;
                    word-break: break-word;
                    white-space: pre-wrap;
                }
                #editor[contenteditable="true"]:empty:before {
                    content: "$placeholder";
                    color: $placeholderColorHex;
                    pointer-events: none;
                    display: block;
                }
                ul, ol {
                    margin: 4px 0;
                    padding-left: 24px;
                }
                li {
                    margin: 2px 0;
                }
                mark {
                    background-color: #FEF08A;
                    color: #1E293B;
                    padding: 1px 4px;
                    border-radius: 4px;
                }
                font[size="1"] { font-size: 12px; }
                font[size="3"] { font-size: 16px; }
                font[size="5"] { font-size: 20px; }
                font[size="6"] { font-size: 24px; font-weight: bold; }
                b, strong { font-weight: 700; }
                u { text-decoration: underline; }
                s, strike, del { text-decoration: line-through; }
                i, em { font-style: italic; }
            </style>
        </head>
        <body>
            <div id="editor" contenteditable="true" spellcheck="false"></div>

            <script>
                const editor = document.getElementById('editor');

                window.editorApi = {
                    toggleBold: function() {
                        document.execCommand('bold', false, null);
                        this.onInput();
                    },
                    toggleItalic: function() {
                        document.execCommand('italic', false, null);
                        this.onInput();
                    },
                    toggleUnderline: function() {
                        document.execCommand('underline', false, null);
                        this.onInput();
                    },
                    toggleStrikethrough: function() {
                        document.execCommand('strikeThrough', false, null);
                        this.onInput();
                    },
                    toggleBulletList: function() {
                        document.execCommand('insertUnorderedList', false, null);
                        this.onInput();
                    },
                    toggleNumberedList: function() {
                        document.execCommand('insertOrderedList', false, null);
                        this.onInput();
                    },
                    setFontSize: function(sizeVal) {
                        document.execCommand('fontSize', false, sizeVal);
                        this.onInput();
                    },
                    setAlignment: function(alignCmd) {
                        document.execCommand(alignCmd, false, null);
                        this.onInput();
                    },
                    setHighlight: function(hexColor) {
                        document.execCommand('hiliteColor', false, hexColor);
                        this.onInput();
                    },
                    setTextColor: function(hexColor) {
                        document.execCommand('foreColor', false, hexColor);
                        this.onInput();
                    },
                    clearFormatting: function() {
                        document.execCommand('removeFormat', false, null);
                        document.execCommand('unlink', false, null);
                        this.onInput();
                    },
                    undo: function() {
                        document.execCommand('undo', false, null);
                        this.onInput();
                    },
                    redo: function() {
                        document.execCommand('redo', false, null);
                        this.onInput();
                    },
                    insertText: function(text) {
                        document.execCommand('insertText', false, text);
                        this.onInput();
                    },
                    setHtml: function(html) {
                        if (editor.innerHTML !== html) {
                            editor.innerHTML = html;
                            this.notifyState();
                        }
                    },
                    setThemeColors: function(textColor, placeholderColor) {
                        document.body.style.color = textColor;
                        // update placeholder
                    },
                    onInput: function() {
                        if (window.AndroidBridge && window.AndroidBridge.onContentChanged) {
                            window.AndroidBridge.onContentChanged(editor.innerHTML);
                        }
                        this.notifyState();
                    },
                    notifyState: function() {
                        if (!window.AndroidBridge || !window.AndroidBridge.onStateChanged) return;
                        try {
                            const isBold = document.queryCommandState('bold');
                            const isItalic = document.queryCommandState('italic');
                            const isUnderline = document.queryCommandState('underline');
                            const isStrike = document.queryCommandState('strikeThrough');
                            const isBullet = document.queryCommandState('insertUnorderedList');
                            const isNumbered = document.queryCommandState('insertOrderedList');
                            const canUndo = document.queryCommandEnabled('undo');
                            const canRedo = document.queryCommandEnabled('redo');

                            let align = 'left';
                            if (document.queryCommandState('justifyCenter')) align = 'center';
                            else if (document.queryCommandState('justifyRight')) align = 'right';

                            let fontSize = document.queryCommandValue('fontSize') || '3';
                            let fontColor = document.queryCommandValue('foreColor') || '';
                            let hiliteColor = document.queryCommandValue('hiliteColor') || '';

                            window.AndroidBridge.onStateChanged(
                                isBold, isItalic, isUnderline, isStrike,
                                isBullet, isNumbered, canUndo, canRedo,
                                align, fontSize, fontColor, hiliteColor
                            );
                        } catch(e) {}
                    }
                };

                editor.addEventListener('input', () => window.editorApi.onInput());
                editor.addEventListener('keyup', () => window.editorApi.notifyState());
                editor.addEventListener('mouseup', () => window.editorApi.notifyState());
                editor.addEventListener('touchend', () => window.editorApi.notifyState());
                document.addEventListener('selectionchange', () => window.editorApi.notifyState());
            </script>
        </body>
        </html>
    """.trimIndent()
}
