package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.AudioPlayer
import kotlinx.coroutines.delay

@Composable
fun VoiceNotePlayerBar(
    audioPath: String,
    audioPlayer: AudioPlayer,
    onDeleteAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableStateOf(0) }
    var currentPositionMs by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(200)
            if (audioPlayer.isPlaying()) {
                val cur = audioPlayer.getCurrentPosition()
                val dur = audioPlayer.getDuration()
                currentPositionMs = cur
                if (dur > 0) {
                    durationMs = dur
                    currentProgress = cur.toFloat() / dur.toFloat()
                }
            } else {
                isPlaying = false
                currentProgress = 0f
                currentPositionMs = 0
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        audioPlayer.pauseAudio()
                        isPlaying = false
                    } else {
                        audioPlayer.playAudio(audioPath) {
                            isPlaying = false
                            currentProgress = 0f
                        }
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("voice_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (durationMs > 0) formatTime(durationMs) else "Voice Note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = currentProgress,
                    onValueChange = { percent ->
                        currentProgress = percent
                        if (durationMs > 0) {
                            val seekPos = (percent * durationMs).toInt()
                            audioPlayer.seekTo(seekPos)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    audioPlayer.stopAudio()
                    onDeleteAudio()
                },
                modifier = Modifier.testTag("delete_voice_note_button")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Voice Note",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
