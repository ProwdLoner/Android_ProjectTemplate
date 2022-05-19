package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.abstract_class.ProwdRecyclerViewAdapter
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

    // recyclerView 내에서 사용되는 뷰모델 데이터 (내부 LiveData 는 adapter 에서 자동 observe 처리됨)
    val recyclerViewAdapterVmDataMbr: ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.AdapterVmData =
        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.AdapterVmData(
            MutableLiveData(),
            null,
            null
        )


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
    // (아이템 데이터 요청 함수)
    // lastServerItemUid : 아이템을 구분짓는 유니크 아이디.(서버에서 구분하는 아이디로, 0번부터 시작한다고 가정.)
    var getRecyclerViewItemDataListLastServerItemUidMbr: Long = -1

    // pageSize : 한번에 반환하는 아이템 갯수 (이 기준이 변하면 아이템 새로고침을 할 것)
    var getRecyclerViewItemDataListPageSizeMbr: Int = 10

    // sortCode : 무엇을 기준으로 정렬한 아이템을 가져올지에 대한 것. (이 기준이 변하면 아이템 새로고침을 할 것)
    var getRecyclerViewItemDataListSortCodeMbr: Int = 1

    // 서버는 요청을 받으면, 먼저 sortCode 로 리스트를 정렬하고, lastItemUid 의 뒤쪽에서부터 sortCode 만큼의 아이템
    // 리스트를 묶어서 반환한다고 가정(page 가 아닌 lastItemUid 를 사용하는 이유는 다음 페이지에서 아이템 중복을 막기 위한 것)
    // lastItemUid 를 -1 로 입력하면 서버는 리스트 첫번째 아이템부터 반환
    fun getRecyclerViewItemDataList(
        lastServerItemUid: Long,
        pageSize: Int,
        sortCode: Int,
        onComplete: (ArrayList<GetRecyclerViewItemDataListOutputVO>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            val resultData = ArrayList<GetRecyclerViewItemDataListOutputVO>()
            for (idx in 1..getRecyclerViewItemDataListPageSizeMbr) {
                val title = "item$idx"
                resultData.add(
                    GetRecyclerViewItemDataListOutputVO(
                        idx.toLong(),
                        title
                    )
                )
            }

            // 네트워크 요청 대기 시간을 상정
            Thread.sleep(2000)

            onComplete(resultData)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    data class GetRecyclerViewItemDataListOutputVO(
        val serverItemUid: Long,
        val title: String
    )
}