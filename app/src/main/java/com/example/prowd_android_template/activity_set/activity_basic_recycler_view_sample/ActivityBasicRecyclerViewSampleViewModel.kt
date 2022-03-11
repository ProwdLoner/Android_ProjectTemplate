package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.globalVariableConnector.GvcCurrentLoginSessionInfo
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.value_object.ScreenVerticalRecyclerViewAdapterFooterDataOutputVO
import com.example.prowd_android_template.value_object.ScreenVerticalRecyclerViewAdapterHeaderDataOutputVO
import com.example.prowd_android_template.value_object.ScreenVerticalRecyclerViewAdapterItemDataOutputVO
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

class ActivityBasicRecyclerViewSampleViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (SharedPreference 객체)
    val gvcCurrentLoginSessionInfoMbr : GvcCurrentLoginSessionInfo = GvcCurrentLoginSessionInfo(application)

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // 정렬 기준 스피너 항목
    val sortSpinnerColumnArrayMbr =
        arrayOf(
            "title (abc)",
            "title (desc)",
            "content (abc)",
            "content (desc)",
            "writeDate (abc)",
            "writeDate (desc)"
        )

    // 페이지 정렬 기준
    // (sortSpinnerColumnArray 의 인덱스 번호.
    // 이것을 변경하면, 데이터 리스트를 초기화 하고, 페이지 1부터 다시 받아오기)
    // 초기 정렬 기준 설정
    var getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr: Int = 3

    // (플래그 데이터)
    // 설정 변경 여부 : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
    var isChangingConfigurationsMbr = false

    // 데이터 수집 등, 첫번째에만 발동
    var isDataFirstLoadingMbr = true

    // ScreenVerticalRecyclerViewAdapter 아이템 접근 세마포어
    val screenVerticalRecyclerViewAdapterDataSemaphoreMbr = Semaphore(1)


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

    // ScreenVerticalRecyclerViewAdapter 아이템 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 헤더 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 푸터 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 데이터 변경 진행 상태 플래그
    val screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr: MutableLiveData<ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO>> =
        MutableLiveData(ArrayList())

    // ScreenVerticalRecyclerViewAdapter 아이템 데이터 삭제 요청 (요청이 들어오면 삭제 확인 다이얼로그를 표시)
    val screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr: MutableLiveData<Long?> =
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
    // [ScreenVerticalRecyclerViewAdapter 의 데이터 요청]
    // (헤더 데이터)
    private val getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsync(
        onComplete: (ScreenVerticalRecyclerViewAdapterHeaderDataOutputVO) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr.acquire()

            // 원래는 네트워크에서 실제 데이터 리스트 가져오기

            // 더미 데이터 가져오기 (json)
            val assetManager = applicationMbr.resources.assets
            val inputStream =
                assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_header_dummy_data.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val jObject = JSONObject(jsonString)
            val uid = jObject.getLong("uidLong")
            val content = jObject.getString("contentString")

            // json 배열 파싱
            val dummyData = ScreenVerticalRecyclerViewAdapterHeaderDataOutputVO(
                uid,
                content
            )

            onComplete(dummyData)

            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr.release()

        }
    }

    // (푸터 데이터)
    private val getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsync(
        onComplete: (ScreenVerticalRecyclerViewAdapterFooterDataOutputVO) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr.acquire()

            // 원래는 네트워크에서 실제 데이터 리스트 가져오기

            // 더미 데이터 가져오기 (json)
            val assetManager = applicationMbr.resources.assets
            val inputStream =
                assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_footer_dummy_data.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val jObject = JSONObject(jsonString)
            val uid = jObject.getLong("uidLong")
            val content = jObject.getString("contentString")

            // json 배열 파싱
            val dummyData = ScreenVerticalRecyclerViewAdapterFooterDataOutputVO(
                uid,
                content
            )

            onComplete(dummyData)

            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr.release()

        }
    }

    // (아이템 데이터)
    private val getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr = Semaphore(1)

    // ScreenVerticalRecyclerViewAdapter 데이터 현 페이지 (초기화 시에는 데이터 리스트를 초기화 하고 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr = 1

    // 한 페이지 아이템 개수 (이것을 변경하면, 데이터 리스트를 초기화 하고, 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataPageItemSizeMbr: Int = 20

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync(
        pageNum: Int,
        onComplete: (ArrayList<ScreenVerticalRecyclerViewAdapterItemDataOutputVO>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.acquire()

            // 원래는 네트워크에서 실제 데이터 리스트 가져오기
            val resultDataList: ArrayList<ScreenVerticalRecyclerViewAdapterItemDataOutputVO> =
                ArrayList()

            // 더미 데이터 가져오기 (json)
            val assetManager = applicationMbr.resources.assets
            val inputStream =
                assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_item_dummy_data.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val jObject = JSONObject(jsonString)
            val dummyDataJsonList = jObject.getJSONArray("dummyDataList")
            val dummyDataList = ArrayList<ScreenVerticalRecyclerViewAdapterItemDataOutputVO>()

            // json 배열 파싱
            for (dummyDataListIdx in 0 until dummyDataJsonList.length()) {
                val dummyDataObj = dummyDataJsonList.getJSONObject(dummyDataListIdx)
                val uid = dummyDataObj.getLong("uidLong")
                val title = dummyDataObj.getString("titleString")
                val content = dummyDataObj.getString("contentString")
                val writeDate = dummyDataObj.getString("writeDateString")
                dummyDataList.add(
                    ScreenVerticalRecyclerViewAdapterItemDataOutputVO(
                        uid,
                        title,
                        content,
                        writeDate
                    )
                )
            }

            // 기준에 따른 정렬
            when (getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr) {
                0 -> {
                    // 타이틀 내림차순 정렬
                    dummyDataList.sortWith(compareBy { it.title })
                }
                1 -> {
                    // 타이틀 오름차순 정렬
                    dummyDataList.sortWith(compareByDescending { it.title })
                }
                2 -> {
                    // 콘텐츠 내림차순 정렬
                    dummyDataList.sortWith(compareBy { it.content })
                }
                3 -> {
                    // 콘텐츠 오름차순 정렬
                    dummyDataList.sortWith(compareByDescending { it.content })
                }
                4 -> {
                    // 콘텐츠 내림차순 정렬
                    dummyDataList.sortWith(compareBy {
                        val transFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date: Date = transFormat.parse(it.writeDate)!!

                        date.time
                    })
                }
                5 -> {
                    // 콘텐츠 오름차순 정렬
                    dummyDataList.sortWith(compareByDescending {
                        val transFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val date: Date = transFormat.parse(it.writeDate)!!

                        date.time
                    })
                }
            }

            for (dummyDataListIdx in 0 until dummyDataList.size) {
                val dummyDataObj = dummyDataList[dummyDataListIdx]

                val startIdx =
                    (pageNum - 1) *
                            getScreenVerticalRecyclerViewAdapterItemDataPageItemSizeMbr
                val endIdx = startIdx + getScreenVerticalRecyclerViewAdapterItemDataPageItemSizeMbr

                if (dummyDataListIdx >= endIdx) {
                    break
                }

                if (dummyDataListIdx >= startIdx) {
                    resultDataList.add(dummyDataObj)
                }
            }
            onComplete(resultDataList)

            getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.release()

        }
    }

    // (아이템 데이터 삭제)
    private val deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)

    fun deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsync(
        contentUid: Long,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.acquire()

            // 원래는 네트워크에서 contentUid 의 아이템을 삭제 후 결과를 반환

            onComplete()

            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}