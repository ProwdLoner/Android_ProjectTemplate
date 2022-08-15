package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.BuildConfig
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil
import com.xxx.yyy.ScriptC_crop
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// todo : 180 도 회전시 프리뷰 거꾸로 나오는 문제(restart 가 되지 않고 있음)
class ActivityBasicCamera2ApiSample : AppCompatActivity() {
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicCamera2ApiSampleBinding

    // (뷰 모델 객체)
    lateinit var viewModelMbr: ViewModel

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // (데이터)
    // 카메라 실행 객체
    private lateinit var cameraObjMbr: CameraObj


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 액티비티 실행  = onCreate() → onStart() → onResume()
    //     액티비티 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     액티비티 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     액티비티 종료 = onPause() → onStop() → onDestroy()
    //     앨티비티 화면 회전 = onPause() → onSaveInstanceState() → onStop() → onDestroy() →
    //         onCreate(savedInstanceState) → onStart() → onResume()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (화면을 꺼지지 않도록 하는 플래그)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()

        // (라이브 데이터 설정 : 뷰모델 데이터 반영 작업)
        onCreateSetLiveData()
    }

    override fun onResume() {
        super.onResume()

        // (액티비티 진입 필수 권한 확인)
        // 진입 필수 권한이 클리어 되어야 로직이 실행
        permissionRequestCallbackMbr = { permissions ->
            var isPermissionAllGranted = true
            var neverAskAgain = false
            for (activityPermission in viewModelMbr.activityPermissionArrayMbr) {
                if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                    // 권한 클리어 플래그를 변경하고 break
                    isPermissionAllGranted = false
                    neverAskAgain = !shouldShowRequestPermissionRationale(activityPermission)
                    break
                }
            }

            if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                allPermissionsGranted()
            } else if (neverAskAgain) { // 다시 묻지 않기 선택
                viewModelMbr.binaryChooseDialogInfoLiveDataMbr.value =
                    DialogBinaryChoose.DialogInfoVO(
                        true,
                        "권한 요청",
                        "해당 서비스를 이용하기 위해선\n" +
                                "카메라 권한 승인이 필요합니다.\n" +
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
                                var isPermissionAllGranted1 = true
                                for (activityPermission in viewModelMbr.activityPermissionArrayMbr) {
                                    if (ActivityCompat.checkSelfPermission(
                                            this,
                                            activityPermission
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) { // 거부된 필수 권한이 존재
                                        // 권한 클리어 플래그를 변경하고 break
                                        isPermissionAllGranted1 = false
                                        break
                                    }
                                }

                                if (isPermissionAllGranted1) { // 권한 승인
                                    allPermissionsGranted()
                                } else { // 권한 거부
                                    viewModelMbr.confirmDialogInfoLiveDataMbr.value =
                                        DialogConfirm.DialogInfoVO(
                                            true,
                                            "권한 요청",
                                            "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
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
                                    "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
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
                                    "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
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

            } else { // 단순 거부
                viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                    true,
                    "권한 필요",
                    "서비스를 실행하기 위해 필요한 권한이 거부되었습니다.",
                    "뒤로가기",
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
        }

        // 연결된 카메라가 없을 때
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
        } else {
            permissionRequestMbr.launch(viewModelMbr.activityPermissionArrayMbr)
        }
    }

    override fun onPause() {
        if (cameraObjMbr.mediaRecorderStatusCodeMbr == 3 ||
            cameraObjMbr.mediaRecorderStatusCodeMbr == 4
        ) { // 레코딩 중이라면 기존 레코딩 세션을 제거 후 프리뷰 세션으로 전환
            // 기존 저장 폴더 백업
            val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File

            // (카메라를 서페이스 까지 초기화)
            cameraObjMbr.unsetCameraOutputSurfaces(
                onComplete = {
                    // 기존 저장 폴더 삭제
                    videoFile.delete()

                    // 미디어 레코드를 제외한 카메라 세션 준비
                    // 지원 사이즈 탐지
                    val chosenPreviewSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            3,
                            Long.MAX_VALUE,
                            3.0 / 2.0
                        )!!

                    val previewConfigVo =
                        // 설정 객체 반환
                        arrayListOf(
                            CameraObj.PreviewConfigVo(
                                chosenPreviewSurfaceSize,
                                bindingMbr.cameraPreviewAutoFitTexture
                            )
                        )

                    // 지원 사이즈 탐지
                    val chosenAnalysisImageReaderSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            2,
                            500 * 500,
                            3.0 / 2.0
                        )!!

                    val analysisImageReaderConfigVo =
                        // 설정 객체 반환
                        CameraObj.ImageReaderConfigVo(
                            chosenAnalysisImageReaderSurfaceSize,
                            imageReaderCallback = { reader ->
                                analyzeImage(reader)
                            }
                        )

                    val chosenCaptureImageReaderSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            2,
                            Long.MAX_VALUE,
                            3.0 / 2.0
                        )!!

                    val captureImageReaderConfigVo =
                        // 설정 객체 반환
                        CameraObj.ImageReaderConfigVo(
                            chosenCaptureImageReaderSurfaceSize,
                            imageReaderCallback = { reader ->
                                captureImage(reader)
                            }
                        )

                    // (카메라 변수 설정)
                    // 떨림 보정
                    cameraObjMbr.setCameraStabilization(
                        true,
                        onComplete = {},
                        onError = {})

                    // (카메라 서페이스 설정)
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        captureImageReaderConfigVo,
                        null,
                        analysisImageReaderConfigVo,
                        onComplete = {
                            // (카메라 리퀘스트 설정)
                            cameraObjMbr.repeatingRequestOnTemplate(
                                forPreview = true,
                                forMediaRecorder = false,
                                forAnalysisImageReader = true,
                                onComplete = {

                                },
                                onError = {

                                }
                            )
                        },
                        onError = {

                        }
                    )
                }
            )
        } else {
            cameraObjMbr.stopRepeatingRequest(onCameraPause = {}, onError = {})
        }

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 설정 변경(화면회전)을 했는지 여부를 반영
        viewModelMbr.isActivityRecreatedMbr = true

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        // 카메라 해소
        cameraObjMbr.destroyCameraObject(onComplete = {})

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
    // (초기 객체 생성)
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityBasicCamera2ApiSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ViewModel::class.java]

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
                permissionRequestCallbackMbr = null
            }

        // ActivityResultLauncher 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
            resultLauncherCallbackMbr = null
        }


        // (최초 사용 카메라 객체 생성)
        val cameraInfoList = CameraObj.getAllSupportedCameraInfoList(this)

        if (cameraInfoList.size == 0) {
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

                // 기존 아이디가 현재 지원 카메라 리스트에 있는지를 확인
                val lastCameraIdIdx = cameraInfoList.indexOfFirst {
                    it.cameraId == viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId
                }

                if (lastCameraIdIdx != -1) {
                    // 기존 아이디가 제공 아이디 리스트에 있을 때
                    viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId!!
                } else {
                    // 기존 아이디가 제공 아이디 리스트에 없을 때 = 후면 카메라를 먼저 적용
                    val backCameraIdx = cameraInfoList.indexOfFirst {
                        it.facing == CameraCharacteristics.LENS_FACING_BACK
                    }

                    // 후면 카메라 적용 검토
                    if (backCameraIdx != -1) {
                        cameraInfoList[backCameraIdx].cameraId
                    } else { // 후면 카메라 아이디 지원 불가
                        // 전면 카메라 적용 검토
                        val frontCameraIdx = cameraInfoList.indexOfFirst {
                            it.facing == CameraCharacteristics.LENS_FACING_FRONT
                        }

                        if (frontCameraIdx != -1) {
                            cameraInfoList[frontCameraIdx].cameraId
                        } else {
                            // 전후면 카메라 지원 불가시 카메라 id 리스트 첫번째 아이디 적용
                            cameraInfoList[0].cameraId
                        }
                    }
                }
            } else {
                // 기존 아이디가 제공 아이디 리스트에 없을 때 = 후면 카메라를 먼저 적용
                val backCameraIdx = cameraInfoList.indexOfFirst {
                    it.facing == CameraCharacteristics.LENS_FACING_BACK
                }

                // 후면 카메라 적용 검토
                if (backCameraIdx != -1) {
                    cameraInfoList[backCameraIdx].cameraId
                } else { // 후면 카메라 아이디 지원 불가
                    // 전면 카메라 적용 검토
                    val frontCameraIdx = cameraInfoList.indexOfFirst {
                        it.facing == CameraCharacteristics.LENS_FACING_FRONT
                    }

                    if (frontCameraIdx != -1) {
                        cameraInfoList[frontCameraIdx].cameraId
                    } else {
                        // 전후면 카메라 지원 불가시 카메라 id 리스트 첫번째 아이디 적용
                        cameraInfoList[0].cameraId
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

    // (초기 뷰 설정)
    private fun onCreateInitView() {
        bindingMbr.btn2.setOnClickListener {
            cameraObjMbr.captureRequest(null, onError = {})
        }
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
            val cameraInfoList = CameraObj.getAllSupportedCameraInfoList(this)
            val cameraItemList = ArrayList<String>()

            for (id in cameraInfoList) {
                cameraItemList.add("Camera ${id.cameraId}")
            }

            var checkedIdx = cameraInfoList.indexOfFirst {
                it.cameraId == viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId
            }

            if (checkedIdx == -1) {
                checkedIdx = 0
            }

            viewModelMbr.imageProcessingPauseMbr = true
            viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.value =
                DialogRadioButtonChoose.DialogInfoVO(
                    isCancelable = true,
                    title = "카메라 선택",
                    contentMsg = null,
                    radioButtonContentList = cameraItemList,
                    checkedItemIdx = checkedIdx,
                    cancelBtnTxt = null,
                    onRadioItemClicked = {
                    },
                    onCancelBtnClicked = {
                        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false
                    },
                    onSelectBtnClicked = {
                        val checkedCameraId = cameraInfoList[it].cameraId

                        if (checkedCameraId != viewModelMbr.cameraConfigInfoSpwMbr.currentCameraId) {
                            // 기존 저장 폴더 백업
                            val videoFile =
                                if (cameraObjMbr.mediaRecorderStatusCodeMbr == 3 || cameraObjMbr.mediaRecorderStatusCodeMbr == 4) {
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
                                // 지원 사이즈 탐지
                                val chosenPreviewSurfaceSize =
                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                                        1,
                                        Long.MAX_VALUE,
                                        3.0 / 2.0
                                    )!!

                                val previewConfigVo =
                                    // 설정 객체 반환
                                    arrayListOf(
                                        CameraObj.PreviewConfigVo(
                                            chosenPreviewSurfaceSize,
                                            bindingMbr.cameraPreviewAutoFitTexture
                                        )
                                    )

                                // 지원 사이즈 탐지
                                val chosenImageReaderSurfaceSize =
                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                                        2,
                                        500 * 500,
                                        3.0 / 2.0
                                    )!!

                                val analysisImageReaderConfigVo =
                                    // 설정 객체 반환
                                    CameraObj.ImageReaderConfigVo(
                                        chosenImageReaderSurfaceSize,
                                        imageReaderCallback = { reader ->
                                            analyzeImage(reader)
                                        }
                                    )

                                val chosenCaptureImageReaderSurfaceSize =
                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                                        2,
                                        Long.MAX_VALUE,
                                        3.0 / 2.0
                                    )!!

                                val captureImageReaderConfigVo =
                                    // 설정 객체 반환
                                    CameraObj.ImageReaderConfigVo(
                                        chosenCaptureImageReaderSurfaceSize,
                                        imageReaderCallback = { reader ->
                                            captureImage(reader)
                                        }
                                    )

                                // (카메라 변수 설정)
                                // todo : 세팅 함수는 그대로 두되, setCameraRequest 에서 한번에 설정하도록
                                // 떨림 보정
                                cameraObjMbr.setCameraStabilization(
                                    true,
                                    onComplete = {},
                                    onError = {})

                                // (카메라 서페이스 설정)
                                cameraObjMbr.setCameraOutputSurfaces(
                                    previewConfigVo,
                                    captureImageReaderConfigVo,
                                    null,
                                    analysisImageReaderConfigVo,
                                    onComplete = {
                                        // (카메라 리퀘스트 설정)
                                        cameraObjMbr.repeatingRequestOnTemplate(
                                            forPreview = true,
                                            forMediaRecorder = false,
                                            forAnalysisImageReader = true,
                                            onComplete = {

                                            },
                                            onError = {

                                            }
                                        )
                                    },
                                    onError = {

                                    }
                                )
                            }
                        }
                        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false
                    },
                    onCanceled = {
                        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.value = null
                        viewModelMbr.imageProcessingPauseMbr = false
                    }
                )
        }

        // todo 녹화중 화면 효과
        // todo 방해 금지 모드로 회전 및 pause 가 불가능하도록 처리
        bindingMbr.btn1.setOnClickListener {
            // 처리 완료까지 중복 클릭 방지
            bindingMbr.btn1.isEnabled = false

            if (cameraObjMbr.mediaRecorderStatusCodeMbr != 3 && cameraObjMbr.mediaRecorderStatusCodeMbr != 4) { // 현재 레코딩 중이 아닐 때
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

                cameraObjMbr.unsetCameraOutputSurfaces {
                    // 지원 사이즈 탐지
                    val chosenPreviewSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            1,
                            Long.MAX_VALUE,
                            3.0 / 2.0
                        )!!

                    val previewConfigVo =
                        // 설정 객체 반환
                        arrayListOf(
                            CameraObj.PreviewConfigVo(
                                chosenPreviewSurfaceSize,
                                bindingMbr.cameraPreviewAutoFitTexture
                            )
                        )

                    // 지원 사이즈 탐지
                    val chosenImageReaderSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            2,
                            500 * 500,
                            3.0 / 2.0
                        )!!

                    val analysisImageReaderConfigVo =
                        // 설정 객체 반환
                        CameraObj.ImageReaderConfigVo(
                            chosenImageReaderSurfaceSize,
                            imageReaderCallback = { reader ->
                                analyzeImage(reader)
                            }
                        )

                    // 지원 사이즈 탐지
                    val chosenSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            3,
                            Long.MAX_VALUE,
                            3.0 / 2.0
                        )!!

                    val mediaRecorderConfigVo =
                        // 설정 객체 반환
                        CameraObj.MediaRecorderConfigVo(
                            chosenSurfaceSize,
                            File("${this.filesDir.absolutePath}/${System.currentTimeMillis()}.mp4"),
                            true,
                            Int.MAX_VALUE,
                            Int.MAX_VALUE,
                            Int.MAX_VALUE
                        )

                    val chosenCaptureImageReaderSurfaceSize =
                        cameraObjMbr.getNearestSupportedCameraOutputSize(
                            2,
                            Long.MAX_VALUE,
                            3.0 / 2.0
                        )!!

                    val captureImageReaderConfigVo =
                        // 설정 객체 반환
                        CameraObj.ImageReaderConfigVo(
                            chosenCaptureImageReaderSurfaceSize,
                            imageReaderCallback = { reader ->
                                captureImage(reader)
                            }
                        )

                    // (카메라 변수 설정)
                    // 떨림 보정
                    cameraObjMbr.setCameraStabilization(
                        true,
                        onComplete = {},
                        onError = {})

                    // (카메라 서페이스 설정)
                    cameraObjMbr.setCameraOutputSurfaces(
                        previewConfigVo,
                        captureImageReaderConfigVo,
                        mediaRecorderConfigVo,
                        null,
                        onComplete = {
                            // (카메라 리퀘스트 설정)
                            cameraObjMbr.repeatingRequestOnTemplate(
                                forPreview = true,
                                forMediaRecorder = true,
                                forAnalysisImageReader = false,
                                onComplete = {
                                    // (미디어 레코딩 녹화 실행)
                                    cameraObjMbr.startMediaRecording(onComplete = {
                                        runOnUiThread {
                                            bindingMbr.btn1.isEnabled = true
                                        }
                                    }, onError = {})
                                },
                                onError = {

                                }
                            )
                        },
                        onError = {
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
                cameraObjMbr.unsetCameraOutputSurfaces(
                    onComplete = {
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
                                finish()
                            }
                            resultLauncherMbr.launch(mediaPlayerIntent)
                        }
                    }
                )
            }
        }

    }

    // (라이브 데이터 설정)
    private fun onCreateSetLiveData() {
        // 로딩 다이얼로그 출력 플래그
        viewModelMbr.progressLoadingDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogProgressLoading) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogProgressLoading(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // progressSample2 진행도
        viewModelMbr.progressDialogSample2ProgressValue.observe(this) {
            if (it != -1) {
                val loadingText = "로딩중 $it%"
                if (dialogMbr != null) {
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressMessageTxt.text =
                        loadingText
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.visibility =
                        View.VISIBLE
                    (dialogMbr as DialogProgressLoading).bindingMbr.progressBar.progress = it
                }
            }
        }

        // 선택 다이얼로그 출력 플래그
        viewModelMbr.binaryChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogBinaryChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogBinaryChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 확인 다이얼로그 출력 플래그
        viewModelMbr.confirmDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogConfirm) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogConfirm(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }

        // 라디오 버튼 선택 다이얼로그 출력 플래그
        viewModelMbr.radioButtonChooseDialogInfoLiveDataMbr.observe(this) {
            if (it == null) {
                if (dialogMbr is DialogRadioButtonChoose) {
                    dialogMbr?.dismiss()
                    dialogMbr = null
                }
            } else {
                dialogMbr?.dismiss()

                dialogMbr = DialogRadioButtonChoose(
                    this,
                    it
                )
                dialogMbr?.show()
            }
        }
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    private fun allPermissionsGranted() {
        if (!viewModelMbr.isActivityRecreatedMbr) { // 화면 회전이 아닐때
            if (!viewModelMbr.doItAlreadyMbr) {
                // (액티비티 실행시 처음 한번만 실행되는 로직)
                viewModelMbr.doItAlreadyMbr = true

                // (초기 데이터 수집)

                // (알고리즘)
                // (카메라 실행)
                // 지원 사이즈 탐지
                val chosenPreviewSurfaceSize =
                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                        1,
                        Long.MAX_VALUE,
                        3.0 / 2.0
                    )!!

                val previewConfigVo =
                    // 설정 객체 반환
                    arrayListOf(
                        CameraObj.PreviewConfigVo(
                            chosenPreviewSurfaceSize,
                            bindingMbr.cameraPreviewAutoFitTexture
                        )
                    )

                // 지원 사이즈 탐지
                val chosenImageReaderSurfaceSize =
                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                        2,
                        500 * 500,
                        3.0 / 2.0
                    )!!

                val analysisImageReaderConfigVo =
                    // 설정 객체 반환
                    CameraObj.ImageReaderConfigVo(
                        chosenImageReaderSurfaceSize,
                        imageReaderCallback = { reader ->
                            analyzeImage(reader)
                        }
                    )

                val chosenCaptureImageReaderSurfaceSize =
                    cameraObjMbr.getNearestSupportedCameraOutputSize(
                        2,
                        Long.MAX_VALUE,
                        3.0 / 2.0
                    )!!

                val captureImageReaderConfigVo =
                    // 설정 객체 반환
                    CameraObj.ImageReaderConfigVo(
                        chosenCaptureImageReaderSurfaceSize,
                        imageReaderCallback = { reader ->
                            captureImage(reader)
                        }
                    )

                // (카메라 변수 설정)
                // todo : 세팅 함수는 그대로 두되, setCameraRequest 에서 한번에 설정하도록
                // 떨림 보정
                cameraObjMbr.setCameraStabilization(
                    true,
                    onComplete = {},
                    onError = {})

                // (카메라 서페이스 설정)
                cameraObjMbr.setCameraOutputSurfaces(
                    previewConfigVo,
                    captureImageReaderConfigVo,
                    null,
                    analysisImageReaderConfigVo,
                    onComplete = {
                        // (카메라 리퀘스트 설정)
                        cameraObjMbr.repeatingRequestOnTemplate(
                            forPreview = true,
                            forMediaRecorder = false,
                            forAnalysisImageReader = true,
                            onComplete = {

                            },
                            onError = {

                            }
                        )
                    },
                    onError = {

                    }
                )
            } else {
                // (회전이 아닌 onResume 로직) : 권한 클리어
                // (뷰 데이터 로딩)
                // : 유저가 변경되면 해당 유저에 대한 데이터로 재구축
                val sessionToken = viewModelMbr.currentLoginSessionInfoSpwMbr.sessionToken
                if (sessionToken != viewModelMbr.currentUserSessionTokenMbr) { // 액티비티 유저와 세션 유저가 다를 때
                    // 진입 플래그 변경
                    viewModelMbr.currentUserSessionTokenMbr = sessionToken

                    // (데이터 수집)

                    // (알고리즘)
                }

                // (알고리즘)
                // (카메라 실행)
                // todo
                cameraObjMbr.repeatingRequestOnTemplate(
                    forPreview = true,
                    forMediaRecorder = false,
                    forAnalysisImageReader = true,
                    onComplete = {

                    },
                    onError = {

                    }
                )
            }
        } else { // 화면 회전일 때

        }

        // 화면 회전 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED


        // onResume 의 가장 마지막엔 설정 변경(화면회전) 여부를 초기화
        viewModelMbr.isActivityRecreatedMbr = false
    }

    // GPU 접속 제한 세마포어
    private val gpuSemaphoreMbr = Semaphore(1)

    // 최대 이미지 프로세싱 개수 (현재 처리중인 이미지 프로세싱 개수가 이것을 넘어가면 그냥 return)
    private var asyncImageProcessingOnProgressMbr = false
    private val asyncImageProcessingOnProgressSemaphoreMbr = Semaphore(1)

    // (카메라 이미지 실시간 처리 콜백)
    // 카메라에서 이미지 프레임을 받아올 때마다 이것이 실행됨
    private fun analyzeImage(reader: ImageReader) {
        try {
            // (1. Image 객체 정보 추출)
            // reader 객체로 받은 image 객체의 이미지 정보가 처리되어 close 될 때까지는 동기적으로 처리
            // image 객체가 빨리 close 되고 나머지는 비동기 처리를 하는게 좋음

            // 프레임 이미지 객체
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // 조기 종료 플래그
            if (cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
                isDestroyed || isFinishing // 액티비티 자체가 종료
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
                    cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                    viewModelMbr.imageProcessingPauseMbr || // imageProcessing 정지 신호
                    isDestroyed || isFinishing // 액티비티 자체가 종료
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
                    if (!isDestroyed && !isFinishing) {
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
                            360 - cameraObjMbr.cameraInfoVoMbr.sensorOrientation
                        }
                        Surface.ROTATION_90 -> { // 카메라 기본 방향에서 역시계 방향 90도 회전 상태
                            // if sensorOrientationMbr = 90 -> 0
                            90 - cameraObjMbr.cameraInfoVoMbr.sensorOrientation
                        }
                        Surface.ROTATION_180 -> {
                            // if sensorOrientationMbr = 90 -> 90
                            180 - cameraObjMbr.cameraInfoVoMbr.sensorOrientation
                        }
                        Surface.ROTATION_270 -> {
                            // if sensorOrientationMbr = 90 -> 180
                            270 - cameraObjMbr.cameraInfoVoMbr.sensorOrientation
                        }
                        else -> {
                            0
                        }
                    }

                gpuSemaphoreMbr.acquire()
                val rotatedCameraImageFrameBitmap =
                    RenderScriptUtil.rotateBitmapCounterClock(
                        viewModelMbr.renderScriptMbr,
                        viewModelMbr.scriptCRotatorMbr,
                        cameraImageFrameBitmap,
                        rotateCounterClockAngle
                    )
                gpuSemaphoreMbr.release()

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
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

                gpuSemaphoreMbr.acquire()
                val resizedBitmap = RenderScriptUtil.resizeBitmapIntrinsic(
                    viewModelMbr.renderScriptMbr,
                    viewModelMbr.scriptIntrinsicResizeMbr,
                    rotatedCameraImageFrameBitmap,
                    dstSize.width,
                    dstSize.height
                )
                gpuSemaphoreMbr.release()

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
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

                gpuSemaphoreMbr.acquire()
                val croppedBitmap = RenderScriptUtil.cropBitmap(
                    viewModelMbr.renderScriptMbr,
                    viewModelMbr.scriptCCropMbr,
                    rotatedCameraImageFrameBitmap,
                    Rect(
                        (cropAreaRatioRectF.left * rotatedCameraImageFrameBitmap.width).toInt(),
                        (cropAreaRatioRectF.top * rotatedCameraImageFrameBitmap.height).toInt(),
                        (cropAreaRatioRectF.right * rotatedCameraImageFrameBitmap.width).toInt(),
                        (cropAreaRatioRectF.bottom * rotatedCameraImageFrameBitmap.height).toInt()
                    )
                )
                gpuSemaphoreMbr.release()

                // 디버그를 위한 표시
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
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

    // (카메라 이미지 실시간 처리 콜백)
    // 카메라에서 이미지 프레임을 받아올 때마다 이것이 실행됨
    private fun captureImage(reader: ImageReader) {
        try {
            val file = File("${this.filesDir.absolutePath}/${System.currentTimeMillis()}.jpg")

            // 프레임 이미지 객체
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // 조기 종료 플래그
            if (cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                isDestroyed || isFinishing // 액티비티 자체가 종료
            ) {
                imageObj.close()
                return
            }

            val buffer: ByteBuffer = CustomUtil.cloneByteBuffer(imageObj.planes[0].buffer)
            imageObj.close()
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            val output = FileOutputStream(file)
            output.write(bytes)
            output.close()

            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    Glide.with(this)
                        .load(file)
                        .transform(FitCenter())
                        .into(bindingMbr.captureImg)
                }

                viewModelMbr.executorServiceMbr?.execute {
                    Thread.sleep(2000)
                    file.delete()
                }
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
        gpuSemaphoreMbr.acquire()
        val result = RenderScriptUtil.yuv420888ToARgb8888BitmapIntrinsic(
            viewModelMbr.renderScriptMbr,
            viewModelMbr.scriptIntrinsicYuvToRGBMbr,
            imageWidth,
            imageHeight,
            yuvByteArray
        )
        gpuSemaphoreMbr.release()

        return result
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (뷰모델 객체)
    // : 액티비티 reCreate 이후에도 남아있는 데이터 묶음 = 뷰의 데이터 모델
    //     뷰모델이 맡은 것은 화면 회전시에도 불변할 데이터의 저장
    class ViewModel(application: Application) : AndroidViewModel(application) {
        // <멤버 상수 공간>
        // (repository 모델)
        val repositorySetMbr: RepositorySet = RepositorySet.getInstance(application)

        // (스레드 풀)
        val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

        // (SharedPreference 객체)
        // 현 로그인 정보 접근 객체
        val currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw =
            CurrentLoginSessionInfoSpw(application)

        // 카메라 설정 정보 접근 객체
        val cameraConfigInfoSpwMbr: ActivityBasicCamera2ApiSampleSpw =
            ActivityBasicCamera2ApiSampleSpw(application)

        // (앱 진입 필수 권한 배열)
        // : 앱 진입에 필요한 권한 배열.
        //     ex : Manifest.permission.INTERNET
        val activityPermissionArrayMbr: Array<String> = arrayOf(Manifest.permission.CAMERA)

        // 랜더 스크립트
        val renderScriptMbr: RenderScript = RenderScript.create(application)

        // intrinsic yuv to rgb
        val scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(
            renderScriptMbr,
            Element.U8_4(renderScriptMbr)
        )

        // image rotate
        val scriptCRotatorMbr: ScriptC_rotator = ScriptC_rotator(renderScriptMbr)

        // image crop
        val scriptCCropMbr: ScriptC_crop = ScriptC_crop(renderScriptMbr)

        // image resize
        val scriptIntrinsicResizeMbr: ScriptIntrinsicResize = ScriptIntrinsicResize.create(
            renderScriptMbr
        )


        // ---------------------------------------------------------------------------------------------
        // <멤버 변수 공간>
        // (최초 실행 플래그) : 액티비티가 실행되고, 권한 체크가 끝난 후의 최초 로직이 실행되었는지 여부
        var doItAlreadyMbr = false

        // (설정 변경 여부) : 의도적인 액티비티 종료가 아닌 화면 회전과 같은 상황
        var isActivityRecreatedMbr = false

        // (이 화면에 도달한 유저 계정 고유값) : 세션 토큰이 없다면 비회원 상태
        var currentUserSessionTokenMbr: String? = null

        // 이미지 리더 프로세싱 일시정지 여부
        var imageProcessingPauseMbr = false


        // ---------------------------------------------------------------------------------------------
        // <뷰모델 라이브데이터 공간>
        // 로딩 다이얼로그 출력 정보
        val progressLoadingDialogInfoLiveDataMbr: MutableLiveData<DialogProgressLoading.DialogInfoVO?> =
            MutableLiveData(null)

        val progressDialogSample2ProgressValue: MutableLiveData<Int> =
            MutableLiveData(-1)

        // 선택 다이얼로그 출력 정보
        val binaryChooseDialogInfoLiveDataMbr: MutableLiveData<DialogBinaryChoose.DialogInfoVO?> =
            MutableLiveData(null)

        // 확인 다이얼로그 출력 정보
        val confirmDialogInfoLiveDataMbr: MutableLiveData<DialogConfirm.DialogInfoVO?> =
            MutableLiveData(null)

        // 라디오 버튼 선택 다이얼로그 출력 정보
        val radioButtonChooseDialogInfoLiveDataMbr: MutableLiveData<DialogRadioButtonChoose.DialogInfoVO?> =
            MutableLiveData(null)


        // ---------------------------------------------------------------------------------------------
        // <클래스 생명주기 공간>
        override fun onCleared() {
            // 랜더 스크립트 객체 해소
            scriptCCropMbr.destroy()
            scriptIntrinsicResizeMbr.destroy()
            scriptIntrinsicYuvToRGBMbr.destroy()
            scriptCRotatorMbr.destroy()
            renderScriptMbr.finish()
            renderScriptMbr.destroy()

            super.onCleared()
        }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
    }

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