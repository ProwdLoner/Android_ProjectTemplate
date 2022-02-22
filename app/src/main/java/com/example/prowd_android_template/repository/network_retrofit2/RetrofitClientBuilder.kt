package com.example.prowd_android_template.repository.network_retrofit2

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


// [레트로핏 빌더] : 한 IP 에 대한 접근 객체
object RetrofitClientBuilder {
    // <멤버 변수 공간>
    private const val connectTimeOutMilliSecondMbr = 3000L
    private const val readTimeOutMilliSecondMbr = 3000L
    private const val writeTimeOutMilliSecondMbr = 3000L
    private const val retryOnConnectionFailureMbr = false


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (baseUrl 에 접속하는 레트로핏 객체를 생성 반환하는 함수)
    fun getRetrofitClient(baseUrl: String): Retrofit {
        // 클라이언트 설정 객체
        val okHttpClientBuilder = OkHttpClient.Builder()

        // 리퀘스트 파라미터 인터셉터
        val baseParamInterceptor = Interceptor { chain: Interceptor.Chain ->
            val originRequest = chain.request()
            val addedUrl: HttpUrl = originRequest.url.newBuilder()
                .build()
            val finalRequest: Request = originRequest.newBuilder()
                .url(addedUrl).method(originRequest.method, originRequest.body)
                .build()
            chain.proceed(finalRequest)
        }

        okHttpClientBuilder.addInterceptor(baseParamInterceptor)

        // 네트워크 로깅 인터셉터
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        okHttpClientBuilder.addInterceptor(httpLoggingInterceptor)

        // 연결 설정
        okHttpClientBuilder.connectTimeout(connectTimeOutMilliSecondMbr, TimeUnit.MILLISECONDS)
        okHttpClientBuilder.readTimeout(readTimeOutMilliSecondMbr, TimeUnit.MILLISECONDS)
        okHttpClientBuilder.writeTimeout(writeTimeOutMilliSecondMbr, TimeUnit.MILLISECONDS)
        okHttpClientBuilder.retryOnConnectionFailure(retryOnConnectionFailureMbr)

        // 위 설정에 따른 retrofit 객체 생성 및 반환
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build()).build()
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}