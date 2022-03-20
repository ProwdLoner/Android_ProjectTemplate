package com.example.prowd_android_template.repository.network_retrofit2

import com.example.prowd_android_template.network_request_api_set.test.TestRequestApi
import java.util.concurrent.Semaphore

// Retrofit2 함수 처리는 데이터 접근에 시간이 걸리기에 내부적으로 모두 Async 처리가 되어있음
class RepositoryNetworkRetrofit2 private constructor() {
    // <멤버 변수 공간>
    // (Network Request Api 객체)
    val testRequestApiMbr: TestRequestApi =
        (RetrofitClientBuilder.getRetrofitClient("https://www.google.com/"))
            .create(TestRequestApi::class.java)


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    // (싱글톤 설정)
    companion object {
        private val singletonSemaphore = Semaphore(1)
        private var instance: RepositoryNetworkRetrofit2? = null

        fun getInstance(): RepositoryNetworkRetrofit2 {
            singletonSemaphore.acquire()

            if (null == instance) {
                instance = RepositoryNetworkRetrofit2()
            }

            singletonSemaphore.release()

            return instance!!
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}