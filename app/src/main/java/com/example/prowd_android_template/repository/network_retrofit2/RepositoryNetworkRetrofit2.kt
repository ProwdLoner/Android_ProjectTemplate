package com.example.prowd_android_template.repository.network_retrofit2

import com.example.prowd_android_template.repository.network_retrofit2.request_apis.RequestApiTest
import java.util.concurrent.Semaphore

// Retrofit2 함수 네트워크 URL 과 API 객체를 이어주고 제공하는 역할
// : 주소 1개당 API 객체 1개를 생성하여 (Network Request Api 객체) 공간에 추가하여 사용
class RepositoryNetworkRetrofit2 private constructor() {
    // <멤버 변수 공간>
    // (Network Request Api 객체)
    val testRequestApiMbr: RequestApiTest =
        (RetrofitClientBuilder.getRetrofitClient("https://www.google.com/"))
            .create(RequestApiTest::class.java)


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