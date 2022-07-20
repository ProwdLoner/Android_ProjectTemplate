package com.example.prowd_android_template.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// (백그라운드에서 실행되는 서비스)
// 앱이 종료되면 같이 종료됩니다.
class BackgroundServiceTest : Service() {
    // <멤버 변수 공간>
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    private var isCountingStopMbr = true
    private val isCountingStopSemaphoreMbr = Semaphore(1)
    private var serviceActionMbr = "stop"

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

        if (intent.action == "start" && serviceActionMbr != "start") {
            serviceActionMbr = "start"

            isCountingStopSemaphoreMbr.acquire()
            isCountingStopMbr = false
            isCountingStopSemaphoreMbr.release()

            executorServiceMbr.execute {
                val broadcastIntent = Intent()

                for (count in 1..100) {
                    isCountingStopSemaphoreMbr.acquire()
                    if (isCountingStopMbr) {
                        isCountingStopSemaphoreMbr.release()
                        break
                    }
                    isCountingStopSemaphoreMbr.release()

                    Thread.sleep(100)

                    broadcastIntent.action = "BackgroundServiceTest"
                    broadcastIntent.putExtra("status", count.toString())
                    sendBroadcast(broadcastIntent)
                }

                broadcastIntent.action = "BackgroundServiceTest"
                broadcastIntent.putExtra("status", "백그라운드 서비스 종료")
                sendBroadcast(broadcastIntent)

                serviceActionMbr = "stop"
            }
        } else if (intent.action == "stop") {
            serviceActionMbr = "stop"

            isCountingStopSemaphoreMbr.acquire()
            isCountingStopMbr = true
            isCountingStopSemaphoreMbr.release()
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