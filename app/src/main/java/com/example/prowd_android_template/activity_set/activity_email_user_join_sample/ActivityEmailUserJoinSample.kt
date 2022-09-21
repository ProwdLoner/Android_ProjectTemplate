package com.example.prowd_android_template.activity_set.activity_email_user_join_sample

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.activity_set.activity_user_login_sample.ActivityUserLoginSample
import com.example.prowd_android_template.application_session_service.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.application_session_service.UserSessionUtil
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityEmailUserJoinSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_class.ThreadConfluenceObj
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.regex.Pattern

class ActivityEmailUserJoinSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf()


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityEmailUserJoinSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityEmailUserJoinSampleAdapterSet

    // (SharedPreference 객체)
    // 클래스 비휘발성 저장객체
    lateinit var classSpwMbr: ActivityEmailUserJoinSampleSpw

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

    // 키보드 바깥을 누르면 키보드를 숨김
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val focusView: View? = currentFocus
        if (focusView != null) {
            val rect = Rect()
            focusView.getGlobalVisibleRect(rect)
            val x = ev.x.toInt()
            val y = ev.y.toInt()
            if (!rect.contains(x, y)) {
                val imm: InputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(focusView.windowToken, 0)
                focusView.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    // : 클래스에서 사용할 객체를 초기 생성
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityEmailUserJoinSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityEmailUserJoinSampleAdapterSet()

        // SPW 객체 생성
        classSpwMbr = ActivityEmailUserJoinSampleSpw(application)
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
        bindingMbr.goToUserLoginSampleBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityUserLoginSample::class.java
                )
            startActivity(intent)
        }

        // 에러 문구 초기화
        bindingMbr.passwordInputLayout.error = null
        bindingMbr.passwordInputLayout.isErrorEnabled = false
        bindingMbr.passwordCheckInputLayout.error = null
        bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
        bindingMbr.emailInputLayout.error = null
        bindingMbr.emailInputLayout.isErrorEnabled = false
        bindingMbr.nickNameInputLayout.error = null
        bindingMbr.nickNameInputLayout.isErrorEnabled = false

        // 인증 이전 비활성화
        bindingMbr.passwordInputLayout.isEnabled = false
        bindingMbr.passwordInputLayout.isFocusable = false
        bindingMbr.passwordCheckInputLayout.isEnabled = false
        bindingMbr.passwordCheckInputLayout.isFocusable = false
        bindingMbr.emailUserJoin.isEnabled = false
        bindingMbr.emailUserJoin.isFocusable = false
        bindingMbr.checkEmailVerification.isEnabled = false
        bindingMbr.checkEmailVerification.isFocusable = false
        bindingMbr.nickNameInputLayout.isEnabled = false
        bindingMbr.nickNameInputLayout.isFocusable = false

        // 인증 이전 숨기기
        bindingMbr.checkEmailVerification.visibility = View.GONE
        bindingMbr.userInputContainer.visibility = View.GONE
        bindingMbr.emailUserJoin.visibility = View.GONE

        // 아이디 입력 리스너
        bindingMbr.emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // 아이디용 이메일 변경시 모두 초기화

                // 에러 문구 초기화
                bindingMbr.passwordInputLayout.error = null
                bindingMbr.passwordInputLayout.isErrorEnabled = false
                bindingMbr.passwordCheckInputLayout.error = null
                bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
                bindingMbr.emailInputLayout.error = null
                bindingMbr.emailInputLayout.isErrorEnabled = false
                bindingMbr.nickNameInputLayout.error = null
                bindingMbr.nickNameInputLayout.isErrorEnabled = false

                // 인증 이전 비활성화
                bindingMbr.passwordInputLayout.isEnabled = false
                bindingMbr.passwordInputLayout.isFocusable = false
                bindingMbr.passwordCheckInputLayout.isEnabled = false
                bindingMbr.passwordCheckInputLayout.isFocusable = false
                bindingMbr.emailUserJoin.isEnabled = false
                bindingMbr.emailUserJoin.isFocusable = false
                bindingMbr.checkEmailVerification.isEnabled = false
                bindingMbr.checkEmailVerification.isFocusable = false
                bindingMbr.nickNameInputLayout.isEnabled = false
                bindingMbr.nickNameInputLayout.isFocusable = false

                // 인증 이전 숨기기
                bindingMbr.checkEmailVerification.visibility = View.GONE
                bindingMbr.userInputContainer.visibility = View.GONE
                bindingMbr.emailUserJoin.visibility = View.GONE
            }

            override fun afterTextChanged(s: Editable) {
            }
        })


        // 이메일 본인 확인
        // : 본인 이메일인지 확인하기 위해 버튼을 누르면 확인 이메일이 발송됨
        // 이메일 검증 알고리즘 정리
        // 1. 유저가 인증요청 버튼을 누르면 서버에 인증 요청을 보내고, 요청이 완료되었는지를 확인하기 위한 요청 번호를 받음.
        //      동시에 인증 요청 버튼은 인증 확인 버튼이 됨
        // 2. 서버측에선 발송 요청이 오면 먼저 해당 이메일 가입여부를 판단
        //     가입된 이메일이 없다면 랜덤번호를 생성하여 인증 요청 테이블을 작성
        //     인증 요청 테이블은 "요청 번호", "이메일", "비밀번호", "검증여부", "요청일시", "활성 비활성 여부"를 가짐
        //     요청이 오면 먼저 해당 이메일로 온 요청이 있는지를 확인하고 있다면 해당 요청을 비활성화.
        //     고유값 요청번호를 생성하고, 받아온 이메일, 생성한 비밀번호, 현재시간을 넣고 검증여부는 검증안됨으로 추가
        //     정보 저장이 끝나면 해당 이메일에 요청번호, 요청 비밀번호를 파라미터로 하는 검증용 링크를 가진 버튼을 생성하여 보냄
        //     발송 요청을 보낸 유저에겐 생성된 요청번호를 전달함.
        // 3. 사용자가 이메일을 확인하고 이메일에서 해당 링크를 송신하는 버튼을 누르면 서버에 검증 완료 요청을 송신
        // 4. 서버는 검증 완료 요청을 받으면, DB를 탐색하여 해당 요청번호와 비밀번호에 맞는 정보를 탐색하고, 요청일시로 만료시간
        //     검증을 한 후 정보가 존재하며 만료되지 않았을 시엔 테이블의 검증여부를 검증상태로 변경
        //     만료되었다면 만료되었다는 화면을 반환. 혹시나 비밀번호가 맞지 않는 해킹시도가 있으면 이를 처리
        // 5. 유저는 이메일 인증을 한 후 앱으로 돌아와 요청확인 버튼을 눌러 서버에 요청번호를 전달하여 확인 요청을 함.
        // 6. 서버는 요청확인 요청이 오면 인증 요청 테이블을 검색해서 해당 요청 번호가 만료되지 않았고, 검증 완료되었는지를 확인 후 결과 반환
        var emailVerificationRequestUid: Long? = null
        bindingMbr.sendEmailVerification.setOnClickListener {
            // 에러 문구 초기화
            bindingMbr.passwordInputLayout.error = null
            bindingMbr.passwordInputLayout.isErrorEnabled = false
            bindingMbr.passwordCheckInputLayout.error = null
            bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
            bindingMbr.emailInputLayout.error = null
            bindingMbr.emailInputLayout.isErrorEnabled = false
            bindingMbr.nickNameInputLayout.error = null
            bindingMbr.nickNameInputLayout.isErrorEnabled = false

            // 인증 이전 비활성화
            bindingMbr.passwordInputLayout.isEnabled = false
            bindingMbr.passwordInputLayout.isFocusable = false
            bindingMbr.passwordCheckInputLayout.isEnabled = false
            bindingMbr.passwordCheckInputLayout.isFocusable = false
            bindingMbr.emailUserJoin.isEnabled = false
            bindingMbr.emailUserJoin.isFocusable = false
            bindingMbr.checkEmailVerification.isEnabled = false
            bindingMbr.checkEmailVerification.isFocusable = false
            bindingMbr.nickNameInputLayout.isEnabled = false
            bindingMbr.nickNameInputLayout.isFocusable = false

            // 인증 이전 숨기기
            bindingMbr.checkEmailVerification.visibility = View.GONE
            bindingMbr.userInputContainer.visibility = View.GONE
            bindingMbr.emailUserJoin.visibility = View.GONE

            // 입력값 적합성 검증
            val emailId = bindingMbr.emailInput.text.toString().trim()

            val emailPattern = android.util.Patterns.EMAIL_ADDRESS

            when {
                "" == emailId.trim() -> { // userName 이 공란일 때
                    bindingMbr.emailInputLayout.error = "이메일을 입력해주세요."
                    bindingMbr.emailInput.requestFocus()
                }

                !emailPattern.matcher(emailId).matches() -> {
                    bindingMbr.emailInputLayout.error = "이메일 형식이 아닙니다."
                    bindingMbr.emailInput.requestFocus()
                }

                else -> { // 적합
                    shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                        false,
                        "검증 요청을 하는 중입니다.",
                        onCanceled = {}
                    )

                    val postEmailVerificationRequestCallback =
                        { statusCode: Int, emailVerificationRequestUid1: Long? ->
                            runOnUiThread {
                                shownDialogInfoVOMbr = null

                                when (statusCode) {
                                    1 -> { // 검증 요청 완료
                                        emailVerificationRequestUid = emailVerificationRequestUid1
                                        bindingMbr.checkEmailVerification.isEnabled = true
                                        bindingMbr.checkEmailVerification.isFocusable = true

                                        bindingMbr.checkEmailVerification.visibility = View.VISIBLE

                                        shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                            true,
                                            "인증 발송",
                                            "인증 이메일을 전송했습니다.\n" +
                                                    "이메일을 확인해주세요.",
                                            null,
                                            onCheckBtnClicked = {
                                                shownDialogInfoVOMbr = null

                                            },
                                            onCanceled = {
                                                shownDialogInfoVOMbr = null

                                            }
                                        )
                                    }
                                    2 -> { // 아이디 중복
                                        bindingMbr.emailInputLayout.error = "이미 가입된 회원입니다."
                                        bindingMbr.emailInput.requestFocus()
                                        emailVerificationRequestUid = null
                                    }
                                    -1 -> { // 네트워크 에러
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
                                    else -> { // 그외 서버 에러
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
                                }
                            }
                        }

                    // 이메일 검증 요청
                    executorServiceMbr.execute {
                        // 아래는 원래 네트워크 서버에서 처리하는 로직
                        // 이메일 중복검사
                        val idCount =
                            repositorySetMbr.databaseRoomMbr.appDatabaseMbr.testUserInfoTableDao()
                                .getIdCount(emailId, 1)

                        if (idCount != 0) { // 아이디 중복
                            postEmailVerificationRequestCallback(2, null)
                        } else {
                            // 12 라는 반환값은 검증 리퀘스트 고유값을 가정. 실제론 서버에서 생성해서 반환해줌
                            postEmailVerificationRequestCallback(1, 12)
                        }
                    }
                }
            }
        }

        // 인증 확인 : emailVerificationRequestUid 의 리퀘스트가 승인되었는지 여부 확인
        bindingMbr.checkEmailVerification.setOnClickListener {
            shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                false,
                "이메일 인증 상태 확인중입니다.",
                onCanceled = {}
            )

            val emailVerificationCheckCallback = { statusCode: Int ->
                runOnUiThread {
                    shownDialogInfoVOMbr = null

                    when (statusCode) {
                        1 -> { // 승인
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "이메일 인증",
                                "이메일 인증이 완료되었습니다.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null

                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null

                                }
                            )

                            bindingMbr.checkEmailVerification.isEnabled = false
                            bindingMbr.checkEmailVerification.isFocusable = false

                            bindingMbr.passwordInputLayout.isEnabled = true
                            bindingMbr.passwordInputLayout.isFocusable = true
                            bindingMbr.passwordCheckInputLayout.isEnabled = true
                            bindingMbr.passwordCheckInputLayout.isFocusable = true
                            bindingMbr.emailUserJoin.isEnabled = true
                            bindingMbr.emailUserJoin.isFocusable = true
                            bindingMbr.nickNameInputLayout.isEnabled = true
                            bindingMbr.nickNameInputLayout.isFocusable = true

                            bindingMbr.userInputContainer.visibility = View.VISIBLE
                            bindingMbr.emailUserJoin.visibility = View.VISIBLE
                        }

                        2 -> { // 대기중
                            Toast.makeText(
                                this,
                                "인증 대기중입니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        3 -> { // 만료된 요청
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "이메일 인증 만료",
                                "만료된 요청입니다.\n다시 한번 인증 발송을 해주세요.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    bindingMbr.checkEmailVerification.isEnabled = false
                                    bindingMbr.checkEmailVerification.isFocusable = false
                                    bindingMbr.checkEmailVerification.visibility = View.GONE
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null
                                    bindingMbr.checkEmailVerification.isEnabled = false
                                    bindingMbr.checkEmailVerification.isFocusable = false
                                    bindingMbr.checkEmailVerification.visibility = View.GONE
                                }
                            )

                        }

                        -1 -> { // 네트워크 에러
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
                        else -> { // 그외 서버 에러
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
                    }
                }
            }

            // 이메일 인증 확인 요청
            executorServiceMbr.execute {
                // 아래는 원래 네트워크 서버에서 처리하는 로직
                emailVerificationCheckCallback(1)
            }
        }

        // 닉네임 입력 리스너
        bindingMbr.nickNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // 만약 text input 에 에러가 있는 경우 입력시 에러를 제거
                bindingMbr.passwordInputLayout.error = null
                bindingMbr.passwordInputLayout.isErrorEnabled = false
                bindingMbr.passwordCheckInputLayout.error = null
                bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
                bindingMbr.nickNameInputLayout.error = null
                bindingMbr.nickNameInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable) {
            }
        })

        // 비밀번호 입력 리스너
        bindingMbr.passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // 만약 text input 에 에러가 있는 경우 입력시 에러를 제거
                bindingMbr.passwordInputLayout.error = null
                bindingMbr.passwordInputLayout.isErrorEnabled = false
                bindingMbr.passwordCheckInputLayout.error = null
                bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
                bindingMbr.nickNameInputLayout.error = null
                bindingMbr.nickNameInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable) {
            }
        })

        // 비밀번호 확인 리스너
        bindingMbr.passwordCheckInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // 만약 text input 에 에러가 있는 경우 입력시 에러를 제거
                bindingMbr.passwordInputLayout.error = null
                bindingMbr.passwordInputLayout.isErrorEnabled = false
                bindingMbr.passwordCheckInputLayout.error = null
                bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
                bindingMbr.nickNameInputLayout.error = null
                bindingMbr.nickNameInputLayout.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable) {

            }
        })

        bindingMbr.emailUserJoin.setOnClickListener {
            shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                false,
                null,
                onCanceled = {}
            )

            // 에러 문구 초기화
            bindingMbr.passwordInputLayout.error = null
            bindingMbr.passwordInputLayout.isErrorEnabled = false
            bindingMbr.passwordCheckInputLayout.error = null
            bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
            bindingMbr.emailInputLayout.error = null
            bindingMbr.emailInputLayout.isErrorEnabled = false
            bindingMbr.nickNameInputLayout.error = null
            bindingMbr.nickNameInputLayout.isErrorEnabled = false

            // 이메일 가입
            val emailId = bindingMbr.emailInput.text.toString().trim()
            val pwStr = bindingMbr.passwordInput.text.toString()
            val pwCheckStr = bindingMbr.passwordCheckInput.text.toString()
            val nickNameStr = bindingMbr.nickNameInput.text.toString()

            val emailPattern = android.util.Patterns.EMAIL_ADDRESS
            val pwPattern =
                Pattern.compile("^.*(?=^.{8,16}\$)(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@#\$%^&+=]).*\$")
            val nicknamePattern = Pattern.compile("^[0-9a-zA-Zㄱ-ㅎ가-힣 ]{1,10}\$")
            // 정보 유효성 검증
            when {
                "" == emailId.trim() -> { // userName 이 공란일 때
                    shownDialogInfoVOMbr = null

                    bindingMbr.emailInput.error = "이메일을 입력해주세요."
                    bindingMbr.emailInput.requestFocus()
                }

                !emailPattern.matcher(emailId).matches() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.emailInput.error = "이메일 형식이 아닙니다."
                    bindingMbr.emailInput.requestFocus()
                }

                "" == nickNameStr.trim() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.nickNameInput.error = "닉네임을 입력해주세요."
                    bindingMbr.nickNameInput.requestFocus()
                }

                !nicknamePattern.matcher(nickNameStr).matches() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.nickNameInput.error = "1~10자의 한글/영문/숫자만 가능합니다."
                    bindingMbr.nickNameInput.requestFocus()
                }

                "" == pwStr.trim() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.passwordInput.error = "비밀번호를 입력해주세요."
                    bindingMbr.passwordInput.requestFocus()
                }

                !pwPattern.matcher(pwStr).matches() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.passwordInput.error = "8~16자의 영문/숫자 조합만 가능합니다."
                    bindingMbr.passwordInput.requestFocus()
                }

                "" == pwCheckStr.trim() -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.passwordCheckInput.error = "비밀번호 확인을 입력해주세요."
                    bindingMbr.passwordCheckInput.requestFocus()
                }

                pwStr != pwCheckStr -> {
                    shownDialogInfoVOMbr = null

                    bindingMbr.passwordCheckInput.error = "비밀번호가 일치하지 않습니다."
                    bindingMbr.passwordCheckInput.requestFocus()
                }

                else -> { // 모든 항목이 충족되면 로그인 정보 검증 수행
                    shownDialogInfoVOMbr = DialogProgressLoading.DialogInfoVO(
                        false,
                        "회원 가입 요청 중입니다.",
                        onCanceled = {}
                    )

                    // 회원가입
                    UserSessionUtil.userJoinToServer(
                        this,
                        UserSessionUtil.UserJoinInputVo(
                            1,
                            emailId,
                            pwStr,
                            nickNameStr
                        ),
                        onComplete = { statusCode: Int ->
                            shownDialogInfoVOMbr = null

                            when (statusCode) {
                                1 -> { // 회원 가입 완료
                                    shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                        true,
                                        "회원가입 완료",
                                        "회원가입을 완료했습니다.",
                                        "닫기",
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
                                2 -> { // 입력값 에러
                                    throw Exception("user join input error")
                                }
                                3 -> { // 이미 가입된 회원
                                    bindingMbr.emailInput.error = "현재 사용중인 아이디입니다."
                                    bindingMbr.emailInput.requestFocus()

                                    // 에러 문구 초기화
                                    bindingMbr.passwordInputLayout.error = null
                                    bindingMbr.passwordInputLayout.isErrorEnabled = false
                                    bindingMbr.passwordCheckInputLayout.error = null
                                    bindingMbr.passwordCheckInputLayout.isErrorEnabled = false
                                    bindingMbr.emailInputLayout.error = null
                                    bindingMbr.emailInputLayout.isErrorEnabled = false
                                    bindingMbr.nickNameInputLayout.error = null
                                    bindingMbr.nickNameInputLayout.isErrorEnabled = false

                                    // 인증 이전 비활성화
                                    bindingMbr.passwordInputLayout.isEnabled = false
                                    bindingMbr.passwordInputLayout.isFocusable = false
                                    bindingMbr.passwordCheckInputLayout.isEnabled = false
                                    bindingMbr.passwordCheckInputLayout.isFocusable = false
                                    bindingMbr.emailUserJoin.isEnabled = false
                                    bindingMbr.emailUserJoin.isFocusable = false
                                    bindingMbr.checkEmailVerification.isEnabled = false
                                    bindingMbr.checkEmailVerification.isFocusable = false
                                    bindingMbr.nickNameInputLayout.isEnabled = false
                                    bindingMbr.nickNameInputLayout.isFocusable = false

                                    // 인증 이전 숨기기
                                    bindingMbr.checkEmailVerification.visibility = View.GONE
                                    bindingMbr.userInputContainer.visibility = View.GONE
                                    bindingMbr.emailUserJoin.visibility = View.GONE

                                }
                                -1 -> { // 네트워크 에러
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
                                else -> { // 그외 서버 에러
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
                            }
                        }
                    )
                }
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

            // 로그인 된 상태라면 진입 금지
            if (currentUserUidMbr != null) {
                finish()
            }
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

                // 로그인 된 상태라면 진입 금지
                if (currentUserUidMbr != null) {
                    finish()
                }
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
                // (c1. 리스트 초기화)

                // (c2. 로더 추가)
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
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있을 때
                                // (c4. 빈 리스트 처리)

                                threadConfluenceObj.threadComplete()
                            } else {
                                runOnUiThread {
                                    // (c5. 받아온 아이템 추가)

                                    // (c6. 스크롤을 가장 앞으로 이동)
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
                getItemListOnComplete(-2, null)
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


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (클래스 비휘발 저장 객체)
    class ActivityEmailUserJoinSampleSpw(application: Application) {
        // <멤버 변수 공간>
        // SharedPreference 접근 객체
        private val spMbr = application.getSharedPreferences(
            "ActivityEmailUserJoinSampleSpw",
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
    class ActivityEmailUserJoinSampleAdapterSet {
        // 어뎁터 #1

    }
}