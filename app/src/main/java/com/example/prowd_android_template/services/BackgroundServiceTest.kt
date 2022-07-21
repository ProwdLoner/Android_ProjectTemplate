package com.example.prowd_android_template.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore


// todo
// (백그라운드에서 실행되는 서비스)
// 앱이 종료되면 같이 종료됩니다.
class BackgroundServiceTest : Service() {
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
            val maxCount = 30
            for (count in maxCount downTo 0) {
                // 조기 종료 파악
                onStartCommandEarlyStopRequestSemaphoreMbr.acquire()
                if (onStartCommandEarlyStopRequestMbr) { // 조기종료
                    onStartCommandEarlyStopRequestMbr = false
                    onStartCommandEarlyStopRequestSemaphoreMbr.release()

                    if (onStartCommandEarlyStopCallbackMbr != null) {
                        onStartCommandEarlyStopCallbackMbr!!()

                        onStartCommandEarlyStopCallbackMbr = null
                    } else {
                        // 조기 종료 브로드캐스트
                        val broadcastIntent1 = Intent()
                        broadcastIntent1.action = "BackgroundServiceTest"
                        broadcastIntent1.putExtra("status", "백그라운드 서비스 조기 종료")
                        sendBroadcast(broadcastIntent1)

                        stopSelf()
                        // 서비스 상태 코드 변경
                        onStartCommandStatusSemaphoreMbr.acquire()
                        onStartCommandStatusMbr = 0
                        onStartCommandStatusSemaphoreMbr.release()
                    }
                    return@execute
                }
                onStartCommandEarlyStopRequestSemaphoreMbr.release()

                // 진행 사항 표시
                val broadcastIntent = Intent()
                broadcastIntent.action = "BackgroundServiceTest"
                broadcastIntent.putExtra("status", count.toString())
                sendBroadcast(broadcastIntent)

                // 서비스 작업 샘플 의사 대기시간
                Thread.sleep(100)
            }

            // 서비스 완료 브로드 캐스트
            val broadcastIntent = Intent()
            broadcastIntent.action = "BackgroundServiceTest"
            broadcastIntent.putExtra("status", "백그라운드 서비스 완료")
            sendBroadcast(broadcastIntent)

            // 서비스 상태 코드 변경
            onStartCommandStatusSemaphoreMbr.acquire()
            onStartCommandStatusMbr = 0
            onStartCommandStatusSemaphoreMbr.release()

            stopSelf()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}