package com.example.prowd_android_template.repository.network_retrofit2.request_api_set.test.request_vo

data class GetTestItemInfoListInputVO(
    @Transient
    val headerMap: Map<String, String?>,

    @Transient
    // 정렬 기준
    val sortingCode: Int,

    @Transient
    // 한번에 받아올 페이지 크기
    val pageLength: Int,

    @Transient
    // 이전에 요청된 마지막 아이템 uid, 첫페이지라면 null
    val lastUid: Long?

)