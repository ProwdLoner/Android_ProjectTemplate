package com.example.prowd_android_template.activity_set.activity_network_recycler_view_sample_editor

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicRecyclerViewSampleEditorBinding
import kotlinx.parcelize.Parcelize
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

class ActivityNetworkRecyclerViewSampleEditor : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityBasicRecyclerViewSampleEditorBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityNetworkRecyclerViewSampleEditorViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicRecyclerViewSampleEditorBinding.inflate(layoutInflater)
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
        viewModelMbr =
            ViewModelProvider(this)[ActivityNetworkRecyclerViewSampleEditorViewModel::class.java]

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
        // 초기 업로드 버튼 세팅
        bindingMbr.contentUploadBtn.setOnClickListener {
            val contentTitleTxt = bindingMbr.contentTitleEditor.text.toString()
            val contentTxt = bindingMbr.contentEditor.text.toString()
            val utcDataFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
            utcDataFormat.timeZone = TimeZone.getTimeZone("UTC")
            val writeTime = utcDataFormat.format(Date())

            // 로딩 다이얼로그 생성
            viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                DialogProgressLoading.DialogInfoVO(
                    false,
                    "처리중입니다.",
                    onCanceled = {
                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null
                    }
                )

            // 서버에 생성 요청
            viewModelMbr.addScreenVerticalRecyclerViewAdapterItemDataOnVMAsync(
                contentTitleTxt,
                contentTxt,
                writeTime,
                onComplete = {
                    runOnUiThread {
                        // 로딩 다이얼로그 제거
                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null

                        val resultIntent = intent
                        resultIntent.putExtra(
                            "result", ResultVo(
                                it,
                                contentTitleTxt,
                                contentTxt,
                                writeTime
                            )
                        )
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                },
                onError = { addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult ->
                    runOnUiThread {
                        // 로딩 다이얼로그 제거
                        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null

                        if (addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult is SocketTimeoutException) { // 타임아웃 에러
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "네트워크 에러",
                                    "현재 네트워크 상태가 원활하지 않습니다.\n잠시 후 다시 시도해주세요.",
                                    "확인",
                                    onCheckBtnClicked = {
                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            null
                                    },
                                    onCanceled = {
                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            null
                                    }
                                )
                        } else { // 그외 에러
                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                DialogConfirm.DialogInfoVO(
                                    true,
                                    "서버 에러",
                                    "현재 서버의 상태가 원활하지 않습니다.\n" +
                                            "잠시 후 다시 시도해주세요.\n" +
                                            "\n" +
                                            "에러 메시지 :\n" +
                                            "${addScreenVerticalRecyclerViewAdapterItemDataOnVMAsyncResult.message}",
                                    "확인",
                                    onCheckBtnClicked = {
                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            null
                                    },
                                    onCanceled = {
                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            null

                                    }
                                )
                        }
                    }
                }
            )
        }

        setContentUploadBtn()

        // 글자를 입력할 때마다 업로드 버튼 활성화 세팅
        bindingMbr.contentTitleEditor.addTextChangedListener {
            setContentUploadBtn()
        }
        bindingMbr.contentEditor.addTextChangedListener {
            setContentUploadBtn()
        }
    }

    // 업로드 버튼 세팅
    private fun setContentUploadBtn() {
        val contentTitleTxtSize = bindingMbr.contentTitleEditor.text.toString().length
        val contentTxtSize = bindingMbr.contentEditor.text.toString().length

        // 업로드 버튼 활성 / 비활성화 = 모든 텍스트가 한글자 이상일 때
        if (0 < contentTitleTxtSize && 0 < contentTxtSize) {
            // 버튼 활성
            bindingMbr.contentUploadBtn.setBackgroundColor(Color.parseColor("#FF5B92E4"))
            bindingMbr.contentUploadBtn.isEnabled = true
            bindingMbr.contentUploadBtn.isClickable = true
        } else {
            // 버튼 비활성
            bindingMbr.contentUploadBtn.setBackgroundColor(Color.parseColor("#FFE6E6E6"))
            bindingMbr.contentUploadBtn.isEnabled = false
            bindingMbr.contentUploadBtn.isClickable = false
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
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    @Parcelize
    data class ResultVo(
        val itemContentUid: Long,
        val itemTitle: String,
        val itemContentBody: String,
        val writeTime: String
    ) : Parcelable
}