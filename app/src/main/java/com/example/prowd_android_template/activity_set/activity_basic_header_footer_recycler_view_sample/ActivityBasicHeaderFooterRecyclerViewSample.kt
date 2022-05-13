package com.example.prowd_android_template.activity_set.activity_basic_header_footer_recycler_view_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicHeaderFooterRecyclerViewSampleBinding

class ActivityBasicHeaderFooterRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicHeaderFooterRecyclerViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicHeaderFooterRecyclerViewSampleViewModel

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicHeaderFooterRecyclerViewSampleBinding.inflate(layoutInflater)
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

                // 기본 헤더 푸터 생성
                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                    arrayListOf(
                        ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Header.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                            null
                        ),
                        ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Footer.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                            null
                        )
                    )

                //  데이터 로딩
                // 헤더 푸터 데이터 로딩
                viewModelMbr.isRecyclerViewAdapterHeaderLoadingLiveDataMbr.value = true
                viewModelMbr.isRecyclerViewAdapterFooterLoadingLiveDataMbr.value = true

                // 로딩 아이템 생성
                var adapterDataList: ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                    adapterSetMbr.recyclerViewAdapter.getCurrentItemDeepCopyReplica()

                adapterDataList.add(
                    1,
                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                        adapterSetMbr.recyclerViewAdapter.maxUidMbr
                    )
                )

                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value = adapterDataList

                viewModelMbr.executorServiceMbr?.execute {
                    // 네트워크 요청시 헤더, 푸터, 아이템 각각의 비동기적인 처리가 필요 (뮤텍스와 싱크)

                    // 헤더 대기 시간 가정
                    Thread.sleep(500)
                    runOnUiThread {
                        viewModelMbr.isRecyclerViewAdapterHeaderLoadingLiveDataMbr.value = false

                        viewModelMbr.executorServiceMbr?.execute {
                            // 주입용 데이터 준비
                            adapterDataList =
                                adapterSetMbr.recyclerViewAdapter.getCurrentItemDeepCopyReplica()
                            val headerVo =
                                adapterDataList.first() as ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Header.ItemVO
                            val newHeaderVo =
                                ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Header.ItemVO(
                                    headerVo.itemUid,
                                    "헤더입니다."
                                )
                            adapterDataList.removeFirst()
                            adapterDataList.add(
                                0,
                                newHeaderVo
                            )

                            // 화면 반영
                            runOnUiThread {
                                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                                    adapterDataList

                                viewModelMbr.executorServiceMbr?.execute {
                                    // 푸터 대기 시간 가정
                                    Thread.sleep(500)
                                    runOnUiThread {
                                        viewModelMbr.isRecyclerViewAdapterFooterLoadingLiveDataMbr.value =
                                            false

                                        viewModelMbr.executorServiceMbr?.execute {
                                            // 주입용 데이터 준비
                                            adapterDataList =
                                                adapterSetMbr.recyclerViewAdapter.getCurrentItemDeepCopyReplica()
                                            val footerVo =
                                                adapterDataList.last() as ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Footer.ItemVO
                                            val newFooterVo =
                                                ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Footer.ItemVO(
                                                    footerVo.itemUid,
                                                    "푸터입니다."
                                                )
                                            adapterDataList.removeLast()
                                            adapterDataList.add(
                                                newFooterVo
                                            )

                                            // 화면 반영
                                            runOnUiThread {
                                                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                                                    adapterDataList

                                                viewModelMbr.executorServiceMbr?.execute {
                                                    // 아이템 대기 가정
                                                    Thread.sleep(1000)
                                                    // 아이템 로더 제거
                                                    adapterDataList =
                                                        adapterSetMbr.recyclerViewAdapter.getCurrentItemDeepCopyReplica()
                                                    adapterDataList.removeAt(1)

                                                    runOnUiThread {
                                                        viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                                                            adapterDataList
                                                        viewModelMbr.executorServiceMbr?.execute {
                                                            // 아이템 데이터
                                                            adapterDataList =
                                                                adapterSetMbr.recyclerViewAdapter.getCurrentItemDeepCopyReplica()
                                                            adapterDataList.addAll(
                                                                1,
                                                                arrayListOf(
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item1"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item2"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item3"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item4"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item5"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item6"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item7"
                                                                    ),
                                                                    ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                                                        adapterSetMbr.recyclerViewAdapter.maxUidMbr,
                                                                        "item8"
                                                                    )
                                                                )
                                                            )

                                                            runOnUiThread {
                                                                viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.value =
                                                                    adapterDataList
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
            ViewModelProvider(this)[ActivityBasicHeaderFooterRecyclerViewSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet(
            ActivityBasicHeaderFooterRecyclerViewSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.recyclerView,
                true,
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

        // recyclerViewAdapter 데이터 리스트
        viewModelMbr.recyclerViewAdapterItemDataListLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.setNewItemList(it)
        }

        viewModelMbr.isRecyclerViewAdapterHeaderLoadingLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.isHeaderLoading = it
        }

        viewModelMbr.isRecyclerViewAdapterFooterLoadingLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.isFooterLoading = it
        }
    }
}