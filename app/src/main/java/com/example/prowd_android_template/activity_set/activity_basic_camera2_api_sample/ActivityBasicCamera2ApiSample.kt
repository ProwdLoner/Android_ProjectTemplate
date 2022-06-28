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
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil
import com.xxx.yyy.ScriptC_crop
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList

// todo : 레코딩 버튼 누른 후 빠르게 회전시 멈춤 현상
// todo : 180 도 회전시 프리뷰 거꾸로 나오는 문제(restart 가 되지 않고 있음)
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

    // 라디오 버튼 다이얼로그
    var radioBtnDialogMbr: DialogRadioButtonChoose? = null

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

            // (카메라를 서페이스 까지 초기화)
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

                    // (카메라 변수 설정)
                    // 떨림 보정
                    cameraObjMbr.setCameraStabilization(
                        true,
                        executorOnCameraStabilizationSettingComplete = {})

                    // (카메라 서페이스 설정)
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        imageReaderConfigVo,
                        null,
                        executorOnSurfaceAllReady = {
                            // (카메라 리퀘스트 설정)
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
                                    // todo : 비동기라서 start 를 안할시 실행 안될 위험이 있음
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
        radioBtnDialogMbr?.dismiss()

        // 카메라 해소
        cameraObjMbr.destroyCameraObject(executorOnCameraClear = {})

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
        val cameraIdList = CameraObj.getCameraIdList(this)

        if (cameraIdList.size == 0) {
            // 지원하는 카메라가 없음
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "에러",
                "지원되는 카메라를 찾을 수 없습니다.",
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

        // 초기 카메라 아이디 선정
        val cameraId: String =
            if (viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId != null) { // 기존 아이디가 있을 때
                if (cameraIdList.contains(viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId)) {
                    // 기존 아이디가 제공 아이디 리스트에 있을 때
                    viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId!!
                } else {
                    // 기존 아이디가 제공 아이디 리스트에 없을 때

                    // 후면 카메라 적용 검토
                    if (CameraObj.getCameraIdFromFacing(
                            this,
                            CameraCharacteristics.LENS_FACING_BACK
                        ) != null &&
                        cameraIdList.contains(
                            CameraObj.getCameraIdFromFacing(
                                this,
                                CameraCharacteristics.LENS_FACING_BACK
                            )!!
                        )
                    ) {
                        CameraObj.getCameraIdFromFacing(
                            this,
                            CameraCharacteristics.LENS_FACING_BACK
                        )!!
                    } else { // 후면 카메라 아이디 지원 불가
                        // 전면 카메라 적용 검토
                        if (CameraObj.getCameraIdFromFacing(
                                this,
                                CameraCharacteristics.LENS_FACING_FRONT
                            ) != null &&
                            cameraIdList.contains(
                                CameraObj.getCameraIdFromFacing(
                                    this,
                                    CameraCharacteristics.LENS_FACING_FRONT
                                )!!
                            )
                        ) {
                            CameraObj.getCameraIdFromFacing(
                                this,
                                CameraCharacteristics.LENS_FACING_FRONT
                            )!!
                        } else {
                            // 전후면 카메라 지원 불가시 카메라 id 리스트 첫번째 아이디 적용
                            cameraIdList[0]
                        }
                    }
                }
            } else { // 기존 아이디가 없을 때
                // 후면 카메라 적용 검토
                if (CameraObj.getCameraIdFromFacing(
                        this,
                        CameraCharacteristics.LENS_FACING_BACK
                    ) != null &&
                    cameraIdList.contains(
                        CameraObj.getCameraIdFromFacing(
                            this,
                            CameraCharacteristics.LENS_FACING_BACK
                        )!!
                    )
                ) {
                    CameraObj.getCameraIdFromFacing(
                        this,
                        CameraCharacteristics.LENS_FACING_BACK
                    )!!
                } else { // 후면 카메라 아이디 지원 불가
                    // 전면 카메라 적용 검토
                    if (CameraObj.getCameraIdFromFacing(
                            this,
                            CameraCharacteristics.LENS_FACING_FRONT
                        ) != null &&
                        cameraIdList.contains(
                            CameraObj.getCameraIdFromFacing(
                                this,
                                CameraCharacteristics.LENS_FACING_FRONT
                            )!!
                        )
                    ) {
                        CameraObj.getCameraIdFromFacing(
                            this,
                            CameraCharacteristics.LENS_FACING_FRONT
                        )!!
                    } else {
                        // 전후면 카메라 지원 불가시 카메라 id 리스트 첫번째 아이디 적용
                        cameraIdList[0]
                    }
                }
            }

        val cameraObj = CameraObj.getInstance(
            this,
            cameraId,
            onCameraDisconnectedAndClearCamera = {

            }
        )!!

        cameraObjMbr = cameraObj

        // 핀치 줌 설정
        cameraObjMbr.setCameraPinchZoomTouchListener(bindingMbr.cameraPreviewAutoFitTexture)

        viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId = cameraId
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
        bindingMbr.debugYuvToRgbImg.setOnClickListener {
            bindingMbr.debugYuvToRgbImg.visibility = View.GONE
            bindingMbr.debugRotateImg.visibility = View.VISIBLE
            bindingMbr.debugResizeImg.visibility = View.GONE
            bindingMbr.debugCropImg.visibility = View.GONE

            bindingMbr.debugImageLabel.text = "ROTATE"
        }
        bindingMbr.debugRotateImg.setOnClickListener {
            bindingMbr.debugYuvToRgbImg.visibility = View.GONE
            bindingMbr.debugRotateImg.visibility = View.GONE
            bindingMbr.debugResizeImg.visibility = View.VISIBLE
            bindingMbr.debugCropImg.visibility = View.GONE

            bindingMbr.debugImageLabel.text = "RESIZE (half)"
        }
        bindingMbr.debugResizeImg.setOnClickListener {
            bindingMbr.debugYuvToRgbImg.visibility = View.GONE
            bindingMbr.debugRotateImg.visibility = View.GONE
            bindingMbr.debugResizeImg.visibility = View.GONE
            bindingMbr.debugCropImg.visibility = View.VISIBLE

            bindingMbr.debugImageLabel.text = "CROP"
        }
        bindingMbr.debugCropImg.setOnClickListener {
            bindingMbr.debugYuvToRgbImg.visibility = View.VISIBLE
            bindingMbr.debugRotateImg.visibility = View.GONE
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
            bindingMbr.btn1.y =
                bindingMbr.btn1.y - CustomUtil.getSoftNavigationBarHeightPixel(this)

            bindingMbr.btn2.y =
                bindingMbr.btn2.y - CustomUtil.getSoftNavigationBarHeightPixel(this)

            bindingMbr.btn3.y =
                bindingMbr.btn3.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
        }

        // 카메라 전환
        bindingMbr.cameraChangeBtn.setOnClickListener {
            val cameraIdList = CameraObj.getCameraIdList(this)
            val cameraItemList = ArrayList<String>()

            for (id in cameraIdList) {
                cameraItemList.add("Camera $id")
            }

            var checkedIdx = cameraIdList.indexOfFirst {
                it == viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId
            }

            if (checkedIdx == -1) {
                checkedIdx = 0
            }

            viewModelMbr.imageProcessingPauseMbr = true
            viewModelMbr.radioButtonDialogInfoLiveDataMbr.value =
                DialogRadioButtonChoose.DialogInfoVO(
                    isCancelable = true,
                    title = "카메라 선택",
                    contentMsg = null,
                    radioButtonContentList = cameraItemList,
                    checkedItemIdx = checkedIdx,
                    cancelBtnTxt = null,
                    onRadioItemClicked = {
                        viewModelMbr.radioButtonDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false

                        val checkedCameraId = cameraIdList[it]

                        if (checkedCameraId != viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId) {
                            // 기존 저장 폴더 백업
                            val videoFile = if (cameraObjMbr.isRecordingMbr) {
                                cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File
                            } else {
                                null
                            }

                            cameraObjMbr.destroyCameraObject {
                                videoFile?.delete()

                                val cameraObj = CameraObj.getInstance(
                                    this,
                                    checkedCameraId,
                                    onCameraDisconnectedAndClearCamera = {

                                    }
                                )!!

                                cameraObjMbr = cameraObj

                                // 핀치 줌 설정
                                cameraObjMbr.setCameraPinchZoomTouchListener(bindingMbr.cameraPreviewAutoFitTexture)

                                viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId =
                                    checkedCameraId

                                // (카메라 실행)
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

                                // (카메라 변수 설정)
                                // todo : 세팅 함수는 그대로 두되, setCameraRequest 에서 한번에 설정하도록
                                // 떨림 보정
                                cameraObjMbr.setCameraStabilization(
                                    true,
                                    executorOnCameraStabilizationSettingComplete = {})

                                // (카메라 서페이스 설정)
                                cameraObjMbr.setCameraOutputSurfaces(
                                    previewConfigVo,
                                    imageReaderConfigVo,
                                    null,
                                    executorOnSurfaceAllReady = {
                                        // (카메라 리퀘스트 설정)
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
                                                // (카메라 실행)
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
                            }
                        }
                    },
                    onCancelBtnClicked = {
                        viewModelMbr.radioButtonDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false
                    },
                    onCanceled = {
                        viewModelMbr.radioButtonDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false
                    }
                )
        }

        // todo 녹화중 화면 효과
        // todo 방해 금지 모드로 회전 및 pause 가 불가능하도록 처리
        bindingMbr.btn1.setOnClickListener {
            // 처리 완료까지 중복 클릭 방지
            bindingMbr.btn1.isEnabled = false

            if (!(cameraObjMbr.isRecordingMbr)) { // 현재 레코딩 중이 아닐 때
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
                                null,
                                true
                            )
                        } else {
                            // 지원 사이즈가 없기에 null 반환
                            null
                        }

                    // (카메라 변수 설정)
                    // 떨림 보정
                    cameraObjMbr.setCameraStabilization(
                        true,
                        executorOnCameraStabilizationSettingComplete = {})

                    // (카메라 서페이스 설정)
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        imageReaderConfigVo,
                        mediaRecorderConfigVo,
                        executorOnSurfaceAllReady = {
                            // (카메라 리퀘스트 설정)
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
                                    // (카메라 실행)
                                    cameraObjMbr.runCameraRequest(
                                        true,
                                        null,
                                        executorOnRequestComplete = {
                                            // (미디어 레코딩 녹화 실행)
                                            cameraObjMbr.startMediaRecording(onComplete = {
                                                runOnUiThread {
                                                    bindingMbr.btn1.isEnabled = true
                                                }
                                            })
                                        },
                                        executorOnError = {
                                            runOnUiThread {
                                                bindingMbr.btn1.isEnabled = true
                                            }
                                        })
                                },
                                executorOnError = {
                                    runOnUiThread {
                                        bindingMbr.btn1.isEnabled = true
                                    }
                                }
                            )
                        },
                        executorOnError = {
                            runOnUiThread {
                                bindingMbr.btn1.isEnabled = true
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

                        // (카메라 변수 설정)
                        // 떨림 보정
                        cameraObjMbr.setCameraStabilization(
                            true,
                            executorOnCameraStabilizationSettingComplete = {})

                        // (카메라 서페이스 설정)
                        cameraObjMbr.setCameraOutputSurfaces(
                            previewConfigVo,
                            imageReaderConfigVo,
                            null,
                            executorOnSurfaceAllReady = {
                                // (카메라 리퀘스트 설정)
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
                                            bindingMbr.btn1.isEnabled = true

                                            // (결과물 감상)
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
                                            bindingMbr.btn1.isEnabled = true
                                        }
                                    }
                                )
                            },
                            executorOnError = {
                                runOnUiThread {
                                    bindingMbr.btn1.isEnabled = true
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

    private fun onCameraPermissionChecked(isOnCreate: Boolean) {
        Log.e("awb", cameraObjMbr.autoWhiteBalanceSupportedMbr.toString())
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

            // (카메라 변수 설정)
            // todo : 세팅 함수는 그대로 두되, setCameraRequest 에서 한번에 설정하도록
            // 떨림 보정
            cameraObjMbr.setCameraStabilization(
                true,
                executorOnCameraStabilizationSettingComplete = {})

            // (카메라 서페이스 설정)
            cameraObjMbr.setCameraOutputSurfaces(
                previewConfigVo,
                imageReaderConfigVo,
                null,
                executorOnSurfaceAllReady = {
                    // (카메라 리퀘스트 설정)
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
                            // (카메라 실행)
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
            // (카메라 실행)
            cameraObjMbr.runCameraRequest(
                true,
                null,
                executorOnRequestComplete = {},
                executorOnError = {})
        }
    }

    // 최대 이미지 프로세싱 개수 (현재 처리중인 이미지 프로세싱 개수가 이것을 넘어가면 그냥 return)
    private var asyncImageProcessingOnProgressMbr = false
    private val asyncImageProcessingOnProgressSemaphoreMbr = Semaphore(1)

    // (카메라 이미지 실시간 처리 콜백)
    // 카메라에서 이미지 프레임을 받아올 때마다 이것이 실행됨
    private fun processImage(reader: ImageReader) {
        try {
            // (1. Image 객체 정보 추출)
            // reader 객체로 받은 image 객체의 이미지 정보가 처리되어 close 될 때까지는 동기적으로 처리
            // image 객체가 빨리 close 되고 나머지는 비동기 처리를 하는게 좋음

            // 프레임 이미지 객체
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // 조기 종료 플래그
            if (!cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
                viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
                isDestroyed // 액티비티 자체가 종료
            ) {
                imageObj.close()
                return
            }

            // 안정화를 위하여 Image 객체의 필요 데이터를 clone
            // 이번 프레임 수집 시간 (time stamp nano sec -> milli sec)
            val imageGainTimeMs: Long = imageObj.timestamp / 1000 / 1000

            val imageWidth = imageObj.width
            val imageHeight = imageObj.height
            val pixelCount = imageWidth * imageHeight

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

            val planeBuffer0: ByteBuffer = CustomUtil.cloneByteBuffer(plane0.buffer)
            val planeBuffer1: ByteBuffer = CustomUtil.cloneByteBuffer(plane1.buffer)
            val planeBuffer2: ByteBuffer = CustomUtil.cloneByteBuffer(plane2.buffer)

            imageObj.close()

            // 여기까지, camera2 api 이미지 리더에서 발행하는 image 객체를 처리하는 사이클이 완성

            // (2. 비동기 이미지 프로세싱 시작)
            viewModelMbr.executorServiceMbr?.execute {

                // 조기 종료 확인
                asyncImageProcessingOnProgressSemaphoreMbr.acquire()
                if (asyncImageProcessingOnProgressMbr || // 현재 비동기 이미지 프로세싱 중일 때
                    !cameraObjMbr.isRepeatingMbr || // repeating 상태가 아닐 경우
                    viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
                    isDestroyed // 액티비티 자체가 종료
                ) {
                    asyncImageProcessingOnProgressSemaphoreMbr.release()
                    return@execute
                }
                asyncImageProcessingOnProgressMbr = true
                asyncImageProcessingOnProgressSemaphoreMbr.release()

                // (3. 이미지 객체에서 추출한 YUV 420 888 바이트 버퍼를 ARGB 8888 비트맵으로 변환)
                val cameraImageFrameBitmap = yuv420888ByteBufferToArgb8888Bitmap(
                    imageWidth,
                    imageHeight,
                    pixelCount,
                    rowStride0,
                    pixelStride0,
                    planeBuffer0,
                    rowStride1,
                    pixelStride1,
                    planeBuffer1,
                    rowStride2,
                    pixelStride2,
                    planeBuffer2
                )

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed) {
                        Glide.with(this)
                            .load(cameraImageFrameBitmap)
                            .transform(FitCenter())
                            .into(bindingMbr.debugYuvToRgbImg)
                    }
                }

                // (4. 이미지를 회전)
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

                val rotatedCameraImageFrameBitmap =
                    RenderScriptUtil.rotateBitmapCounterClock(
                        renderScriptMbr,
                        scriptCRotatorMbr,
                        cameraImageFrameBitmap,
                        rotateCounterClockAngle
                    )

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed) {
                        Glide.with(this)
                            .load(rotatedCameraImageFrameBitmap)
                            .transform(FitCenter())
                            .into(bindingMbr.debugRotateImg)
                    }
                }

                // (5. 리사이징 테스트)
                // 리사이징 사이즈
                val dstSize = Size(
                    rotatedCameraImageFrameBitmap.width / 2,
                    rotatedCameraImageFrameBitmap.height / 2
                )

                val resizedBitmap = RenderScriptUtil.resizeBitmapIntrinsic(
                    renderScriptMbr,
                    scriptIntrinsicResizeMbr,
                    rotatedCameraImageFrameBitmap,
                    dstSize.width,
                    dstSize.height
                )

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed) {
                        Glide.with(this)
                            .load(resizedBitmap)
                            .transform(FitCenter())
                            .into(bindingMbr.debugResizeImg)
                    }
                }

                // (6. Crop 테스트)
                // 좌표 width, height 을 1로 두었을 때, 어느 영역을 자를건지에 대한 비율
                val cropAreaRatioRectF = RectF(
                    0.3f,
                    0.3f,
                    0.7f,
                    0.7f
                )

                val croppedBitmap = RenderScriptUtil.cropBitmap(
                    renderScriptMbr,
                    scriptCCropMbr,
                    rotatedCameraImageFrameBitmap,
                    Rect(
                        (cropAreaRatioRectF.left * rotatedCameraImageFrameBitmap.width).toInt(),
                        (cropAreaRatioRectF.top * rotatedCameraImageFrameBitmap.height).toInt(),
                        (cropAreaRatioRectF.right * rotatedCameraImageFrameBitmap.width).toInt(),
                        (cropAreaRatioRectF.bottom * rotatedCameraImageFrameBitmap.height).toInt()
                    )
                )

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed) {
                        Glide.with(this)
                            .load(croppedBitmap)
                            .transform(FitCenter())
                            .into(bindingMbr.debugCropImg)
                    }
                }

                // 프로세스 한 사이클이 끝나면 반드시 count 를 내릴 것!
                asyncImageProcessingOnProgressSemaphoreMbr.acquire()
                asyncImageProcessingOnProgressMbr = false
                asyncImageProcessingOnProgressSemaphoreMbr.release()

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // (YUV 420 888 ByteBuffer 를 ARGB 8888 Bitmap 으로 변환하는 함수)
    private fun yuv420888ByteBufferToArgb8888Bitmap(
        imageWidth: Int,
        imageHeight: Int,
        pixelCount: Int,
        rowStride0: Int,
        pixelStride0: Int,
        planeBuffer0: ByteBuffer,
        rowStride1: Int,
        pixelStride1: Int,
        planeBuffer1: ByteBuffer,
        rowStride2: Int,
        pixelStride2: Int,
        planeBuffer2: ByteBuffer
    ): Bitmap {
        val yuvByteArray =
            ByteArray(pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)

        val rowBuffer0 = ByteArray(rowStride0)
        val outputStride0 = 1

        var outputOffset0 = 0

        val imageCrop0 = Rect(0, 0, imageWidth, imageHeight)

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

        val imageCrop1 = Rect(0, 0, imageWidth, imageHeight)

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

        val imageCrop2 = Rect(0, 0, imageWidth, imageHeight)

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

        return RenderScriptUtil.yuv420888ToARgb8888BitmapIntrinsic(
            renderScriptMbr,
            scriptIntrinsicYuvToRGBMbr,
            imageWidth,
            imageHeight,
            yuvByteArray
        )
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    data class ImageDataVo(
        val imageTimeMs: Long,
        val imageWidth: Int,
        val imageHeight: Int,
        val pixelCount: Int,
        val rowStride0: Int,
        val pixelStride0: Int,
        val planeBuffer0: ByteBuffer,
        val rowStride1: Int,
        val pixelStride1: Int,
        val planeBuffer1: ByteBuffer,
        val rowStride2: Int,
        val pixelStride2: Int,
        val planeBuffer2: ByteBuffer
    )
}