package com.example.prowd_android_template.activity_set.activity_home

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
import com.example.prowd_android_template.activity_set.activity_camera_sample_list.ActivityCameraSampleList
import com.example.prowd_android_template.activity_set.activity_etc_sample_list.ActivityEtcSampleList
import com.example.prowd_android_template.activity_set.activity_image_processing_sample_list.ActivityImageProcessingSampleList
import com.example.prowd_android_template.activity_set.activity_jni_sample_list.ActivityJniSampleList
import com.example.prowd_android_template.activity_set.activity_media_player_sample_list.ActivityMediaPlayerSampleList
import com.example.prowd_android_template.activity_set.activity_recycler_view_sample_list.ActivityRecyclerViewSampleList
import com.example.prowd_android_template.activity_set.activity_view_pager_sample_list.ActivityViewPagerSampleList
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityHomeBinding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ActivityHome : AppCompatActivity() {
    // <멤버 상수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()

    // (스레드 풀)
    val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityHomeBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ViewModel

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw


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
            for (activityPermission in activityPermissionArrayMbr) {
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

        permissionRequestMbr.launch(activityPermissionArrayMbr)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 설정 변경(화면회전)을 했는지 여부를 반영
        viewModelMbr.isActivityRecreatedMbr = true

        super.onSaveInstanceState(outState)
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
        bindingMbr = ActivityHomeBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ViewModel::class.java]

        // 레포지토리 객체 생성
        repositorySetMbr = RepositorySet.getInstance(this)

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

        // 로그인 SPW 생성
        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)
    }

    // (초기 뷰 설정)
    private fun onCreateInitView() {
        // 리사이클러 뷰 샘플 목록 이동 버튼
        bindingMbr.goToRecyclerViewSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityRecyclerViewSampleList::class.java
                )
            startActivity(intent)
        }

        // 뷰 페이저 샘플 목록 이동 버튼
        bindingMbr.goToViewPagerSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityViewPagerSampleList::class.java
                )
            startActivity(intent)
        }

        // 카메라 샘플 목록 이동 버튼
        bindingMbr.goToCameraSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityCameraSampleList::class.java
                )
            startActivity(intent)
        }

        // 미디어 플레이어 샘플 목록 이동 버튼
        bindingMbr.goToMediaPlayerSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityMediaPlayerSampleList::class.java
                )
            startActivity(intent)
        }

        // 이미지 처리 샘플 목록 이동 버튼
        bindingMbr.goToImageProcessingSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityImageProcessingSampleList::class.java
                )
            startActivity(intent)
        }

        bindingMbr.goToJniSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityJniSampleList::class.java
                )
            startActivity(intent)
        }
        // 기타 샘플 목록 이동 버튼
        bindingMbr.goToEtcSampleListBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityEtcSampleList::class.java
                )
            startActivity(intent)
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
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    private fun allPermissionsGranted() {
        if (!viewModelMbr.isActivityRecreatedMbr) { // 화면 회전이 아닐때
            if (!viewModelMbr.doItAlreadyMbr) {
                viewModelMbr.doItAlreadyMbr = true

                // ---------------------------------------------------------------------------------
                // (실질적인 onCreate 로직) : 권한 클리어 + 처음 실행

            }

            // -------------------------------------------------------------------------------------
            // (실질적인 onResume 로직) : 권한 클리어
            // (뷰 데이터 로딩)
            // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
            val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != viewModelMbr.currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                // 데이터 수집
            }

        } else { // 화면 회전일 때

        }

        // onResume 의 가장 마지막엔 설정 변경(화면회전) 여부를 초기화
        viewModelMbr.isActivityRecreatedMbr = false
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (뷰모델 객체)
    // : 액티비티 reCreate 이후에도 남아있는 데이터 묶음 = 뷰의 데이터 모델
    class ViewModel(application: Application) : AndroidViewModel(application) {
        // <멤버 변수 공간>
        // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
        var doItAlreadyMbr = false

        // (설정 변경 여부) : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
        var isActivityRecreatedMbr = false

        // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
        var currentUserSessionTokenMbr: String? = null


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


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }
}