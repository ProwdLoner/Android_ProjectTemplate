package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.value_object.RepoNetGetTestInfoOutputVO
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

// todo 리사이클러 아이템 라이브 데이터 제거
class ActivityBasicRecyclerViewSampleViewModel(application: Application) :
    AndroidViewModel(application) {
    // <멤버 변수 공간>
    private val applicationMbr = application

    // (repository 모델)
    private val repositorySetMbr: RepositorySet = RepositorySet.getInstance(applicationMbr)

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (데이터)
    // 이 화면에 도달한 유저 계정 고유값(세션 토큰이 없다면 비회원 상태)
    var currentUserSessionTokenMbr: String? = null

    // 로그인 정보 객체
    lateinit var loginPrefMbr: SharedPreferences

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

    // 셔플 여부(새 아이템 추가 위치에 반영)
    var isShuffledMbr = false


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

    // ScreenVerticalRecyclerViewAdapter 아이템 데이터 변경 진행 상태 플래그
    var changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 헤더 데이터 변경 진행 상태 플래그
    var changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 푸터 데이터 변경 진행 상태 플래그
    var changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 데이터 변경 진행 상태 플래그
    var screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr: MutableLiveData<ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO>> =
        MutableLiveData(ArrayList())


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCleared() {
        executorServiceMbr?.shutdown()
        executorServiceMbr = null
        super.onCleared()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // [ScreenVerticalRecyclerViewAdapter 의 아이템 데이터 요청]
    private val getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr = Semaphore(1)

    // ScreenVerticalRecyclerViewAdapter 데이터 현 페이지 (초기화 시에는 데이터 리스트를 초기화 하고 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr = 1

    // 한 페이지 아이템 개수 (이것을 변경하면, 데이터 리스트를 초기화 하고, 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataPageItemSizeMbr: Int = 20

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync(
        activity: Activity,
        pageNum: Int,
        onComplete: (ArrayList<RepoNetGetTestInfoOutputVO>) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.acquire()

            // todo 네트워크 실제 데이터 리스트 가져오기
            val resultDataList: ArrayList<RepoNetGetTestInfoOutputVO> = ArrayList()

            // 더미 데이터 가져오기 (json)
            val assetManager = activity.resources.assets
            val inputStream =
                assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_dummy_data.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val jObject = JSONObject(jsonString)
            val dummyDataJsonList = jObject.getJSONArray("dummyDataList")
            val dummyDataList = ArrayList<RepoNetGetTestInfoOutputVO>()

            // json 배열 파싱
            for (dummyDataListIdx in 0 until dummyDataJsonList.length()) {
                val dummyDataObj = dummyDataJsonList.getJSONObject(dummyDataListIdx)
                val uid = dummyDataObj.getInt("uidInt")
                val title = dummyDataObj.getString("titleString")
                val content = dummyDataObj.getString("contentString")
                val writeDate = dummyDataObj.getString("writeDateString")
                dummyDataList.add(
                    RepoNetGetTestInfoOutputVO(
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

            if (!activity.isFinishing) {
                activity.runOnUiThread {
                    onComplete(resultDataList)
                }
            }

            getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.release()

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}