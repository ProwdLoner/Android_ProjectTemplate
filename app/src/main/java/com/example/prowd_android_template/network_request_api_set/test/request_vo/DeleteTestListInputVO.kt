package com.example.prowd_android_template.network_request_api_set.test.request_vo


data class DeleteTestListInputVO(
    @Transient
    val headerMap: Map<String, String?>,

    @Transient
    val testId : Int
)