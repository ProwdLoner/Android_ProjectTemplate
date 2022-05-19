package com.example.prowd_android_template.activity_set.activity_dialog_sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityDialogSampleBinding

class ActivityDialogSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityDialogSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityDialogSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null


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

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (데이터 갱신 시점 적용)
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 화면 회전이 아닐 때
            val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken

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
        progressLoadingDialogMbr?.dismiss()
        binaryChooseDialogMbr?.dismiss()
        confirmDialogMbr?.dismiss()

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

    }

    // viewModel 저장용 데이터 초기화
    private fun createViewModelDataObjects() {
        if (!viewModelMbr.isChangingConfigurationsMbr) { // 설정 변경(화면회전)이 아닐 때에 발동

            // 현 액티비티 진입 유저 저장
            viewModelMbr.currentUserSessionTokenMbr =
                viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
        }
    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 로딩 다이얼로그 테스트 버튼
        bindingMbr.testLoadingDialogBtn.setOnClickListener {
            viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                DialogProgressLoading.DialogInfoVO(
                    true,
                    "로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중, 로딩중",
                    onCanceled = {
                        val myToast = Toast.makeText(
                            this,
                            "progressLoadingDialogMbr? - canceled",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                    }
                )
        }

        bindingMbr.testLoadingDialogWithProgressBarBtn.setOnClickListener {
            if (viewModelMbr.progressDialogSample2ProgressValue.value != 0
            ) {
                return@setOnClickListener
            }

            viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                DialogProgressLoading.DialogInfoVO(
                    false,
                    "로딩중 0%",
                    onCanceled = {}
                )

            progressLoadingDialogMbr?.bindingMbr?.progressBar?.max = 100

            viewModelMbr.executorServiceMbr?.execute {
                for (count in 0..100) {
                    runOnUiThread {
                        viewModelMbr.progressDialogSample2ProgressValue.value = count
                    }

                    Thread.sleep(100)
                }

                runOnUiThread {
                    viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                    viewModelMbr.progressDialogSample2ProgressValue.value = 0
                }
            }
        }

        // 선택 다이얼로그 테스트 버튼
        bindingMbr.testBinaryChooseDialogBtn.setOnClickListener {
            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                DialogBinaryChoose.DialogInfoVO(
                    true,
                    "선택 다이얼로그 테스트",
                    "Yes or No?",
                    null,
                    null,
                    onPosBtnClicked = {
                        val myToast = Toast.makeText(
                            this,
                            "binaryChooseDialogMbr? - pos",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                    },
                    onNegBtnClicked = {
                        val myToast = Toast.makeText(
                            this,
                            "binaryChooseDialogMbr? - neg",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                    },
                    onCanceled = {
                        val myToast = Toast.makeText(
                            this,
                            "binaryChooseDialogMbr? - canceled",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null
                    }
                )
        }

        // 확인 다이얼로그 테스트 버튼
        bindingMbr.testConfirmDialogBtn.setOnClickListener {
            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                DialogConfirm.DialogInfoVO(
                    true,
                    "확인 다이얼로그 테스트",
                    "Check Dialog",
                    null,
                    onCheckBtnClicked = {
                        val myToast = Toast.makeText(
                            this,
                            "confirmDialogMbr? - check",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    },
                    onCanceled = {
                        val myToast = Toast.makeText(
                            this,
                            "confirmDialogMbr? - canceled",
                            Toast.LENGTH_SHORT
                        )
                        myToast.show()

                        viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    }
                )
        }

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

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            val loadingText = "로딩중 $it%"
            progressLoadingDialogMbr?.bindingMbr?.progressMessageTxt?.text = loadingText
            progressLoadingDialogMbr?.bindingMbr?.progressBar?.visibility = View.VISIBLE
            progressLoadingDialogMbr?.bindingMbr?.progressBar?.progress = it
        }
    }
}