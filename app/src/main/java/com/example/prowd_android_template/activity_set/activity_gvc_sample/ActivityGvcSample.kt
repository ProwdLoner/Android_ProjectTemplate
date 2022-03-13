package com.example.prowd_android_template.activity_set.activity_gvc_sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.activity_set.activity_gvc_sample_controller.ActivityGvcSampleController
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityGvcSampleBinding


// GVC 란?
// Global Variable Connector 로,
// static 변수를 사용했을 때에 메모리에 상주하는 문제, 그리고 OS 정책에 따라 멋대로 초기화되는 문제에 대비하기 위해,
// Shared Preference 를 보다 쉽게 사용해주도록 하는 래핑 클래스입니다.

// 단순히 다른 액티비티에서 result 만 얻어올 것이라면 resultLauncher 를 사용하면 되는데,
// 이는 바로 이후의 액티비티에 한정되는 것으로,

// 만약 히스토리에 남겨진 상태에서 이후 불특정 액티비티의 작용을 받으려면 GVC 를 사용하면 됩니다.
// 로그인 세션 범용 GVC 가 대표적으로,
// 액티비티 종속 GVC 는, 예를들면 즐겨찾기 목록 화면이라 가정하면, 다른 상세보기 화면에서 즐겨찾기를 했을 때에,
// 즐겨찾기 목록을 갱신하고자 한다면 목록 화면에서 GVC 를 감시하고, 상세보기에서 GVC 상태를 변경하는 방식으로 사용합니다.
class ActivityGvcSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityGvcSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityGvcSampleViewModel

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
        bindingMbr = ActivityGvcSampleBinding.inflate(layoutInflater)
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
        // 액티비티 GVC 가져오기
        val messageInfoVo = viewModelMbr.thisActivityGvcMbr.getData()

        // gvc 에 저장된 데이터 표현 후 비워주기
        // gvc 가 플래그로 사용됨
        if (messageInfoVo.message1 != null || messageInfoVo.message2 != null) {
            val myToast = Toast.makeText(
                this,
                "message1 : ${messageInfoVo.message1}, message2 : ${messageInfoVo.message2}",
                Toast.LENGTH_SHORT
            )
            myToast.show()

            viewModelMbr.thisActivityGvcMbr.setData(
                ActivityGvcSampleGvc.ColumnVo(null, null)
            )
        }

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때
            val loginInfo = viewModelMbr.currentLoginSessionInfoGvcMbr.getData()

            val sessionToken = loginInfo.sessionToken

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
        viewModelMbr = ViewModelProvider(this)[ActivityGvcSampleViewModel::class.java]

    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동
            val loginInfo = viewModelMbr.currentLoginSessionInfoGvcMbr.getData()

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr = loginInfo.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 전역 변수 조작 화면으로 이동
        bindingMbr.goToGvcControlActivityBtn.setOnClickListener {
            val intent =
                Intent(
                    this,
                    ActivityGvcSampleController::class.java
                )
            startActivity(intent)
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