package com.example.prowd_android_template.util_class

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// (스레드 합류 객체)
class ThreadConfluenceObj(
    private val numberOfThreadsBeingJoinedMbr: Int, // 합쳐지는 스레드 총개수
    private val onComplete: () -> Unit
) {
    // <멤버 변수 공간>
    // (스레드 풀)
    private val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()
    private var threadAccessCountMbr = 0
    private val threadAccessCountSemaphoreMbr = Semaphore(1)


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // 한 스레드 작업이 종료 되었을 때 이를 실행
    fun threadComplete() {
        if (threadAccessCountMbr < 0) { // 오버플로우 방지
            return
        }

        executorServiceMbr.execute {
            threadAccessCountSemaphoreMbr.acquire()
            // 스레드 접근 카운트 +1
            ++threadAccessCountMbr

            if (threadAccessCountMbr != numberOfThreadsBeingJoinedMbr) {
                // 접근 카운트가 합류 총 개수를 넘었을 때 or 접근 카운트가 합류 총 개수에 미치지 못했을 때
                threadAccessCountSemaphoreMbr.release()
            } else { // 접근 카운트가 합류 총 개수에 다다랐을 때
                threadAccessCountSemaphoreMbr.release()
                onComplete()
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}