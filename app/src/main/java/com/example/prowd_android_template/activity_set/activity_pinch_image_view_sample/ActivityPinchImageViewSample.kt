package com.example.prowd_android_template.activity_set.activity_pinch_image_view_sample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.example.prowd_android_template.R
import com.example.prowd_android_template.activity_set.activity_pinch_image_viewer.ActivityPinchImageViewer
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityPinchImageViewSampleBinding
import java.io.File
import java.io.FileOutputStream

class ActivityPinchImageViewSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityPinchImageViewSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityPinchImageViewSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        createMemberObjects()

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // (뷰 객체 바인딩)
        // 여기까지는 화면이 나오지 않으니 앞의 작업은 가벼워야함
        setContentView(bindingMbr.root)

        // (이외 생명주기 로직)
        onCreateLogic()
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

        // 설정 변경(화면회전)을 했는지 여부를 초기화
        // onResume 의 가장 마지막
        viewModelMbr.isChangingConfigurationsMbr = false
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
        progressLoadingDialogMbr?.dismiss()
        radioBtnDialogMbr?.dismiss()

        super.onDestroy()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 객체
        bindingMbr = ActivityPinchImageViewSampleBinding.inflate(layoutInflater)

        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityPinchImageViewSampleViewModel::class.java]

    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // 이미지가 너무 크면 xml 에서 곧바로 설정시 에러 발생. 고로 Glide 를 사용해줌
        // (혹은 drawable 폴더 각 해상도에 맞게 각 사이즈 이미지를 분배하는 것도 좋음)
        if (!isFinishing && !isDestroyed) {
            Glide.with(this)
                .load(R.drawable.img_activity_pinch_image_view_sample)
                .transform(CenterCrop())
                .into(bindingMbr.sampleImage)
        }

        bindingMbr.sampleImage.setOnClickListener {
            viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value =
                DialogProgressLoading.DialogInfoVO(
                    false,
                    "고해상도 이미지를 준비중입니다.",
                    onCanceled = {}
                )

            viewModelMbr.executorServiceMbr?.execute {
                // 비트맵 추출
                val bitmap = (AppCompatResources.getDrawable(
                    this,
                    R.drawable.img_activity_pinch_image_view_sample
                ) as BitmapDrawable).bitmap

                // 비트맵을 파일로 저장
                val tempFile = File(cacheDir, "temp.jpg")
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                out.close()

                val intent =
                    Intent(
                        this,
                        ActivityPinchImageViewer::class.java
                    )
                intent.putExtra("image_file_path", tempFile.absolutePath)

                runOnUiThread {
                    viewModelMbr.progressLoadingDialogInfoLiveDataMbr.value = null

                    startActivity(intent)
                }
            }
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
    }

    private fun onCreateLogic() {

    }
}