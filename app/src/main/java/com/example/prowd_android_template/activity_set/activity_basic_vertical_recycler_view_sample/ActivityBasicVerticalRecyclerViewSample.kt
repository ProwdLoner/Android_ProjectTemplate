package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.abstract_class.ProwdRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicVerticalRecyclerViewSampleBinding
import java.net.SocketTimeoutException

// todo : 에러 화면 처리, 아이템 없을 때의 처리
class ActivityBasicVerticalRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicVerticalRecyclerViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicVerticalRecyclerViewSampleViewModel

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicVerticalRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicVerticalRecyclerViewSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

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

                //  데이터 로딩
                refreshScreenData()
            }
        }
    }

    override fun onStop() {
        // 설정 변경(화면회전)을 했는지 여부를 초기화
        viewModelMbr.isChangingConfigurationsMbr = false
        super.onStop()
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
            ViewModelProvider(this)[ActivityBasicVerticalRecyclerViewSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicVerticalRecyclerViewSampleAdapterSet(
            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.recyclerView,
                true,
                viewModelMbr.recyclerViewAdapterDataMbr,
                null
            )
        )
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 위로 이동 버튼
        bindingMbr.goToUpBtn.setOnClickListener {
            bindingMbr.recyclerView.smoothScrollToPosition(0)
        }

        // 아래로 이동 버튼
        bindingMbr.goToDownBtn.setOnClickListener {
            bindingMbr.recyclerView.smoothScrollToPosition(adapterSetMbr.recyclerViewAdapter.currentItemListMbr.lastIndex)
        }

        // 아이템 셔플
        bindingMbr.doShuffleBtn.setOnClickListener {
            viewModelMbr.executorServiceMbr?.execute {
                viewModelMbr.recyclerViewAdapterDataItemSemaphore.acquire()
                // 현재 리스트 기반으로 변경을 주고 싶다면 아래와 같이 카피를 가져와서 조작하는 것을 권장
                // (이동, 삭제, 생성의 경우는 그냥 current 를 해도 되지만 동일 위치의 아이템 정보 수정시에는 필수)
                val item =
                    adapterSetMbr.recyclerViewAdapter.getCurrentItemListDeepCopyReplicaOnlyItem()
                item.shuffle()

                runOnUiThread {
                    viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = item
                    viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
                }
            }
        }

        // 아이템 추가
        bindingMbr.addItemBtn.setOnClickListener {
            viewModelMbr.executorServiceMbr?.execute {
                viewModelMbr.recyclerViewAdapterDataItemSemaphore.acquire()
                // todo 반짝임 효과
                val item =
                    adapterSetMbr.recyclerViewAdapter.getCurrentItemListDeepCopyReplicaOnlyItem()
                val lastServerUid =
                    (item.last() as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid
                item.add(
                    ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                        lastServerUid + 1,
                        "added ${lastServerUid + 1}"
                    )
                )

                runOnUiThread {
                    viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = item
                    bindingMbr.recyclerView.smoothScrollToPosition(adapterSetMbr.recyclerViewAdapter.getCurrentItemListOnlyItemLastIndex())
                    viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
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
    }

    // 화면 데이터 전체 새로고침
    private fun refreshScreenData() {
        viewModelMbr.executorServiceMbr?.execute {
            // (데이터 초기화)
            viewModelMbr.recyclerViewAdapterDataItemSemaphore.acquire()
            runOnUiThread {
                viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = ArrayList()
            }
            viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr = -1

            // (로딩 처리)
            // 로더 아이템을 추가
            val adapterDataList: ArrayList<ProwdRecyclerViewAdapter.AdapterItemAbstractVO> =
                adapterSetMbr.recyclerViewAdapter.getCurrentItemListDeepCopyReplicaOnlyItem()

            adapterDataList.add(
                ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                    adapterSetMbr.recyclerViewAdapter.maxUidMbr
                )
            )

            runOnUiThread {
                viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = adapterDataList
            }

            // (데이터 요청)
            viewModelMbr.getRecyclerViewItemDataList(
                viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr,
                viewModelMbr.getRecyclerViewItemDataListPageSizeMbr,
                viewModelMbr.getRecyclerViewItemDataListSortCodeMbr,
                onComplete = {
                    runOnUiThread {
                        // 로더 제거
                        viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = ArrayList()

                        if (it.isEmpty()) {
                            viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
                            return@runOnUiThread
                        }

                        // 아이템 갱신
                        val newItemList =
                            ArrayList<ProwdRecyclerViewAdapter.AdapterItemAbstractVO>()
                        for (data in it) {
                            newItemList.add(
                                ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                    adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                    data.serverItemUid,
                                    data.title
                                )
                            )
                        }

                        viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = newItemList
                        viewModelMbr.getRecyclerViewItemDataListLastServerItemUidMbr =
                            (newItemList.last() as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid
                        viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
                    }
                },
                onError = {
                    runOnUiThread {
                        // 로더 제거
                        viewModelMbr.recyclerViewAdapterDataMbr.itemListLiveData.value = ArrayList()

                        if (it is SocketTimeoutException) { // 타임아웃 에러
                            // todo
                            viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
                        } else { // 그외 에러
                            // todo
                            viewModelMbr.recyclerViewAdapterDataItemSemaphore.release()
                        }
                    }
                }
            )

        }
    }
}