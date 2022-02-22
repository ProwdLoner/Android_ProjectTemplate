package com.example.prowd_android_template.activity_set.activity_init

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_home.ActivityHome
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.databinding.ActivityInitBinding
import java.net.SocketTimeoutException

class ActivityInit : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityInitBinding

    // (뷰 모델 객체)
    private lateinit var viewModelMbr: ActivityInitViewModel

    // (다이얼로그 객체)
    // 네트워크 에러 다이얼로그(타임아웃 등 retrofit 반환 에러)
    private lateinit var networkErrorDialogMbr: DialogBinaryChoose

    // 서버 에러 다이얼로그(정해진 서버 반환 코드 외의 상황)
    private lateinit var serverErrorDialogMbr: DialogBinaryChoose

    // 업데이트 요청 다이얼로그(앱 실행 최소 버전 미달 시점에 요청)
    private lateinit var versionUpdateDialogMbr: DialogBinaryChoose

    private lateinit var delayCountDownTimerMbr: CountDownTimer


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
        initViewObject()

        // (로직 실행)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr) { // 화면 대기가 끝나지 않았을 때
            // 화면 체류 delay 실행
            delayCountDownTimerMbr = object :
                CountDownTimer(viewModelMbr.countDownRestMilliSecMbr, viewModelMbr.countDownIntervalMbr) {
                override fun onTick(millisUntilFinished: Long) {
                    // 초 마다 화면에 카운트 다운
                    if (viewModelMbr.countDownRestMilliSecMbr.toFloat() % 1000f == 0f) {
                        bindingMbr.countDownTxt.text =
                            (viewModelMbr.countDownRestMilliSecMbr.toFloat() / 1000f).toInt()
                                .toString()
                    }

                    viewModelMbr.countDownRestMilliSecMbr =
                        viewModelMbr.countDownRestMilliSecMbr - viewModelMbr.countDownIntervalMbr
                }

                override fun onFinish() {
                    viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr = true
                    goToNextActivity()
                }
            }.start()
        }

        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동
            doActivityInit()
        }
    }

    override fun onPause() {
        if (!viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr) { // 화면 대기가 끝나지 않았을 때
            delayCountDownTimerMbr.cancel()
        }

        super.onPause()
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
        networkErrorDialogMbr.dismiss()
        serverErrorDialogMbr.dismiss()
        versionUpdateDialogMbr.dismiss()

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

        // (다이얼로그 생성)
        networkErrorDialogMbr = DialogBinaryChoose(
            this,
            true,
            "네트워크 에러",
            "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            "다시시도",
            "종료",
            onPosBtnClickedMbr = {
                viewModelMbr.isNetworkErrorDialogShownMbr = false

                // 로직 다시 실행
                doActivityInit()
            },
            onNegBtnClickedMbr = {
                viewModelMbr.isNetworkErrorDialogShownMbr = false
                finish()
            },
            onCanceledMbr = {
                viewModelMbr.isNetworkErrorDialogShownMbr = false
                finish()
            }
        )

        serverErrorDialogMbr = DialogBinaryChoose(
            this,
            true,
            "서버 에러",
            "현재 서버의 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            "다시시도",
            "종료",
            onPosBtnClickedMbr = {
                viewModelMbr.isServerErrorDialogShownMbr = false

                // 로직 다시 실행
                doActivityInit()
            },
            onNegBtnClickedMbr = {
                viewModelMbr.isServerErrorDialogShownMbr = false
                finish()
            },
            onCanceledMbr = {
                viewModelMbr.isServerErrorDialogShownMbr = false
                finish()
            }
        )

        versionUpdateDialogMbr = DialogBinaryChoose(
            this,
            true,
            "업데이트 안내",
            "서비스를 이용하기 위해\n업데이트 하시겠습니까?",
            null,
            null,
            onPosBtnClickedMbr = { // 긍정
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
                    viewModelMbr.isVersionUpdateDialogShownMbr = false
                    finish()
                }
            },
            onNegBtnClickedMbr = { // 부정
                viewModelMbr.isVersionUpdateDialogShownMbr = false
                finish()
            },
            onCanceledMbr = {
                viewModelMbr.isVersionUpdateDialogShownMbr = false
                finish()
            }
        )
    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

        }
    }

    // 초기 뷰 설정
    private fun initViewObject() {
        // (화면 회전시 뷰모델 정보에 따른 화면 복구)
        // 네트워크 에러 다이얼로그 여부
        if (viewModelMbr.isNetworkErrorDialogShownMbr) {
            networkErrorDialogMbr.show()
        } else {
            networkErrorDialogMbr.dismiss()
        }

        // 서버 에러 다이얼로그 여부
        if (viewModelMbr.isServerErrorDialogShownMbr) {
            serverErrorDialogMbr.show()
        } else {
            serverErrorDialogMbr.dismiss()
        }

        // 버전 업데이트 다이얼로그 여부
        if (viewModelMbr.isVersionUpdateDialogShownMbr) {
            versionUpdateDialogMbr.show()
        } else {
            versionUpdateDialogMbr.dismiss()
        }

        // (뷰 정보 설정)
        bindingMbr.countDownTxt.text =
            (viewModelMbr.countDownRestMilliSecMbr.toFloat() / 1000f).toInt().toString()

        // (리스너 설정)
    }

    // 액티비티 초기화 로직
    private fun doActivityInit() {
        // 앱 버전 체크 실행
        if (!viewModelMbr.checkAppVersionAsyncOnProgressedMbr) {
            // 메소드 실행중이 아닐 때,

            viewModelMbr.checkAppVersionAsync(
                onComplete = { needUpdate ->
                    runOnUiThread checkAppVersionAsyncComplete@{
                        if (needUpdate) { // 업데이트 필요
                            viewModelMbr.isVersionUpdateDialogShownMbr = true
                            versionUpdateDialogMbr.show()
                        } else { // 업데이트 불필요
                            // 로그인 검증 실행
                            if (!viewModelMbr.checkLoginSessionAsyncOnProgressedMbr) {
                                // 메소드 실행중이 아닐 때,

                                val loginPref = this@ActivityInit.getSharedPreferences(
                                    getString(R.string.pref_login),
                                    Context.MODE_PRIVATE
                                )

                                val loginType: Int =
                                    loginPref.getInt(
                                        getString(R.string.pref_login_login_type_int),
                                        0
                                    )
                                val serverId: String? =
                                    loginPref.getString(
                                        getString(R.string.pref_login_user_server_id_string),
                                        null
                                    )
                                val serverPw: String? =
                                    loginPref.getString(
                                        getString(R.string.pref_login_user_server_pw_string),
                                        null
                                    )

                                viewModelMbr.checkLoginSessionAsync(
                                    ActivityInitViewModel.CheckLoginSessionParameterVO(
                                        loginType,
                                        serverId,
                                        serverPw
                                    ),
                                    onComplete = { checkLoginSessionResult ->
                                        runOnUiThread checkLoginSessionAsyncComplete@{
                                            // 검증 후 결과를 sharedPreferences 에 대입
                                            with(loginPref.edit()) {
                                                putString(
                                                    getString(R.string.pref_login_session_token_string),
                                                    checkLoginSessionResult.sessionToken
                                                )
                                                putString(
                                                    getString(R.string.pref_login_user_nick_name_string),
                                                    checkLoginSessionResult.userNickName
                                                )
                                                putInt(
                                                    getString(R.string.pref_login_login_type_int),
                                                    checkLoginSessionResult.loginType
                                                )
                                                putString(
                                                    getString(R.string.pref_login_user_server_id_string),
                                                    checkLoginSessionResult.userServerId
                                                )
                                                putString(
                                                    getString(R.string.pref_login_user_server_pw_string),
                                                    checkLoginSessionResult.userServerPw
                                                )

                                                apply()
                                            }

                                            // 다음 엑티비티로 이동
                                            goToNextActivity()
                                        }
                                    },
                                    onError = { checkLoginSessionAsyncError ->
                                        runOnUiThread checkLoginSessionAsyncError@{
                                            if (checkLoginSessionAsyncError is SocketTimeoutException) { // 타임아웃 에러
                                                viewModelMbr.isNetworkErrorDialogShownMbr = true
                                                networkErrorDialogMbr.show()
                                            } else {
                                                viewModelMbr.isServerErrorDialogShownMbr = true
                                                serverErrorDialogMbr.contentMbr =
                                                    "현재 서버의 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.\n\n에러 메시지 :\n${checkLoginSessionAsyncError.message}"
                                                serverErrorDialogMbr.show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                onError = { checkAppVersionAsyncError ->
                    runOnUiThread checkAppVersionAsyncError@{
                        if (checkAppVersionAsyncError is SocketTimeoutException) { // 타임아웃 에러
                            viewModelMbr.isNetworkErrorDialogShownMbr = true
                            networkErrorDialogMbr.show()
                        } else {
                            viewModelMbr.isServerErrorDialogShownMbr = true
                            serverErrorDialogMbr.contentMbr =
                                "현재 서버의 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.\n\n에러 메시지 :\n${checkAppVersionAsyncError.message}"
                            serverErrorDialogMbr.show()
                        }
                    }
                }
            )
        }
    }

    private fun goToNextActivity() {
        if (viewModelMbr.delayGoToNextActivityAsyncCompletedOnceMbr && // 앱 대기 시간이 끝났을 때
            viewModelMbr.checkAppVersionAsyncCompletedOnceMbr && // 앱 버전 검증이 끝났을 때
            viewModelMbr.checkLoginSessionAsyncCompletedOnceMbr // 로그인 검증이 끝났을 때
        ) {
            val intent =
                Intent(
                    this,
                    ActivityHome::class.java
                )
            startActivity(intent)
            finish()
        }
    }
}