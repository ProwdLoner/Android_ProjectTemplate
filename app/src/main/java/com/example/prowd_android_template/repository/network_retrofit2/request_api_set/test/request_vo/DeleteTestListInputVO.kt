package com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo


data class DeleteTestListInputVO(
    @Transient
    val headerMap: Map<String, String>,

    @Transient
    val testId : Int
)