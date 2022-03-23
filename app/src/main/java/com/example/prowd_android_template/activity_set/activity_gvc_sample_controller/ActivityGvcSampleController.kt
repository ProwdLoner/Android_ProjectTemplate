package com.example.prowd_android_template.activity_set.activity_gvc_sample_controller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityGvcSampleControllerBinding

// 메시지 세팅 버튼을 누르고 ActivityGvcSample 로 돌아가면, 해당 메시지가 토스트로 출력 됨
class ActivityGvcSampleController : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityGvcSampleControllerBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityGvcSampleControllerViewModel

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
        bindingMbr = ActivityGvcSampleControllerBinding.inflate(layoutInflater)
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

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때
            val sessionToken = viewModelMbr.currentLoginSessionInfoGvcMbr.sessionToken

            if (viewModelMbr.isDataFirstLoadingMbr || // 데이터 최초 로딩 시점일 때 혹은,
                sessionToken != viewModelMbr.currentUserSessionTokenMbr // 액티비티 유저와 세션 유저가 다를 때
            ) {
                // 진입 플래그 변경
                viewModelMbr.isDataFirstLoadingMbr = false
                viewModelMbr.currentUserSessionTokenMbr = sessionToken

                //  데이터 로딩
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
        viewModelMbr = ViewModelProvider(this)[ActivityGvcSampleControllerViewModel::class.java]

    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoGvcMbr.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 메시지1 세팅 버튼
        bindingMbr.setGvcMessage1.setOnClickListener {
            // 메시지 입력 전이라면 메시지를 입력, 메시지 입력 상태라면 메시지를 비우기
            if ("" == bindingMbr.message1Txt.text.toString()) {
                val message1 = "메시지1"

                viewModelMbr.activityGvcSampleGvcMbr.message1 = message1

                bindingMbr.message1Txt.text = message1
            } else {
                val message1 = null

                viewModelMbr.activityGvcSampleGvcMbr.message1 = message1

                bindingMbr.message1Txt.text = message1
            }
        }

        // 메시지2 세팅 버튼
        bindingMbr.setGvcMessage2.setOnClickListener {
            // 메시지 입력 전이라면 메시지를 입력, 메시지 입력 상태라면 메시지를 비우기
            if ("" == bindingMbr.message2Txt.text.toString()) {
                val message2 = "메시지2"

                viewModelMbr.activityGvcSampleGvcMbr.message2 = message2

                bindingMbr.message2Txt.text = message2
            } else {
                val message2 = null

                viewModelMbr.activityGvcSampleGvcMbr.message2 = message2

                bindingMbr.message2Txt.text = message2
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
    }
}