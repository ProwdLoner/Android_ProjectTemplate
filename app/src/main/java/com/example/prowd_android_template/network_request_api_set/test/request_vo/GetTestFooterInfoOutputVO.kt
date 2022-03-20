package com.example.prowd_android_template.network_request_api_set.test.request_vo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GetTestFooterInfoOutputVO(
    @SerializedName("uid")
    @Expose
    val uid: Long,
    @SerializedName("content")
    @Expose
    val content: String
)