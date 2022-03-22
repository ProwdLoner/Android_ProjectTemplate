package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample_editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.common_global_variable_connector.CurrentLoginSessionInfoGvc
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class ActivityBasicRecyclerViewSampleEditorViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    val currentLoginSessionInfoGvcMbr: CurrentLoginSessionInfoGvc =
        CurrentLoginSessionInfoGvc(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // 뷰 개발 모드 플래그 (= 더미 데이터를 사용)
    private val isViewDevModeMbr = true


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


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCleared() {
        executorServiceMbr?.shutdown()
        executorServiceMbr = null
        super.onCleared()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (아이템 데이터 추가)
    private val addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)

    fun addScreenVerticalRecyclerViewAdapterItemDataOnVMAsync(
        contentTitleTxt: String,
        contentTxt: String,
        writeTime: String,
        onComplete: (Long) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.acquire()

            if (isViewDevModeMbr) {
                // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
                Thread.sleep(500)

                // 원래는 네트워크에서 아이템을 추가 후 결과로 해당 아이템의 uid 를 반환
                val itemUid = 1L
                onComplete(itemUid)

                addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()
            } else {
                // TODO : 실제 리포지토리 처리
                addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()
            }

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}