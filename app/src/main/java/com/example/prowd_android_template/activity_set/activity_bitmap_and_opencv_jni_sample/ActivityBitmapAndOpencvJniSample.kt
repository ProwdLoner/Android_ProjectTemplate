package com.example.prowd_android_template.activity_set.activity_bitmap_and_opencv_jni_sample

import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.prowd_android_template.R
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBitmapAndOpencvJniSampleBinding
import com.example.prowd_android_template.native_wrapper.NativeWrapperOpenCvTest

class ActivityBitmapAndOpencvJniSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBitmapAndOpencvJniSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBitmapAndOpencvJniSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // 라디오 버튼 선택 다이얼로그
    var radioButtonChooseDialogMbr: DialogRadioButtonChoose? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBitmapAndOpencvJniSampleBinding.inflate(layoutInflater)
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
        confirmDialogMbr?.dismiss()
        radioButtonChooseDialogMbr?.dismiss()

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
            ViewModelProvider(this)[ActivityBitmapAndOpencvJniSampleViewModel::class.java]

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
        // (RGB 수치 파악)
        // redBitmap.config == ARGB_8888
        val redRgb = NativeWrapperOpenCvTest.getBitmapRGB(
            (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_red
            ) as BitmapDrawable).bitmap
        )

        val redRgbTxt = "R : ${redRgb[0]}\nG : ${redRgb[1]}\nB : ${redRgb[2]}"
        bindingMbr.redRgbTxt.text = redRgbTxt

        val greenRgb = NativeWrapperOpenCvTest.getBitmapRGB(
            (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_green
            ) as BitmapDrawable).bitmap
        )

        val greenRgbTxt = "R : ${greenRgb[0]}\nG : ${greenRgb[1]}\nB : ${greenRgb[2]}"
        bindingMbr.greenRgbTxt.text = greenRgbTxt

        val blueRgb = NativeWrapperOpenCvTest.getBitmapRGB(
            (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_blue
            ) as BitmapDrawable).bitmap
        )
        val blueRgbTxt = "R : ${blueRgb[0]}\nG : ${blueRgb[1]}\nB : ${blueRgb[2]}"
        bindingMbr.blueRgbTxt.text = blueRgbTxt

        // (RGB 이미지 클릭 리스너 설정)
        bindingMbr.redSrcImg.setOnClickListener {
            val redBitmap1 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_red
            ) as BitmapDrawable).bitmap
            val copyBitmap1 = redBitmap1.copy(redBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(redBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val redBitmap2 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_red
            ) as BitmapDrawable).bitmap
            val copyBitmap2 = redBitmap2.copy(redBitmap2.config, true)
            NativeWrapperOpenCvTest.getCopyBitmap(redBitmap2, copyBitmap2)

            Glide.with(this)
                .load(copyBitmap2)
                .into(bindingMbr.copyImg)
        }

        bindingMbr.greenSrcImg.setOnClickListener {
            val greenBitmap1 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_green
            ) as BitmapDrawable).bitmap
            val copyBitmap1 = greenBitmap1.copy(greenBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(greenBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val greenBitmap2 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_green
            ) as BitmapDrawable).bitmap
            val copyBitmap2 = greenBitmap2.copy(greenBitmap2.config, true)
            NativeWrapperOpenCvTest.getCopyBitmap(greenBitmap2, copyBitmap2)

            Glide.with(this)
                .load(copyBitmap2)
                .into(bindingMbr.copyImg)
        }

        bindingMbr.blueSrcImg.setOnClickListener {
            val blueBitmap1 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_blue
            ) as BitmapDrawable).bitmap
            val copyBitmap1 = blueBitmap1.copy(blueBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(blueBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val blueBitmap2 = (AppCompatResources.getDrawable(
                this,
                R.drawable.img_activity_bitmap_and_opencv_jni_sample_blue
            ) as BitmapDrawable).bitmap
            val copyBitmap2 = blueBitmap2.copy(blueBitmap2.config, true)
            NativeWrapperOpenCvTest.getCopyBitmap(blueBitmap2, copyBitmap2)

            Glide.with(this)
                .load(copyBitmap2)
                .into(bindingMbr.copyImg)
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

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            if (it != -1) {
                val loadingText = "로딩중 $it%"
                progressLoadingDialogMbr?.bindingMbr?.progressMessageTxt?.text = loadingText
                progressLoadingDialogMbr?.bindingMbr?.progressBar?.visibility = View.VISIBLE
                progressLoadingDialogMbr?.bindingMbr?.progressBar?.progress = it
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

        // 라디오 버튼 선택 다이얼로그 출력 플래그
        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.observe(this) {
            if (it != null) {
                radioButtonChooseDialogMbr?.dismiss()

                radioButtonChooseDialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                radioButtonChooseDialogMbr?.show()
            } else {
                radioButtonChooseDialogMbr?.dismiss()
                radioButtonChooseDialogMbr = null
            }
        }
    }
}