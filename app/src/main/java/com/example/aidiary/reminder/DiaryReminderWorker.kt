package com.example.aidiary.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.aidiary.R
import java.util.concurrent.TimeUnit
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class DiaryReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ensureChannel()
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今天想记录点什么？")
            .setContentText("写几句今天做了什么，三餐也可以顺手记下来。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        return Result.success()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日记提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        applicationContext
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "diary_reminders"
        private const val WORK_NAME = "daily_diary_reminder"

        fun schedule(context: Context, reminderTime: String = "21:30") {
            val initialDelay = delayUntilNext(reminderTime)
            val request = PeriodicWorkRequestBuilder<DiaryReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun delayUntilNext(reminderTime: String): Duration {
            val targetTime = runCatching { LocalTime.parse(reminderTime) }.getOrDefault(LocalTime.of(21, 30))
            val now = LocalDateTime.now()
            var target = now.withHour(targetTime.hour).withMinute(targetTime.minute).withSecond(0).withNano(0)
            if (!target.isAfter(now)) target = target.plusDays(1)
            return Duration.between(now, target)
        }
    }
}
