package com.example.prowd_android_template.util_object

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationWrapper {
    // (Notification 실행)
    fun showNotification(
        context: Context,
        channelId: String,
        channelName: String,
        channelImportance: Int,
        setChannelSpace: (NotificationChannel) -> Unit,
        setNotificationBuilderSpace: (NotificationCompat.Builder) -> Unit,
        notificationId: Int
    ) {
        val channel = NotificationChannel(channelId, channelName, channelImportance)

        // 채널 설정을 유저에게 넘기기
        setChannelSpace(channel)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notificationBuilderMbr = NotificationCompat.Builder(context, channelId)

        // notification 설정을 유저에게 넘기기
        setNotificationBuilderSpace(notificationBuilderMbr)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(notificationId, notificationBuilderMbr.build())
    }

    // (Notification 빌더 생성 및 반환)
    fun getNotificationBuilder(
        context: Context,
        channelId: String,
        channelName: String,
        channelImportance: Int,
        setChannelSpace: (NotificationChannel) -> Unit
    ): NotificationCompat.Builder {
        val channel = NotificationChannel(channelId, channelName, channelImportance)

        // 채널 설정을 유저에게 넘기기
        setChannelSpace(channel)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, channelId)
    }

    // (Notification 종료)
    fun dismissNotification(
        context: Context,
        notificationId: Int
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }

    // (NotificationChannel 종료)
    fun dismissNotificationChannel(
        context: Context,
        channelId: String
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(channelId)

    }
}