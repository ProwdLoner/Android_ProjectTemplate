package com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test

import com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo.PutTestListInputVO
import com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo.PostTestListInputVO
import retrofit2.Response
import retrofit2.http.*

interface TestRequestApi {
    @GET("search")
    @Headers("Content-Type: application/json")
    suspend fun getTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Query("q") query: String
    ): Response<Unit>

    @PUT("test/list")
    @Headers("Content-Type: application/json")
    suspend fun putTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Body inputVO: PutTestListInputVO
    ): Response<Unit>

    @POST("test/list")
    @Headers("Content-Type: application/json")
    suspend fun postTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Body inputVo: PostTestListInputVO
    ): Response<Unit>

    @DELETE("test/list")
    @Headers("Content-Type: application/json")
    suspend fun deleteTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Query("testId") testId: Int
    ): Response<Unit>
}