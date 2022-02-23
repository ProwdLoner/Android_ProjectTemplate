package com.example.prowd_android_template.activity_set.activity_dialog_sample

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityDialogSampleBinding

class ActivityDialogSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityDialogSampleBinding

    // (뷰 모델 객체)
    private lateinit var viewModelMbr: ActivityDialogSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private lateinit var progressLoadingDialogMbr: DialogProgressLoading

    // 선택 다이얼로그
    private lateinit var binaryChooseDialogMbr: DialogBinaryChoose

    // 확인 다이얼로그
    private lateinit var confirmDialogMbr: DialogConfirm


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityDialogSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (초기 뷰 설정)
        viewSetting()

        // (로직 실행)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

        }
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

            if (sessionToken != viewModelMbr.currentUserSessionTokenMbr){// 액티비티 유저와 세션 유저가 다를 때
                //  데이터 로딩

                // 현 액티비티 진입 유저 저장
                viewModelMbr.currentUserSessionTokenMbr = sessionToken
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
        progressLoadingDialogMbr.dismiss()
        binaryChooseDialogMbr.dismiss()
        confirmDialogMbr.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityDialogSampleViewModel::class.java]

        // (다이얼로그 생성)
        progressLoadingDialogMbr = DialogProgressLoading(
            this,
            true,
            "로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중",
            onCanceledMbr = {
                val myToast = Toast.makeText(
                    this,
                    "progressLoadingDialogMbr - canceled",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isProgressLoadingDialogShownLiveDataMbr.value = false
            }
        )

        binaryChooseDialogMbr = DialogBinaryChoose(
            this,
            true,
            "네트워크 에러",
            "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            null,
            null,
            onPosBtnClickedMbr = {
                val myToast = Toast.makeText(
                    this,
                    "binaryChooseDialogMbr - pos",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isBinaryChooseDialogShownLiveDataMbr.value = false
            },
            onNegBtnClickedMbr = {
                val myToast = Toast.makeText(
                    this,
                    "binaryChooseDialogMbr - neg",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isBinaryChooseDialogShownLiveDataMbr.value = false
            },
            onCanceledMbr = {
                val myToast = Toast.makeText(
                    this,
                    "binaryChooseDialogMbr - canceled",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isBinaryChooseDialogShownLiveDataMbr.value = false
            }
        )

        confirmDialogMbr = DialogConfirm(
            this,
            true,
            "서버 에러",
            "현재 서버의 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            null,
            onCheckBtnClickedMbr = {
                val myToast = Toast.makeText(
                    this,
                    "confirmDialogMbr - check",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isConfirmDialogShownLiveDataMb.value = false
            },
            onCanceledMbr = {
                val myToast = Toast.makeText(
                    this,
                    "confirmDialogMbr - canceled",
                    Toast.LENGTH_SHORT
                )
                myToast.show()

                viewModelMbr.isConfirmDialogShownLiveDataMb.value = false
            }
        )

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
        // 로딩 다이얼로그 테스트 버튼
        bindingMbr.testLoadingDialogBtn.setOnClickListener {
            viewModelMbr.isProgressLoadingDialogShownLiveDataMbr.value = true
            progressLoadingDialogMbr.show()
        }

        // 선택 다이얼로그 테스트 버튼
        bindingMbr.testBinaryChooseDialogBtn.setOnClickListener {
            viewModelMbr.isBinaryChooseDialogShownLiveDataMbr.value = true
            binaryChooseDialogMbr.show()
        }

        // 확인 다이얼로그 테스트 버튼
        bindingMbr.testConfirmDialogBtn.setOnClickListener {
            viewModelMbr.isConfirmDialogShownLiveDataMb.value = true
            confirmDialogMbr.show()
        }

    }

    // 라이브 데이터 설정
    private fun setLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.isProgressLoadingDialogShownLiveDataMbr.observe(this) {
            if (it) {
                progressLoadingDialogMbr.show()
            } else {
                progressLoadingDialogMbr.dismiss()
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.isBinaryChooseDialogShownLiveDataMbr.observe(this) {
            if (it) {
                binaryChooseDialogMbr.show()
            } else {
                binaryChooseDialogMbr.dismiss()
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.isConfirmDialogShownLiveDataMb.observe(this) {
            if (it) {
                confirmDialogMbr.show()
            } else {
                confirmDialogMbr.dismiss()
            }
        }
    }
}