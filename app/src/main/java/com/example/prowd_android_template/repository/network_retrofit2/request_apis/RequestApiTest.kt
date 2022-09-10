package com.example.prowd_android_template.repository.network_retrofit2.request_apis

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

// (한 주소에 대한 API 요청명세)
// 사용법은 아래 기본 사용 샘플을 참고하여 추상함수를 작성하여 사용
interface RequestApiTest {
    @GET("search")
    @Headers("Content-Type: application/json")
    suspend fun getTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Query("q") query: String
    ): Call<Unit>


    ////////////////////////////////////////////////////////////////////////////////////////////////
    @PUT("test/list")
    @Headers("Content-Type: application/json")
    suspend fun putTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Body inputVO: PutTestListInputVO
    ): Call<Unit>

    data class PutTestListInputVO(
        @Transient
        val headerMap: Map<String, String>,

        @SerializedName("seq")
        val seq: Int,

        @SerializedName("title")
        val title: String,

        @SerializedName("content")
        val content: String
    )


    ////////////////////////////////////////////////////////////////////////////////////////////////
    @POST("test/list")
    @Headers("Content-Type: application/json")
    suspend fun postTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Body inputVo: PostTestListInputVO
    ): Call<Unit>

    data class PostTestListInputVO(
        @Transient
        val headerMap: Map<String, String?>,

        @SerializedName("seq")
        val seq: Int,

        @SerializedName("title")
        val title: String,

        @SerializedName("content")
        val content: String
    )


    ////////////////////////////////////////////////////////////////////////////////////////////////
    @DELETE("test/list")
    @Headers("Content-Type: application/json")
    suspend fun deleteTestList(
        @HeaderMap headerMap: Map<String, String>,
        @Query("testId") testId: Int
    ): Call<Unit>

}