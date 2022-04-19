package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_object.RenderScriptUtil


// todo : 카메라는 screen 회전을 막아둠 (= 카메라 정지를 막기 위하여.) 보다 세련된 방식을 찾기
class ActivityBasicCamera2ApiSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    private lateinit var bindingMbr: ActivityBasicCamera2ApiSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicCamera2ApiSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    private var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    private var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    private var confirmDialogMbr: DialogConfirm? = null

    // 카메라 실행 객체
    private var backCameraObjMbr: CameraObj? = null


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicCamera2ApiSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (화면을 꺼지지 않도록 하는 플래그)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (상태창 투명화)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

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

        // 카메라 실행
        isImageProcessingPause = false
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) { // 권한 승인 상태
            // (카메라 실행)
            // 카메라 생성
            backCameraObjMbr?.openCamera(
                onCameraDeviceReady = {
                    // (서페이스 생성)
                    // 이미지 리더 생성
                    backCameraObjMbr?.createImageReader(
                        CameraObj.ImageReaderConfigVo(
                            Long.MAX_VALUE,
                            1f,
                            ImageFormat.YUV_420_888,
                            2,
                            imageReaderCallback = { reader ->
                                processImage(reader)
                            }
                        )
                    )

                    // 프리뷰 생성
                    backCameraObjMbr?.createPreviewInfoAsync(
                        arrayListOf(bindingMbr.cameraPreviewAutoFitTexture),
                        onPreviewTextureReady = { // 프리뷰 서페이스 준비 완료
                            // 카메라 세션 리퀘스트 빌더 생성
                            backCameraObjMbr?.createCameraCaptureRequest()

                            // 카메라 세션 생성
                            backCameraObjMbr?.createCameraSession(
                                onCaptureSessionCreated = { // 카메라 세션 생성 완료
                                    // 카메라 세션 실행
                                    backCameraObjMbr?.runCameraCaptureSession()

                                },
                                onError = {
                                    // todo 세션 생성 실패

                                }
                            )
                        }
                    )
                },
                onError = {
                    // todo 디바이스 생성 실패

                }
            )
        } else { // 권한 비승인 상태
            // todo : 권한 필요 다이얼로그 보여주고 뒤로가기 (원래 권한이 없으면 진입이 불가하지만 보험용)
        }

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

    override fun onPause() {
        isImageProcessingPause = true
        backCameraObjMbr?.stopCameraCaptureSession()
        backCameraObjMbr?.deleteCameraSession()
        backCameraObjMbr?.deleteCameraCaptureRequest()
        backCameraObjMbr?.deletePreviewInfoAsync()
        backCameraObjMbr?.deleteImageReader()
        backCameraObjMbr?.closeCamera()

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
        progressLoadingDialogMbr?.dismiss()
        binaryChooseDialogMbr?.dismiss()
        progressLoadingDialogMbr?.dismiss()

        super.onDestroy()
    }

    override fun onBackPressed() {
        isImageProcessingPause = true
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = DialogBinaryChoose.DialogInfoVO(
            true,
            "카메라 종료",
            "카메라를 종료하시겠습니까?",
            "종료",
            "취소",
            onPosBtnClicked = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                finish()
            },
            onNegBtnClicked = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                isImageProcessingPause = false
            },
            onCanceled = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                isImageProcessingPause = false
            }
        )
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityBasicCamera2ApiSampleViewModel::class.java]

        // 사용 카메라 객체 생성
        // 후방 카메라
        val backCameraId = CameraObj.chooseCameraId(this, CameraCharacteristics.LENS_FACING_BACK)
        if (null != backCameraId) {
            backCameraObjMbr =
                CameraObj.getInstance(this, backCameraId)
        }

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
        bindingMbr.cameraCloseBtn.setOnClickListener {
            isImageProcessingPause = true
            viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = DialogBinaryChoose.DialogInfoVO(
                true,
                "카메라 종료",
                "카메라를 종료하시겠습니까?",
                "종료",
                "취소",
                onPosBtnClicked = {
                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                    finish()
                },
                onNegBtnClicked = {
                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                    isImageProcessingPause = false
                },
                onCanceled = {
                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                    isImageProcessingPause = false
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
    }

    // (카메라 이미지 실시간 처리 콜백)
    private var isImageProcessingPause = false
    private fun processImage(reader: ImageReader?) {
        try {
            if (null == reader) {
                return
            }

            // (YUV to RGB)
            // todo : gpu support 가 필요한 작업 목록 =
            // todo : 1. yuv to rgb  2. rotate image  3. crop image  4. resize image  5. etc...

            // todo : yuv to rgb 와 camera sensor orientation 에 따른 rotate 를 한번에 처리하기
            val imageObj: Image = reader.acquireLatestImage() ?: return
            yuvImageToArgbBitmap(imageObj, onConvertingComplete = { yuvImageToArgbBitmap ->
                // 이미지 회전
//                bitmap =
//                    RenderScriptUtil.rotateBitmap(
//                        viewModelMbr.renderScript,
//                        viewModelMbr.scriptCRotator,
//                        bitmap,
//                        360 - backCameraObjMbr!!.sensorOrientationMbr
//                    )

                runOnUiThread {
                    if (!isDestroyed) {
                        Glide.with(this)
                            .load(yuvImageToArgbBitmap)
                            .transform(FitCenter())
                            .into(bindingMbr.testImg)
                    }
                }

                // todo

            })
        } catch (e: Exception) {
            // 발생 가능한 에러 :
            // 1. 이미지 객체를 사용하려 할 때, 액티비티가 종료되어 이미지 객체가 종료된 상황
            // 2. Camera2 api 내부 에러 등
            e.printStackTrace()
            finish()
        }
    }

    // (yuv 카메라 raw 데이터를 rgb bitmap 객체로 변환)
    // 콜백 반환값 : 변환 완료 Bitmap
    // 주의 : 받은 Image 객체는 return 전에 꼭 close() 를 해줄 것
    private var yuvImageToArgbBitmapOnProgressMbr = false
    private fun yuvImageToArgbBitmap(
        image: Image,
        onConvertingComplete: (Bitmap) -> Unit
    ) {
        // 병렬처리를 위한 플래그
        // 아래 작업도중이라면 바로 return
        if (yuvImageToArgbBitmapOnProgressMbr || // 작업중
            isImageProcessingPause || // 이미지 프로세싱 중지 상태
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            image.close()
            return
        }

        val logicStartTime = SystemClock.elapsedRealtime()

        // (이미지 데이터 복사 = yuv to rgb bitmap 변환 로직 병렬처리를 위한 데이터 백업)
        // 최대한 빨리 imageObj 를 닫기 위하여(= 프레임을 다음으로 넘기기 위하여) imageObj 정보를 ByteArray 로 복사하여 사용
        val imgWidth: Int = image.width
        val imgHeight: Int = image.height

        val yuvByteArray = RenderScriptUtil.yuv420888ImageToByteArray(image)

        // 이미지 데이터가 복사되어 image 객체 해제
        image.close()

        // 시간이 드는 작업은 미리 만들어두고 이를 다른 스레드에서 사용하도록 함.
        // 더블 버퍼 구조와 비슷하게 구성해도 됨
        yuvImageToArgbBitmapOnProgressMbr = true
        viewModelMbr.executorServiceMbr?.execute {
            // (YUV420 Image to ARGB8888 Bitmap)

            // RenderScript 사용
            val bitmap =
                RenderScriptUtil.yuv420888ToRgbBitmapIntrinsic(
                    viewModelMbr.renderScript,
                    viewModelMbr.scriptIntrinsicYuvToRGB,
                    imgWidth,
                    imgHeight,
                    yuvByteArray
                )

            runOnUiThread {
                bindingMbr.yuvToRgbMs.text =
                    (SystemClock.elapsedRealtime() - logicStartTime).toString()
            }

            yuvImageToArgbBitmapOnProgressMbr = false

            onConvertingComplete(bitmap)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}