package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ActivityBasicVerticalRecyclerViewSampleViewModel(application: Application) :
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

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // recyclerView 데이터
    val recyclerViewAdapterItemDataListMbr: ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
        ArrayList()


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
    fun getRecyclerViewHeaderData(
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            onComplete()
        }
    }

    fun getRecyclerViewFooterData(
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            onComplete()
        }
    }

    fun getRecyclerViewItemDataList(
        onComplete: (ArrayList<GetRecyclerViewItemDataListOutputVO>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            val resultData = arrayListOf(
                GetRecyclerViewItemDataListOutputVO(
                    "item1"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item2"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item3"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item4"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item5"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item6"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item7"
                ),
                GetRecyclerViewItemDataListOutputVO(
                    "item8"
                )
            )

            // 네트워크 요청 대기 시간을 상정
            Thread.sleep(2000)

            onComplete(resultData)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    data class GetRecyclerViewItemDataListOutputVO(
        val title: String
    )
}