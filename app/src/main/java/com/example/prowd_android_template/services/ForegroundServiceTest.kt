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
import java.util.concurrent.Semaphore

// (notification 과 동시에 사용되는 서비스)
// notification 이 뷰 역할을 하여 앱이 꺼지더라도 계속 실행됨
class ForegroundServiceTest : Service() {
    // <멤버 변수 공간>
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // 서비스 작업 상태 코드
    // 0 : stop
    // 1 : start
    private var serviceActionStatusMbr = 0
    private val serviceActionStatusMbrSemaphoreMbr = Semaphore(1)
    private var asyncActionStopRequestMbr = false // 비동기 작업 조기 종료 리퀘스트
    private val asyncActionStopRequestMbrSemaphoreMbr = Semaphore(1)


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_STICKY
        }

        serviceActionStatusMbrSemaphoreMbr.acquire()
        if (intent.action == "start" &&
            serviceActionStatusMbr != 1
        ) { // start 액션 명령 and 현 상태가 start 실행중이 아닐 때 = 조기종료 프로세스 진행중에도 스킵
            serviceActionStatusMbr = 1
            serviceActionStatusMbrSemaphoreMbr.release()

            // (Foreground 용 Notification 생성)
            val serviceIdMbr = 1

            val title = "테스트 타이틀"
            val content = "테스트 본문입니다."

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

            // (서비스 로직 실행)
            val maxCount = 30
            executorServiceMbr.execute {
                for (count in 0..maxCount) {
                    asyncActionStopRequestMbrSemaphoreMbr.acquire()
                    if (asyncActionStopRequestMbr) { // 조기종료
                        asyncActionStopRequestMbr = false
                        asyncActionStopRequestMbrSemaphoreMbr.release()

                        // 서비스 상태 코드 변경
                        serviceActionStatusMbrSemaphoreMbr.acquire()
                        serviceActionStatusMbr = 0
                        serviceActionStatusMbrSemaphoreMbr.release()
                        return@execute
                    }
                    asyncActionStopRequestMbrSemaphoreMbr.release()

                    // 서비스 작업 샘플 의사 대기시간
                    Thread.sleep(100)

                    // 서비스 진행
                    notificationBuilderMbr.setProgress(maxCount, count, false)
                    startForeground(serviceIdMbr, notificationBuilderMbr.build())
                }

                // 서비스 완료
                notificationBuilderMbr.setProgress(0, 0, false)
                startForeground(serviceIdMbr, notificationBuilderMbr.build())

                // 서비스 상태 코드 변경
                serviceActionStatusMbrSemaphoreMbr.acquire()
                serviceActionStatusMbr = 0
                serviceActionStatusMbrSemaphoreMbr.release()

                stopSelf()
            }
        } else if (intent.action == "stop") {
            serviceActionStatusMbrSemaphoreMbr.release()
            asyncActionStopRequestMbrSemaphoreMbr.acquire()
            asyncActionStopRequestMbr = true
            asyncActionStopRequestMbrSemaphoreMbr.release()
            stopForeground(true)
        } else {
            serviceActionStatusMbrSemaphoreMbr.release()
        }

        return super.onStartCommand(intent, flags, startId)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}