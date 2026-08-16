package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechToTextManager(
    private val context: Context,
    private val onResult: ((String) -> Unit)? = null,
    private val onError: ((String) -> Unit)? = null,
    private val onListeningStateChange: ((Boolean) -> Unit)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            val err = "Speech recognition is not available on this device"
            _errorMessage.value = err
            onError?.invoke(err)
            return
        }

        stopListening() // clean up any previous instance

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    onListeningStateChange?.invoke(true)
                    _errorMessage.value = null
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    onListeningStateChange?.invoke(false)
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    onListeningStateChange?.invoke(false)
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network connection required"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        else -> "Speech recognition error ($error)"
                    }
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        _errorMessage.value = message
                        onError?.invoke(message)
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    onListeningStateChange?.invoke(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val rawText = matches[0]
                        val formatted = applySpokenPunctuation(rawText)
                        _finalText.value = if (_finalText.value.isBlank()) formatted else "${_finalText.value} $formatted"
                        _partialText.value = ""
                        onResult?.invoke(formatted)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val raw = matches[0]
                        _partialText.value = applySpokenPunctuation(raw)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak clearly into microphone...")
        }

        runCatching {
            speechRecognizer?.startListening(intent)
        }.onFailure {
            val err = "Failed to start speech recognition: ${it.localizedMessage}"
            _errorMessage.value = err
            onError?.invoke(err)
            _isListening.value = false
            onListeningStateChange?.invoke(false)
        }
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _isListening.value = false
        onListeningStateChange?.invoke(false)
    }

    fun destroy() {
        stopListening()
    }

    fun reset() {
        stopListening()
        _partialText.value = ""
        _finalText.value = ""
        _errorMessage.value = null
    }

    fun setManualText(text: String) {
        _finalText.value = text
    }

    companion object {
        /**
         * Converts spoken punctuation terms like "comma", "period", "question mark", "new line"
         * into their actual punctuation marks.
         */
        fun applySpokenPunctuation(input: String): String {
            var text = input
            val replacements = listOf(
                Regex("(?i)\\bperiod\\b") to ".",
                Regex("(?i)\\bfull stop\\b") to ".",
                Regex("(?i)\\bcomma\\b") to ",",
                Regex("(?i)\\bquestion mark\\b") to "?",
                Regex("(?i)\\bexclamation (mark|point)\\b") to "!",
                Regex("(?i)\\bcolon\\b") to ":",
                Regex("(?i)\\bsemicolon\\b") to ";",
                Regex("(?i)\\bnew line\\b") to "\n",
                Regex("(?i)\\benter\\b") to "\n",
                Regex("(?i)\\bquote\\b") to "\"",
                Regex("(?i)\\bend quote\\b") to "\"",
                Regex("(?i)\\bopen quote\\b") to "\"",
                Regex("(?i)\\bclose quote\\b") to "\"",
                Regex("(?i)\\bhyphen\\b") to "-",
                Regex("(?i)\\bdash\\b") to " - ",
                Regex("(?i)\\bbullet\\b") to "• "
            )

            for ((regex, replacement) in replacements) {
                text = regex.replace(text, replacement)
            }

            // Capitalize sentences after '.', '!', '?'
            return text.replace(Regex("([.!?]\\s+)([a-z])")) { match ->
                match.groupValues[1] + match.groupValues[2].uppercase(Locale.getDefault())
            }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}

