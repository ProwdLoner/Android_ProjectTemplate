package com.example.prowd_android_template.activity_set.activity_init

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.activity_set.activity_home.ActivityHome
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityInitBinding
import java.net.SocketTimeoutException

// todo : 카운팅 중 화면 회전시 멈춤
class ActivityInit : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityInitBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityInitViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null

    // 카운터 객체
    lateinit var delayCountDownTimerMbr: CountDownTimer

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // 앱 사용 권한 모음
    private lateinit var applicationPermissionArrayMbr: Array<String>


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityInitBinding.inflate(layoutInflater)
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
        if (!viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr) { // 화면 대기가 끝나지 않았을 때
            // 화면 체류 delay 실행
            delayCountDownTimerMbr = object :
                CountDownTimer(
                    viewModelMbr.countDownRestMilliSecMbr,
                    viewModelMbr.countDownIntervalMbr
                ) {
                override fun onTick(millisUntilFinished: Long) {
                    // 초 마다 화면에 카운트 다운
                    if (viewModelMbr.countDownRestMilliSecMbr.toFloat() % 1000f == 0f) {
                        viewModelMbr.countDownNumberLiveDataMbr.value =
                            (viewModelMbr.countDownRestMilliSecMbr.toFloat() / 1000f).toInt()
                    }

                    viewModelMbr.countDownRestMilliSecMbr =
                        viewModelMbr.countDownRestMilliSecMbr - viewModelMbr.countDownIntervalMbr
                }

                override fun onFinish() {
                    viewModelMbr.countDownNumberLiveDataMbr.value = 0
                    viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr = true
                    goToNextActivity()
                }
            }.start()
        }

        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            if (viewModelMbr.isDataFirstLoadingMbr // 데이터 최초 로딩 시점일 때
            ) {
                // 진입 플래그 변경
                viewModelMbr.isDataFirstLoadingMbr = false

                doActivityInit()
            }
        }

        // 설정 변경(화면회전)을 했는지 여부를 초기화
        // onResume 의 가장 마지막
        viewModelMbr.isChangingConfigurationsMbr = false
    }

    override fun onPause() {
        if (!viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr) { // 화면 대기가 끝나지 않았을 때
            // 다른 화면으로 이동하면 화면 대기 시간이 흐르지 않음
            delayCountDownTimerMbr.cancel()
        }

        super.onPause()
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
        viewModelMbr = ViewModelProvider(this)[ActivityInitViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // Manifest.xml 에 설정된 모든 권한 배열
        applicationPermissionArrayMbr = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {

    }

    // 액티비티 초기화 로직
    private fun doActivityInit() {
        // 앱 버전 체크 실행
        if (!viewModelMbr.checkAppVersionAsyncOnProgressedMbr) {
            // 메소드 실행중이 아닐 때,

            viewModelMbr.checkAppVersionAsync(
                executorOnComplete = { needUpdate ->
                    runOnUiThread checkAppVersionAsyncComplete@{
                        if (needUpdate) { // 업데이트 필요
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    true,
                                    "업데이트 안내",
                                    "서비스를 이용하기 위해\n업데이트 하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = { // 긍정
                                        // 업데이트 페이지로 이동
                                        try {
                                            this.startActivity(
                                                Intent(
                                                    "android.intent.action.VIEW",
                                                    Uri.parse("market://details?id=${this.packageName}")
                                                )
                                            )
                                        } catch (e: ActivityNotFoundException) {
                                            this.startActivity(
                                                Intent(
                                                    "android.intent.action.VIEW",
                                                    Uri.parse("https://play.google.com/store/apps/details?id=${this.packageName}")
                                                )
                                            )
                                        } finally {
                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                null
                                            finish()
                                        }
                                    },
                                    onNegBtnClicked = { // 부정
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    },
                                    onCanceled = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    }
                                )
                        } else { // 업데이트 불필요
                            // 로그인 검증 실행
                            if (!viewModelMbr.checkLoginSessionAsyncOnProgressedMbr) {
                                // 메소드 실행중이 아닐 때,

                                val isAutoLogin: Boolean =
                                    viewModelMbr.currentLoginSessionInfoSpwMbr.isAutoLogin
                                val loginType: Int =
                                    viewModelMbr.currentLoginSessionInfoSpwMbr.loginType
                                val serverId: String? =
                                    viewModelMbr.currentLoginSessionInfoSpwMbr.userServerId
                                val serverPw: String? =
                                    viewModelMbr.currentLoginSessionInfoSpwMbr.userServerPw

                                viewModelMbr.checkLoginSessionAsync(
                                    ActivityInitViewModel.CheckLoginSessionParameterVO(
                                        isAutoLogin,
                                        loginType,
                                        serverId,
                                        serverPw
                                    ),
                                    executorOnComplete = { checkLoginSessionResult ->
                                        runOnUiThread checkLoginSessionAsyncComplete@{
                                            // 검증 후 결과를 sharedPreferences 에 대입
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.isAutoLogin =
                                                checkLoginSessionResult.isAutoLogin
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken =
                                                checkLoginSessionResult.sessionToken
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.userNickName =
                                                checkLoginSessionResult.userNickName
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.loginType =
                                                checkLoginSessionResult.loginType
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.userServerId =
                                                checkLoginSessionResult.userServerId
                                            viewModelMbr.currentLoginSessionInfoSpwMbr.userServerPw =
                                                checkLoginSessionResult.userServerPw

                                            // (앱 권한 처리 : 앱 설치시 한번만 실행)
                                            checkAppPermissions()
                                        }
                                    },
                                    executorOnError = { checkLoginSessionAsyncError ->
                                        runOnUiThread checkLoginSessionAsyncError@{
                                            if (checkLoginSessionAsyncError is SocketTimeoutException) { // 타임아웃 에러
                                                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                    DialogBinaryChoose.DialogInfoVO(
                                                        true,
                                                        "네트워크 에러",
                                                        "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                                        "다시시도",
                                                        "종료",
                                                        onPosBtnClicked = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null

                                                            // 로직 다시 실행
                                                            doActivityInit()
                                                        },
                                                        onNegBtnClicked = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null
                                                            finish()
                                                        },
                                                        onCanceled = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null
                                                            finish()
                                                        }
                                                    )
                                            } else { // 그외 에러
                                                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                    DialogBinaryChoose.DialogInfoVO(
                                                        true,
                                                        "서버 에러",
                                                        "현재 서버의 상태가 원활하지 않습니다.\n" +
                                                                "잠시 후 다시 시도해주세요.\n" +
                                                                "\n" +
                                                                "에러 메시지 :\n" +
                                                                "${checkLoginSessionAsyncError.message}",
                                                        "다시시도",
                                                        "종료",
                                                        onPosBtnClicked = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null

                                                            // 로직 다시 실행
                                                            doActivityInit()
                                                        },
                                                        onNegBtnClicked = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null
                                                            finish()
                                                        },
                                                        onCanceled = {
                                                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                                                null
                                                            finish()
                                                        }
                                                    )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                executorOnError = { checkAppVersionAsyncError ->
                    runOnUiThread checkAppVersionAsyncError@{
                        if (checkAppVersionAsyncError is SocketTimeoutException) { // 타임아웃 에러
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    true,
                                    "네트워크 에러",
                                    "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                    "다시시도",
                                    "종료",
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null

                                        // 로직 다시 실행
                                        doActivityInit()
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    },
                                    onCanceled = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    }
                                )
                        } else {
                            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                DialogBinaryChoose.DialogInfoVO(
                                    true,
                                    "서버 에러",
                                    "현재 서버의 상태가 원활하지 않습니다.\n" +
                                            "잠시 후 다시 시도해주세요.\n" +
                                            "\n" +
                                            "에러 메시지 :\n" +
                                            "${checkAppVersionAsyncError.message}",
                                    "다시시도",
                                    "종료",
                                    onPosBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null

                                        // 로직 다시 실행
                                        doActivityInit()
                                    },
                                    onNegBtnClicked = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    },
                                    onCanceled = {
                                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                                            null
                                        finish()
                                    }
                                )
                        }
                    }
                }
            )
        }
    }

    // 앱 권한 체크 실행
    private fun checkAppPermissions() {
        if (viewModelMbr.isCheckAppPermissionsOnProgressMbr) { // 현재 내부 처리가 동작중이라면 return
            return
        }

        viewModelMbr.isCheckAppPermissionsOnProgressMbr = true

        if (viewModelMbr.isCheckAppPermissionsCompletedOnceMbr || // 이전에 완료된 경우
            viewModelMbr.thisSpwMbr.isPermissionInitShownBefore // 이전에 권한 체크를 했던 경우(= 앱 최초 실행이 아닐 때)
        ) {

            viewModelMbr.isCheckAppPermissionsOnProgressMbr = false
            viewModelMbr.isCheckAppPermissionsCompletedOnceMbr = true
            viewModelMbr.thisSpwMbr.isPermissionInitShownBefore = true

            // 다음 엑티비티로 이동
            goToNextActivity()

            return
        }

        // 1. 앱 내부 모든 필요 권한들에 대한 메시지 띄워주기
        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
            DialogConfirm.DialogInfoVO(
                true,
                "앱 필요 권한 요청",
                "이 앱을 실행하려면\n아래와 같은 권한이 필요합니다.\n" +
                        "\n1. 푸시 권한 :\n유용한 정보를 얻을 수 있습니다.\n" +
                        "\n2. 카메라 사용 권한 :\n사진 찍기 서비스에 사용됩니다.\n" +
                        "\n3. 위치 정보 접근 권한 :\n위치 기반 컨텐츠 제공에 사용됩니다.\n" +
                        "\netc..",
                null,
                onCheckBtnClicked = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                        null

                    requestAppPermissions()

                },
                onCanceled = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                        null

                    requestAppPermissions()

                }
            )
    }

    // 앱 권한 요청 실행
    private fun requestAppPermissions() {
        // 앱 내부 모든 필요 권한들을 묻기

        // (커스텀 권한 요청)
        // 푸시 권한 요청
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = DialogBinaryChoose.DialogInfoVO(
            false,
            "푸시 권한 요청",
            "이벤트 수신을 위해\n푸시 알람을 받으시겠습니까?",
            null,
            null,
            onPosBtnClicked = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                // 권한 상태 저장
                viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted = true

                // 디바이스 권한 요청
                permissionRequestCallbackMbr = {
                    // 앱 권한 체크 플래그 변경
                    viewModelMbr.isCheckAppPermissionsOnProgressMbr = false
                    viewModelMbr.isCheckAppPermissionsCompletedOnceMbr = true
                    viewModelMbr.thisSpwMbr.isPermissionInitShownBefore = true

                    // 다음 엑티비티로 이동
                    goToNextActivity()
                }
                permissionRequestMbr.launch(applicationPermissionArrayMbr)

            },
            onNegBtnClicked = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                // 권한 상태 저장
                viewModelMbr.customDevicePermissionInfoSpwMbr.isPushPermissionGranted = false

                // 디바이스 권한 요청
                permissionRequestCallbackMbr = {
                    // 앱 권한 체크 플래그 변경
                    viewModelMbr.isCheckAppPermissionsOnProgressMbr = false
                    viewModelMbr.isCheckAppPermissionsCompletedOnceMbr = true
                    viewModelMbr.thisSpwMbr.isPermissionInitShownBefore = true

                    // 다음 엑티비티로 이동
                    goToNextActivity()
                }
                permissionRequestMbr.launch(applicationPermissionArrayMbr)

            },
            onCanceled = {
                // cancel 불가
            }
        )
    }

    private fun goToNextActivity() {
        viewModelMbr.goToNextActivitySemaphoreMbr.acquire()
        if (viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr && // 앱 대기 시간이 끝났을 때
            viewModelMbr.checkAppVersionAsyncCompletedOnceMbr && // 앱 버전 검증이 끝났을 때
            viewModelMbr.checkLoginSessionAsyncCompletedOnceMbr && // 로그인 검증이 끝났을 때
            viewModelMbr.isCheckAppPermissionsCompletedOnceMbr && // 앱 권한 체크가 끝났을 때
            (!isDestroyed && !isFinishing) // 종료되지 않았을 때
        ) {
            val intent =
                Intent(
                    this,
                    ActivityHome::class.java
                )
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
        viewModelMbr.goToNextActivitySemaphoreMbr.release()
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

        // 카운트 다운 출력
        viewModelMbr.countDownNumberLiveDataMbr.observe(this) {
            bindingMbr.countDownTxt.text = it.toString()
        }
    }
}