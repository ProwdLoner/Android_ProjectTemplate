package com.example.prowd_android_template.activity_set.activity_init

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.activity_set.activity_home.ActivityHome
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityInitBinding
import com.example.prowd_android_template.repository.RepositorySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class ActivityInit : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityInitBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (SharedPreference 객체)
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
    private var permissionRequestOnProgressMbr = false
    private val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // 액티비티 대기 카운터 객체
    private var delayCountDownTimerMbr: CountDownTimer? = null

    // delayCountDownTimerMbr 인터벌 MilliSec
    val delayCountDownTimerIntervalMsMbr = 100L

    // delayCountDownTimerMbr 남은 시간 MilliSec
    var delayCountDownTimerRestMilliSecMbr = 1000L

    val goToNextActivitySemaphoreMbr = Semaphore(1)

    // 앱 기본 대기 시간이 완료 플래그
    var waitToGoToNextActivityCompletedMbr = false

    // 앱 버전 체크 완료 플래그
    var checkAppVersionCompletedMbr = false

    // 로그인 체크 완료 플래그
    var checkLoginCompletedMbr = false


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
                        var isPermissionAllGranted = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted = false
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

                            allPermissionsGranted()
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
                                            var isPermissionAllGranted1 = true
                                            for (activityPermission in activityPermissionArrayMbr) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        this,
                                                        activityPermission
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) { // 거부된 필수 권한이 존재
                                                    // 권한 클리어 플래그를 변경하고 break
                                                    isPermissionAllGranted1 = false
                                                    break
                                                }
                                            }

                                            if (isPermissionAllGranted1) { // 권한 승인
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

                                                allPermissionsGranted()
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
        executorServiceMbr.execute {
            goToNextActivitySemaphoreMbr.acquire()
            if (!waitToGoToNextActivityCompletedMbr) { // 화면 대기가 끝나지 않았을 때
                goToNextActivitySemaphoreMbr.release()
                // 화면이 멈추면 카운터도 멈춤
                delayCountDownTimerMbr?.cancel()
            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        super.onDestroy()
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


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    // : 클래스에서 사용할 객체를 초기 생성
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityInitBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

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
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.sessionToken
            getScreenDataAndShow()

            // (알고리즘)
            checkAppVersion()

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val sessionToken = currentLoginSessionInfoSpwMbr.sessionToken
            if (sessionToken != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = sessionToken

                // (데이터 수집)
                getScreenDataAndShow()
            }

        }

        // (onResume)
        startDelayTimer()
    }

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    private fun getScreenDataAndShow() {

    }

    // (대기시간 타이머 실행)
    private fun startDelayTimer() {
        executorServiceMbr.execute {
            goToNextActivitySemaphoreMbr.acquire()
            if (waitToGoToNextActivityCompletedMbr) {// 화면 대기가 이전에 끝났을 때
                goToNextActivitySemaphoreMbr.release()
                return@execute
            }
            goToNextActivitySemaphoreMbr.release()

            // 화면 딜레이 타이머 실행
            runOnUiThread {
                delayCountDownTimerMbr = object :
                    CountDownTimer(
                        delayCountDownTimerRestMilliSecMbr,
                        delayCountDownTimerIntervalMsMbr
                    ) {
                    override fun onTick(millisUntilFinished: Long) {
                        // 초 마다 화면에 카운트 다운
                        if (delayCountDownTimerRestMilliSecMbr.toFloat() % 1000f == 0f) {
                            bindingMbr.countDownTxt.text =
                                (delayCountDownTimerRestMilliSecMbr.toFloat() / 1000f).toInt()
                                    .toString()
                        }

                        delayCountDownTimerRestMilliSecMbr -= delayCountDownTimerIntervalMsMbr
                    }

                    override fun onFinish() {
                        bindingMbr.countDownTxt.text = "0"

                        goToNextActivitySemaphoreMbr.acquire()
                        waitToGoToNextActivityCompletedMbr = true
                        goToNextActivitySemaphoreMbr.release()

                        goToNextActivity()
                    }
                }.start()
            }
        }
    }

    // (앱 버전 체크)
    private fun checkAppVersion() {
        // (정보 요청 콜백)
        // 네트워크 요청 부분을 구현할 때에는, 아직 API 명세도 정해지지 않은 상황을 가정.
        //     현재 화면을 구현하는데 필요한 정보를 추려내서 임시로 콜백을 만들어두고,
        //     추후 실제 API 가 제공되면 이 응답을 미리 만들어둔 뷰 모델에 맞춰서 콜백을 실행시키면 됨
        // statusCode : 서버 반환 상태값. 1이라면 정상 동작 -1 이라면 타임아웃
        // minUpdateVersion : 최소 요청 버전. 이것에 미치지 못하면 업데이트 필요. 정상 응답이 아니면 null.
        val onComplete: (statusCode: Int, minUpdateVersion: String?) -> Unit =
            { statusCode, minUpdateVersion ->
                runOnUiThread {
                    when (statusCode) {
                        1 -> {// 정상 동작
                            // 현재 버전 = ex : "1.0.0"
                            val currentVersion =
                                packageManager.getPackageInfo(packageName, 0).versionName
                            val currVersionSplit = currentVersion.split(".")

                            val minUpdateVersionSplit = minUpdateVersion!!.split(".")

                            // 현재 버전이 서버 업데이트 기준 미달일 때에 강제 업데이트 수행
                            val needUpdate =
                                (currVersionSplit[0].toInt() < minUpdateVersionSplit[0].toInt() // 앞 버전 확인
                                        || currVersionSplit[1].toInt() < minUpdateVersionSplit[1].toInt() // 중간 버전 확인
                                        || currVersionSplit[2].toInt() < minUpdateVersionSplit[2].toInt())

                            if (needUpdate) { // 업데이트가 필요
                                // 업데이트 여부를 묻고 종료
                                shownDialogInfoVOMbr =
                                    DialogBinaryChoose.DialogInfoVO(
                                        false,
                                        "업데이트 안내",
                                        "서비스를 이용하기 위해 업데이트가 필요합니다.\n업데이트 화면으로 이동하시겠습니까?",
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
                                                shownDialogInfoVOMbr =
                                                    null
                                                finish()
                                            }
                                        },
                                        onNegBtnClicked = { // 부정
                                            shownDialogInfoVOMbr =
                                                null
                                            finish()
                                        },
                                        onCanceled = {}
                                    )
                            } else { // 업데이트 불필요
                                goToNextActivitySemaphoreMbr.acquire()
                                checkAppVersionCompletedMbr = true
                                goToNextActivitySemaphoreMbr.release()

                                checkLogin()
                            }
                        }
                        -1 -> { // 네트워크 에러
                            shownDialogInfoVOMbr = DialogBinaryChoose.DialogInfoVO(
                                false,
                                "네트워크 불안정",
                                "현재 네트워크 연결이 불안정합니다.",
                                "다시시도",
                                "종료",
                                onPosBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    checkAppVersion()
                                },
                                onNegBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    finish()
                                },
                                onCanceled = {}
                            )
                        }
                        else -> { // 그외 서버 에러
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "기술적 문제",
                                "기술적 문제가 발생했습니다.\n잠시후 다시 시도해주세요.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null

                                    finish()
                                }
                            )
                        }
                    }
                }
            }

        // 네트워크 요청
        executorServiceMbr.execute {
            // 네트워크 대기시간 가정
            Thread.sleep(300)
            onComplete(1, "1.0.0")
        }
    }

    // (초기 로그인 체크)
    private fun checkLogin() {
        val isAutoLogin: Boolean =
            currentLoginSessionInfoSpwMbr.isAutoLogin
        val loginType: Int =
            currentLoginSessionInfoSpwMbr.loginType

        if (!isAutoLogin || loginType == 0) { // 로그인 검증 불필요
            // 비회원 처리
            currentLoginSessionInfoSpwMbr.setLogout()

            goToNextActivitySemaphoreMbr.acquire()
            checkLoginCompletedMbr = true
            goToNextActivitySemaphoreMbr.release()

            goToNextActivity()
            return
        }

        // 로그인 검증 필요
        // 검증용 정보 가져오기 (SNS 로그인이라면 OAuth 로그인 검증 후 반환되는 id, 토큰을 반환)
        val serverId: String?
        val serverPw: String?

        when (loginType) {
            1 -> { // 저장된 이메일 아이디를 가져와 사용
                serverId = currentLoginSessionInfoSpwMbr.loginId
                serverPw = currentLoginSessionInfoSpwMbr.loginPw
            }
            2 -> {
                serverId = "구글 SNS Id 실패시 null"
                serverPw = "구글 SNS Token 실패시 null"
            }
            3 -> {
                serverId = "카카오 SNS Id 실패시 null"
                serverPw = "카카오 SNS Token 실패시 null"
            }
            4 -> {
                serverId = "네이버 SNS Id 실패시 null"
                serverPw = "네이버 SNS Token 실패시 null"
            }
            else -> {
                serverId = null
                serverPw = null
            }
        }

        if (serverId == null || serverPw == null) { // 로그인 불가
            // 비회원 처리
            currentLoginSessionInfoSpwMbr.setLogout()

            goToNextActivitySemaphoreMbr.acquire()
            checkLoginCompletedMbr = true
            goToNextActivitySemaphoreMbr.release()

            goToNextActivity()
            return
        }

        // (정보 요청 콜백)
        // statusCode : 서버 반환 상태값. -1 이라면 타임아웃
        // sessionToken : 로그인 완료시 반환되는 세션토큰
        val onComplete: (statusCode: Int, sessionToken: String?, userNickName: String?) -> Unit =
            { statusCode, sessionToken, userNickName ->
                runOnUiThread {
                    when (statusCode) {
                        1 -> {// 로그인 완료
                            // 회원 처리
                            currentLoginSessionInfoSpwMbr.isAutoLogin = true
                            currentLoginSessionInfoSpwMbr.sessionToken = sessionToken
                            currentLoginSessionInfoSpwMbr.userNickName = userNickName

                            goToNextActivitySemaphoreMbr.acquire()
                            checkLoginCompletedMbr = true
                            goToNextActivitySemaphoreMbr.release()

                            goToNextActivity()
                        }
                        2 -> { // 로그인 정보 불일치
                            // 비회원 처리
                            currentLoginSessionInfoSpwMbr.setLogout()

                            goToNextActivitySemaphoreMbr.acquire()
                            checkLoginCompletedMbr = true
                            goToNextActivitySemaphoreMbr.release()

                            goToNextActivity()
                        }
                        -1 -> { // 네트워크 에러
                            shownDialogInfoVOMbr = DialogBinaryChoose.DialogInfoVO(
                                false,
                                "네트워크 불안정",
                                "현재 네트워크 연결이 불안정합니다.",
                                "다시시도",
                                "종료",
                                onPosBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    checkLogin()
                                },
                                onNegBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    finish()
                                },
                                onCanceled = {}
                            )
                        }
                        else -> { // 그외 서버 에러
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "기술적 문제",
                                "기술적 문제가 발생했습니다.\n잠시후 다시 시도해주세요.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null

                                    finish()
                                }
                            )
                        }
                    }
                }
            }

        executorServiceMbr.execute {
            // 네트워크 비동기 요청을 가정
            Thread.sleep(500)
            onComplete(1, "##ADRE_DRTG_1234", "행복한 너구리")
        }
    }

    private fun goToNextActivity() {
        goToNextActivitySemaphoreMbr.acquire()
        if (waitToGoToNextActivityCompletedMbr && // 앱 대기 시간이 끝났을 때
            checkAppVersionCompletedMbr && // 앱 버전 검증이 끝났을 때
            checkLoginCompletedMbr && // 로그인 검증이 끝났을 때
            (!isDestroyed && !isFinishing) // 종료되지 않았을 때
        ) {
            goToNextActivitySemaphoreMbr.release()
            val intent =
                Intent(
                    this,
                    ActivityHome::class.java
                )
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
        goToNextActivitySemaphoreMbr.release()
    }

    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}