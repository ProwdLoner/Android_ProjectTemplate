package com.example.prowd_android_template.activity_set.activity_basic_vertical_recycler_view_sample

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.prowd_android_template.R
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.application_session_service.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicVerticalRecyclerViewSampleBinding
import com.example.prowd_android_template.databinding.ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItem1Binding
import com.example.prowd_android_template.databinding.ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItemLoaderBinding
import com.example.prowd_android_template.databinding.ItemEmptyBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_class.ThreadConfluenceObj
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// 세로 리사이클러 뷰 예시
class ActivityBasicVerticalRecyclerViewSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicVerticalRecyclerViewSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicVerticalRecyclerViewSampleAdapterSet

    // (SharedPreference 객체)
    // 클래스 비휘발성 저장객체
    lateinit var classSpwMbr: ActivityBasicVerticalRecyclerViewSampleSpw

    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

    // (스레드 풀)
    val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null
    var shownDialogInfoVOMbr: InterfaceDialogInfoVO? = null
        set(value) {
            when (value) {
                is DialogBinaryChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogBinaryChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogConfirm.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogConfirm(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogProgressLoading.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogProgressLoading(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogRadioButtonChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogRadioButtonChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                else -> {
                    dialogMbr?.dismiss()
                    dialogMbr = null

                    field = null
                    return
                }
            }
            field = value
        }

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((Map<String, Boolean>) -> Unit))? = null
    var permissionRequestOnProgressMbr = false
    val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()
    }

    override fun onResume() {
        super.onResume()

        // (권한 체크 후 함수 실행)
        // : requestPermission 시에 onPause 되고, onResume 이 다시 실행되므로 리퀘스트 복귀 시엔 여기를 지나게 되어있음
        var isPermissionAllGranted = true
        for (activityPermission in activityPermissionArrayMbr) {
            if (checkSelfPermission(activityPermission)
                == PackageManager.PERMISSION_DENIED
            ) { // 거부된 필수 권한이 존재
                // 권한 클리어 플래그를 변경하고 break
                isPermissionAllGranted = false
                break
            }
        }

        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
            allPermissionsGranted()
            return
        }

        // (권한 비충족으로 인한 권한 요청)
        // : 권한 요청시엔 onPause 되었다가 다시 onResume 으로 복귀함
        executorServiceMbr.execute {
            permissionRequestOnProgressSemaphoreMbr.acquire()
            runOnUiThread {
                if (!permissionRequestOnProgressMbr) { // 현재 권한 요청중이 아님
                    permissionRequestOnProgressMbr = true
                    permissionRequestOnProgressSemaphoreMbr.release()
                    // (액티비티 진입 필수 권한 확인)
                    // 진입 필수 권한이 클리어 되어야 로직이 실행

                    // 권한 요청 콜백
                    permissionRequestCallbackMbr = { permissions ->
                        var isPermissionAllGranted1 = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted1 = false
                                break
                            }
                        }

                        if (isPermissionAllGranted1) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

                        } else if (!neverAskAgain) { // 단순 거부
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "권한 필요",
                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                "뒤로가기",
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                }
                            )

                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "해당 서비스를 이용하기 위해선\n" +
                                            "필수 권한 승인이 필요합니다.\n" +
                                            "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.fromParts("package", packageName, null)

                                        resultLauncherCallbackMbr = {
                                            // 설정 페이지 복귀시 콜백
                                            var isPermissionAllGranted2 = true
                                            for (activityPermission in activityPermissionArrayMbr) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        this,
                                                        activityPermission
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) { // 거부된 필수 권한이 존재
                                                    // 권한 클리어 플래그를 변경하고 break
                                                    isPermissionAllGranted2 = false
                                                    break
                                                }
                                            }

                                            if (isPermissionAllGranted2) { // 권한 승인
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

                                            } else { // 권한 거부
                                                shownDialogInfoVOMbr =
                                                    DialogConfirm.DialogInfoVO(
                                                        true,
                                                        "권한 요청",
                                                        "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                        "뒤로가기",
                                                        onCheckBtnClicked = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        },
                                                        onCanceled = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        }
                                                    )
                                            }
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    }
                                )

                        }
                    }

                    // 권한 요청
                    permissionRequestMbr.launch(activityPermissionArrayMbr)
                } else { // 현재 권한 요청중
                    permissionRequestOnProgressSemaphoreMbr.release()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        permissionRequestOnProgressSemaphoreMbr.acquire()
        if (permissionRequestOnProgressMbr) {
            permissionRequestOnProgressSemaphoreMbr.release()
            // 권한 요청중엔 onPause 가 실행될 수 있기에 아래에 위치할 정상 pause 로직 도달 방지
            return
        }
        permissionRequestOnProgressSemaphoreMbr.release()

        // (onPause 알고리즘)
    }

    override fun onDestroy() {
        super.onDestroy()

        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    // : 해당 이벤트 발생시 처리할 로직을 작성
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 화면회전 landscape

        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // 화면회전 portrait

        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (아이템 내용 변경)
    fun putActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
        serverUid: Long,
        text: String,
        onComplete: () -> Unit
    ) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            // 로더 추가
            runOnUiThread {
                shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                    false,
                    "저장중입니다. 잠시만 기다려주세요.",
                    onCanceled = {}
                )
            }

            // (정보 요청 콜백)
            // statusCode
            // : 서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            val putItemOnComplete: (statusCode: Int) -> Unit =
                { statusCode ->
                    // 로더 제거
                    runOnUiThread {
                        shownDialogInfoVOMbr = null
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            val cloneItemList =
                                adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

                            // 데이터 화면 변경
                            val idx =
                                cloneItemList.indexOfFirst {
                                    (it as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO)
                                        .serverItemUid == serverUid
                                }

                            (cloneItemList[idx] as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).title =
                                text

                            // 받아온 아이템 추가
                            runOnUiThread {
                                adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList)
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        -1 -> { // 네트워크 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "네트워크 불안정",
                                    "현재 네트워크 연결이 불안정합니다.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        else -> { // 그외 서버 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "기술적 문제",
                                    "기술적 문제가 발생했습니다.\n잠시후 다시 시도해주세요.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                    }
                }

            // 네트워크 요청
            executorServiceMbr.execute {
                // 요청 대기시간 가정
                Thread.sleep(1000)

                putItemOnComplete(1)
            }
        }
    }

    // (아이템 삭제)
    fun deleteActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
        serverUid: Long,
        onComplete: () -> Unit
    ) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            // 로더 추가
            runOnUiThread {
                shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                    false,
                    "삭제중입니다. 잠시만 기다려주세요.",
                    onCanceled = {}
                )
            }

            // (정보 요청 콜백)
            // statusCode
            // : 서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            val deleteItemOnComplete: (statusCode: Int) -> Unit =
                { statusCode ->
                    // 로더 제거
                    runOnUiThread {
                        shownDialogInfoVOMbr = null
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            val cloneItemList =
                                adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

                            // 데이터 화면 변경
                            val idx =
                                cloneItemList.indexOfFirst {
                                    (it as ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO)
                                        .serverItemUid == serverUid
                                }

                            cloneItemList.removeAt(idx)

                            // 받아온 아이템 추가
                            runOnUiThread {
                                adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList)
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        -1 -> { // 네트워크 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "네트워크 불안정",
                                    "현재 네트워크 연결이 불안정합니다.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        else -> { // 그외 서버 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "기술적 문제",
                                    "기술적 문제가 발생했습니다.\n잠시후 다시 시도해주세요.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                    }
                }

            // 네트워크 요청
            executorServiceMbr.execute {
                // 요청 대기시간 가정
                Thread.sleep(1000)

                deleteItemOnComplete(1)
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    // : 클래스에서 사용할 객체를 초기 생성
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityBasicVerticalRecyclerViewSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicVerticalRecyclerViewSampleAdapterSet(
            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter(
                this,
                bindingMbr.recyclerView,
                true, // 세로 스크롤인지 가로 스크롤인지
                1, // 이 개수를 늘리면 그리드 레이아웃으로 변화
                onScrollReachTheEnd = {
                    getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataList(
                        onComplete = {}
                    )
                }
            ))

        // SPW 객체 생성
        classSpwMbr = ActivityBasicVerticalRecyclerViewSampleSpw(application)
        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)

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

    }

    // (초기 뷰 설정)
    // : 뷰 리스너 바인딩, 초기 뷰 사이즈, 위치 조정 등
    private fun onCreateInitView() {
        // 화면 리플레시
        bindingMbr.screenRefreshLayout.setOnRefreshListener {
            refreshWholeScreenData(onComplete = {
                bindingMbr.screenRefreshLayout.isRefreshing = false
            })
        }

        // 아이템 추가
        bindingMbr.addItemBtn.setOnClickListener {
            postActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
                "추가 아이템",
                onComplete = {})
        }

        // 아이템 셔플 테스트
        bindingMbr.doShuffleBtn.setOnClickListener {
            executorServiceMbr.execute {
                screenDataSemaphoreMbr.acquire()

                // 아이템 셔플
                val item =
                    adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                item.shuffle()
                runOnUiThread {
                    adapterSetMbr.recyclerViewAdapter.setItemList(item)
                }

                screenDataSemaphoreMbr.release()
            }
        }
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    // : 실질적인 액티비티 로직 실행구역
    private var doItAlreadyMbr = false
    private var currentUserUidMbr: String? = null // 유저 식별가능 정보 - null 이라면 비회원
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (권한이 충족된 onCreate)
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.userUid
            refreshWholeScreenData(onComplete = {})

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val userUid = currentLoginSessionInfoSpwMbr.userUid
            if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = userUid

                // (데이터 수집)
                refreshWholeScreenData(onComplete = {})
            }

        }

        // (onResume)
    }

    // 화면 데이터 갱신관련 세마포어
    private val screenDataSemaphoreMbr = Semaphore(1)

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    //     onComplete = 네트워크 실패든 성공이든 데이터 요청 후 응답을 받아와 해당 상태에 따라 스크린 뷰 처리를 완료한 시점
    //     'c숫자' 로 표기된 부분은 원하는대로 커스텀
    private fun refreshWholeScreenData(onComplete: () -> Unit) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            runOnUiThread {
                // (리스트 초기화)
                adapterSetMbr.recyclerViewAdapter.setItemList(ArrayList())

                // (로더 추가)
                adapterSetMbr.recyclerViewAdapter.setItemList(
                    arrayListOf(
                        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
                        )
                    )
                )
            }

            // (스레드 합류 객체 생성)
            // : 헤더, 푸터, 아이템 리스트의 각 데이터를 비동기적으로 요청했을 때, 그 합류용으로 사용되는 객체
            //     numberOfThreadsBeingJoinedMbr 에 비동기 처리 개수를 적고,
            //     각 처리 완료시마다 threadComplete 를 호출하면 됨
            val threadConfluenceObj =
                ThreadConfluenceObj(
                    3,
                    onComplete = {
                        screenDataSemaphoreMbr.release()
                        onComplete()
                    }
                )

            // (정보 요청 콜백)
            // 아이템 리스트
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 itemList 는 null
            val getItemListOnComplete: (statusCode: Int, itemList: ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>?) -> Unit =
                { statusCode, itemList ->
                    runOnUiThread {
                        // (c3. 로더 제거)
                        adapterSetMbr.recyclerViewAdapter.setItemList(ArrayList())
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있을 때
                                // (c4. 빈 리스트 처리)
                                threadConfluenceObj.threadComplete()
                            } else {
                                runOnUiThread {
                                    // (c5. 받아온 아이템 추가)
                                    adapterSetMbr.recyclerViewAdapter.setItemList(itemList)

                                    // (c6. 스크롤을 가장 앞으로 이동)
                                    bindingMbr.recyclerView.scrollToPosition(0)
                                }

                                threadConfluenceObj.threadComplete()
                            }
                        }
                        -1 -> { // 네트워크 에러
                            // (c7. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러그외 서버 에러
                            // (c8. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 헤더 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getHeaderItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterHeaderAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c9. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            runOnUiThread {
                                // (c10. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c11. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c12. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 푸터 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getFooterItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterFooterAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c13. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            runOnUiThread {
                                // (c14. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c15. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c16. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // (네트워크 요청)
            // (c17. 아이템 리스트 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                // 요청 대기시간 가정
                Thread.sleep(1000)

                val resultObj =
                    arrayListOf<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>()

                for (idx in 0L..10L) {
                    val title = "item$idx"
                    resultObj.add(
                        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                            idx,
                            title
                        )
                    )
                }

                getItemListOnComplete(1, resultObj)
            }

            // (c18. 헤더 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getHeaderItemOnComplete(-2, null)
            }

            // (c19. 푸터 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getFooterItemOnComplete(-2, null)
            }

            // (c20. 그외 스크린 데이터 가져오기)

        }
    }

    // ActivityBasicVerticalRecyclerViewSampleAdapterSet - RecyclerViewAdapter 의 다음 페이지 데이터 리스트 가져오기
    // : serverItemUid 를 네트워크 요청에 입력하면 서버에선 해당 uid 이후의 데이터 리스트를 반환
    private var getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
        false

    private fun getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataList(
        onComplete: () -> Unit
    ) {
        if (getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr) {
            return
        }
        getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
            true

        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            val cloneItemList1 =
                adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

            // 로더 추가
            cloneItemList1.add(
                ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.ItemLoader.ItemVO(
                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr
                )
            )

            runOnUiThread {
                adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList1)

                val lastItemIdx = adapterSetMbr.recyclerViewAdapter.currentDataListLastIndexMbr

                // 로더 추가시 스크롤을 내리기
                bindingMbr.recyclerView.smoothScrollToPosition(lastItemIdx)
            }

            // (정보 요청 콜백)
            // statusCode
            // : 서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 itemList 는 null
            val networkOnComplete: (statusCode: Int, itemList: ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>?) -> Unit =
                { statusCode, itemList ->
                    val cloneItemList2 =
                        adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr
                    val beforeLastItemIdx =
                        adapterSetMbr.recyclerViewAdapter.currentDataListLastIndexMbr

                    // 로더 제거
                    cloneItemList2.removeLast()
                    runOnUiThread {
                        adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList2)
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있을 때
                                getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
                                    false
                                screenDataSemaphoreMbr.release()
                                onComplete()
                            } else {
                                // 받아온 아이템 추가
                                cloneItemList2.addAll(itemList)
                                runOnUiThread {
                                    adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList2)
                                    bindingMbr.recyclerView.scrollToPosition(beforeLastItemIdx)
                                }

                                getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
                                    false
                                screenDataSemaphoreMbr.release()
                                onComplete()
                            }
                        }
                        -1 -> { // 네트워크 에러

                            getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
                                false
                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        else -> { // 그외 서버 에러

                            getActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterNextPageDataListOnProgressMbr =
                                false
                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                    }
                }

            // 네트워크 요청
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                // 요청 대기시간 가정
                Thread.sleep(1000)

                val lastItemServerUid = if (cloneItemList1.size < 1) { // 아이템 리스트에 실질적인 아이템이 없을 때,
                    -1
                } else {
                    (cloneItemList1[cloneItemList1.lastIndex - 1] as
                            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid
                }

                val resultObj =
                    arrayListOf<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>()

                for (idx in lastItemServerUid + 1..lastItemServerUid + 11) {
                    val title = "item$idx"
                    resultObj.add(
                        ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                            adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                            idx,
                            title
                        )
                    )
                }

                networkOnComplete(1, resultObj)
            }
        }
    }

    // (아이템 추가)
    private fun postActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
        text: String,
        onComplete: () -> Unit
    ) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            // 로더 추가
            runOnUiThread {
                shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                    false,
                    "저장중입니다. 잠시만 기다려주세요.",
                    onCanceled = {}
                )
            }

            // (정보 요청 콜백)
            // statusCode
            // : 서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     serverUid = 해당 아이템에 대한 서버 Uid
            val networkOnComplete: (statusCode: Int, serverUid: Long?) -> Unit =
                { statusCode, serverUid ->
                    val cloneItemList =
                        adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr

                    // 로더 제거
                    runOnUiThread {
                        shownDialogInfoVOMbr = null
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            // 입력한 데이터 객체
                            val addedItem =
                                ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO(
                                    adapterSetMbr.recyclerViewAdapter.nextItemUidMbr,
                                    serverUid!!,
                                    text
                                )

                            // 받아온 아이템 추가
                            cloneItemList.add(addedItem)
                            runOnUiThread {
                                adapterSetMbr.recyclerViewAdapter.setItemList(cloneItemList)

                                val lastItemIdx =
                                    adapterSetMbr.recyclerViewAdapter.currentDataListLastIndexMbr
                                bindingMbr.recyclerView.scrollToPosition(lastItemIdx)
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        -1 -> { // 네트워크 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "네트워크 불안정",
                                    "현재 네트워크 연결이 불안정합니다.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                        else -> { // 그외 서버 에러
                            runOnUiThread {
                                shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                    true,
                                    "기술적 문제",
                                    "기술적 문제가 발생했습니다.\n잠시후 다시 시도해주세요.",
                                    null,
                                    onCheckBtnClicked = {
                                        shownDialogInfoVOMbr = null
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null
                                    }
                                )
                            }

                            screenDataSemaphoreMbr.release()
                            onComplete()
                        }
                    }
                }

            // 네트워크 요청
            executorServiceMbr.execute {
                // 요청 대기시간 가정
                Thread.sleep(1000)
                val lastItemUid =
                    (adapterSetMbr.recyclerViewAdapter.currentItemListCloneMbr.last() as
                            ActivityBasicVerticalRecyclerViewSampleAdapterSet.RecyclerViewAdapter.Item1.ItemVO).serverItemUid
                networkOnComplete(1, lastItemUid + 1)
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (클래스 비휘발 저장 객체)
    class ActivityBasicVerticalRecyclerViewSampleSpw(application: Application) {
        // <멤버 변수 공간>
        // SharedPreference 접근 객체
        private val spMbr = application.getSharedPreferences(
            "ActivityBasicVerticalRecyclerViewSampleSpw",
            Context.MODE_PRIVATE
        )

//        var testData: String?
//            get() {
//                return spMbr.getString(
//                    "testData",
//                    null
//                )
//            }
//            set(value) {
//                with(spMbr.edit()) {
//                    putString(
//                        "testData",
//                        value
//                    )
//                    apply()
//                }
//            }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>

    }

    // (액티비티 내 사용 어뎁터 모음)
    // : 액티비티 내 사용할 어뎁터가 있다면 본문에 클래스 추가 후 인자로 해당 클래스의 인스턴스를 받도록 하기
    class ActivityBasicVerticalRecyclerViewSampleAdapterSet(
        val recyclerViewAdapter: RecyclerViewAdapter
    ) {
        // 어뎁터 #1
        class RecyclerViewAdapter(
            private val parentViewMbr: ActivityBasicVerticalRecyclerViewSample,
            targetView: RecyclerView,
            isVertical: Boolean,
            oneRowItemCount: Int,
            onScrollReachTheEnd: (() -> Unit)?
        ) : AbstractProwdRecyclerViewAdapter(
            parentViewMbr,
            targetView,
            isVertical,
            oneRowItemCount,
            onScrollReachTheEnd
        ) {
            // <멤버 변수 공간>


            // ---------------------------------------------------------------------------------------------
            // <메소드 오버라이딩 공간>
            // 아이템 뷰 타입 결정
            override fun getItemViewType(position: Int): Int {
                return when (currentDataListCloneMbr[position]) {
                    is AdapterHeaderAbstractVO -> {
                        Header::class.hashCode()
                    }

                    is AdapterFooterAbstractVO -> {
                        Footer::class.hashCode()
                    }

                    // 여기서부터 아래로는 아이템 유형에 따른 중복 클래스를 사용하여 설정
                    // 아이템 로더 클래스 역시 아이템에 해당하여, 종류를 바꾸어 뷰를 변경
                    is ItemLoader.ItemVO -> {
                        ItemLoader::class.hashCode()
                    }

                    is Item1.ItemVO -> {
                        Item1::class.hashCode()
                    }

                    else -> {
                        Item1::class.hashCode()
                    }
                }
            }

            // 아이템 뷰타입에 따른 xml 화면 반환
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return when (viewType) {
                    // 헤더 / 푸터를 사용하지 않을 것이라면 item_empty 를 사용
                    Header::class.hashCode() -> {
                        Header.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_empty,
                                    parent,
                                    false
                                )
                        )
                    }

                    Footer::class.hashCode() -> {
                        Footer.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_empty,
                                    parent,
                                    false
                                )
                        )
                    }

                    // 아래로는 사용할 아이템 타입에 따른 뷰를 설정
                    ItemLoader::class.hashCode() -> {
                        ItemLoader.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item_loader,
                                    parent,
                                    false
                                )
                        )
                    }

                    Item1::class.hashCode() -> {
                        Item1.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item1,
                                    parent,
                                    false
                                )
                        )
                    }

                    // 아이템이 늘어나면 추가

                    else -> {
                        Item1.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(
                                    R.layout.item_activity_basic_vertical_recycler_view_sample_adapter_recycler_view_item1,
                                    parent,
                                    false
                                )
                        )
                    }
                }
            }

            // 아이템 뷰 생성 시점 로직
            // 주의 : 반환되는 position 이 currentDataList 인덱스와 같지 않을 수 있음.
            //     최초 실행시에는 같지만 아이템이 지워질 경우 position 을 0 부터 재정렬하는게 아님.
            //     고로 데이터 조작시 주의할것.
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (holder) {
                    is Header.ViewHolder -> { // 헤더 아이템 바인딩
//                    val binding = holder.binding
//                    val copyEntity = currentDataListCloneMbr[position] as Header.ItemVO
                    }

                    is Footer.ViewHolder -> { // 푸터 아이템 바인딩
//                    val binding = holder.binding
//                    val copyEntity = currentDataListCloneMbr[position] as Footer.ItemVO
                    }

                    is ItemLoader.ViewHolder -> { // 아이템 로더 아이템 바인딩
//                    val binding = holder.binding
//                    val copyEntity = currentDataListCloneMbr[position] as ItemLoader.ItemVO
                    }

                    is Item1.ViewHolder -> { // 아이템1 아이템 바인딩
                        val binding = holder.binding
                        val copyEntity = currentDataListCloneMbr[position] as Item1.ItemVO

                        binding.title.text = copyEntity.title

                        // 아이템 변경
                        binding.root.setOnClickListener {
                            parentViewMbr.putActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
                                copyEntity.serverItemUid,
                                "(Item Clicked!)",
                                onComplete = {})
                        }

                        // 아이템 제거 버튼
                        binding.deleteBtn.setOnClickListener {
                            parentViewMbr.deleteActivityBasicVerticalRecyclerViewSampleAdapterSetRecyclerViewAdapterData(
                                copyEntity.serverItemUid,
                                onComplete = {})
                        }

                    }

                    // 아이템이 늘어나면 추가
                }
            }

            // 아이템 내용 동일성 비교(아이템 내용/화면 변경시 사용될 기준)
            override fun isContentSame(
                oldItem: AdapterDataAbstractVO,
                newItem: AdapterDataAbstractVO
            ): Boolean {
                return when (oldItem) {
                    is Header.ItemVO -> {
                        if (newItem is Header.ItemVO) { // 아이템 서로 타입이 같으면,
                            // 내용 비교
                            oldItem == newItem
                        } else { // 아이템 서로 타입이 다르면,
                            // 무조건 다른 아이템
                            false
                        }
                    }

                    is Footer.ItemVO -> {
                        if (newItem is Footer.ItemVO) { // 아이템 서로 타입이 같으면,
                            // 내용 비교
                            oldItem == newItem
                        } else { // 아이템 서로 타입이 다르면,
                            // 무조건 다른 아이템
                            false
                        }
                    }

                    is ItemLoader.ItemVO -> {
                        if (newItem is ItemLoader.ItemVO) { // 아이템 서로 타입이 같으면,
                            // 내용 비교
                            oldItem == newItem
                        } else { // 아이템 서로 타입이 다르면,
                            // 무조건 다른 아이템
                            false
                        }
                    }

                    is Item1.ItemVO -> {
                        if (newItem is Item1.ItemVO) { // 아이템 서로 타입이 같으면,
                            // 내용 비교
                            oldItem == newItem
                        } else { // 아이템 서로 타입이 다르면,
                            // 무조건 다른 아이템
                            false
                        }
                    }

                    // 아이템이 늘어나면 추가

                    else -> {
                        oldItem == newItem
                    }
                }
            }

            // 아이템 복제 로직 (서로 다른 타입에 대응하기 위해 구현이 필요)
            override fun getDeepCopyReplica(newItem: AdapterDataAbstractVO): AdapterDataAbstractVO {
                return when (newItem) {
                    is Header.ItemVO -> {
                        newItem.copy()
                    }

                    is Footer.ItemVO -> {
                        newItem.copy()
                    }

                    is ItemLoader.ItemVO -> {
                        newItem.copy()
                    }

                    is Item1.ItemVO -> {
                        newItem.copy()
                    }

                    // 아이템이 늘어나면 추가

                    else -> {
                        newItem
                    }
                }
            }


            // ---------------------------------------------------------------------------------------------
            // <공개 메소드 공간>


            // ---------------------------------------------------------------------------------------------
            // <비공개 메소드 공간>


            // ---------------------------------------------------------------------------------------------
            // <내부 클래스 공간>
            // (아이템 클래스)
            // 헤더 / 푸터를 사용하지 않을 것이라면 item_empty 를 사용 및 ItemVO 데이터를 임시 데이터로 채우기
            class Header {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemEmptyBinding =
                        ItemEmptyBinding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                class ItemVO : AdapterHeaderAbstractVO() {
                    fun copy(): Footer.ItemVO {
                        return Footer.ItemVO()
                    }
                }
            }

            class Footer {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemEmptyBinding =
                        ItemEmptyBinding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                class ItemVO : AdapterFooterAbstractVO() {
                    fun copy(): ItemVO {
                        return ItemVO()
                    }
                }
            }

            class ItemLoader {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItemLoaderBinding =
                        ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItemLoaderBinding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                data class ItemVO(
                    override val itemUid: Long
                ) : AdapterItemAbstractVO(itemUid)
            }

            class Item1 {
                data class ViewHolder(
                    val view: View,
                    val binding: ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItem1Binding =
                        ItemActivityBasicVerticalRecyclerViewSampleAdapterRecyclerViewItem1Binding.bind(
                            view
                        )
                ) : RecyclerView.ViewHolder(view)

                data class ItemVO(
                    override val itemUid: Long,
                    val serverItemUid: Long,
                    var title: String
                ) : AdapterItemAbstractVO(itemUid)
            }

            // 아이템이 늘어나면 추가

        }
    }
}