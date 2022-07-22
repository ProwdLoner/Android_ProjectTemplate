package com.example.prowd_android_template.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class BackgroundServiceBroadcastTest : Service() {
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
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (비동기 서비스 작업 실행)
    private fun startAsyncTask(intent: Intent?, flags: Int, startId: Int) {
        val broadcastIntent = Intent()
        broadcastIntent.action = "ActivityBroadcastReceiverSample"
        broadcastIntent.putExtra("value", "Background Service")
        sendBroadcast(broadcastIntent)

        stopSelf()
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}