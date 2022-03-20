package com.example.prowd_android_template.network_request_api_set.test.request_vo

data class GetTestListInputVO(
    @Transient
    val headerMap: Map<String, String?>,

    @Transient
    val page: Int
)