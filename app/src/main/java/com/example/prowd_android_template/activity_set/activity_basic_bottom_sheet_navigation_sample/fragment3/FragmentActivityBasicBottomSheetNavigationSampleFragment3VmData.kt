package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment3

import android.app.Application
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService

// (Activity ViewModel 에 저장되어 Fragment 에서 ViewModel 로 사용되는 클래스)
data class FragmentActivityBasicBottomSheetNavigationSampleFragment3VmData(
    val application: Application,
    val repositorySetMbr: RepositorySet,
    val executorServiceMbr: ExecutorService?
) {
    // <멤버 변수 공간>
    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
        CurrentLoginSessionInfoSpw(application)

    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr: Boolean = true


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}