package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_REMINDER = "com.example.ACTION_DISMISS_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (intent.action == ACTION_DISMISS_REMINDER) {
            val noteIdHashCode = intent.getIntExtra("NOTIFICATION_ID", 0)
            if (noteIdHashCode != 0) {
                notificationManager.cancel(noteIdHashCode)
            }
            return
        }

        val noteId = intent.getStringExtra("NOTE_ID") ?: return
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "Note Reminder"
        val rawSnippet = intent.getStringExtra("NOTE_SNIPPET") ?: ""
        val noteSnippet = RichTextRenderer.stripHtml(rawSnippet).trim()
        val notifId = noteId.hashCode()

        val channelId = "mynotes_reminders_channel_persistent"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active notifications requiring manual acknowledgment for scheduled note reminders"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open Note Action
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_NOTE_ID", noteId)
        }

        val pendingOpenIntent = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            putExtra("NOTIFICATION_ID", notifId)
        }

        val pendingDismissIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (noteSnippet.isNotBlank()) noteSnippet else "Scheduled reminder for this note"
        val displayTitle = if (noteTitle.isNotBlank()) noteTitle else "Note Reminder"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false) // User can swipe to dismiss or tap explicit buttons
            .setAutoCancel(true)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open Note", pendingOpenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", pendingDismissIntent)
            .build()

        notificationManager.notify(notifId, notification)
    }
}


