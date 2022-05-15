package com.example.prowd_android_template.util_class

import android.os.Handler
import android.os.HandlerThread

// <안드로이드 스레드셋 오브젝트 클래스>
class HandlerThreadObj(val threadName: String) {
    // <멤버 변수 공간>
    // (스레드 정보 객체)
    private var handlerThread: HandlerThread? = null
    var handler: Handler? = null
        private set


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (스레드 생성 함수)
    fun startHandlerThread() {
        if (isThreadObjAlive()) {
            return
        }
        handlerThread = HandlerThread(threadName)
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
    }

    // (스레드 종료 함수)
    // 스레드를 safely 하게 종료하고 스레드 관련 객체들을 비움
    fun stopHandlerThread() {
        if (!isThreadObjAlive()) {
            return
        }
        handlerThread!!.quitSafely()
        try {
            handlerThread!!.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // (스레드 실행 함수)
    fun runInHandlerThread(runnable: Runnable) {
        if (!isThreadObjAlive()) {
            return
        }
        handler!!.post(runnable)
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (스레드 확인 함수)
    // 스레드가 생성되어 정상 동작중이면 true
    fun isThreadObjAlive(): Boolean {
        return (null != handler && null != handlerThread && handlerThread!!.isAlive)
    }
}