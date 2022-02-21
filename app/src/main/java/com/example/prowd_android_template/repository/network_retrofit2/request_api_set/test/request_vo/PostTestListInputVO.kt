package com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class PostTestListInputVO(
    @Transient
    val headerMap: Map<String, String>,

    @SerializedName("seq")
    @Expose
    val seq : Int,

    @SerializedName("title")
    @Expose
    val title: String,

    @SerializedName("content")
    @Expose
    val content : String
)