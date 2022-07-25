package com.example.prowd_android_template.activity_set.activity_bitmap_and_opencv_jni_sample

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
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
        // 뷰 객체
        bindingMbr = ActivityBitmapAndOpencvJniSampleBinding.inflate(layoutInflater)

        // 뷰 모델 객체 생성
        viewModelMbr =
            ViewModelProvider(this)[ActivityBitmapAndOpencvJniSampleViewModel::class.java]

    }

    // 초기 뷰 설정
    private fun viewSetting() {
        // (RGB 수치 파악)
        // redBitmap.config == ARGB_8888
        val redBitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawRGB(255, 0, 0)
        }

        Glide.with(this)
            .load(redBitmap)
            .into(bindingMbr.redSrcImg)

        val redRgb = NativeWrapperOpenCvTest.getBitmapRGB(redBitmap)

        val redRgbTxt = "R : ${redRgb[0]}\nG : ${redRgb[1]}\nB : ${redRgb[2]}"
        bindingMbr.redRgbTxt.text = redRgbTxt

        val greenBitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawRGB(0, 255, 0)
        }

        Glide.with(this)
            .load(greenBitmap)
            .into(bindingMbr.greenSrcImg)

        val greenRgb = NativeWrapperOpenCvTest.getBitmapRGB(greenBitmap)

        val greenRgbTxt = "R : ${greenRgb[0]}\nG : ${greenRgb[1]}\nB : ${greenRgb[2]}"
        bindingMbr.greenRgbTxt.text = greenRgbTxt

        val blueBitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawRGB(0, 0, 255)
        }

        Glide.with(this)
            .load(blueBitmap)
            .into(bindingMbr.blueSrcImg)

        val blueRgb = NativeWrapperOpenCvTest.getBitmapRGB(blueBitmap)
        val blueRgbTxt = "R : ${blueRgb[0]}\nG : ${blueRgb[1]}\nB : ${blueRgb[2]}"
        bindingMbr.blueRgbTxt.text = blueRgbTxt

        // (RGB 이미지 클릭 리스너 설정)
        bindingMbr.redSrcImg.setOnClickListener {
            val redBitmap1 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(255, 0, 0)
            }
            val copyBitmap1 = redBitmap1.copy(redBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(redBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val redBitmap2 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(255, 0, 0)
            }
            val copyBitmap2 = redBitmap2.copy(redBitmap2.config, true)
            NativeWrapperOpenCvTest.getCopyBitmap(redBitmap2, copyBitmap2)

            Glide.with(this)
                .load(copyBitmap2)
                .into(bindingMbr.copyImg)
        }

        bindingMbr.greenSrcImg.setOnClickListener {
            val greenBitmap1 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(0, 255, 0)
            }
            val copyBitmap1 = greenBitmap1.copy(greenBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(greenBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val greenBitmap2 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(0, 255, 0)
            }
            val copyBitmap2 = greenBitmap2.copy(greenBitmap2.config, true)
            NativeWrapperOpenCvTest.getCopyBitmap(greenBitmap2, copyBitmap2)

            Glide.with(this)
                .load(copyBitmap2)
                .into(bindingMbr.copyImg)
        }

        bindingMbr.blueSrcImg.setOnClickListener {
            val blueBitmap1 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(0, 0, 255)
            }
            val copyBitmap1 = blueBitmap1.copy(blueBitmap1.config, true)
            NativeWrapperOpenCvTest.getGrayBitmap(blueBitmap1, copyBitmap1)

            Glide.with(this)
                .load(copyBitmap1)
                .into(bindingMbr.grayImg)

            val blueBitmap2 = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawRGB(0, 0, 255)
            }
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

    private fun onCreateLogic() {

    }
}