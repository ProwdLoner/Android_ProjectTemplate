package com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment1.FragmentActivityBasicBottomSheetNavigationSampleFragment1VmData
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment2.FragmentActivityBasicBottomSheetNavigationSampleFragment2VmData
import com.example.prowd_android_template.activity_set.activity_basic_bottom_sheet_navigation_sample.fragment3.FragmentActivityBasicBottomSheetNavigationSampleFragment3VmData
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
    val gvcCurrentLoginSessionInfoMbr: GvcCurrentLoginSessionInfo =
        GvcCurrentLoginSessionInfo(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // (Fragment 화면 데이터)
    // Fragment1
    var fragment1Data = FragmentActivityBasicBottomSheetNavigationSampleFragment1VmData()

    // Fragment2
    var fragment2Data = FragmentActivityBasicBottomSheetNavigationSampleFragment2VmData()

    // Fragment3
    var fragment3Data = FragmentActivityBasicBottomSheetNavigationSampleFragment3VmData()


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>
    // 네트워크 에러 다이얼로그 출력 정보
    val networkErrorDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO> =
        MutableLiveData(null)

    // 서버 에러 다이얼로그 출력 정보
    val serverErrorDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO> =
        MutableLiveData(null)

    // 로딩 다이얼로그 출력 정보
    val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO> =
        MutableLiveData(null)

    // 플래그먼트 클릭 위치 정보
    // 플래그먼트간 정보 공유 테스트용
    val fragmentClickedPositionLiveDataMbr: MutableLiveData<Int> = MutableLiveData(null)


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