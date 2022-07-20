package com.example.prowd_android_template.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 백그라운드에서 실행되는 서비스
// 앱이 종료되면 같이 종료됩니다.
class BackgroundServiceTest : Service() {
    // <멤버 변수 공간>
    var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onBind(intent: Intent): IBinder {
        return object : Binder() {

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return super.onStartCommand(intent, flags, startId)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}