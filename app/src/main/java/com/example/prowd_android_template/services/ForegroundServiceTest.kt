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


// todo noti 생성 함수 정리
// (notification 과 동시에 사용되는 서비스)
// notification 이 뷰 역할을 하여 앱이 꺼지더라도 계속 실행됨
class ForegroundServiceTest : Service() {
    // <멤버 변수 공간>
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // onStartCommand 작업 상태 코드
    // 0 : stop
    // 1 : start
    private var onStartCommandStatusMbr = 0
    private val onStartCommandStatusSemaphoreMbr = Semaphore(1)

    // onStartCommand 작업 조기 종료 리퀘스트
    private var onStartCommandEarlyStopRequestMbr = false
    private val onStartCommandEarlyStopRequestSemaphoreMbr = Semaphore(1)

    // onStartCommand 작업 조기 종료 콜백
    private var onStartCommandEarlyStopCallbackMbr: (() -> Unit)? = null

    //(notification 설정)
    private val serviceIdMbr = 1
    private val titleMbr = "테스트 타이틀"
    private val channelIdMbr = "$packageName-${getString(R.string.app_name)}"


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_STICKY
        }

        if (intent.action == "start") { // start 액션 명령
            onStartCommandStatusSemaphoreMbr.acquire()
            if (onStartCommandStatusMbr == 1) {// 기존에 start 명령 실행중이라면,
                onStartCommandStatusSemaphoreMbr.release()

                // 조기 종료 후 새 작업 실행 콜백 설정 (가장 최근에 요청한 콜백만 실행됨)
                onStartCommandEarlyStopCallbackMbr = {
                    startAsyncTask(intent, flags, startId)
                }
                // 기존 명령 취소
                onStartCommandEarlyStopRequestSemaphoreMbr.acquire()
                onStartCommandEarlyStopRequestMbr = true
                onStartCommandEarlyStopRequestSemaphoreMbr.release()
            } else {
                onStartCommandStatusMbr = 1
                onStartCommandStatusSemaphoreMbr.release()
                startAsyncTask(intent, flags, startId)
            }
        } else if (intent.action == "stop") {
            onStartCommandEarlyStopRequestSemaphoreMbr.acquire()
            onStartCommandEarlyStopRequestMbr = true
            onStartCommandEarlyStopRequestSemaphoreMbr.release()
        }

        return super.onStartCommand(intent, flags, startId)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (비동기 서비스 작업 실행)
    private fun startAsyncTask(intent: Intent?, flags: Int, startId: Int) {
        executorServiceMbr.execute {
            // (Foreground 용 Notification 생성)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // (API 26 이상의 경우 - Notification Channel 설정)
                val channel = NotificationChannel(
                    channelIdMbr,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "App notification channel"
                channel.setShowBadge(false)

                val notificationManager = this.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }

            // (notification 설정)
            val notificationBuilderMbr = NotificationCompat.Builder(this, channelIdMbr)
            // 아이콘 설정
            notificationBuilderMbr.setSmallIcon(R.mipmap.ic_launcher)
            // 타이틀 설정
            notificationBuilderMbr.setContentTitle(titleMbr)
            // 본문 설정
            notificationBuilderMbr.setContentText("테스트 실행중입니다.")
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

            val maxCount = 30
            for (count in 0..maxCount) {
                // 조기종료 파악
                onStartCommandEarlyStopRequestSemaphoreMbr.acquire()
                if (onStartCommandEarlyStopRequestMbr) { // 조기종료
                    onStartCommandEarlyStopRequestMbr = false
                    onStartCommandEarlyStopRequestSemaphoreMbr.release()

                    if (onStartCommandEarlyStopCallbackMbr != null) {
                        onStartCommandEarlyStopCallbackMbr!!()

                        onStartCommandEarlyStopCallbackMbr = null
                    } else {
                        notificationBuilderMbr.setProgress(0, 0, false)
                        notificationBuilderMbr.setContentText("테스트 조기 종료")
                        startForeground(serviceIdMbr, notificationBuilderMbr.build())

                        stopForeground(true)
                        // 서비스 상태 코드 변경
                        onStartCommandStatusSemaphoreMbr.acquire()
                        onStartCommandStatusMbr = 0
                        onStartCommandStatusSemaphoreMbr.release()
                    }
                    return@execute
                }
                onStartCommandEarlyStopRequestSemaphoreMbr.release()

                // 작업 결과 표시
                notificationBuilderMbr.setProgress(maxCount, count, false)
                startForeground(serviceIdMbr, notificationBuilderMbr.build())

                // 서비스 작업 샘플 의사 대기시간
                Thread.sleep(100)
            }

            // 서비스 완료
            stopForeground(true)

            // 서비스 상태 코드 변경
            onStartCommandStatusSemaphoreMbr.acquire()
            onStartCommandStatusMbr = 0
            onStartCommandStatusSemaphoreMbr.release()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}