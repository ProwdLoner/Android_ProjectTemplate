package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import android.app.Application
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicVerticalRecyclerViewSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// todo : 신코드 적용
// todo : 에러 화면 처리, 아이템 없을 때의 처리
// 세로 리사이클러 뷰 예시
class ActivityBasicVerticalRecyclerViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicVerticalRecyclerViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ViewModel

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicVerticalRecyclerViewSampleAdapterSet

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((Map<String, Boolean>) -> Unit))? = null

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 액티비티 실행  = onCreate() → onStart() → onResume()
    //     액티비티 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     액티비티 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     액티비티 종료 = onPause() → onStop() → onDestroy()
    //     앨티비티 화면 회전 = onPause() → onSaveInstanceState() → onStop() → onDestroy() →
    //         onCreate(savedInstanceState) → onStart() → onResume()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        onCreateSetLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (액티비티 진입 필수 권한 확인)
        // 진입 필수 권한이 클리어 되어야 로직이 실행
        permissionRequestCallbackMbr = { permissions ->
            var isPermissionAllGranted = true
            for (activityPermission in viewModelMbr.activityPermissionArrayMbr) {
                if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                        true,
                        "권한 필요",
                        "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
                        "뒤로가기",
                        onCheckBtnClicked = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                            finish()
                        },
                        onCanceled = {
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value = null

                            finish()
                        }
                    )

                    // 권한 클리어 플래그를 변경하고 break
                    isPermissionAllGranted = false
                    break
                }
            }

            if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                allPermissionsGranted()
            }
        }

        permissionRequestMbr.launch(viewModelMbr.activityPermissionArrayMbr)
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityBasicVerticalRecyclerViewSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // ActivityResultLauncher 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
            resultLauncherCallbackMbr = null
        }

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicVerticalRecyclerViewSampleAdapterSet(
            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.recyclerView,
                true, // 세로 스크롤인지 가로 스크롤인지
                1, // 이 개수를 늘리면 그리드 레이아웃으로 변화
                onScrollReachTheEnd = {
                    getRecyclerViewAdapterItemList(
                        false,
                        onComplete = {})
                }
            )
        )
    }

    // (초기 뷰 설정)
    private fun onCreateInitView() {
        // 아이템 추가
        bindingMbr.addItemBtn.setOnClickListener {
            postRecyclerViewAdapterItemList("추가 아이템", onComplete = {})
        }

        // 아이템 셔플 테스트
        bindingMbr.doShuffleBtn.setOnClickListener {
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
            if (viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr) {
                viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                return@setOnClickListener
            }
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = true
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()

            // 아이템 셔플
            val item =
                adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
            item.shuffle()
            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value = item

            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
        }

        // 화면 리플레시
        bindingMbr.screenRefreshLayout.setOnRefreshListener {
            viewModelMbr.screenRefreshLayoutOnLoadingLiveDataMbr.value = true
            getRecyclerViewAdapterItemList(true, onComplete = {
                viewModelMbr.screenRefreshLayoutOnLoadingLiveDataMbr.value = false
            })
        }
    }

    // (라이브 데이터 설정)
    private fun onCreateSetLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogProgressLoading) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogProgressLoading(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            if (it != -1) {
                val loadingText = "로딩중 $it%"
                if (dialogMbr != null) {
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressMessageTxt.text =
                        loadingText
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.visibility =
                        View.VISIBLE
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.progress = it
                }
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogBinaryChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogBinaryChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.confirmDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogConfirm) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogConfirm(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 라디오 버튼 선택 다이얼로그 출력 플래그
        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogRadioButtonChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 리사이클러 뷰 어뎁터 데이터 바인딩
        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.observe(this) {
            adapterSetMbr.recyclerViewAdapter.setItemList(it)
        }

        viewModelMbr.screenRefreshLayoutOnLoadingLiveDataMbr.observe(this) {
            bindingMbr.screenRefreshLayout.isRefreshing = it
        }
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    private fun allPermissionsGranted() {
            if (!viewModelMbr.doItAlreadyMbr) {
                // (액티비티 실행시 처음 한번만 실행되는 로직)
                viewModelMbr.doItAlreadyMbr = true

                // (초기 데이터 수집)
                getRecyclerViewAdapterItemList(true, onComplete = {})

                // (알고리즘)
            } else {
                // (회전이 아닌 onResume 로직) : 권한 클리어
                // (뷰 데이터 로딩)
                // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
                val userUid = viewModelMbr.currentLoginSessionInfoSpwMbr.userUid
                if (userUid != viewModelMbr.currentUseruserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                    // 진입 플래그 변경
                    viewModelMbr.currentUseruserUidMbr = userUid

                    // (데이터 수집)
                    getRecyclerViewAdapterItemList(true, onComplete = {})

                    // (알고리즘)
                }

                // (알고리즘)
            }
    }

    // (데이터 요청 함수)
    private fun getRecyclerViewAdapterItemList(requestRefresh: Boolean, onComplete: () -> Unit) {
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
        if (viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr) {
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
            onComplete()
            return
        }
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = true
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()

        // (로딩 처리)
        val cloneItemList =
            adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

        // 리스트 초기화 여부
        if (requestRefresh) {
            // 리스트 초기화
            cloneItemList.clear()
            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value = cloneItemList
        }

        // 아이템 리스트 마지막에 로더 추가
        cloneItemList.add(
            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
            )
        )
        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value = cloneItemList

        // 로더 추가시 스크롤을 내리기
        bindingMbr.recyclerView.smoothScrollToPosition(adapterSetMbr.recyclerViewAdapter.currentDataListLastIndexMbr)

        // (정보 요청 콜백)
        // statusCode : 서버 반환 상태값. -1 이라면 타임아웃
        // userUid : 로그인 완료시 반환되는 세션토큰
        val networkOnComplete: (statusCode: Int, itemList: ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>?) -> Unit =
            { statusCode, itemList ->
                runOnUiThread {
                    when (statusCode) {
                        1 -> {// 완료
                            // 로더 제거
                            cloneItemList.removeLast()
                            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                cloneItemList

                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있다면 그냥 멈추기
                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                                onComplete()
                            } else {
                                // 받아온 아이템 추가
                                cloneItemList.addAll(itemList)
                                viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                    cloneItemList

                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                                viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                                onComplete()
                            }
                        }
                        -1 -> { // 네트워크 에러
                            // todo
                            // 로더 제거
                            cloneItemList.removeLast()
                            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                cloneItemList

                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                            onComplete()
                        }
                        else -> { // 그외 서버 에러
                            // todo
                            // 로더 제거
                            cloneItemList.removeLast()
                            viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                                cloneItemList

                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                            onComplete()
                        }
                    }
                }
            }

        // 네트워크 비동기 요청을 가정
        // lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
        // 데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
        viewModelMbr.executorServiceMbr.execute {
            Thread.sleep(1000)

            val clone = adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
            val firstAddedItemUid =
                if (clone.isNotEmpty() && clone.size > 1) {
                    (clone[clone.lastIndex - 1] as
                            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid + 1
                } else {
                    0
                }

            val resultObj = arrayListOf<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>()

            if (firstAddedItemUid < 30) {
                for (idx in firstAddedItemUid..firstAddedItemUid + 10) {
                    val title = "item$idx"
                    resultObj.add(
                        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                            idx,
                            title
                        )
                    )
                }
            }

            networkOnComplete(1, resultObj)
        }
    }

    // todo : 로딩 처리
    private fun postRecyclerViewAdapterItemList(text: String, onComplete: () -> Unit) {
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
        if (viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr) {
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
            onComplete()
            return
        }
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = true
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()

        val networkOnComplete: (Int, Long) -> Unit = { statusCode, serverUid ->
            runOnUiThread {
                when (statusCode) {
                    1 -> {// 완료
                        val cloneItemList =
                            adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

                        cloneItemList.add(
                            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                serverUid,
                                text
                            )
                        )

                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                            cloneItemList

                        bindingMbr.recyclerView.scrollToPosition(adapterSetMbr.recyclerViewAdapter.currentItemListLastIndexMbr)

                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    -1 -> { // 네트워크 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    else -> { // 그외 서버 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                }
            }
        }

        // 네트워크 비동기 요청을 가정
        viewModelMbr.executorServiceMbr.execute {
            Thread.sleep(1000)
            val lastItemUid =
                (adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr.last() as
                        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid
            networkOnComplete(1, lastItemUid + 1)
        }
    }

    // todo : 로딩 처리
    // 아이템 데이터 변경 요청
    fun putRecyclerViewItemData(
        serverUid: Long,
        text: String,
        onComplete: () -> Unit
    ) {
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
        if (viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr) {
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
            onComplete()
            return
        }
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = true
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()

        val networkOnComplete: (Int) -> Unit = { statusCode ->
            runOnUiThread {
                when (statusCode) {
                    1 -> {// 완료
                        val cloneItemList =
                            adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                        val idx =
                            cloneItemList.indexOfFirst {
                                (it as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO)
                                    .serverItemUid == serverUid
                            }

                        (cloneItemList[idx] as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).title =
                            text

                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                            cloneItemList

                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    -1 -> { // 네트워크 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    else -> { // 그외 서버 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                }
            }
        }

        // 네트워크 비동기 요청을 가정
        viewModelMbr.executorServiceMbr.execute {
            networkOnComplete(1)
        }
    }

    // todo : 로딩 처리
    // 아이템 데이터 제거 요청
    fun deleteRecyclerViewItemData(
        serverUid: Long,
        onComplete: () -> Unit
    ) {
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
        if (viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr) {
            viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
            onComplete()
            return
        }
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = true
        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()

        val networkOnComplete: (Int) -> Unit = { statusCode ->
            runOnUiThread {
                when (statusCode) {
                    1 -> {// 완료
                        val cloneItemList =
                            adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                        val idx =
                            cloneItemList.indexOfFirst {
                                (it as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO)
                                    .serverItemUid == serverUid
                            }

                        cloneItemList.removeAt(idx)

                        viewModelMbr.recyclerViewAdapterItemListLiveDataMbr.value =
                            cloneItemList

                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    -1 -> { // 네트워크 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                    else -> { // 그외 서버 에러
                        // todo
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.acquire()
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressMbr = false
                        viewModelMbr.getRecyclerViewAdapterItemListOnProgressSemaphoreMbr.release()
                        onComplete()
                    }
                }
            }
        }

        // 네트워크 비동기 요청을 가정
        viewModelMbr.executorServiceMbr.execute {
            networkOnComplete(1)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (뷰모델 객체)
    // : 액티비티 reCreate 이후에도 남아있는 데이터 묶음 = 뷰의 데이터 모델
    //     뷰모델이 맡은 것은 화면 회전시에도 불변할 데이터의 저장
    class ViewModel(application: Application) : AndroidViewModel(application) {
        // <멤버 상수 공간>
        // (repository 모델)
        val repositorySetMbr: RepositorySet = RepositorySet.getInstance(application)

        // (스레드 풀)
        val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
            CurrentLoginSessionInfoSpw(application)

        // (앱 진입 필수 권한 배열)
        // : 앱 진입에 필요한 권한 배열.
        //     ex : Manifest.permission.INTERNET
        val activityPermissionArrayMbr: Array<String> = arrayOf()


        // ---------------------------------------------------------------------------------------------
        // <멤버 변수 공간>
        // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
        var doItAlreadyMbr = false

        // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
        var currentUseruserUidMbr: String? = null

        // 중복 요청 금지를 위한 상태 플래그
        var getRecyclerViewAdapterItemListOnProgressMbr = false
        val getRecyclerViewAdapterItemListOnProgressSemaphoreMbr = Semaphore(1)


        // ---------------------------------------------------------------------------------------------
        // <뷰모델 라이브데이터 공간>
        // 로딩 다이얼로그 출력 정보
        val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
            MutableLiveData(null)

        val progressDialogSample2ProgressValue: MutableLiveData<Int> =
            MutableLiveData(-1)

        // 선택 다이얼로그 출력 정보
        val binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
            MutableLiveData(null)

        // 확인 다이얼로그 출력 정보
        val confirmDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO?> =
            MutableLiveData(null)

        // 라디오 버튼 선택 다이얼로그 출력 정보
        val radioButtonChooseDialogInfoLiveDataMbr: MutableLiveData<DialogRadioButtonChoose.DialogInfoVO?> =
            MutableLiveData(null)

        // recyclerView 내에서 사용되는 뷰모델 데이터
        val recyclerViewAdapterItemListLiveDataMbr: MutableLiveData<ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>> =
            MutableLiveData(ArrayList())

        val screenRefreshLayoutOnLoadingLiveDataMbr: MutableLiveData<Boolean> =
            MutableLiveData(false)


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }
}