package com.example.prowd_android_template.activity_set.activity_network_recycler_view_sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.network_request_api_set.test.request_vo.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

class ActivityNetworkRecyclerViewSampleViewModel(application: Application) :
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

    // ScreenVerticalRecyclerViewAdapter 아이템 접근 세마포어
    val screenVerticalRecyclerViewAdapterDataSemaphoreMbr = Semaphore(1)


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

    // ScreenVerticalRecyclerViewAdapter 아이템 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 헤더 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 푸터 데이터 변경 진행 상태 플래그
    val changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)

    // ScreenVerticalRecyclerViewAdapter 아이템 데이터 삭제 요청 (요청이 들어오면 삭제 확인 다이얼로그를 표시)
    val screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr: MutableLiveData<Long?> =
        MutableLiveData(null)

    // ScreenVerticalRecyclerViewAdapter 데이터
    val screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr: MutableLiveData<ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO>> =
        MutableLiveData(ArrayList())
    val isScreenVerticalRecyclerViewAdapterHeaderLoadingLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)
    val isScreenVerticalRecyclerViewAdapterFooterLoadingLiveDataMbr: MutableLiveData<Boolean> =
        MutableLiveData(false)


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
    var getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncOnProgressedMbr = false
        private set

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsync(
        onComplete: (GetTestHeaderInfoOutputVO) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr.acquire()
            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncOnProgressedMbr = true

            // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
            Thread.sleep(500)

            // 원래는 네트워크에서 실제 데이터 리스트 가져오기

            try {
                // 서버 요청
                // 서버 반환값
                val networkResponseCode = 200

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드
                        // 더미 데이터 가져오기 (json)
                        val assetManager = applicationMbr.resources.assets
                        val inputStream =
                            assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_header_dummy_data.json")
                        val jsonString = inputStream.bufferedReader().use { it.readText() }

                        val jObject = JSONObject(jsonString)
                        val uid = jObject.getLong("uidLong")
                        val content = jObject.getString("contentString")

                        // json 배열 파싱
                        val dummyData = GetTestHeaderInfoOutputVO(
                            uid,
                            content
                        )

                        onComplete(dummyData)

                        getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncOnProgressedMbr =
                            false
                        getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncOnProgressedMbr = false
                getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncSemaphoreMbr.release()
            }


        }
    }

    // (푸터 데이터)
    private val getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)
    var getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncOnProgressedMbr = false
        private set

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsync(
        onComplete: (GetTestFooterInfoOutputVO) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr.acquire()
            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncOnProgressedMbr = true

            // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
            Thread.sleep(500)

            try {
                // 서버 요청
                // 서버 반환값
                val networkResponseCode = 200

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드

                        // 더미 데이터 가져오기 (json)
                        val assetManager = applicationMbr.resources.assets
                        val inputStream =
                            assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_footer_dummy_data.json")
                        val jsonString = inputStream.bufferedReader().use { it.readText() }

                        val jObject = JSONObject(jsonString)
                        val uid = jObject.getLong("uidLong")
                        val content = jObject.getString("contentString")

                        // json 배열 파싱
                        val dummyData = GetTestFooterInfoOutputVO(
                            uid,
                            content
                        )

                        onComplete(dummyData)

                        getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncOnProgressedMbr =
                            false
                        getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncOnProgressedMbr = false
                getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncSemaphoreMbr.release()
            }
        }
    }

    // (아이템 데이터)
    private val getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr = Semaphore(1)
    var getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncOnProgressedMbr = false
        private set

    // ScreenVerticalRecyclerViewAdapter 데이터 현 페이지 (초기화 시에는 데이터 리스트를 초기화 하고 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr: Long? = null

    // 한 페이지 아이템 개수 (이것을 변경하면, 데이터 리스트를 초기화 하고, 페이지 1부터 다시 받아오기)
    var getScreenVerticalRecyclerViewAdapterItemDataPageItemListSizeMbr: Int = 20

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
    var getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr: Int = 5

    // 현재 설정으로 다음 데이터 리스트 요청
    fun getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync(
        onComplete: (GetTestItemInfoListOutputVO?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.acquire()
            getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncOnProgressedMbr = true

            // 원하는 리스트 사이즈가 0이라면 그냥 리턴
            if (0 == getScreenVerticalRecyclerViewAdapterItemDataPageItemListSizeMbr) {
                onComplete(null)
                getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncOnProgressedMbr = false
                getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.release()

                return@execute
            }

            // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
            Thread.sleep(1500)

            try {
                // 서버 요청
                // 서버 반환값
                val networkResponseCode = 200

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드
                        // 더미 데이터 가져오기 (json)
                        val assetManager = applicationMbr.resources.assets
                        val inputStream =
                            assetManager.open("activity_basic_recycler_view_sample_screen_vertical_recycler_view_adapter_item_dummy_data.json")
                        val jsonString = inputStream.bufferedReader().use { it.readText() }

                        val jObject = JSONObject(jsonString)
                        val dummyDataJsonList = jObject.getJSONArray("dummyDataList")

                        val dummyDataList = ArrayList<GetTestItemInfoListOutputVO.TestInfo>()

                        // json 배열 파싱
                        for (dummyDataListIdx in 0 until dummyDataJsonList.length()) {
                            val dummyDataObj = dummyDataJsonList.getJSONObject(dummyDataListIdx)
                            val uid = dummyDataObj.getLong("uidLong")
                            val title = dummyDataObj.getString("titleString")
                            val content = dummyDataObj.getString("contentString")
                            val writeDate = dummyDataObj.getString("writeDateString")
                            dummyDataList.add(
                                GetTestItemInfoListOutputVO.TestInfo(
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

                        val startIdx = dummyDataList.indexOfFirst {
                            it.uid == getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr
                        } + 1

                        var addedItemCount = 0

                        val resultDataList = GetTestItemInfoListOutputVO(
                            100,
                            ArrayList()
                        )

                        for (dummyDataListIdx in startIdx until dummyDataList.size) {
                            resultDataList.testList.add(dummyDataList[dummyDataListIdx])

                            ++addedItemCount
                            if (addedItemCount == getScreenVerticalRecyclerViewAdapterItemDataPageItemListSizeMbr) {
                                break
                            }
                        }
                        onComplete(resultDataList)

                        getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncOnProgressedMbr =
                            false
                        getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncOnProgressedMbr = false
                getScreenVerticalRecyclerViewAdapterDataAsyncSemaphoreMbr.release()
            }

        }
    }

    // (아이템 데이터 삭제)
    private val deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr =
        Semaphore(1)
    var deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncOnProgressedMbr = false
        private set

    fun deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsync(
        contentUid: Long,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        executorServiceMbr?.execute {
            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.acquire()
            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncOnProgressedMbr = true

            // 디버그용 딜레이 시간 설정(네트워크 응답 시간이라 가정)
            Thread.sleep(500)

            try {
                // 서버 요청
                // 서버 반환값
                val networkResponseCode = 200

                when (networkResponseCode) {
                    200 -> { // 정상 응답이라 결정한 코드

                        onComplete()
                        deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncOnProgressedMbr =
                            false
                        deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()
                    }
                    else -> { // 정의된 응답 코드 외의 응답일 때 = 크래쉬를 발생
                        throw Throwable("$networkResponseCode")
                    }
                }
            } catch (t: Throwable) {
                onError(t)
                deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncOnProgressedMbr = false
                deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()
            }

            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncOnProgressedMbr = false
            deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncSemaphoreMbr.release()

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
}