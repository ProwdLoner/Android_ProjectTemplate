package com.example.prowd_android_template.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_basic_service_sample.ActivityBasicServiceSample
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// notification 과 동시에 사용되는 서비스
// notification 이 뷰 역할을 하여 앱이 꺼지더라도 계속 실행됨
class ForegroundServiceTest : Service() {
    // <멤버 변수 공간>
    private val serviceIdMbr = 1

    val title = "테스트 타이틀"
    val content = "테스트 본문입니다."

    var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "$packageName-${getString(R.string.app_name)}"

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
        val notificationBuilderMbr = NotificationCompat.Builder(this, channelId)
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
        val clickIntent = Intent(baseContext, ActivityBasicServiceSample::class.java)
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

        startForeground(serviceIdMbr, notificationBuilderMbr.build())

        var count = 0
        executorServiceMbr.execute {
            while (count <= 100) {
                count += 10
                Thread.sleep(1000)

                notificationBuilderMbr.setProgress(100, count, false)
                startForeground(serviceIdMbr, notificationBuilderMbr.build())
            }

            notificationBuilderMbr.setProgress(0, 0, false)
            startForeground(serviceIdMbr, notificationBuilderMbr.build())
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