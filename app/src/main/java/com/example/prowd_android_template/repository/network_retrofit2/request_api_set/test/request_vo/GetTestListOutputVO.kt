package com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class GetTestListOutputVO(
    @SerializedName("totalRows")
    @Expose
    val totalRows:Int,
    @SerializedName("testList")
    @Expose
    val testList:ArrayList<TestInfo>,
){
    @Parcelize
    data class TestInfo(
        val seq : Int,
        val title: String,
        val content : String
    ): Parcelable
}