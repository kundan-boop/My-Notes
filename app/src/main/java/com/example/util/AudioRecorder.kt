package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(): File? {
        val voiceDir = File(context.filesDir, "voice_notes").apply { if (!exists()) mkdirs() }
        val outputFile = File(voiceDir, "audio_${UUID.randomUUID()}.m4a")
        currentOutputFile = outputFile

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            runCatching {
                prepare()
                start()
            }
        }
        return outputFile
    }

    fun stopRecording(): String? {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        return currentOutputFile?.absolutePath
    }

    fun getMaxAmplitude(): Int {
        return runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
    }

    fun cancelRecording() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
