package com.example.prowd_android_template.network_request_api_set.test.request_vo

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class GetTestItemInfoListOutputVO(
    @SerializedName("totalRows")
    @Expose
    val totalRows: Int,
    @SerializedName("testList")
    @Expose
    val testList: ArrayList<TestInfo>,
) {
    @Parcelize
    data class TestInfo(
        val uid: Long,
        val title: String,
        val content: String,
        val writeDate: String
    ) : Parcelable
}