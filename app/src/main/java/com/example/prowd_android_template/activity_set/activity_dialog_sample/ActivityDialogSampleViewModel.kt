package com.example.prowd_android_template.activity_set.activity_dialog_sample

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ActivityDialogSampleViewModel(application: Application) : AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (설정 변경 여부) : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // (데이터)
    // 로그인 정보 객체
    lateinit var loginPrefMbr: SharedPreferences

    // (플래그 데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = false


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>
    // 로딩 다이얼로그 출력 정보
    var progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
        MutableLiveData(null)

    // 선택 다이얼로그 출력 정보
    var binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
        MutableLiveData(null)

    // 확인 다이얼로그 출력 정보
    var confirmDialogInfoLiveDataMb: MutableLiveData<DialogConfirm.DialogInfoVO?> =
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