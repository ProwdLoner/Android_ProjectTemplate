package com.example.prowd_android_template.activity_set.activity_basic_recycler_view_sample

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.AbstractRecyclerViewAdapter
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicRecyclerViewSampleBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// todo 현재 정렬 기준에 * 넣기
class ActivityBasicRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityBasicRecyclerViewSampleBinding

    // (뷰 모델 객체)
    private lateinit var viewModelMbr: ActivityBasicRecyclerViewSampleViewModel

    // (어뎁터 객체)
    private lateinit var adapterSetMbr: ActivityBasicRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 네트워크 에러 다이얼로그(타임아웃 등 retrofit 반환 에러)
    private var networkErrorDialogMbr: DialogConfirm? = null

    // 서버 에러 다이얼로그(정해진 서버 반환 코드 외의 상황)
    private var serverErrorDialogMbr: DialogConfirm? = null


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

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (초기 뷰 설정)
        viewSetting()
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때

            val sessionToken =
                viewModelMbr.loginPrefMbr.getString(
                    getString(R.string.pref_login),
                    null
                )

            if (viewModelMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != viewModelMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                viewModelMbr.isDataFirstLoadingMbr = false
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                // (ScreenVerticalRecyclerViewAdapter 데이터 로딩)
                // 기본 데이터 주입(푸터, 헤더 기본 골격)
                viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                    arrayListOf(
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO(
                            adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                            ""
                        ),
                        ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO(
                            adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                            ""
                        )
                    )

                // 헤더 데이터 로딩 중복 요청 금지
                if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value!!) {
                    return
                }

                // 헤더 데이터 로딩 플래그 실행
                viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value =
                    true

                // 헤더 데이터 로딩
                getScreenVerticalRecyclerViewAdapterHeaderDataAsync {
                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.value =
                        false
                }

                // 푸터 데이터 로딩 중복 요청 금지
                if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value!!) {
                    return
                }

                // 푸터 데이터 로딩 플래그 실행
                viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value =
                    true

                // 푸터 데이터 로딩
                getScreenVerticalRecyclerViewAdapterFooterDataAsync {
                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.value =
                        false
                }

                // 아이템 데이터 로딩 중복 요청 금지
                if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                    return
                }

                // 아이템 데이터 로딩 플래그 실행
                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                    true

                getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync {
                    // 아이템 데이터 로딩 플래그 종료
                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                        false
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
        networkErrorDialogMbr?.dismiss()
        serverErrorDialogMbr?.dismiss()
        progressLoadingDialogMbr?.dismiss()

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
        adapterSetMbr = ActivityBasicRecyclerViewSampleAdapterSet(this, viewModelMbr)

    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 로그인 데이터 객체 생성
            viewModelMbr.loginPrefMbr = this.getSharedPreferences(
                getString(R.string.pref_login),
                Context.MODE_PRIVATE
            )

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.loginPrefMbr.getString(
                    getString(R.string.pref_login_session_token_string),
                    null
                )
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // (리사이클러 뷰 설정)
        // (ScreenVerticalRecyclerViewAdapter)
        // 리사이클러 뷰 레이아웃 설정
        // todo 결합
        val scrollAdapterLayoutManager = LinearLayoutManager(this)
        scrollAdapterLayoutManager.orientation = LinearLayoutManager.VERTICAL
        bindingMbr.screenVerticalRecyclerView.layoutManager = scrollAdapterLayoutManager

        // 리사이클러 뷰 어뎁터 설정
        bindingMbr.screenVerticalRecyclerView.adapter =
            adapterSetMbr.screenVerticalRecyclerViewAdapter

        // 리사이클러 뷰 스크롤 설정
        bindingMbr.screenVerticalRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) { // 스크롤 다운을 했을 때에만 발동
                    val visibleItemCount = scrollAdapterLayoutManager.childCount
                    val totalItemCount = scrollAdapterLayoutManager.itemCount
                    val pastVisibleItems =
                        scrollAdapterLayoutManager.findFirstVisibleItemPosition()

                    if (visibleItemCount + pastVisibleItems >= totalItemCount) {

                        if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                            return
                        }

                        // 아이템 데이터 로딩 플래그 실행
                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            true

                        getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync(
                            onComplete = {
                                // 아이템 데이터 로딩 플래그 종료
                                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                    false
                            }
                        )
                    }
                }
            }
        })

        // 화면 리플레시
        bindingMbr.screenRefreshLayout.setOnRefreshListener {

            if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                // 현재 데이터 변경이 일어나고 있다면,
                // 리플래시 취소
                bindingMbr.screenRefreshLayout.isRefreshing = false
                return@setOnRefreshListener
            }

            // 아이템 데이터 로딩 플래그 실행
            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                true

            // 리스트 페이지 초기화
            viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr = 1

            // 리스트 데이터 초기화
            clearScreenVerticalRecyclerViewAdapterItemData()

            viewModelMbr.isShuffledMbr = false

            getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync(
                onComplete = {
                    // 아이템 데이터 로딩 플래그 종료
                    viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                        false
                }
            )
        }

        // 아이템 셔플 테스트
        bindingMbr.itemShuffleBtn.setOnClickListener {
            if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                return@setOnClickListener
            }

            // 아이템 데이터 로딩 플래그 실행
            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                true

            // 어뎁터 주입용 데이터 리스트 클론 생성
            val screenVerticalRecyclerViewAdapterDataListCopy =
                adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

            // 헤더 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
            val headerIdx =
                screenVerticalRecyclerViewAdapterDataListCopy.indexOfFirst {
                    it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO
                }

            // 아이템 리스트의 첫번째 인덱스
            val firstItemIdx = if (-1 == headerIdx) { // 헤더가 존재하지 않으면,
                0
            } else { // 헤더가 존재하면,
                1
            }

            // 푸터 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
            val footerIdx =
                screenVerticalRecyclerViewAdapterDataListCopy.indexOfLast {
                    it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO
                }

            // 아이템 리스트의 마지막 인덱스
            val endItemIdx = if (-1 == footerIdx) {
                screenVerticalRecyclerViewAdapterDataListCopy.size - 1
            } else {
                footerIdx - 1
            }

            val itemDataList =
                screenVerticalRecyclerViewAdapterDataListCopy.slice(firstItemIdx..endItemIdx)
            val sortedItemDataList = itemDataList.shuffled()

            val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                java.util.ArrayList()

            if (headerIdx != -1) {
                // 헤더가 존재하면 헤더 데이터를 먼저 추가
                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[headerIdx])
            }

            adapterDataList.addAll(sortedItemDataList)

            if (footerIdx != -1) {
                // 푸터가 존재하면 푸터 데이터를 마지막에 추가
                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[footerIdx])
            }

            // 화면 갱신
            viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                adapterDataList

            viewModelMbr.isShuffledMbr = true

            // 아이템 데이터 로딩 플래그 종료
            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                false
        }

        // 위로 이동 버튼
        bindingMbr.goToTopBtn.setOnClickListener {
            bindingMbr.screenVerticalRecyclerView.smoothScrollToPosition(0)
        }

        // 아래로 이동 버튼
        bindingMbr.goToBottomBtn.setOnClickListener {
            bindingMbr.screenVerticalRecyclerView.smoothScrollToPosition(
                adapterSetMbr.screenVerticalRecyclerViewAdapter.currentItemListMbr.size - 1
            )
        }

        // add 버튼 = 셔플 상태라면 순서 상관 없이 마지막에 추가, 정렬 상태라면 정렬 기준에 맞도록 추가
        bindingMbr.addItemBtn.setOnClickListener {
            if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                return@setOnClickListener
            }

            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                true

            // 어뎁터 주입용 데이터 리스트 클론 생성
            val screenVerticalRecyclerViewAdapterDataListCopy =
                adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

            // 헤더 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
            val headerIdx =
                screenVerticalRecyclerViewAdapterDataListCopy.indexOfFirst {
                    it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO
                }

            // 아이템 리스트의 첫번째 인덱스
            val firstItemIdx = if (-1 == headerIdx) { // 헤더가 존재하지 않으면,
                0
            } else { // 헤더가 존재하면,
                1
            }

            // 푸터 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
            val footerIdx =
                screenVerticalRecyclerViewAdapterDataListCopy.indexOfLast {
                    it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO
                }

            // 아이템 리스트의 마지막 인덱스
            val endItemIdx = if (-1 == footerIdx) {
                screenVerticalRecyclerViewAdapterDataListCopy.size - 1
            } else {
                footerIdx - 1
            }

            // 추가할 아이템
            // todo : 실제 추가할 때에는 리포지토리에 반영하고, uid 등 서버 입력 시점에 정해지는 데이터를 받아오고 그 여부에 따라 화면 갱신
            // get UTC now
            val utcDataFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
            utcDataFormat.timeZone = TimeZone.getTimeZone("UTC")
            val utcTimeString = utcDataFormat.format(Date())

            val addedItem =
                ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Item1.ItemVO(
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid,
                    1,
                    "added title ${screenVerticalRecyclerViewAdapterDataListCopy.size}",
                    "added content ${screenVerticalRecyclerViewAdapterDataListCopy.size}",
                    utcTimeString
                )

            // 정렬 상태와 상관 없이 가장 뒤에 추가
            // 그 다음 아이템 정렬 여부에 따라 정렬 후 반영 -> 추가된 아이템 위치로 스크롤 이동
            screenVerticalRecyclerViewAdapterDataListCopy.add(
                endItemIdx + 1,
                addedItem
            )

            // 정렬을 위한 아이템 리스트 추출
            val itemDataList =
                ArrayList(screenVerticalRecyclerViewAdapterDataListCopy.slice(firstItemIdx..endItemIdx + 1))

            if (!viewModelMbr.isShuffledMbr) { // 셔플 상태가 아니면 == 정렬 상태라면,
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
            }

            val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                java.util.ArrayList()

            if (headerIdx != -1) {
                // 헤더가 존재하면 헤더 데이터를 먼저 추가
                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[headerIdx])
            }

            adapterDataList.addAll(itemDataList)

            if (footerIdx != -1) {
                // 푸터가 존재하면 푸터 데이터를 마지막에 추가
                adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[footerIdx])
            }

            // 리스트에서 addedItem.itemUid 의 위치를 가져오기
            val newItemIdx = adapterDataList.indexOfFirst {
                it.itemUid == addedItem.itemUid
            }

            // 리스트 화면 갱신
            viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                adapterDataList

            // 새로 추가된 아이템의 위치로 스크롤 이동
            bindingMbr.screenVerticalRecyclerView.scrollToPosition(
                newItemIdx
            )

            adapterSetMbr.screenVerticalRecyclerViewAdapter.blinkIdx = newItemIdx

            viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                false
        }

        // 스피너 설정
        val sortSpinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            viewModelMbr.sortSpinnerColumnArrayMbr
        )
        bindingMbr.itemSortSpinner.adapter = sortSpinnerAdapter
        bindingMbr.itemSortSpinner.setSelection(viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr)

        // 스피너 정렬 기준 변경
        bindingMbr.itemSortSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position != viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr) {
                        // 정렬 기준을 변경하기
                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataPageItemSortByMbr =
                            position

                        // 변경된 기준에 따른 데이터 초기화
                        if (viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value!!) {
                            return
                        }

                        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                            true

                        // 리스트 페이지 초기화
                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr = 1

                        // 리스트 데이터 초기화
                        clearScreenVerticalRecyclerViewAdapterItemData()
                        viewModelMbr.isShuffledMbr = false

                        getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync(
                            onComplete = {
                                viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.value =
                                    false
                            }
                        )

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

        // 네트워크 에러 다이얼로그 출력 플래그
        viewModelMbr.networkErrorDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                networkErrorDialogMbr = DialogConfirm(
                    this,
                    it
                )
                networkErrorDialogMbr?.show()
            } else {
                networkErrorDialogMbr?.dismiss()
                networkErrorDialogMbr = null
            }
        }

        // 서버 에러 다이얼로그 출력 플래그
        viewModelMbr.serverErrorDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                serverErrorDialogMbr = DialogConfirm(
                    this,
                    it
                )
                serverErrorDialogMbr?.show()
            } else {
                serverErrorDialogMbr?.dismiss()
                serverErrorDialogMbr = null
            }
        }

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 아이템 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterItemDataOnProgressLiveDataMbr.observe(
            this
        ) {
            if (it) {
                bindingMbr.itemSortSpinner.isEnabled = false
                bindingMbr.itemSortSpinner.isClickable = false
            } else {
                bindingMbr.screenRefreshLayout.isRefreshing = false
                bindingMbr.itemSortSpinner.isEnabled = true
                bindingMbr.itemSortSpinner.isClickable = true
            }
        }

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 헤더 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterHeaderDataOnProgressLiveDataMbr.observe(
            this
        ) {

        }

        // screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr 푸터 데이터 로딩 중 플래그
        viewModelMbr.changeScreenVerticalRecyclerViewAdapterFooterDataOnProgressLiveDataMbr.observe(
            this
        ) {

        }

        // screenVerticalRecyclerViewAdapter 데이터 리스트
        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.observe(this) {
            adapterSetMbr.screenVerticalRecyclerViewAdapter.setNewItemList(it)
        }
    }

    // ScreenVerticalRecyclerViewAdapter 의 헤더 데이터 갱신
    // todo : 로더 추가, 리스트 데이터쪽에 세마포어
    private fun getScreenVerticalRecyclerViewAdapterHeaderDataAsync(onComplete: () -> Unit) {
        // 디버그용 딜레이 시간 설정
        object :
            CountDownTimer(
                500L,
                100L
            ) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                // 어뎁터 주입용 데이터 리스트 클론 생성
                val screenVerticalRecyclerViewAdapterDataListCopy =
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                // 헤더 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
                val headerIdx =
                    screenVerticalRecyclerViewAdapterDataListCopy.indexOfFirst {
                        it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO
                    }

                val headerUid = if (-1 != headerIdx) {
                    // 헤더가 이미 존재하면 해당 아이디를 가져오기
                    screenVerticalRecyclerViewAdapterDataListCopy[headerIdx].itemUid
                } else {
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid
                }

                if (-1 != headerIdx) {
                    // 헤더가 존재하면 가장 앞에 존재할 헤더를 제거
                    screenVerticalRecyclerViewAdapterDataListCopy.removeAt(headerIdx)
                }

                // todo 새로운 데이터 가져오기
                val headerData =
                    ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO(
                        headerUid,
                        "헤더 본문입니다."
                    )

                // 데이터를 가장 앞에 추가
                screenVerticalRecyclerViewAdapterDataListCopy.add(0, headerData)
                // 아이템 갱신
                viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                    screenVerticalRecyclerViewAdapterDataListCopy

                onComplete()
            }
        }.start()
    }

    // ScreenVerticalRecyclerViewAdapter 의 푸터 데이터 갱신
    // todo : 로더 추가, 리스트 데이터쪽에 세마포어
    private fun getScreenVerticalRecyclerViewAdapterFooterDataAsync(onComplete: () -> Unit) {
        // 디버그용 딜레이 시간 설정
        object :
            CountDownTimer(
                500L,
                100L
            ) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                // 어뎁터 주입용 데이터 리스트 클론 생성
                val screenVerticalRecyclerViewAdapterDataListCopy =
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                // 푸터 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
                val footerIdx =
                    screenVerticalRecyclerViewAdapterDataListCopy.indexOfLast {
                        it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO
                    }

                val footerUid = if (-1 != footerIdx) {
                    // 헤더가 이미 존재하면 해당 아이디를 가져오기
                    screenVerticalRecyclerViewAdapterDataListCopy[footerIdx].itemUid
                } else {
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.maxUid
                }

                if (-1 != footerIdx) {
                    // 푸터가 존재하면 가장 뒤에 존재할 푸터를 제거
                    screenVerticalRecyclerViewAdapterDataListCopy.removeAt(
                        footerIdx
                    )
                }

                // 새로운 데이터 가져오기
                val footerData =
                    ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO(
                        footerUid,
                        "푸터 본문입니다."
                    )

                // 데이터를 가장 뒤에 추가
                screenVerticalRecyclerViewAdapterDataListCopy.add(footerData)

                // 아이템 갱신
                viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                    screenVerticalRecyclerViewAdapterDataListCopy

                onComplete()
            }
        }.start()
    }

    // ScreenVerticalRecyclerViewAdapter 의 아이템 데이터 갱신
    // todo : 로더 추가, 리스트 데이터쪽에 세마포어
    private fun getScreenVerticalRecyclerViewAdapterItemDataNextPageAsync(onComplete: () -> Unit) {
        // 디버그용 딜레이 시간 설정
        object :
            CountDownTimer(
                1000L,
                100L
            ) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                // 어뎁터 주입용 데이터 리스트 클론 생성
                val screenVerticalRecyclerViewAdapterDataListCopy =
                    adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

                // 푸터 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
                val footerIdx =
                    screenVerticalRecyclerViewAdapterDataListCopy.indexOfLast {
                        it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO
                    }

                // 아이템 리스트 마지막 인덱스
                val endItemIdx = if (-1 == footerIdx) {
                    screenVerticalRecyclerViewAdapterDataListCopy.size - 1
                } else {
                    footerIdx - 1
                }

                // 아이템 데이터 가져오기
                viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync(
                    this@ActivityBasicRecyclerViewSample,
                    viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr,
                    onComplete = { dataList ->

                        if (0 == dataList.size) {
                            onComplete()
                            return@getScreenVerticalRecyclerViewAdapterItemDataNextPageOnVMAsync
                        }

                        viewModelMbr.getScreenVerticalRecyclerViewAdapterItemDataCurrentPageMbr++

                        // 외부 데이터를 어뎁터용 데이터로 변형
                        val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
                            java.util.ArrayList()

                        for (data in dataList) {
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
                            endItemIdx + 1,
                            adapterDataList
                        )

                        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
                            screenVerticalRecyclerViewAdapterDataListCopy

                        onComplete()
                    })
            }
        }.start()
    }

    // ScreenVerticalRecyclerViewAdapter 데이터 초기화
    private fun clearScreenVerticalRecyclerViewAdapterItemData() {
        // 어뎁터 주입용 데이터 리스트 클론 생성
        val screenVerticalRecyclerViewAdapterDataListCopy =
            adapterSetMbr.screenVerticalRecyclerViewAdapter.getCurrentItemDeepCopyReplica()

        // 헤더 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
        val headerIdx =
            screenVerticalRecyclerViewAdapterDataListCopy.indexOfFirst {
                it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Header.ItemVO
            }

        // 푸터 인덱스 찾기 = 존재하지 않으면 -1 (리스트에서 하나만 존재한다고 가정)
        val footerIdx =
            screenVerticalRecyclerViewAdapterDataListCopy.indexOfLast {
                it is ActivityBasicRecyclerViewSampleAdapterSet.ScreenVerticalRecyclerViewAdapter.Footer.ItemVO
            }

        val adapterDataList: java.util.ArrayList<AbstractRecyclerViewAdapter.AdapterItemAbstractVO> =
            java.util.ArrayList()

        if (headerIdx != -1) {
            // 헤더가 존재하면 헤더 데이터를 먼저 추가
            adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[headerIdx])
        }

        if (footerIdx != -1) {
            // 푸터가 존재하면 푸터 데이터를 마지막에 추가
            adapterDataList.add(screenVerticalRecyclerViewAdapterDataListCopy[footerIdx])
        }

        viewModelMbr.screenVerticalRecyclerViewAdapterItemDataListLiveDataMbr.value =
            adapterDataList
    }
}