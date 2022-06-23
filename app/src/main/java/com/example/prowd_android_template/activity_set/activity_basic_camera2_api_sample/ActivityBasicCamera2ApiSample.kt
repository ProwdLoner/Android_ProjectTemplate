package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicResize
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.BuildConfig
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil
import com.xxx.yyy.ScriptC_crop
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

// todo : 레코딩 버튼 누른 후 빠르게 회전시 멈춤 현상
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
    private lateinit var cameraObjMbr: CameraObj

    // 랜더 스크립트
    private lateinit var renderScriptMbr: RenderScript

    // intrinsic yuv to rgb
    private lateinit var scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB

    // image rotate
    private lateinit var scriptCRotatorMbr: ScriptC_rotator

    // image crop
    private lateinit var scriptCCropMbr: ScriptC_crop

    // image resize
    private lateinit var scriptIntrinsicResizeMbr: ScriptIntrinsicResize


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
        if (cameraObjMbr.isRecordingMbr) { // 레코딩 중이라면 기존 레코딩 세션을 제거 후 프리뷰 세션으로 전환
            // 기존 저장 폴더 백업
            val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File

            // 카메라를 서페이스 까지 초기화
            cameraObjMbr.stopCameraObject(
                executorOnCameraStop = {
                    // 기존 저장 폴더 삭제
                    videoFile.delete()

                    // 미디어 레코드를 제외한 카메라 세션 준비
                    val previewConfigVo =
                        if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                            // 지원 사이즈 탐지
                            val chosenPreviewSurfaceSize =
                                CustomUtil.getNearestSupportedCameraOutputSize(
                                    cameraObjMbr.previewSurfaceSupportedSizeListMbr!!,
                                    Long.MAX_VALUE,
                                    3.0 / 2.0
                                )

                            // 설정 객체 반환
                            arrayListOf(
                                CameraObj.PreviewConfigVo(
                                    chosenPreviewSurfaceSize,
                                    bindingMbr.cameraPreviewAutoFitTexture
                                )
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    val imageReaderConfigVo =
                        if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                            // 지원 사이즈 탐지
                            val chosenImageReaderSurfaceSize =
                                CustomUtil.getNearestSupportedCameraOutputSize(
                                    cameraObjMbr.imageReaderSurfaceSupportedSizeListMbr!!,
                                    500 * 500,
                                    3.0 / 2.0
                                )

                            // 설정 객체 반환
                            CameraObj.ImageReaderConfigVo(
                                chosenImageReaderSurfaceSize,
                                imageReaderCallback = { reader ->
                                    processImage(reader)
                                }
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    // 카메라 서페이스 설정
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        imageReaderConfigVo,
                        null,
                        executorOnSurfaceAllReady = {
                            // 떨림 보정
                            cameraObjMbr.setCameraStabilization(
                                true,
                                executorOnCameraStabilizationSettingComplete = {})

                            // 카메라 리퀘스트 설정
                            cameraObjMbr.setCameraRequest(
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
                                },
                                executorOnCameraRequestBuilderSet = {

                                },
                                executorOnError = {

                                }
                            )
                        },
                        executorOnError = {

                        }
                    )
                }
            )
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

        // 카메라를 디바이스 객체까지 전부 초기화
        cameraObjMbr.clearCameraObject(executorOnCameraClear = {})

        // 랜더 스크립트 객체 해소
        scriptCCropMbr.destroy()
        scriptIntrinsicResizeMbr.destroy()
        scriptIntrinsicYuvToRGBMbr.destroy()
        scriptCRotatorMbr.destroy()
        renderScriptMbr.finish()
        renderScriptMbr.destroy()

        super.onDestroy()
    }

    override fun onBackPressed() {
        viewModelMbr.imageProcessingPauseMbr = true
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

                viewModelMbr.imageProcessingPauseMbr = false
            },
            onCanceled = {
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value = null

                viewModelMbr.imageProcessingPauseMbr = false
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

        scriptCCropMbr = ScriptC_crop(renderScriptMbr)

        scriptIntrinsicResizeMbr = ScriptIntrinsicResize.create(
            renderScriptMbr
        )

        // (최초 사용 카메라 객체 생성)
        val cameraId =
            CameraObj.getCameraIdFromFacing(this, CameraCharacteristics.LENS_FACING_BACK)!!

        val cameraObj = CameraObj.getInstance(
            this,
            cameraId,
            onCameraDisconnectedAndClearCamera = {
                Log.e("disconnect", "camera disconnect")
            }
        )!!

        cameraObjMbr = cameraObj

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
        // (디버그 이미지 뷰 전환 기능)
        bindingMbr.debugRotateImg.setOnClickListener {
            bindingMbr.debugRotateImg.visibility = View.GONE
            bindingMbr.debugResizeImg.visibility = View.VISIBLE
            bindingMbr.debugCropImg.visibility = View.GONE

            bindingMbr.debugImageLabel.text = "RESIZE (half)"
        }
        bindingMbr.debugResizeImg.setOnClickListener {
            bindingMbr.debugRotateImg.visibility = View.GONE
            bindingMbr.debugResizeImg.visibility = View.GONE
            bindingMbr.debugCropImg.visibility = View.VISIBLE

            bindingMbr.debugImageLabel.text = "CROP"
        }
        bindingMbr.debugCropImg.setOnClickListener {
            bindingMbr.debugRotateImg.visibility = View.VISIBLE
            bindingMbr.debugResizeImg.visibility = View.GONE
            bindingMbr.debugCropImg.visibility = View.GONE

            bindingMbr.debugImageLabel.text = "ORIGIN"
        }

        // 화면 방향에 따른 뷰 마진 설정
        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display!!.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }

        if (deviceOrientation == Surface.ROTATION_0) {
            bindingMbr.recordBtn.y =
                bindingMbr.recordBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
        } else if (deviceOrientation == Surface.ROTATION_90) {
            bindingMbr.recordBtn.x =
                bindingMbr.recordBtn.x - CustomUtil.getSoftNavigationBarHeightPixel(this)
        }

        // 지원하는 미디어 레코더 사이즈가 없다면 녹화 버튼을 없애기
        if (null == cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr) {
            bindingMbr.recordBtn.visibility = View.GONE
        } else {
            bindingMbr.recordBtn.visibility = View.VISIBLE
        }

        // todo 녹화중 화면 효과
        // todo 방해 금지 모드로 회전 및 pause 가 불가능하도록 처리
        bindingMbr.recordBtn.setOnClickListener {
            // 처리 완료까지 중복 클릭 방지
            bindingMbr.recordBtn.isEnabled = false

            if (!(cameraObjMbr.isRecordingMbr)) { // 레코딩 중이 아닐 때
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

                cameraObjMbr.stopCameraObject {
                    val previewConfigVo =
                        if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                            // 지원 사이즈 탐지
                            val chosenPreviewSurfaceSize =
                                CustomUtil.getNearestSupportedCameraOutputSize(
                                    cameraObjMbr.previewSurfaceSupportedSizeListMbr!!,
                                    Long.MAX_VALUE,
                                    3.0 / 2.0
                                )

                            // 설정 객체 반환
                            arrayListOf(
                                CameraObj.PreviewConfigVo(
                                    chosenPreviewSurfaceSize,
                                    bindingMbr.cameraPreviewAutoFitTexture
                                )
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    val imageReaderConfigVo =
                        if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                            // 지원 사이즈 탐지
                            val chosenImageReaderSurfaceSize =
                                CustomUtil.getNearestSupportedCameraOutputSize(
                                    cameraObjMbr.imageReaderSurfaceSupportedSizeListMbr!!,
                                    500 * 500,
                                    3.0 / 2.0
                                )

                            // 설정 객체 반환
                            CameraObj.ImageReaderConfigVo(
                                chosenImageReaderSurfaceSize,
                                imageReaderCallback = { reader ->
                                    processImage(reader)
                                }
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    val mediaRecorderConfigVo =
                        if (null != cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr) {
                            // 지원 사이즈 탐지
                            val chosenSurfaceSize =
                                CustomUtil.getNearestSupportedCameraOutputSize(
                                    cameraObjMbr.mediaRecorderSurfaceSupportedSizeListMbr!!,
                                    Long.MAX_VALUE,
                                    3.0 / 2.0
                                )

                            // 설정 객체 반환
                            CameraObj.MediaRecorderConfigVo(
                                chosenSurfaceSize,
                                File("${this.filesDir.absolutePath}/${System.currentTimeMillis()}.mp4"),
                                null,
                                null,
                                false
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    // 카메라 서페이스 설정
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        imageReaderConfigVo,
                        mediaRecorderConfigVo,
                        executorOnSurfaceAllReady = {
                            // 떨림 보정
                            cameraObjMbr.setCameraStabilization(
                                true,
                                executorOnCameraStabilizationSettingComplete = {})

                            // 카메라 리퀘스트 설정
                            cameraObjMbr.setCameraRequest(
                                onPreview = true,
                                onImageReader = true,
                                onMediaRecorder = true,
                                CameraDevice.TEMPLATE_RECORD,
                                executorOnCameraRequestSettingTime = {
                                    // Auto WhiteBalance, Auto Focus, Auto Exposure
                                    it.set(
                                        CaptureRequest.CONTROL_MODE,
                                        CameraMetadata.CONTROL_MODE_AUTO
                                    )
                                },
                                executorOnCameraRequestBuilderSet = {
                                    // 카메라 실행
                                    cameraObjMbr.runCameraRequest(
                                        true,
                                        null,
                                        executorOnRequestComplete = {
                                            cameraObjMbr.startMediaRecording(onComplete = {

                                                runOnUiThread {
                                                    bindingMbr.recordBtn.isEnabled = true
                                                }
                                            })
                                        },
                                        executorOnError = {
                                            runOnUiThread {
                                                bindingMbr.recordBtn.isEnabled = true
                                            }
                                        })
                                },
                                executorOnError = {
                                    runOnUiThread {
                                        bindingMbr.recordBtn.isEnabled = true
                                    }
                                }
                            )
                        },
                        executorOnError = {
                            runOnUiThread {
                                bindingMbr.recordBtn.isEnabled = true
                            }
                        }
                    )
                }

            } else { // 레코딩 중일때
                // 화면 고정 풀기
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                // 기존 저장 폴더 백업
                val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File

                // 카메라 초기화
                cameraObjMbr.stopCameraObject(
                    executorOnCameraStop = {
                        // 미디어 레코드를 제외한 카메라 세션 준비
                        val previewConfigVo =
                            if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                                // 지원 사이즈 탐지
                                val chosenPreviewSurfaceSize =
                                    CustomUtil.getNearestSupportedCameraOutputSize(
                                        cameraObjMbr.previewSurfaceSupportedSizeListMbr!!,
                                        Long.MAX_VALUE,
                                        3.0 / 2.0
                                    )

                                // 설정 객체 반환
                                arrayListOf(
                                    CameraObj.PreviewConfigVo(
                                        chosenPreviewSurfaceSize,
                                        bindingMbr.cameraPreviewAutoFitTexture
                                    )
                                )
                            } else {
                                // 지원 사이즈가 없기에 null 반환
                                null
                            }

                        val imageReaderConfigVo =
                            if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                                // 지원 사이즈 탐지
                                val chosenImageReaderSurfaceSize =
                                    CustomUtil.getNearestSupportedCameraOutputSize(
                                        cameraObjMbr.imageReaderSurfaceSupportedSizeListMbr!!,
                                        500 * 500,
                                        3.0 / 2.0
                                    )

                                // 설정 객체 반환
                                CameraObj.ImageReaderConfigVo(
                                    chosenImageReaderSurfaceSize,
                                    imageReaderCallback = { reader ->
                                        processImage(reader)
                                    }
                                )
                            } else {
                                // 지원 사이즈가 없기에 null 반환
                                null
                            }

                        // 카메라 서페이스 설정
                        cameraObjMbr.setCameraOutputSurfaces(
                            previewConfigVo,
                            imageReaderConfigVo,
                            null,
                            executorOnSurfaceAllReady = {
                                // 떨림 보정
                                cameraObjMbr.setCameraStabilization(
                                    true,
                                    executorOnCameraStabilizationSettingComplete = {})

                                // 카메라 리퀘스트 설정
                                cameraObjMbr.setCameraRequest(
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
                                    },
                                    executorOnCameraRequestBuilderSet = {
                                        runOnUiThread {
                                            bindingMbr.recordBtn.isEnabled = true

                                            // 결과물 감상
                                            val mediaPlayerIntent = Intent()
                                            mediaPlayerIntent.action = Intent.ACTION_VIEW
                                            mediaPlayerIntent.setDataAndType(
                                                FileProvider.getUriForFile(
                                                    this@ActivityBasicCamera2ApiSample,
                                                    "${BuildConfig.APPLICATION_ID}.provider",
                                                    videoFile
                                                ), MimeTypeMap.getSingleton()
                                                    .getMimeTypeFromExtension(videoFile.extension)
                                            )
                                            mediaPlayerIntent.flags =
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                                            resultLauncherCallbackMbr = {
                                                videoFile.delete()
                                            }
                                            resultLauncherMbr.launch(mediaPlayerIntent)
                                        }
                                    },
                                    executorOnError = {
                                        runOnUiThread {
                                            bindingMbr.recordBtn.isEnabled = true
                                        }
                                    }
                                )
                            },
                            executorOnError = {
                                runOnUiThread {
                                    bindingMbr.recordBtn.isEnabled = true
                                }
                            }
                        )
                    }
                )
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
    }

    private fun onCameraPermissionChecked(isOnCreate: Boolean) {
        if (isOnCreate) { // 처음 카메라 설정 시점
            // (카메라 실행)
            val previewConfigVo = if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                // 지원 사이즈 탐지
                val chosenPreviewSurfaceSize = CustomUtil.getNearestSupportedCameraOutputSize(
                    cameraObjMbr.previewSurfaceSupportedSizeListMbr!!,
                    Long.MAX_VALUE,
                    3.0 / 2.0
                )

                // 설정 객체 반환
                arrayListOf(
                    CameraObj.PreviewConfigVo(
                        chosenPreviewSurfaceSize,
                        bindingMbr.cameraPreviewAutoFitTexture
                    )
                )
            } else {
                // 지원 사이즈가 없기에 null 반환
                null
            }

            val imageReaderConfigVo = if (null != cameraObjMbr.previewSurfaceSupportedSizeListMbr) {
                // 지원 사이즈 탐지
                val chosenImageReaderSurfaceSize =
                    CustomUtil.getNearestSupportedCameraOutputSize(
                        cameraObjMbr.imageReaderSurfaceSupportedSizeListMbr!!,
                        500 * 500,
                        3.0 / 2.0
                    )

                // 설정 객체 반환
                CameraObj.ImageReaderConfigVo(
                    chosenImageReaderSurfaceSize,
                    imageReaderCallback = { reader ->
                        processImage(reader)
                    }
                )
            } else {
                // 지원 사이즈가 없기에 null 반환
                null
            }

            // 카메라 서페이스 설정
            cameraObjMbr.setCameraOutputSurfaces(
                previewConfigVo,
                imageReaderConfigVo,
                null,
                executorOnSurfaceAllReady = {
                    // 떨림 보정
                    cameraObjMbr.setCameraStabilization(
                        true,
                        executorOnCameraStabilizationSettingComplete = {})

                    // 카메라 리퀘스트 설정
                    cameraObjMbr.setCameraRequest(
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
                        },
                        executorOnCameraRequestBuilderSet = {
                            // 카메라 실행
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
    // 카메라에서 이미지 프레임을 받아올 때마다 이것이 실행됨
    private fun processImage(reader: ImageReader) {
        // (0. Image 객체 요청)
        val imageObj: Image = reader.acquireLatestImage() ?: return

        val imageTimeStamp: Long = SystemClock.elapsedRealtime()

        if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            imageObj.close()
            return
        }

        // 안정화를 위하여 Image 객체의 필요 데이터를 clone
        val imageSize = Size(imageObj.width, imageObj.height)
        val pixelCount = imageSize.width * imageSize.height

        // image planes 를 순회하면 yuvByteArray 채우기
        val plane0: Image.Plane = imageObj.planes[0]
        val plane1: Image.Plane = imageObj.planes[1]
        val plane2: Image.Plane = imageObj.planes[2]

        val rowStride0: Int = plane0.rowStride
        val pixelStride0: Int = plane0.pixelStride

        val rowStride1: Int = plane1.rowStride
        val pixelStride1: Int = plane1.pixelStride

        val rowStride2: Int = plane2.rowStride
        val pixelStride2: Int = plane2.pixelStride

        if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            imageObj.close()
            return
        }
        val planeBuffer0: ByteBuffer = CustomUtil.cloneByteBuffer(plane0.buffer)

        if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            imageObj.close()
            return
        }
        val planeBuffer1: ByteBuffer = CustomUtil.cloneByteBuffer(plane1.buffer)

        if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            imageObj.close()
            return
        }
        val planeBuffer2: ByteBuffer = CustomUtil.cloneByteBuffer(plane2.buffer)

        imageObj.close()

        // 여기까지, camera2 api 이미지 리더에서 발행하는 image 객체를 처리하는 사이클이 완성
        // 아래부터 멀티 스레드를 사용 가능

        // (1. 이미지 객체에서 추출한 바이트 버퍼를 랜더 스크립트 주입용 바이트 어레이로 변환)
        yuv420888ByteBufferToYuv420888ByteArray(
            imageSize,
            imageTimeStamp,
            pixelCount,
            planeBuffer0,
            rowStride0,
            pixelStride0,
            planeBuffer1,
            rowStride1,
            pixelStride1,
            planeBuffer2,
            rowStride2,
            pixelStride2,
            executorOnComplete = { imageSize1, imageTimeStamp1, yuvByteArray1 ->

                // (2. yuv 420 888 바이트 어레이를 argb 8888 비트맵으로 변환)
                yuv420888ByteArrayToArgb8888Bitmap(
                    imageSize1,
                    imageTimeStamp1,
                    yuvByteArray1,
                    executorOnComplete = { imageTimeStamp2, bitmap2 ->

                        // (3. 이미지를 회전)
                        // 현 디바이스 방향으로 이미지를 맞추기 위해 역시계 방향으로 몇도를 돌려야 하는지
                        val rotateCounterClockAngle: Int =
                            when (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                display!!.rotation
                            } else {
                                windowManager.defaultDisplay.rotation
                            }) {
                                Surface.ROTATION_0 -> { // 카메라 기본 방향
                                    // if sensorOrientationMbr = 90 -> 270
                                    360 - cameraObjMbr.sensorOrientationMbr
                                }
                                Surface.ROTATION_90 -> { // 카메라 기본 방향에서 역시계 방향 90도 회전 상태
                                    // if sensorOrientationMbr = 90 -> 0
                                    90 - cameraObjMbr.sensorOrientationMbr
                                }
                                Surface.ROTATION_180 -> {
                                    // if sensorOrientationMbr = 90 -> 90
                                    180 - cameraObjMbr.sensorOrientationMbr
                                }
                                Surface.ROTATION_270 -> {
                                    // if sensorOrientationMbr = 90 -> 180
                                    270 - cameraObjMbr.sensorOrientationMbr
                                }
                                else -> {
                                    0
                                }
                            }

                        rotateBitmap(
                            rotateCounterClockAngle,
                            imageTimeStamp2,
                            bitmap2,
                            executorOnComplete = { imageTimeStamp3, bitmap3 ->

                                // 디버그를 위한 표시
                                runOnUiThread {
                                    if (!isDestroyed) {
                                        Glide.with(this)
                                            .load(bitmap3)
                                            .transform(FitCenter())
                                            .into(bindingMbr.debugRotateImg)
                                    }
                                }

                                // (4. 리사이징 테스트)
                                // 리사이징 사이즈
                                val resizeSize = Size(bitmap3.width, bitmap3.height)

                                resizeBitmap(
                                    resizeSize,
                                    imageTimeStamp3,
                                    bitmap3,
                                    executorOnComplete = { imageTimeStamp4, resizeBitmap4 ->

                                        // 디버그를 위한 표시
                                        runOnUiThread {
                                            if (!isDestroyed) {
                                                Glide.with(this)
                                                    .load(resizeBitmap4)
                                                    .transform(FitCenter())
                                                    .into(bindingMbr.debugResizeImg)
                                            }
                                        }

                                    }
                                )

                                // (4. Crop 테스트)
                                cropBitmap(
                                    RectF(0.3f, 0.3f, 0.7f, 0.7f),
                                    imageTimeStamp3,
                                    bitmap3,
                                    executorOnComplete = { imageTimeStamp4, bitmap4 ->

                                        // 디버그를 위한 표시
                                        runOnUiThread {
                                            if (!isDestroyed) {
                                                Glide.with(this)
                                                    .load(bitmap4)
                                                    .transform(FitCenter())
                                                    .into(bindingMbr.debugCropImg)
                                            }
                                        }

                                    })
                            })
                    })
            }
        )
    }

    // (바이트 버퍼를 바이트 어레이로 변환하는 함수)
    // executorOnComplete 파라미터 :
    // imageSize, imageTimeStamp, yuvByteArray
    private var byteBufferToByteArrayOnProgressMbr = false
    private val byteBufferToByteArrayOnProgressSemaphoreMbr: Semaphore = Semaphore(1)
    private fun yuv420888ByteBufferToYuv420888ByteArray(
        imageSize: Size,
        imageTimeStamp: Long,
        pixelCount: Int,
        originPlaneBuffer0: ByteBuffer,
        rowStride0: Int,
        pixelStride0: Int,
        originPlaneBuffer1: ByteBuffer,
        rowStride1: Int,
        pixelStride1: Int,
        originPlaneBuffer2: ByteBuffer,
        rowStride2: Int,
        pixelStride2: Int,
        executorOnComplete: (Size, Long, ByteArray) -> Unit
    ) {
        byteBufferToByteArrayOnProgressSemaphoreMbr.acquire()
        if (byteBufferToByteArrayOnProgressMbr ||
            !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            byteBufferToByteArrayOnProgressSemaphoreMbr.release()
            return
        }

        byteBufferToByteArrayOnProgressMbr = true
        byteBufferToByteArrayOnProgressSemaphoreMbr.release()

        // 레퍼런스 데이터 복제 (레퍼런스 변수는 함수 파라미터에 딥 카피가 되지 않으므로 복제)
        val planeBuffer0 = CustomUtil.cloneByteBuffer(originPlaneBuffer0)
        val planeBuffer1 = CustomUtil.cloneByteBuffer(originPlaneBuffer1)
        val planeBuffer2 = CustomUtil.cloneByteBuffer(originPlaneBuffer2)

        viewModelMbr.executorServiceMbr?.execute {
            // image to byteArray
            val yuvByteArray =
                ByteArray(pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)

            val rowBuffer0 = ByteArray(rowStride0)
            val outputStride0 = 1

            var outputOffset0 = 0

            val imageCrop0 = Rect(0, 0, imageSize.width, imageSize.height)

            val planeWidth0 = imageCrop0.width()

            val rowLength0 = if (pixelStride0 == 1 && outputStride0 == 1) {
                planeWidth0
            } else {
                (planeWidth0 - 1) * pixelStride0 + 1
            }

            for (row in 0 until imageCrop0.height()) {
                planeBuffer0.position(
                    (row + imageCrop0.top) * rowStride0 + imageCrop0.left * pixelStride0
                )

                if (pixelStride0 == 1 && outputStride0 == 1) {
                    planeBuffer0.get(yuvByteArray, outputOffset0, rowLength0)
                    outputOffset0 += rowLength0
                } else {
                    planeBuffer0.get(rowBuffer0, 0, rowLength0)
                    for (col in 0 until planeWidth0) {
                        yuvByteArray[outputOffset0] = rowBuffer0[col * pixelStride0]
                        outputOffset0 += outputStride0
                    }
                }
            }

            val rowBuffer1 = ByteArray(rowStride1)
            val outputStride1 = 2

            var outputOffset1: Int = pixelCount + 1

            val imageCrop1 = Rect(0, 0, imageSize.width, imageSize.height)

            val planeCrop1 = Rect(
                imageCrop1.left / 2,
                imageCrop1.top / 2,
                imageCrop1.right / 2,
                imageCrop1.bottom / 2
            )

            val planeWidth1 = planeCrop1.width()

            val rowLength1 = if (pixelStride1 == 1 && outputStride1 == 1) {
                planeWidth1
            } else {
                (planeWidth1 - 1) * pixelStride1 + 1
            }

            for (row in 0 until planeCrop1.height()) {
                planeBuffer1.position(
                    (row + planeCrop1.top) * rowStride1 + planeCrop1.left * pixelStride1
                )

                if (pixelStride1 == 1 && outputStride1 == 1) {
                    planeBuffer1.get(yuvByteArray, outputOffset1, rowLength1)
                    outputOffset1 += rowLength1
                } else {
                    planeBuffer1.get(rowBuffer1, 0, rowLength1)
                    for (col in 0 until planeWidth1) {
                        yuvByteArray[outputOffset1] = rowBuffer1[col * pixelStride1]
                        outputOffset1 += outputStride1
                    }
                }
            }

            val rowBuffer2 = ByteArray(rowStride2)
            val outputStride2 = 2

            var outputOffset2: Int = pixelCount

            val imageCrop2 = Rect(0, 0, imageSize.width, imageSize.height)

            val planeCrop2 = Rect(
                imageCrop2.left / 2,
                imageCrop2.top / 2,
                imageCrop2.right / 2,
                imageCrop2.bottom / 2
            )

            val planeWidth2 = planeCrop2.width()

            val rowLength2 = if (pixelStride2 == 1 && outputStride2 == 1) {
                planeWidth2
            } else {
                (planeWidth2 - 1) * pixelStride2 + 1
            }

            for (row in 0 until planeCrop2.height()) {
                planeBuffer2.position(
                    (row + planeCrop2.top) * rowStride2 + planeCrop2.left * pixelStride2
                )

                if (pixelStride2 == 1 && outputStride2 == 1) {
                    planeBuffer2.get(yuvByteArray, outputOffset2, rowLength2)
                    outputOffset2 += rowLength2
                } else {
                    planeBuffer2.get(rowBuffer2, 0, rowLength2)
                    for (col in 0 until planeWidth2) {
                        yuvByteArray[outputOffset2] = rowBuffer2[col * pixelStride2]
                        outputOffset2 += outputStride2
                    }
                }
            }

            byteBufferToByteArrayOnProgressSemaphoreMbr.acquire()
            byteBufferToByteArrayOnProgressMbr = false
            byteBufferToByteArrayOnProgressSemaphoreMbr.release()
            executorOnComplete(
                Size(imageSize.width, imageSize.height),
                imageTimeStamp,
                yuvByteArray
            )
        }
    }


    // (YUV420888 ByteArray 를 ARGB8888 비트맵으로 변환하는 함수)
    // RenderScript 사용
    // 카메라 방향에 따라 이미지가 정방향이 아닐 수 있음
    // executorOnComplete 파라미터 :
    // imageTimeStamp, argbBitmap
    private var yuv420888ByteArrayToArgb8888BitmapOnProgressMbr = false
    private val yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr: Semaphore = Semaphore(1)
    private fun yuv420888ByteArrayToArgb8888Bitmap(
        imageSize: Size,
        imageTimeStamp: Long,
        originYuv420888ByteArray: ByteArray,
        executorOnComplete: (Long, Bitmap) -> Unit
    ) {
        yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr.acquire()
        if (yuv420888ByteArrayToArgb8888BitmapOnProgressMbr ||
            !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr.release()
            return
        }

        yuv420888ByteArrayToArgb8888BitmapOnProgressMbr = true
        yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr.release()

        // 레퍼런스 데이터 복제 (레퍼런스 변수는 함수 파라미터에 딥 카피가 되지 않으므로 복제)
        val yuv420888ByteArray = originYuv420888ByteArray.clone()

        viewModelMbr.executorServiceMbr?.execute {
            val cameraImageFrameBitmap =
                RenderScriptUtil.yuv420888ToARgb8888BitmapIntrinsic(
                    renderScriptMbr,
                    scriptIntrinsicYuvToRGBMbr,
                    imageSize.width,
                    imageSize.height,
                    yuv420888ByteArray
                )

            yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr.acquire()
            yuv420888ByteArrayToArgb8888BitmapOnProgressMbr = false
            yuv420888ByteArrayToArgb8888BitmapOnProgressSemaphoreMbr.release()
            executorOnComplete(imageTimeStamp, cameraImageFrameBitmap)
        }
    }

    // (비트맵을 디바이스 방향으로 맞추는 함수)
    // RenderScript 사용
    // executorOnComplete 파라미터 :
    // imageTimeStamp, argbBitmap
    private var rotateBitmapOnProgressMbr = false
    private val rotateBitmapOnProgressSemaphoreMbr: Semaphore = Semaphore(1)
    private fun rotateBitmap(
        rotateCounterClockAngle: Int,
        imageTimeStamp: Long,
        originBitmap: Bitmap,
        executorOnComplete: (Long, Bitmap) -> Unit
    ) {
        rotateBitmapOnProgressSemaphoreMbr.acquire()
        if (rotateBitmapOnProgressMbr ||
            !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            rotateBitmapOnProgressSemaphoreMbr.release()
            return
        }

        rotateBitmapOnProgressMbr = true
        rotateBitmapOnProgressSemaphoreMbr.release()

        // 레퍼런스 데이터 복제 (레퍼런스 변수는 함수 파라미터에 딥 카피가 되지 않으므로 복제)
        val bitmap = originBitmap.copy(originBitmap.config, true)

        viewModelMbr.executorServiceMbr?.execute {
            val rotatedBitmap =
                RenderScriptUtil.rotateBitmapCounterClock(
                    renderScriptMbr,
                    scriptCRotatorMbr,
                    bitmap,
                    rotateCounterClockAngle
                )

            rotateBitmapOnProgressSemaphoreMbr.acquire()
            rotateBitmapOnProgressMbr = false
            rotateBitmapOnProgressSemaphoreMbr.release()
            executorOnComplete(
                imageTimeStamp,
                rotatedBitmap
            )
        }
    }

    // (비트맵 리사이징 함수)
    // RenderScript 사용
    // executorOnComplete 파라미터 :
    // imageTimeStamp, resizeBitmap
    private var resizeBitmapOnProgressMbr = false
    private val resizeBitmapOnProgressSemaphoreMbr: Semaphore = Semaphore(1)
    private fun resizeBitmap(
        resizeImageSize: Size,
        imageTimeStamp: Long,
        originBitmap: Bitmap,
        executorOnComplete: (Long, Bitmap) -> Unit
    ) {
        resizeBitmapOnProgressSemaphoreMbr.acquire()
        if (resizeBitmapOnProgressMbr ||
            !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            resizeBitmapOnProgressSemaphoreMbr.release()
            return
        }

        resizeBitmapOnProgressMbr = true
        resizeBitmapOnProgressSemaphoreMbr.release()

        // 레퍼런스 데이터 복제 (레퍼런스 변수는 함수 파라미터에 딥 카피가 되지 않으므로 복제)
        val bitmap = originBitmap.copy(originBitmap.config, true)

        viewModelMbr.executorServiceMbr?.execute {
            val resizeBitmap = RenderScriptUtil.resizeBitmapIntrinsic(
                renderScriptMbr,
                scriptIntrinsicResizeMbr,
                bitmap,
                resizeImageSize.width,
                resizeImageSize.height
            )

            resizeBitmapOnProgressSemaphoreMbr.acquire()
            resizeBitmapOnProgressMbr = false
            resizeBitmapOnProgressSemaphoreMbr.release()
            executorOnComplete(
                imageTimeStamp,
                resizeBitmap
            )
        }
    }

    // (비트맵 크롭 함수)
    // RenderScript 사용
    // cropUnitRectF : 이미지 width, height 를 1로 표준화 했을 때, 어느 위치를 crop 할지에 대한 비율값
    // executorOnComplete 파라미터 :
    // imageTimeStamp, cropBitmap
    private var cropBitmapOnProgressMbr = false
    private val cropBitmapOnProgressSemaphoreMbr: Semaphore = Semaphore(1)
    private fun cropBitmap(
        cropUnitRectF: RectF,
        imageTimeStamp: Long,
        originBitmap: Bitmap,
        executorOnComplete: (Long, Bitmap) -> Unit
    ) {
        cropBitmapOnProgressSemaphoreMbr.acquire()
        if (cropBitmapOnProgressMbr ||
            !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
            viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
            isDestroyed // 액티비티 자체가 종료
        ) {
            cropBitmapOnProgressSemaphoreMbr.release()
            return
        }

        cropBitmapOnProgressMbr = true
        cropBitmapOnProgressSemaphoreMbr.release()

        // 레퍼런스 데이터 복제 (레퍼런스 변수는 함수 파라미터에 딥 카피가 되지 않으므로 복제)
        val bitmap = originBitmap.copy(originBitmap.config, true)

        viewModelMbr.executorServiceMbr?.execute {
            val resultBitmap = RenderScriptUtil.cropBitmap(
                renderScriptMbr,
                scriptCCropMbr,
                bitmap,
                Rect(
                    (cropUnitRectF.left * originBitmap.width).toInt(),
                    (cropUnitRectF.top * originBitmap.height).toInt(),
                    (cropUnitRectF.right * originBitmap.width).toInt(),
                    (cropUnitRectF.bottom * originBitmap.height).toInt()
                )
            )

            cropBitmapOnProgressSemaphoreMbr.acquire()
            cropBitmapOnProgressMbr = false
            cropBitmapOnProgressSemaphoreMbr.release()
            executorOnComplete(imageTimeStamp, resultBitmap)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}