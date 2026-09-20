package com.example.dibina.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

/**
 * Habit reminder receiver that triggers local notifications
 * at scheduled times (e.g. 05:00 morning habits, 20:00 journal reminder).
 */
class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: TYPE_JOURNAL
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = DibinaMessagingService.CHANNEL_ID
        val channelName = DibinaMessagingService.CHANNEL_NAME

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat kebiasaan harian dan diary DIBINA"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (title, message) = when (reminderType) {
            TYPE_MORNING -> Pair(
                "Selamat Pagi Siswa Hebat! 🌅",
                "Waktunya bangun pagi, beribadah shubuh, dan berolahraga segar untuk memulai harimu!"
            )
            TYPE_JOURNAL -> Pair(
                "Waktunya Catat Jurnal DIBINA 📖",
                "Yuk lengkapi 7 Kebiasaan Baikmu hari ini sebelum tidur malam dan jaga streak-mu!"
            )
            else -> Pair(
                "Pengingat 7 Kebiasaan DIBINA ✨",
                "Ayo kumpulkan EXP dan bangun karakter hebatmu hari ini!"
            )
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (reminderType == TYPE_MORNING) 101 else 102,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (reminderType == TYPE_MORNING) 1001 else 1002
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_JOURNAL = "journal"

        fun scheduleReminders(
            context: Context,
            morningEnabled: Boolean,
            journalEnabled: Boolean
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // 1. Morning Reminder (05:00)
            val morningIntent = Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_TYPE, TYPE_MORNING)
            }
            val morningPending = PendingIntent.getBroadcast(
                context,
                101,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (morningEnabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 5)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    morningPending
                )
            } else {
                alarmManager.cancel(morningPending)
            }

            // 2. Evening Journal Reminder (20:00)
            val journalIntent = Intent(context, HabitReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_TYPE, TYPE_JOURNAL)
            }
            val journalPending = PendingIntent.getBroadcast(
                context,
                102,
                journalIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (journalEnabled) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 20)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    journalPending
                )
            } else {
                alarmManager.cancel(journalPending)
            }
        }
    }
}
