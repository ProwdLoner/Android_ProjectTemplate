package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample_editor.ActivityBasicRecyclerViewSampleEditor
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicRecyclerViewSampleBinding
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// 기본 리사이클러 뷰 샘플
// 동작 확인 완료 : 실제 서버 - 클라이언트로 사용하기 위해선 리포지토리 등을 커스텀 할 것.
// 아이템 제거, 이동이 아닌 아이템 내용 변경을 위해서는 동일 주소를 가진 객체를 사용할 것이 아니라, 주소가 다른 클론을 생성해서 변경해야 함을 주의
class ActivityBasicRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityBasicRecyclerViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicRecyclerViewSampleViewModel

    // (어뎁터 객체)
    private lateinit var adapterSetMbr: ActivityBasicRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null

    // (ResultLauncher 객체)
    private lateinit var resultFromActivityBasicRecyclerViewSampleEditor: ActivityResultLauncher<Intent>


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicRecyclerViewSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()
        createResultLaunchers()

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

                // (ScreenVerticalRecyclerViewAdapter 데이터 로딩)
                // 기본 헤더 푸터 생성
                viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                    arrayListOf(
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO(
                            adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                            null,
                            null
                        ),
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO(
                            adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                            null,
                            null
                        )
                    )

                // 리스트 데이터 초기화
                clearScreenVerticalRecyclerViewAdapterItemData()
                viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr = null

                // 헤더 데이터 초기화
                getScreenVerticalRecyclerViewAdapterHeaderDataAsync()

                // 푸터 데이터 초기화
                getScreenVerticalRecyclerViewAdapterFooterDataAsync()

                // 아이템 데이터 초기화
                getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync()
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
        viewModelMbr = ViewModelProvider(this)[ActivityBasicRecyclerViewSampleViewModel::class.java]

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicRecyclerViewSampleAdapterSet(
            ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter(
                this,
                viewModelMbr,
                bindingMbr.screenVerticalRecyclerView,
                true,
                onScrollHitBottom = { // 뷰 스크롤이 가장 아래쪽에 닿았을 때 = 페이징 타이밍
                    getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync()
                }
            ))
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // resultLauncher 생성
    private fun createResultLaunchers() {
        resultFromActivityBasicRecyclerViewSampleEditor = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 받아오는 데이터 : 컨텐츠 uid, 컨텐츠 타이틀, 컨텐츠 본문, 작성 시간
                val intent = result.data!!
                val resultVo =
                    intent.getParcelableExtra<ActivityBasicRecyclerViewSampleEditor.ResultVo>("result")!!

                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                    true

                // 추가할 아이템
                val itemUid = adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid
                val newItem =
                    ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO(
                        itemUid,
                        resultVo.itemContentUid,
                        resultVo.itemTitle,
                        resultVo.itemContentBody,
                        resultVo.writeTime
                    )

                // 멀티 스레드 공유 데이터 리스트 변경시 접근 세마포어 적용(데이터 레플리카를 가져와서 가공하고 반영할 때까지 블록)
                viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                // 어뎁터 주입용 데이터 리스트 클론 생성
                val screenVerticalRecyclerViewAdapterDataListCopy =
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                // 아이템 첫번째 인덱스 위치
                val itemFirstIdx = 1

                // 아이템 마지막 인덱스 위치
                val itemLastIdx =
                    screenVerticalRecyclerViewAdapterDataListCopy.lastIndex - 1

                // 정렬 상태와 상관 없이 가장 뒤에 추가
                // 그 다음 아이템 정렬 여부에 따라 정렬 후 반영 -> 추가된 아이템 위치로 스크롤 이동
                screenVerticalRecyclerViewAdapterDataListCopy.add(
                    itemLastIdx + 1,
                    newItem
                )

                // 정렬을 위한 아이템 리스트 추출
                val itemDataList =
                    ArrayList(screenVerticalRecyclerViewAdapterDataListCopy.slice(itemFirstIdx..itemLastIdx + 1))

                // item 을 기준에 따라 정렬

                // 기준에 따른 정렬
                when (viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr) {
                    0 -> {
                        itemDataList.sortWith(compareBy {
                            (it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).title
                        })
                    }
                    1 -> {
                        itemDataList.sortWith(compareByDescending {
                            (it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).title
                        })
                    }
                    2 -> {
                        itemDataList.sortWith(compareBy {
                            (it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).content
                        })
                    }
                    3 -> {
                        itemDataList.sortWith(compareByDescending {
                            (it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).content
                        })
                    }
                    4 -> {
                        // 콘텐츠 내림차순 정렬
                        itemDataList.sortWith(compareBy {
                            val transFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val date: Date =
                                transFormat.parse((it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).writeDate)!!

                            date.time
                        })
                    }
                    5 -> {
                        // 콘텐츠 오름차순 정렬
                        itemDataList.sortWith(compareByDescending {
                            val transFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            val date: Date =
                                transFormat.parse((it as ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO).writeDate)!!

                            date.time
                        })
                    }
                }

                val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                    java.util.ArrayList()

                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy.firstOrNull()!!)

                adapterDataList.addAll(itemDataList)

                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy.lastOrNull()!!)

                // 리스트에서 addedItem.itemUid 의 위치를 가져오기
                val newItemIdx = adapterDataList.indexOfFirst {
                    it.itemUid == newItem.itemUid
                }

                // 아이템 화면 생성 시점에 반짝이도록 설정
                adapterSetMbr.screenVerticalRecyclerViewAdapter.blinkIdx = newItemIdx

                // 리스트 화면 갱신
                viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                    adapterDataList

                // 새로 추가된 아이템의 위치로 스크롤 이동
                bindingMbr.screenVerticalRecyclerView.scrollToPosition(
                    newItemIdx
                )

                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                    false

                viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
            }
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // (리사이클러 뷰 설정)
        // (ScreenVerticalRecyclerViewAdapter)
        bindingMbr.screenVerticalRecyclerView.adapter =
            adapterSetMbr.screenVerticalRecyclerViewAdapter

        // 화면 리플레시
        bindingMbr.screenRefreshLayout.setOnRefreshListener {
            // 아직 데이터 로딩중이라면 리플레시 버튼 제거
            if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value!! ||
                viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value!! ||
                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!
            ) {
                bindingMbr.screenRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }

            // 리스트 데이터 초기화
            clearScreenVerticalRecyclerViewAdapterItemData()
            viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr = null

            // 헤더 데이터 초기화
            getScreenVerticalRecyclerViewAdapterHeaderDataAsync()

            // 푸터 데이터 초기화
            getScreenVerticalRecyclerViewAdapterFooterDataAsync()

            // 1 페이지 다시 가져오기
            getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync()
        }

        // 위로 이동 버튼
        bindingMbr.goToTopBtn.setOnClickListener {
            bindingMbr.screenVerticalRecyclerView.smoothScrollToPosition(0)
        }

        // 아래로 이동 버튼
        bindingMbr.goToBottomBtn.setOnClickListener {
            bindingMbr.screenVerticalRecyclerView.smoothScrollToPosition(
                adapterSetMbr.screenVerticalRecyclerViewAdapter.currentItemListMbr.lastIndex
            )
        }

        // add 버튼 = 정렬 상태라면 정렬 기준에 맞도록 추가
        // 네트워크에 데이터를 추가하고, 완료된 상태라면 기준에 맞게 로컬에 아이템 추가(실제 서버에 어떤 순서로 저장된지는 리플레시 때에 반영)
        bindingMbr.addItemBtn.setOnClickListener {
            // 받아오는 데이터 : 컨텐츠 uid, 컨텐츠 타이틀, 컨텐츠 본문, 작성 시간
            val gotoIntent =
                Intent(this, ActivityBasicRecyclerViewSampleEditor::class.java)
            resultFromActivityBasicRecyclerViewSampleEditor.launch(gotoIntent)
        }

        // 스피너 설정
        bindingMbr.itemSortSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            viewModelMbr.sortSpinnerColumnArrayMbr
        )
        bindingMbr.itemSortSpinner.setSelection(viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr)

        // 스피너를 이용한 아이템 정렬 기준 변경
        // 아이템 리스트 조작중엔 비활성화
        bindingMbr.itemSortSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position != viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr) {
                        // 현재 기준과 다를 때,

                        // 정렬 기준을 변경하기
                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr =
                            position

                        // 리스트 데이터 초기화
                        clearScreenVerticalRecyclerViewAdapterItemData()
                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr = null

                        getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync()

                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

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

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 아이템 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.observe(
            this
        ) {
            if (it) { // 아이템 로딩중에는,
                // 정렬 스피너 비활성화
                bindingMbr.itemSortSpinner.isEnabled = false
                bindingMbr.itemSortSpinner.isClickable = false

                // 아이템 추가 버튼 비활성화
                bindingMbr.addItemBtn.isEnabled = false
                bindingMbr.addItemBtn.isClickable = false
            } else {
                // 정렬 스피너 활성화
                bindingMbr.itemSortSpinner.isEnabled = true
                bindingMbr.itemSortSpinner.isClickable = true

                // 아이템 추가 버튼 활성화
                bindingMbr.addItemBtn.isEnabled = true
                bindingMbr.addItemBtn.isClickable = true

                // 아래 조건이 모두 충족되면 리플래시 로더 제거
                if (!viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value!! &&
                    !viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value!!
                ) {
                    bindingMbr.screenRefreshLayout.isRefreshing = false
                }
            }
        }

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 헤더 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.observe(
            this
        ) {
            if (it) { // 데이터 로딩중에는,

            } else {
                // 아래 조건이 모두 충족되면 리플래시 로더 제거
                if (!viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value!! &&
                    !viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!
                ) {
                    bindingMbr.screenRefreshLayout.isRefreshing = false
                }
            }
        }

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 푸터 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.observe(
            this
        ) {
            if (it) { // 데이터 로딩중에는,

            } else {
                // 아래 조건이 모두 충족되면 리플래시 로더 제거
                if (!viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value!! &&
                    !viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!
                ) {
                    bindingMbr.screenRefreshLayout.isRefreshing = false
                }
            }
        }

        // screenVerticalRecyclerViewAdapter 데이터 리스트
        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.observe(this) {
            adapterSetMbr.screenVerticalRecyclerViewAdapter.setNewItemList(it)
        }

        // screenVerticalRecyclerViewAdapter 아이템 삭제 요청
        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr.observe(
            this
        ) { contentUid ->
            if (null != contentUid) {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        true,
                        "데이터 삭제",
                        "데이터를 삭제하시겠습니까?",
                        "삭제",
                        "취소",
                        onPosBtnClicked = {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                true

                            // 로딩 다이얼로그 표시
                            viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                                DialogProgressLoading.DialogInfoVO(
                                    false,
                                    "데이터를 삭제하는 중입니다.",
                                    onCanceled = {
                                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                                    }
                                )

                            // 데이터 삭제 : 네트워크에 요청 후 정상 응답이 오면 삭제
                            viewModelMbr.deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsync(
                                contentUid,
                                onComplete = {
                                    runOnUiThread {
                                        // 멀티 스레드 공유 데이터 리스트 변경시 접근 세마포어 적용(데이터 레플리카를 가져와서 가공하고 반영할 때까지 블록)
                                        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                                        // 어뎁터 주입용 데이터 리스트 클론 생성
                                        val screenVerticalRecyclerViewAdapterDataListCopy =
                                            adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()


                                        // 삭제할 아이템의 리스트 인덱스 추출
                                        val itemIdx =
                                            screenVerticalRecyclerViewAdapterDataListCopy.indexOfFirst {
                                                if (it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO) {
                                                    it.contentUid == contentUid
                                                } else {
                                                    false
                                                }
                                            }

                                        if (-1 != itemIdx) {
                                            screenVerticalRecyclerViewAdapterDataListCopy.removeAt(
                                                itemIdx
                                            )
                                        }

                                        // 화면 반영
                                        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                                            screenVerticalRecyclerViewAdapterDataListCopy

                                        // 아이템 제거 요청 해제
                                        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr.value =
                                            null

                                        // 로딩중 다이얼로그 제거
                                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                                            null

                                        // 아이템 변경 접근 플래그 해소
                                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                            false

                                        // 데이터 접근 락 해제
                                        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
                                    }
                                },
                                onError = { deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult ->
                                    runOnUiThread {
                                        // 아이템 제거 요청 해제
                                        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr.value =
                                            null
                                        // 로딩 다이얼로그 제거
                                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                                            null
                                        // 아이템 데이터 접근 플래그 해제
                                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                            false


                                        if (deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult is SocketTimeoutException) { // 타임아웃 에러
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                DialogConfirm.DialogInfoVO(
                                                    true,
                                                    "네트워크 에러",
                                                    "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                                    "확인",
                                                    onCheckBtnClicked = {
                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    },
                                                    onCanceled = {
                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    }
                                                )
                                        } else { // 그외 에러
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                DialogConfirm.DialogInfoVO(
                                                    true,
                                                    "서버 에러",
                                                    "현재 서버의 상태가 원활하지 않습니다.\n" +
                                                            "잠시 후 다시 시도해주세요.\n" +
                                                            "\n" +
                                                            "에러 메시지 :\n" +
                                                            "${deleteScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult.message}",
                                                    "확인",
                                                    onCheckBtnClicked = {
                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null
                                                    },
                                                    onCanceled = {
                                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                            null

                                                    }
                                                )
                                        }
                                    }

                                })
                        },
                        onNegBtnClicked = {
                            // 아이템 제거 요청 해제
                            viewModelMbr.screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr.value =
                                null
                            // 아이템 데이터 접근 플래그 해제
                            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                false
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                        },
                        onCanceled = {
                            // 아이템 제거 요청 해제
                            viewModelMbr.screenVerticalRecyclerViewAdapterItemDataDeleteContentUidLiveDataMbr.value =
                                null
                            // 아이템 데이터 접근 플래그 해제
                            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                false
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                        }
                    )
            } else {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
            }
        }
    }

    // ScreenVerticalRecyclerViewAdapter 의 헤더 데이터 갱신
    private fun getScreenVerticalRecyclerViewAdapterHeaderDataAsync() {
        // 중복 요청 방지
        if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value!!) {
            return
        }

        // 헤더 데이터 로딩 플래그 실행
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value =
            true

        // 헤더 로더 출력 = 헤더의 아이템을 시머로 숨기기
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()
        adapterSetMbr.screenVerticalRecyclerViewAdapter.isHeaderLoading = true
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

        // 리포지토리 데이터 요청
        viewModelMbr.getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsync(
            onComplete = { getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult ->
                runOnUiThread {
                    // 멀티 스레드 공유 데이터 리스트 변경시 접근 세마포어 적용(데이터 레플리카를 가져와서 가공하고 반영할 때까지 블록)
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                    // 헤더로더 제거
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.isHeaderLoading = false

                    // 어뎁터 주입용 데이터 리스트 클론 생성
                    val screenVerticalRecyclerViewAdapterDataListCopy =
                        adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                    // 아이템 고유값 추출 및 기존 헤더 제거
                    val itemUid =
                        screenVerticalRecyclerViewAdapterDataListCopy.removeFirst().itemUid

                    // 외부 데이터를 어뎁터용 데이터로 변형 (구조뿐 아닌 화면에 보여질 타입까지 설정 = VO 클래스 종류 선정)
                    val headerData =
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO(
                            itemUid,
                            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult.uid,
                            getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult.content
                        )

                    // 데이터를 가장 앞에 추가
                    screenVerticalRecyclerViewAdapterDataListCopy.add(0, headerData)

                    // 아이템 갱신
                    viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                        screenVerticalRecyclerViewAdapterDataListCopy

                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value =
                        false

                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
                }
            },
            onError = { getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult ->
                runOnUiThread {
                    // 상태 초기화
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                    // 헤더로더 제거
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.isHeaderLoading = false

                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value =
                        false
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

                    if (getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult is SocketTimeoutException) { // 타임아웃 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "네트워크 에러",
                                "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                }
                            )
                    } else { // 그외 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "서버 에러",
                                "현재 서버의 상태가 원활하지 않습니다.\n" +
                                        "잠시 후 다시 시도해주세요.\n" +
                                        "\n" +
                                        "에러 메시지 :\n" +
                                        "${getScreenVerticalRecyclerViewAdapterHeaderDataOnVMAsyncResult.message}",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                }
                            )
                    }
                }
            }
        )
    }

    // ScreenVerticalRecyclerViewAdapter 의 푸터 데이터 갱신
    private fun getScreenVerticalRecyclerViewAdapterFooterDataAsync() {
        // 중복 요청 방지
        if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value!!) {
            return
        }

        // 데이터 로딩 플래그 실행
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value =
            true

        // 로더 출력
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()
        adapterSetMbr.screenVerticalRecyclerViewAdapter.isFooterLoading = true
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

        // 리포지토리 데이터 요청
        viewModelMbr.getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsync(
            onComplete = { getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult ->
                runOnUiThread {
                    // 멀티 스레드 공유 데이터 리스트 변경시 접근 세마포어 적용(데이터 레플리카를 가져와서 가공하고 반영할 때까지 블록)
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                    // 헤더로더 제거
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.isFooterLoading = false

                    // 어뎁터 주입용 데이터 리스트 클론 생성
                    val screenVerticalRecyclerViewAdapterDataListCopy =
                        adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                    // 기존 푸터 아이템 고유 번호 추출 후 삭제
                    val itemUid =
                        screenVerticalRecyclerViewAdapterDataListCopy.removeLast().itemUid

                    // 외부 데이터를 어뎁터용 데이터로 변형
                    val footerData =
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO(
                            itemUid,
                            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult.uid,
                            getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult.content
                        )

                    // 데이터를 가장 뒤에 추가
                    screenVerticalRecyclerViewAdapterDataListCopy.add(footerData)

                    // 아이템 갱신
                    viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                        screenVerticalRecyclerViewAdapterDataListCopy

                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value =
                        false

                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
                }
            },
            onError = { getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult ->
                runOnUiThread {
                    // 상태 초기화
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

                    // 헤더로더 제거
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.isFooterLoading = false

                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value =
                        false
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

                    if (getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult is SocketTimeoutException) { // 타임아웃 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "네트워크 에러",
                                "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                }
                            )
                    } else { // 그외 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "서버 에러",
                                "현재 서버의 상태가 원활하지 않습니다.\n" +
                                        "잠시 후 다시 시도해주세요.\n" +
                                        "\n" +
                                        "에러 메시지 :\n" +
                                        "${getScreenVerticalRecyclerViewAdapterFooterDataOnVMAsyncResult.message}",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                }
                            )
                    }
                }
            }
        )
    }

    // ScreenVerticalRecyclerViewAdapter 의 아이템 데이터 갱신
    private fun getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync() {
        // 중복 요청 방지
        if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
            return
        }

        // 데이터 로딩 플래그 실행
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
            true

        // 멀티 스레드 공유 데이터 리스트 변경시 접근 세마포어 적용(데이터 레플리카를 가져와서 가공하고 반영할 때까지 블록)
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()

        // (로더 출력)
        // 어뎁터 주입용 데이터 리스트 클론 생성
        val screenVerticalRecyclerViewAdapterDataListCopy =
            adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

        // 아이템 마지막 인덱스 위치
        var itemLastIdx = screenVerticalRecyclerViewAdapterDataListCopy.lastIndex - 1

        // 로더를 추가하고 화면 갱신
        screenVerticalRecyclerViewAdapterDataListCopy.add(
            itemLastIdx + 1,
            ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.ItemLoader.ItemVO(
                adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid
            )
        )

        // 화면 갱신
        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
            screenVerticalRecyclerViewAdapterDataListCopy

        // 로더 추가시 스크롤을 내리기
        bindingMbr.screenVerticalRecyclerView.smoothScrollToPosition(
            adapterSetMbr.screenVerticalRecyclerViewAdapter.currentItemListMbr.lastIndex
        )

        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

        // 리포지토리 데이터 요청
        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync(
            onComplete = { getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult ->
                runOnUiThread {
                    // 로더 지우기
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()
                    screenVerticalRecyclerViewAdapterDataListCopy.clear()
                    screenVerticalRecyclerViewAdapterDataListCopy.addAll(
                        adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()
                    )

                    // 푸터 바로 앞의 로더 제거
                    screenVerticalRecyclerViewAdapterDataListCopy.removeAt(
                        screenVerticalRecyclerViewAdapterDataListCopy.lastIndex - 1
                    )

                    // 아이템 갱신
                    viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                        screenVerticalRecyclerViewAdapterDataListCopy

                    if (null != getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult &&
                        0 != getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult.testList.size
                    ) {
                        // 이번 페이지 데이터 리스트가 비어있지 않다면,
                        // 다음 페이지 추가
                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataLastUidMbr =
                            getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult.testList.last().uid

                        // 아이템 마지막 인덱스 위치
                        itemLastIdx =
                            screenVerticalRecyclerViewAdapterDataListCopy.lastIndex - 1

                        // 외부 데이터를 어뎁터용 데이터로 변형
                        val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                            java.util.ArrayList()

                        for (data in getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult.testList) {
                            adapterDataList.add(
                                ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO(
                                    adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                                    data.uid,
                                    data.title,
                                    data.content,
                                    data.writeDate
                                )
                            )
                        }

                        screenVerticalRecyclerViewAdapterDataListCopy.addAll(
                            itemLastIdx + 1,
                            adapterDataList
                        )

                        // 아이템 갱신
                        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                            screenVerticalRecyclerViewAdapterDataListCopy

                        Log.e(
                            "test",
                            screenVerticalRecyclerViewAdapterDataListCopy.toString()
                        )

                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            false

                        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
                    } else {
                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            false
                        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
                    }
                }
            },
            onError = { getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult ->
                runOnUiThread {
                    // 로더 지우기
                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()
                    screenVerticalRecyclerViewAdapterDataListCopy.clear()
                    screenVerticalRecyclerViewAdapterDataListCopy.addAll(
                        adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()
                    )

                    // 푸터 바로 앞의 로더 제거
                    screenVerticalRecyclerViewAdapterDataListCopy.removeAt(
                        screenVerticalRecyclerViewAdapterDataListCopy.lastIndex - 1
                    )

                    // 아이템 갱신
                    viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                        screenVerticalRecyclerViewAdapterDataListCopy

                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                        false

                    viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()

                    if (getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult is SocketTimeoutException) { // 타임아웃 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "네트워크 에러",
                                "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                }
                            )
                    } else { // 그외 에러
                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "서버 에러",
                                "현재 서버의 상태가 원활하지 않습니다.\n" +
                                        "잠시 후 다시 시도해주세요.\n" +
                                        "\n" +
                                        "에러 메시지 :\n" +
                                        "${getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsyncResult.message}",
                                "확인",
                                onCheckBtnClicked = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null
                                },
                                onCanceled = {
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        null

                                }
                            )
                    }
                }
            }
        )
    }

    // ScreenVerticalRecyclerViewAdapter 데이터 초기화
    private fun clearScreenVerticalRecyclerViewAdapterItemData() {
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.acquire()
        // 어뎁터 주입용 데이터 리스트 클론 생성
        val screenVerticalRecyclerViewAdapterDataListCopy =
            adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

        val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
            java.util.ArrayList()

        adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy.firstOrNull()!!)
        adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy.lastOrNull()!!)

        // 화면 전환
        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
            adapterDataList
        viewModelMbr.screenVerticalRecyclerViewAdapterDataSemaphoreMbr.release()
    }
}