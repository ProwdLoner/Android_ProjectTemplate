package com.example.prowd_android_template.activity_set.activity_permission_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.common_shared_preference_wrapper.CustomDevicePermissionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.network_request_api_set.test.request_vo.GetTestHeaderInfoInputVO
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class ActivityPermissionSampleViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
        CurrentLoginSessionInfoSpw(application)

    // 디바이스 커스텀 권한 정보 접근 객체
    val customDevicePermissionInfoSpwMbr: CustomDevicePermissionInfoSpw =
        CustomDevicePermissionInfoSpw(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // (플래그 데이터)
    // 뷰 개발 모드 플래그 (= 더미 데이터를 사용)
    private val isClientDevModeMbr = true

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true


    // ---------------------------------------------------------------------------------------------
    // <뷰모델 라이브데이터 공간>
    // 로딩 다이얼로그 출력 정보
    val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
        MutableLiveData(null)

    // 선택 다이얼로그 출력 정보
    val binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
        MutableLiveData(null)

    // 확인 다이얼로그 출력 정보
    val confirmDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO?> =
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

    // 푸시 권한 서버 반영
    private val putPushPermissionAsyncSemaphoreMbr = Semaphore(1)
    var putPushPermissionAsyncOnProgressedMbr = false
        private set

    fun putPushPermissionAsync(
        pushPermissionGranted: Boolean,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            putPushPermissionAsyncSemaphoreMbr.acquire()

            if (isClientDevModeMbr) {
                // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
                Thread.sleep(500)

                onComplete()

                putPushPermissionAsyncSemaphoreMbr.release()
            }else{

                // TODO : 실제 리포지토리 처리
                putPushPermissionAsyncSemaphoreMbr.release()
            }

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}