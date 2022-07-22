package com.example.prowd_android_template.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.prowd_android_template.R
import com.example.prowd_android_template.util_object.NotificationWrapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// (notification 과 동시에 사용되는 서비스)
// notification 이 뷰 역할을 하여 앱이 꺼지더라도 계속 실행됨
class ForegroundServiceTest : Service() {
    // <멤버 변수 공간>
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // ui 스레드 핸들러
    private val uiHandler = Handler(Looper.getMainLooper())

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
        // (notification 설정)
        val notificationBuilderMbr = NotificationWrapper.getNotificationBuilder(
            this,
            "$packageName-${getString(R.string.app_name)}",
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
            setChannelSpace = {
                it.description = "App notification channel"
                it.setShowBadge(false)
            }
        )
        // 아이콘 설정
        notificationBuilderMbr.setSmallIcon(R.mipmap.ic_launcher)
        // 타이틀 설정
        notificationBuilderMbr.setContentTitle("포그라운드 서비스")
        // 본문 설정
        notificationBuilderMbr.setContentText("테스트 실행중입니다.")
        // 우선순위 설정
        notificationBuilderMbr.priority = NotificationCompat.PRIORITY_DEFAULT
        // 알림 터치시 삭제여부 설정
        notificationBuilderMbr.setAutoCancel(true)

        val maxTaskCount = 30
        notificationBuilderMbr.setProgress(maxTaskCount, 0, false)
        startForeground(serviceIdMbr, notificationBuilderMbr.build())

        executorServiceMbr.execute {
            for (count in 1..maxTaskCount) {
                // 서비스 작업 샘플 의사 대기시간
                Thread.sleep(100)

                // 조기종료 파악
                onStartCommandEarlyStopRequestSemaphoreMbr.acquire()
                if (onStartCommandEarlyStopRequestMbr) { // 조기종료
                    onStartCommandEarlyStopRequestMbr = false
                    onStartCommandEarlyStopRequestSemaphoreMbr.release()

                    if (onStartCommandEarlyStopCallbackMbr != null) {
                        onStartCommandEarlyStopCallbackMbr!!()

                        onStartCommandEarlyStopCallbackMbr = null
                    } else {
                        uiHandler.post {
                            stopForeground(true)
                        }
                        // 서비스 상태 코드 변경
                        onStartCommandStatusSemaphoreMbr.acquire()
                        onStartCommandStatusMbr = 0
                        onStartCommandStatusSemaphoreMbr.release()
                    }
                    return@execute
                }
                onStartCommandEarlyStopRequestSemaphoreMbr.release()

                // 작업 결과 표시
                notificationBuilderMbr.setProgress(maxTaskCount, count, false)
                uiHandler.post {
                    startForeground(serviceIdMbr, notificationBuilderMbr.build())
                }
            }

            // 서비스 완료
            uiHandler.post {
                stopForeground(true)
            }

            // 서비스 상태 코드 변경
            onStartCommandStatusSemaphoreMbr.acquire()
            onStartCommandStatusMbr = 0
            onStartCommandStatusSemaphoreMbr.release()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}