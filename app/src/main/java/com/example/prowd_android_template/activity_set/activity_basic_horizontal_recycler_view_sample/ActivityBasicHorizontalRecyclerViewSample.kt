package com.example.prowd_android_template.activity_set.activity_basic_horizontal_recycler_view_sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.abstract_class.ProwdRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicHorizontalRecyclerViewSampleBinding
import java.net.SocketTimeoutException

class ActivityBasicHorizontalRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicHorizontalRecyclerViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicHorizontalRecyclerViewSampleViewModel

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicHorizontalRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicHorizontalRecyclerViewSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때
            val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken

            if (viewModelMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != viewModelMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                viewModelMbr.isDataFirstLoadingMbr = false
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                // [데이터 로딩]
                // (리사이클러 뷰 어뎁터)
                viewModelMbr.isRecyclerViewItemLoadingMbr = true
                // 세마포어 acquire 를 위한 별도 스레드 실행
                viewModelMbr.executorServiceMbr?.execute {
                    viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
                    runOnUiThread {
                        // 페이지 초기화
                        viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr = -1

                        // (로딩 처리)
                        // 화면을 비우고 로더 추가
                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                            arrayListOf(
                                ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
                                )
                            )

                        // (리포지토리 데이터 요청)
                        viewModelMbr.getRecyclerViewItemDataList(
                            viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr,
                            viewModelMbr.getRecyclerViewItemDataListPageSizeMbr,
                            viewModelMbr.getRecyclerViewItemDataListSortCodeMbr,
                            executorOnComplete = {
                                runOnUiThread runOnUiThread2@{
                                    // 로더 제거
                                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                        ArrayList()

                                    if (it.isEmpty()) {
                                        viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                                        viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                        return@runOnUiThread2
                                    }

                                    // 페이지 갱신
                                    viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr =
                                        it.last().serverItemUid

                                    // 아이템 갱신
                                    val newItemList =
                                        ArrayList<ProwdRecyclerViewAdapter.AdapterItemAbstractVO>()
                                    for (data in it) {
                                        newItemList.add(
                                            ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                                data.serverItemUid,
                                                data.title
                                            )
                                        )
                                    }

                                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                        newItemList

                                    viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                                    viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                }
                            },
                            executorOnError = {
                                runOnUiThread {
                                    // 로더 제거
                                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                        ArrayList()

                                    viewModelMbr.recyclerViewAdapterItemSemaphore.release()

                                    if (it is SocketTimeoutException) { // 타임아웃 에러
                                        // todo
                                    } else { // 그외 에러
                                        // todo
                                    }

                                    viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                }
                            }
                        )
                    }
                }
            }
        }

        // 설정 변경(화면회전)을 했는지 여부를 초기화
        // onResume 의 가장 마지막
        viewModelMbr.isChangingConfigurationsMbr = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // 설정 변경(화면회전)을 했는지 여부를 반영
        viewModelMbr.isChangingConfigurationsMbr = true
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        progressLoadingDialogMbr?.dismiss()
        binaryChooseDialogMbr?.dismiss()
        confirmDialogMbr?.dismiss()
        radioBtnDialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr =
            ViewModelProvider(this)[ActivityBasicHorizontalRecyclerViewSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicHorizontalRecyclerViewSampleAdapterSet(
            ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.recyclerView,
                false,
                onScrollReachTheEnd = {
                    if (viewModelMbr.isRecyclerViewItemLoadingMbr) {
                        return@RecyclerViewAdapter
                    }
                    viewModelMbr.isRecyclerViewItemLoadingMbr = true

                    viewModelMbr.executorServiceMbr?.execute {
                        viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
                        runOnUiThread {
                            // 다음 페이지 요청

                            // (로딩 처리)
                            // 아이템 리스트 마지막에 로더 추가
                            val cloneItemList =
                                adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                            cloneItemList.add(
                                ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
                                )
                            )
                            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                cloneItemList

                            // 로더 추가시 스크롤을 내리기
                            bindingMbr.recyclerView.smoothScrollToPosition(cloneItemList.lastIndex + 1)

                            // (리포지토리 데이터 요청)
                            viewModelMbr.getRecyclerViewItemDataList(
                                viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr,
                                viewModelMbr.getRecyclerViewItemDataListPageSizeMbr,
                                viewModelMbr.getRecyclerViewItemDataListSortCodeMbr,
                                executorOnComplete = {
                                    runOnUiThread runOnUiThread2@{
                                        // 로더 제거
                                        cloneItemList.removeLast()
                                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                            cloneItemList

                                        if (it.isEmpty()) {
                                            viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                                            viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                            return@runOnUiThread2
                                        }

                                        // 페이지 갱신
                                        viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr =
                                            it.last().serverItemUid

                                        // 아이템 갱신
                                        val newItemList =
                                            ArrayList<ProwdRecyclerViewAdapter.AdapterItemAbstractVO>()
                                        for (data in it) {
                                            newItemList.add(
                                                ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                                    data.serverItemUid,
                                                    data.title
                                                )
                                            )
                                        }

                                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                            newItemList

                                        viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                                        viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                    }
                                },
                                executorOnError = {
                                    runOnUiThread {
                                        // 로더 제거
                                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                            ArrayList()
                                        viewModelMbr.recyclerViewAdapterItemSemaphore.release()

                                        if (it is SocketTimeoutException) { // 타임아웃 에러
                                            // todo
                                        } else { // 그외 에러
                                            // todo
                                        }
                                        viewModelMbr.isRecyclerViewItemLoadingMbr = false
                                    }
                                }
                            )
                        }
                    }
                }
            )
        )
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // todo 화면 가로 리플레시
//        bindingMbr.screenRefreshLayout.setOnRefreshListener {
//            if (viewModelMbr.isRecyclerViewItemLoadingMbr) {
//                bindingMbr.screenRefreshLayout.isRefreshing = false
//                return@setOnRefreshListener
//            }
//            viewModelMbr.isRecyclerViewItemLoadingMbr = true
//
//            viewModelMbr.executorServiceMbr?.execute {
//                viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
//                runOnUiThread {
//
//                    // 페이지 초기화
//                    viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr = -1
//
//                    // (로딩 처리)
//                    // 화면을 비우고 로더 추가
//                    viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value = arrayListOf(
//                        ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
//                            adapterSetMbr.recyclerViewAdapter.maxUidMbr
//                        )
//                    )
//
//                    // (리포지토리 데이터 요청)
//                    viewModelMbr.getRecyclerViewItemDataList(
//                        viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr,
//                        viewModelMbr.getRecyclerViewItemDataListPageSizeMbr,
//                        viewModelMbr.getRecyclerViewItemDataListSortCodeMbr,
//                        onComplete = {
//                            runOnUiThread runOnUiThread2@{
//                                // 로더 제거
//                                viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
//                                    ArrayList()
//
//                                if (it.isEmpty()) {
//                                    bindingMbr.screenRefreshLayout.isRefreshing = false
//                                    viewModelMbr.recyclerViewAdapterItemSemaphore.release()
//                                    viewModelMbr.isRecyclerViewItemLoadingMbr = false
//                                    return@runOnUiThread2
//                                }
//
//                                // 페이지 갱신
//                                viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr =
//                                    it.last().serverItemUid
//
//                                // 아이템 갱신
//                                val newItemList =
//                                    ArrayList<ProwdRecyclerViewAdapter.AdapterItemAbstractVO>()
//                                for (data in it) {
//                                    newItemList.add(
//                                        ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
//                                            adapterSetMbr.recyclerViewAdapter.maxUidMbr,
//                                            data.serverItemUid,
//                                            data.title
//                                        )
//                                    )
//                                }
//
//                                viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
//                                    newItemList
//
//                                bindingMbr.screenRefreshLayout.isRefreshing = false
//                                viewModelMbr.recyclerViewAdapterItemSemaphore.release()
//                                viewModelMbr.isRecyclerViewItemLoadingMbr = false
//                            }
//                        },
//                        onError = {
//                            runOnUiThread {
//                                // 로더 제거
//                                viewModelMbr.recyclerViewAdapterVmDataMbr.itemListLiveData.value =
//                                    ArrayList()
//
//                                bindingMbr.screenRefreshLayout.isRefreshing = false
//                                viewModelMbr.recyclerViewAdapterItemSemaphore.release()
//
//                                if (it is SocketTimeoutException) { // 타임아웃 에러
//                                    // todo
//                                } else { // 그외 에러
//                                    // todo
//                                }
//                                viewModelMbr.isRecyclerViewItemLoadingMbr = false
//                            }
//                        }
//                    )
//                }
//            }
//        }

        // 아이템 셔플
        bindingMbr.doShuffleBtn.setOnClickListener {
            viewModelMbr.isRecyclerViewItemLoadingMbr = true
            viewModelMbr.executorServiceMbr?.execute {
                viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
                runOnUiThread {
                    // 현재 리스트 기반으로 변경을 주고 싶다면 아래와 같이 카피를 가져와서 조작하는 것을 권장
                    // (이동, 삭제, 생성의 경우는 그냥 current 를 해도 되지만 동일 위치의 아이템 정보 수정시에는 필수)
                    val item =
                        adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                    item.shuffle()

                    viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value = item
                    viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                    viewModelMbr.isRecyclerViewItemLoadingMbr = false
                }
            }
        }

        // 아이템 추가
        bindingMbr.addItemBtn.setOnClickListener {
            viewModelMbr.isRecyclerViewItemLoadingMbr = true

            viewModelMbr.executorServiceMbr?.execute {
                viewModelMbr.recyclerViewAdapterItemSemaphore.acquire()
                runOnUiThread {
                    // 처리 다이얼로그 표시
                    viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                        DialogProgressLoading.DialogInfoVO(
                            false,
                            "데이터를 추가합니다.",
                            onCanceled = {}
                        )

                    val serverUid =
                        if (adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr.isEmpty()) {
                            1
                        } else {
                            (adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr.last() as ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid + 1
                        }

                    val data = "Added_$serverUid"

                    viewModelMbr.postRecyclerViewItemData(
                        ActivityBasicHorizontalRecyclerViewSampleViewModel.PostRecyclerViewItemDataInputVo(
                            data
                        ),
                        executorOnComplete = {
                            runOnUiThread {
                                // 처리 다이얼로그 제거
                                viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null

                                val currentItemListClone =
                                    adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

                                currentItemListClone.add(
                                    ActivityBasicHorizontalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                        adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                        it,
                                        data
                                    )
                                )

                                viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                    currentItemListClone

                                bindingMbr.recyclerView.scrollToPosition(adapterSetMbr.recyclerViewAdapter.currentItemListLastIndexMbr)

                                viewModelMbr.recyclerViewAdapterItemSemaphore.release()
                                viewModelMbr.isRecyclerViewItemLoadingMbr = false
                            }
                        },
                        executorOnError = {
                            runOnUiThread {
                                // 처리 다이얼로그 제거
                                viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                                viewModelMbr.recyclerViewAdapterItemSemaphore.release()

                                if (it is SocketTimeoutException) { // 타임아웃 에러
                                    // todo
                                } else { // 그외 에러
                                    // todo
                                }
                                viewModelMbr.isRecyclerViewItemLoadingMbr = false
                            }
                        }
                    )
                }
            }
        }
    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                progressLoadingDialogMbr?.dismiss()

                progressLoadingDialogMbr = DialogProgressLoading(
                    this,
                    it
                )
                progressLoadingDialogMbr?.show()
            } else {
                progressLoadingDialogMbr?.dismiss()
                progressLoadingDialogMbr = null
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                binaryChooseDialogMbr?.dismiss()

                binaryChooseDialogMbr = DialogBinaryChoose(
                    this,
                    it
                )
                binaryChooseDialogMbr?.show()
            } else {
                binaryChooseDialogMbr?.dismiss()
                binaryChooseDialogMbr = null
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.confirmDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                confirmDialogMbr?.dismiss()

                confirmDialogMbr = DialogConfirm(
                    this,
                    it
                )
                confirmDialogMbr?.show()
            } else {
                confirmDialogMbr?.dismiss()
                confirmDialogMbr = null
            }
        }

        // 라디오 버튼 다이얼로그 출력 플래그
        viewModelMbr.radioButtonDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                radioBtnDialogMbr?.dismiss()

                radioBtnDialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                radioBtnDialogMbr?.show()
            } else {
                radioBtnDialogMbr?.dismiss()
                radioBtnDialogMbr = null
            }
        }

        // 리사이클러 뷰 어뎁터 데이터 바인딩
        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.setItemList(it)
        }
    }
}