package com.example.prowd_android_template.activity_set.activity_basic_tab_layout_sample.fragment2

data class FragmentActivityBasicTabLayoutSampleFragment2VmData(
    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr: Boolean = true,

    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null
)