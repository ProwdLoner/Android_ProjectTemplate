package com.example.prowd_android_template.activity_set.activity_home

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_dialog_sample.ActivityDialogSample
import com.example.prowd_android_template.activity_set.activity_recycler_view_sample.ActivityRecyclerViewSample
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityHomeBinding

class ActivityHome : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityHomeBinding

    // (뷰 모델 객체)
    private lateinit var viewModelMbr: ActivityHomeViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private lateinit var progressLoadingDialogMbr: DialogProgressLoading

    // 네트워크 에러 다이얼로그(타임아웃 등 retrofit 반환 에러)
    private lateinit var networkErrorDialogMbr: DialogConfirm

    // 서버 에러 다이얼로그(정해진 서버 반환 코드 외의 상황)
    private lateinit var serverErrorDialogMbr: DialogConfirm


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (뷰 리스너 설정)
        setViewListener()

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

            if (sessionToken != viewModelMbr.currentUserSessionTokenMbr) {// 액티비티 유저와 세션 유저가 다를 때
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
        networkErrorDialogMbr.dismiss()
        serverErrorDialogMbr.dismiss()
        progressLoadingDialogMbr.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityHomeViewModel::class.java]

        // (다이얼로그 생성)
        networkErrorDialogMbr = DialogConfirm(
            this,
            true,
            "네트워크 에러",
            "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            null,
            onCheckBtnClickedMbr = {
                viewModelMbr.isNetworkErrorDialogShownLiveDataMbr.value = false
            },
            onCanceledMbr = {
                viewModelMbr.isNetworkErrorDialogShownLiveDataMbr.value = false
            }
        )

        serverErrorDialogMbr = DialogConfirm(
            this,
            true,
            "서버 에러",
            "현재 서버의 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
            null,
            onCheckBtnClickedMbr = {
                viewModelMbr.isServerErrorDialogShownLiveDataMbr.value = false
            },
            onCanceledMbr = {
                viewModelMbr.isServerErrorDialogShownLiveDataMbr.value = false
            }
        )

        progressLoadingDialogMbr = DialogProgressLoading(
            this,
            true,
            "로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중",
            onCanceledMbr = {
                viewModelMbr.isProgressLoadingDialogShownLiveDataMbr.value = false
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

    // 뷰 리스너 설정
    private fun setViewListener() {
        // 다이얼로그 샘플 이동 버튼
        bindingMbr.goToDialogSampleBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityDialogSample::class.java
                )
            startActivity(intent)
        }

        // 네트워크 샘플 이동 버튼
        bindingMbr.goToNetworkSampleBtn.setOnClickListener {
            // TODO
        }

        // 데이터베이스 샘플 이동 버튼
        bindingMbr.goToDatabaseSampleBtn.setOnClickListener {
            // TODO
        }

        // 회원관리 샘플 이동 버튼
        bindingMbr.goToUserManagementSampleBtn.setOnClickListener {
            // TODO
        }

        // 리사이클러 뷰 샘플 이동 버튼
        bindingMbr.goToRecyclerViewSampleBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityRecyclerViewSample::class.java
                )
            startActivity(intent)
        }

        // JNI 샘플 이동 버튼
        bindingMbr.goToJniSampleBtn.setOnClickListener {
            // TODO
        }

        // 카메라 샘플 이동 버튼
        bindingMbr.goToCameraSampleBtn.setOnClickListener {
            // TODO
        }

        // 이미지 샘플 이동 버튼
        bindingMbr.goToImageSampleBtn.setOnClickListener {
            // TODO
        }

        // 동영상 샘플 이동 버튼
        bindingMbr.goToVideoSampleBtn.setOnClickListener {
            // TODO
        }

        // 오디오 샘플 이동 버튼
        bindingMbr.goToAudioSampleBtn.setOnClickListener {
            // TODO
        }

        // 머신러닝 샘플 이동 버튼
        bindingMbr.goToMachineLearningSampleBtn.setOnClickListener {
            // TODO
        }

        // 지도 샘플 이동 버튼
        bindingMbr.goToMapSampleBtn.setOnClickListener {
            //TODO
        }

        // 달력 샘플 이동 버튼
        bindingMbr.goToCalendarSampleBtn.setOnClickListener {
            // TODO
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

        // 네트워크 에러 다이얼로그 출력 플래그
        viewModelMbr.isNetworkErrorDialogShownLiveDataMbr.observe(this) {
            if (it) {
                networkErrorDialogMbr.show()
            } else {
                networkErrorDialogMbr.dismiss()
            }
        }

        // 서버 에러 다이얼로그 출력 플래그
        viewModelMbr.isServerErrorDialogShownLiveDataMbr.observe(this) {
            if (it) {
                serverErrorDialogMbr.show()
            } else {
                serverErrorDialogMbr.dismiss()
            }
        }
    }
}