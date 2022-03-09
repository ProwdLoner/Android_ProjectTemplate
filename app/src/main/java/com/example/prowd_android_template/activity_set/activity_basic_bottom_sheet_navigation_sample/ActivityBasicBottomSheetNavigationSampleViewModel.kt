package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.globalVariableConnector.GvcCurrentLoginSessionInfo
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ActivityBasicBottomSheetNavigationSampleViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    var gvcCurrentLoginSessionInfoMbr : GvcCurrentLoginSessionInfo = GvcCurrentLoginSessionInfo(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // [Fragment1 데이터]
    // 데이터 수집 등, 첫번째에만 발동
    var isFragment1DataFirstLoadingMbr = true

    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var fragment1CurrentUserSessionTokenMbr: String? = null


    // [Fragment2 데이터]
    // 데이터 수집 등, 첫번째에만 발동
    var isFragment2DataFirstLoadingMbr = true

    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var fragment2CurrentUserSessionTokenMbr: String? = null


    // [Fragment3 데이터]
    // 데이터 수집 등, 첫번째에만 발동
    var isFragment3DataFirstLoadingMbr = true

    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var fragment3CurrentUserSessionTokenMbr: String? = null


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>
    // 네트워크 에러 다이얼로그 출력 정보
    var networkErrorDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO> =
        MutableLiveData(null)

    // 서버 에러 다이얼로그 출력 정보
    var serverErrorDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO> =
        MutableLiveData(null)

    // 로딩 다이얼로그 출력 정보
    var progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO> =
        MutableLiveData(null)


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCleared() {
        executorServiceMbr?.shutdown()
        executorServiceMbr = null
        super.onCleared()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}