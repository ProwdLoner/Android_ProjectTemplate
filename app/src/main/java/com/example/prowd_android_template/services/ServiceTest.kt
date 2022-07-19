package com.example.prowd_android_template.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_basic_service_sample.ActivityBasicServiceSample
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ServiceTest : Service() {
    // <전역 변수 공간>
    companion object {
        // 서비스와 통신하기 위한 전역변수
        // 메모리 릭을 방지하기 위해 되도록 nullable 변수를 사용하고 해제에 주의하기

    }


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    val NOTIFICATION_ID = 101
    private lateinit var notificationBuilderMbr: NotificationCompat.Builder
    private lateinit var notificationManagerCompatMbr: NotificationManagerCompat

    // (스레드 풀)
    var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = "Android Developer"
        val content = "Notifications in Android P"
        val channelId = "$packageName-${getString(R.string.app_name)}"
        val clickIntent = Intent(baseContext, ActivityBasicServiceSample::class.java)

        // (API 26 이상의 경우 - Notification Channel 설정)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "App notification channel"
            channel.setShowBadge(false)

            val notificationManager = this.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // (notification 설정)
        notificationBuilderMbr = NotificationCompat.Builder(this, channelId)
        // 아이콘 설정
        notificationBuilderMbr.setSmallIcon(R.mipmap.ic_launcher)
        // 타이틀 설정
        notificationBuilderMbr.setContentTitle(title)
        // 본문 설정
        notificationBuilderMbr.setContentText(content)
        // 우선순위 설정
        notificationBuilderMbr.priority = NotificationCompat.PRIORITY_DEFAULT
        // 알림 터치시 삭제여부 설정
        notificationBuilderMbr.setAutoCancel(true)
        // 알림 터치시 실행 인텐트 설정
        clickIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                baseContext,
                0,
                clickIntent,
                PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                baseContext,
                0,
                clickIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        notificationBuilderMbr.setContentIntent(pendingIntent)

        notificationBuilderMbr.setProgress(100, 0, false)

        notificationManagerCompatMbr = NotificationManagerCompat.from(this)
        
        startForeground(NOTIFICATION_ID, notificationBuilderMbr.build())

        var count = 0
        executorServiceMbr.execute {
            while (count <= 100){
                count += 10
                Thread.sleep(1000)

                notificationBuilderMbr.setProgress(100, count, false)
                startForeground(NOTIFICATION_ID, notificationBuilderMbr.build())
//                notificationManagerCompatMbr.notify(NOTIFICATION_ID, notificationBuilderMbr.build())
            }

            notificationBuilderMbr.setProgress(0, 0, false)
            startForeground(NOTIFICATION_ID, notificationBuilderMbr.build())
//            notificationManagerCompatMbr.notify(NOTIFICATION_ID, notificationBuilderMbr.build())
        }

        return super.onStartCommand(clickIntent, flags, startId)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}