package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_class.HandlerThreadObj
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil

// todo 실제 카메라처럼 기능 개편
class ActivityBasicCamera2ApiSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicCamera2ApiSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ActivityBasicCamera2ApiSampleViewModel

    // (다이얼로그 객체)
    // 로딩 다이얼로그
    var progressLoadingDialogMbr: DialogProgressLoading? = null

    // 선택 다이얼로그
    var binaryChooseDialogMbr: DialogBinaryChoose? = null

    // 확인 다이얼로그
    var confirmDialogMbr: DialogConfirm? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (ResultLauncher 객체)
    // 액티비티 이동 복귀 객체
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // (데이터)
    // 카메라 실행 객체
    lateinit var cameraObjMbr: CameraObj

    // Camera2 api 핸들러 스레드
    val cameraHandlerThreadMbr = HandlerThreadObj("camera").apply {
        this.startHandlerThread()
    }

    // 이미지 리더 핸들러 스레드
    val imageReaderHandlerThreadMbr = HandlerThreadObj("camera_image_reader").apply {
        this.startHandlerThread()
    }

    // 랜더 스크립트
    private lateinit var renderScriptMbr: RenderScript

    // intrinsic yuv to rgb
    private lateinit var scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB

    // image rotate
    private lateinit var scriptCRotatorMbr: ScriptC_rotator


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (뷰 객체 바인딩)
        bindingMbr = ActivityBasicCamera2ApiSampleBinding.inflate(layoutInflater)
        setContentView(bindingMbr.root)

        // (화면을 꺼지지 않도록 하는 플래그)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (초기 객체 생성)
        createMemberObjects()
        // 뷰모델 저장 객체 생성 = 뷰모델 내에 저장되어 destroy 까지 쭉 유지되는 데이터 초기화
        createViewModelDataObjects()

        // (초기 뷰 설정)
        viewSetting()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        setLiveData()

        // 액티비티 진입 필수 권한 요청
        requestActivityPermission()
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (viewModelMbr.isActivityPermissionClearMbr) {
            onCameraPermissionChecked(false)
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
        if (cameraObjMbr.isRecordingMbr) {
            // 레코딩 중이라면 기존 레코딩을 제거
            // todo
        } else {
            cameraObjMbr.pauseCameraSession(executorOnCameraPause = {})
        }

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

        // 카메라 종료
        cameraObjMbr.clearCameraObject(executorOnCameraClear = {})

        // 카메라 스레드 해소
        cameraHandlerThreadMbr.stopHandlerThread()
        imageReaderHandlerThreadMbr.stopHandlerThread()

        // 랜더 스크립트 객체 해소
        scriptIntrinsicYuvToRGBMbr.destroy()
        scriptCRotatorMbr.destroy()
        renderScriptMbr.finish()
        renderScriptMbr.destroy()

        super.onDestroy()
    }

    override fun onBackPressed() {
        imageProcessingPauseMbr = true
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

                imageProcessingPauseMbr = false
            },
            onCanceled = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                imageProcessingPauseMbr = false
            }
        )
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // 액티비티 초기 진입 필요 권한 확인
    private fun requestActivityPermission() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "카메라 장치가 없습니다.",
                "카메라 장치가 발견되지 않습니다.\n화면을 종료합니다.",
                null,
                onCheckBtnClicked = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                },
                onCanceled = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                }
            )
            return
        }

        permissionRequestCallbackMbr = { permissions ->
            // 카메라 권한
            val isGranted = permissions[Manifest.permission.CAMERA]!!
            val neverAskAgain =
                !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

            if (isGranted) { // 권한 승인
                viewModelMbr.isActivityPermissionClearMbr = true
                onCameraPermissionChecked(true)
            } else { // 권한 거부
                if (!neverAskAgain) {
                    // 단순 거부
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                        DialogConfirm.DialogInfoVO(
                            true,
                            "권한 요청",
                            "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                            null,
                            onCheckBtnClicked = {
                                viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                finish()
                            },
                            onCanceled = {
                                viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                                finish()
                            }
                        )
                } else {
                    // 다시 묻지 않기 선택
                    viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                        DialogBinaryChoose.DialogInfoVO(
                            true,
                            "권한 요청",
                            "해당 서비스를 이용하기 위해선\n" +
                                    "카메라 장치 사용 권한이 필요합니다.\n" +
                                    "권한 설정 화면으로 이동하시겠습니까?",
                            null,
                            null,
                            onPosBtnClicked = {
                                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                // 권한 설정 화면으로 이동
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", packageName, null)

                                resultLauncherCallbackMbr = {
                                    // 설정 페이지 복귀시 콜백

                                    // 권한을 승인했는지 확인
                                    if (ActivityCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) { // 권한이 승인 상태
                                        // 카메라 실행
                                        viewModelMbr.isActivityPermissionClearMbr = true
                                        onCameraPermissionChecked(true)
                                    } else { // 권한 비승인 상태
                                        viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                                null,
                                                onCheckBtnClicked = {
                                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                        null
                                                    finish()
                                                },
                                                onCanceled = {
                                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                        null
                                                    finish()
                                                }
                                            )
                                    }
                                }
                                resultLauncherMbr.launch(intent)
                            },
                            onNegBtnClicked = {
                                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                    DialogConfirm.DialogInfoVO(
                                        true,
                                        "권한 요청",
                                        "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                        null,
                                        onCheckBtnClicked = {
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                null
                                            finish()
                                        },
                                        onCanceled = {
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                null
                                            finish()
                                        }
                                    )
                            },
                            onCanceled = {
                                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                                viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                    DialogConfirm.DialogInfoVO(
                                        true,
                                        "권한 요청",
                                        "해당 서비스를 이용하기 위해선\n카메라 장치 사용 권한이 필요합니다.",
                                        null,
                                        onCheckBtnClicked = {
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                null
                                            finish()
                                        },
                                        onCanceled = {
                                            viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                                null
                                            finish()
                                        }
                                    )
                            }
                        )
                }
            }
        }

        permissionRequestMbr.launch(
            arrayOf(
                Manifest.permission.CAMERA
            )
        )
    }

    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityBasicCamera2ApiSampleViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionRequestCallbackMbr?.let { it1 -> it1(it) }
            permissionRequestCallbackMbr = null
        }

        // 액티비티 이동 복귀 객체 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
            resultLauncherCallbackMbr = null
        }

        // (랜더 스크립트 객체 생성)
        renderScriptMbr = RenderScript.create(application)
        scriptIntrinsicYuvToRGBMbr = ScriptIntrinsicYuvToRGB.create(
            renderScriptMbr,
            Element.U8_4(renderScriptMbr)
        )
        scriptCRotatorMbr = ScriptC_rotator(renderScriptMbr)

        // (사용 카메라 객체 생성)
        val cameraId =
            CameraObj.getCameraIdFromFacing(this, CameraCharacteristics.LENS_FACING_BACK)
        if (null == cameraId) {
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "카메라 에러",
                "카메라를 열 수 없습니다.\n액티비티를 종료합니다.",
                null,
                onCheckBtnClicked = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                },
                onCanceled = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                }
            )
        }

        val cameraObj = CameraObj.getInstance(
            this,
            cameraId!!,
            cameraHandlerThreadMbr.handler!!,
            onCameraDisconnected = {}
        )

        if (cameraObj == null) {
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "카메라 에러",
                "카메라를 열 수 없습니다.\n액티비티를 종료합니다.",
                null,
                onCheckBtnClicked = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                },
                onCanceled = {
                    viewModelMbr.confirmDialogInfoLiveDataMbr.value = null
                    finish()
                }
            )
        }

        cameraObjMbr = cameraObj!!

        // 핀치 줌 설정
        cameraObjMbr.setCameraPinchZoomTouchListener(bindingMbr.cameraPreviewAutoFitTexture)
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
        // 화면 방향에 따른 뷰 마진 설정
        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display!!.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }

        if (deviceOrientation == Surface.ROTATION_0) {
            bindingMbr.recordBtn.y =
                bindingMbr.recordBtn.y - CustomUtil.getNavigationBarHeightPixel(this)
        } else if (deviceOrientation == Surface.ROTATION_90) {
            bindingMbr.recordBtn.x =
                bindingMbr.recordBtn.x - CustomUtil.getNavigationBarHeightPixel(this)
        }

        // 지원하는 미디어 레코더 사이즈가 없다면 녹화 버튼을 없애기
        if (null == cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr) {
            bindingMbr.recordBtn.visibility = View.GONE
        } else {
            bindingMbr.recordBtn.visibility = View.VISIBLE


            // todo
//            val mediaRecorderConfigVo =
//                if (null != cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr) {
//                    val chosenSurfaceSize =
//                        CameraObj.getNearestSupportedCameraOutputSize(
//                            this,
//                            cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr!!,
//                            cameraObjMbr.sensorOrientationMbr,
//                            Long.MAX_VALUE,
//                            2.0 / 3.0
//                        )
//                    CameraObj.MediaRecorderConfigVo(
//                        chosenSurfaceSize,
//                        File("${this.filesDir.absolutePath}/temp.mp4"),
//                        null,
//                        null,
//                        false
//                    )
//                } else {
//                    null
//                }
            // recording pause 시에는 녹화를 멈추고 기존 파일을 제거하도록 처리
            // todo 중복 클릭 방지, 녹화중 화면 효과
            // 방해 금지 모드로 회전 및 pause 가 불가능하도록 처리
//            bindingMbr.recordBtn.setOnClickListener {
//                if (!(cameraObjMbr.isRecordingMbr)) {
//                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
//
//                    cameraObjMbr.setCameraRequestBuilder(
//                        onPreview = true,
//                        onImageReader = true,
//                        onMediaRecorder = true,
//                        CameraDevice.TEMPLATE_RECORD,
//                        onCameraRequestSettingTime = {
//                            // Auto WhiteBalance, Auto Focus, Auto Exposure
//                            it.set(
//                                CaptureRequest.CONTROL_MODE,
//                                CameraMetadata.CONTROL_MODE_AUTO
//                            )
//
//                            // 손떨림 방지
//                            cameraObjMbr.setStabilizationRequest(it)
//                        },
//                        onCameraRequestBuilderSet = {
//                            cameraObjMbr.startMediaRecording()
//                        },
//                        onError = {
//
//                        }
//                    )
//                } else {
//                    // 화면 고정 풀기
//                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//
//                    // 기존 세션 종료
//                    cameraObjMbr.stopMediaRecording()
//
//                    val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File
//
//                    // 결과물 감상
//                    val mediaPlayerIntent = Intent()
//                    mediaPlayerIntent.action = Intent.ACTION_VIEW
//                    mediaPlayerIntent.setDataAndType(
//                        FileProvider.getUriForFile(
//                            this@ActivityBasicCamera2ApiSample,
//                            "${BuildConfig.APPLICATION_ID}.provider",
//                            videoFile
//                        ), MimeTypeMap.getSingleton()
//                            .getMimeTypeFromExtension(videoFile.extension)
//                    )
//                    mediaPlayerIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                            Intent.FLAG_ACTIVITY_CLEAR_TOP
//                    startActivity(mediaPlayerIntent)
//                }
//            }
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

    private fun onCameraPermissionChecked(isOnCreate: Boolean) {
        if (isOnCreate) { // 처음 카메라 설정 시점
            // (카메라 실행)
            val previewConfigVo = if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                // todo : 90도 회전시 프리뷰가 잘림 현상

                // 지원 사이즈 탐지
                val chosenPreviewSurfaceSize = CameraObj.getNearestSupportedCameraOutputSize(
                    this,
                    cameraObjMbr.previewSurfaceSupportedSizeListMbr!!,
                    cameraObjMbr.sensorOrientationMbr,
                    Long.MAX_VALUE,
                    2.0 / 3.0
                )

                arrayListOf(
                    CameraObj.PreviewConfigVo(
                        chosenPreviewSurfaceSize,
                        bindingMbr.cameraPreviewAutoFitTexture
                    )
                )
            } else {
                null
            }

            val imageReaderConfigVo = if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                val chosenImageReaderSurfaceSize =
                    CameraObj.getNearestSupportedCameraOutputSize(
                        this,
                        cameraObjMbr.imageReaderSurfaceSupportedSizeListMbr!!,
                        cameraObjMbr.sensorOrientationMbr,
                        500 * 500,
                        2.0 / 3.0
                    )
                CameraObj.ImageReaderConfigVo(
                    chosenImageReaderSurfaceSize,
                    imageReaderHandlerThreadMbr.handler!!,
                    imageReaderCallback = { reader ->
                        processImage(reader)
                    }
                )
            } else {
                null
            }

            cameraObjMbr.setCameraOutputSurfaces(
                previewConfigVo,
                imageReaderConfigVo,
                null,
                executorOnSurfaceAllReady = {
                    cameraObjMbr.setCameraRequestBuilder(
                        onPreview = true,
                        onImageReader = true,
                        onMediaRecorder = false,
                        CameraDevice.TEMPLATE_PREVIEW,
                        executorOnCameraRequestSettingTime = {
                            // Auto WhiteBalance, Auto Focus, Auto Exposure
                            it.set(
                                CaptureRequest.CONTROL_MODE,
                                CameraMetadata.CONTROL_MODE_AUTO
                            )

                            // 손떨림 방지
                            cameraObjMbr.setStabilizationRequest(it)
                        },
                        executorOnCameraRequestBuilderSet = {
                            cameraObjMbr.runCameraRequest(
                                true,
                                null,
                                executorOnRequestComplete = {},
                                executorOnError = {})
                        },
                        executorOnError = {

                        }
                    )
                },
                executorOnError = {

                }
            )
        } else { // onPause 에서 카메라가 pause 된 시점
            cameraObjMbr.runCameraRequest(
                true,
                null,
                executorOnRequestComplete = {},
                executorOnError = {})
        }
    }

    // (카메라 이미지 실시간 처리 콜백)
    private var yuvByteArrayToArgbBitmapAsyncOnProgressMbr = false
    private var imageProcessingPauseMbr = false
    private fun processImage(reader: ImageReader) {
        try {
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // yuv ByteArray 를 rgb Bitmap 으로 변환 (병렬처리)
            // 반환되는 비트맵 이미지는 카메라 센서 방향에 따라 정방향이 아닐 수 있음.

            // 병렬처리 플래그
            if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
                imageProcessingPauseMbr || // imageProcessing 정지 신호
                isDestroyed || // 액티비티 자체가 종료
                yuvByteArrayToArgbBitmapAsyncOnProgressMbr// 작업중
            ) {
                // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
                imageObj.close()
                return
            }

            // (이미지 데이터 복사 = yuv to rgb bitmap 변환 로직 병렬처리를 위한 데이터 백업)
            // 최대한 빨리 imageObj 를 닫기 위하여(= 프레임을 다음으로 넘기기 위하여) imageObj 정보를 ByteArray 로 복사하여 사용
            val imgWidth: Int = imageObj.width
            val imgHeight: Int = imageObj.height

            val yuvByteArray = RenderScriptUtil.getYuv420888ImageByteArray(imageObj)

            // 이미지 데이터가 복사되어 image 객체 해제
            imageObj.close()

            if (yuvByteArray == null) {
                return
            }

            yuvByteArrayToArgbBitmapAsyncOnProgressMbr = true

            // (YUV420 Image to ARGB8888 Bitmap)
            // RenderScript 사용
            val bitmap =
                RenderScriptUtil.yuv420888ToARgb8888BitmapIntrinsic(
                    renderScriptMbr,
                    scriptIntrinsicYuvToRGBMbr,
                    imgWidth,
                    imgHeight,
                    yuvByteArray
                )

            yuvByteArrayToArgbBitmapAsyncOnProgressMbr = false

            runOnUiThread {
                if (!isDestroyed) {
                    Glide.with(this)
                        .load(bitmap)
                        .transform(FitCenter())
                        .into(bindingMbr.testImg)
                }
            }
        } catch (e: Exception) {
            // 발생 가능한 에러 :
            // 1. 이미지 객체를 사용하려 할 때, 액티비티가 종료되어 이미지 객체가 종료된 상황
            // 2. Camera2 api 내부 에러 등
            e.printStackTrace()
            finish()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}