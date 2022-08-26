package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.BuildConfig
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
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

// todo : 권한 체크 시점에 카메라가 실행되지 않음
class ActivityBasicCamera2ApiSample : AppCompatActivity() {
    // <설정 변수 공간>
    // (앱 진입 필수 권한 배열)
    // : 앱 진입에 필요한 권한 배열.
    //     ex : Manifest.permission.INTERNET
    private val activityPermissionArrayMbr: Array<String> = arrayOf(Manifest.permission.CAMERA)


    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (뷰 바인더 객체)
    lateinit var bindingMbr: ActivityBasicCamera2ApiSampleBinding

    // (repository 모델)
    lateinit var repositorySetMbr: RepositorySet

    // (SharedPreference 객체)
    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

    // 카메라 설정 정보 접근 객체
    lateinit var classSpwMbr: ClassSpw

    // (스레드 풀)
    val executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // (다이얼로그 객체)
    var dialogMbr: Dialog? = null
    var shownDialogInfoVOMbr: InterfaceDialogInfoVO? = null
        set(value) {
            when (value) {
                is DialogBinaryChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogBinaryChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogConfirm.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogConfirm(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogProgressLoading.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogProgressLoading(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                is DialogRadioButtonChoose.DialogInfoVO -> {
                    dialogMbr?.dismiss()

                    dialogMbr = DialogRadioButtonChoose(
                        this,
                        value
                    )
                    dialogMbr?.show()
                }
                else -> {
                    dialogMbr?.dismiss()
                    dialogMbr = null

                    field = null
                    return
                }
            }
            field = value
        }

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((Map<String, Boolean>) -> Unit))? = null
    private var permissionRequestOnProgressMbr = false
    private val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null


    // (랜더 스크립트 객체)
    lateinit var renderScriptMbr: RenderScript

    // intrinsic yuv to rgb
    lateinit var scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB

    // image rotate
    lateinit var scriptCRotatorMbr: ScriptC_rotator

    // image crop
    lateinit var scriptCCropMbr: ScriptC_crop

    // image resize
    lateinit var scriptIntrinsicResizeMbr: ScriptIntrinsicResize


    // 카메라 실행 객체
    private lateinit var cameraObjMbr: CameraObj

    // 이미지 리더 프로세싱 일시정지 여부
    private var imageProcessingPauseMbr = false


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    // : 액티비티 실행  = onCreate() → onStart() → onResume()
    //     액티비티 일시정지 후 재실행 = onPause() → ... -> onResume()
    //     액티비티 정지 후 재실행 = onPause() → onStop() -> ... -> onStart() → onResume()
    //     액티비티 종료 = onPause() → onStop() → onDestroy()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // (초기 객체 생성)
        onCreateInitObject()

        // (초기 뷰 설정)
        onCreateInitView()
    }

    override fun onResume() {
        super.onResume()

        executorServiceMbr.execute {
            permissionRequestOnProgressSemaphoreMbr.acquire()
            runOnUiThread {
                if (!permissionRequestOnProgressMbr) { // 현재 권한 요청중이 아님
                    permissionRequestOnProgressMbr = true
                    permissionRequestOnProgressSemaphoreMbr.release()
                    // (액티비티 진입 필수 권한 확인)
                    // 진입 필수 권한이 클리어 되어야 로직이 실행

                    // 권한 요청 콜백
                    permissionRequestCallbackMbr = { permissions ->
                        var isPermissionAllGranted = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted = false
                                break
                            }
                        }

                        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

                            allPermissionsGranted()
                        } else if (!neverAskAgain) { // 단순 거부
                            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                                true,
                                "권한 필요",
                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                "뒤로가기",
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr = null
                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                    permissionRequestOnProgressMbr = false
                                    permissionRequestOnProgressSemaphoreMbr.release()

                                    finish()
                                }
                            )

                        } else { // 권한 클리어 되지 않음 + 다시 묻지 않기 선택
                            shownDialogInfoVOMbr =
                                DialogBinaryChoose.DialogInfoVO(
                                    false,
                                    "권한 요청",
                                    "해당 서비스를 이용하기 위해선\n" +
                                            "필수 권한 승인이 필요합니다.\n" +
                                            "권한 설정 화면으로 이동하시겠습니까?",
                                    null,
                                    null,
                                    onPosBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        // 권한 설정 화면으로 이동
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        intent.data = Uri.fromParts("package", packageName, null)

                                        resultLauncherCallbackMbr = {
                                            // 설정 페이지 복귀시 콜백
                                            var isPermissionAllGranted1 = true
                                            for (activityPermission in activityPermissionArrayMbr) {
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
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

                                                allPermissionsGranted()
                                            } else { // 권한 거부
                                                shownDialogInfoVOMbr =
                                                    DialogConfirm.DialogInfoVO(
                                                        true,
                                                        "권한 요청",
                                                        "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                        "뒤로가기",
                                                        onCheckBtnClicked = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        },
                                                        onCanceled = {
                                                            shownDialogInfoVOMbr =
                                                                null
                                                            permissionRequestOnProgressSemaphoreMbr.acquire()
                                                            permissionRequestOnProgressMbr = false
                                                            permissionRequestOnProgressSemaphoreMbr.release()

                                                            finish()
                                                        }
                                                    )
                                            }
                                        }
                                        resultLauncherMbr.launch(intent)
                                    },
                                    onNegBtnClicked = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    },
                                    onCanceled = {
                                        shownDialogInfoVOMbr = null

                                        shownDialogInfoVOMbr =
                                            DialogConfirm.DialogInfoVO(
                                                true,
                                                "권한 요청",
                                                "서비스를 실행하기 위한 필수 권한이 거부되었습니다.",
                                                "뒤로가기",
                                                onCheckBtnClicked = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                },
                                                onCanceled = {
                                                    shownDialogInfoVOMbr =
                                                        null
                                                    permissionRequestOnProgressSemaphoreMbr.acquire()
                                                    permissionRequestOnProgressMbr = false
                                                    permissionRequestOnProgressSemaphoreMbr.release()

                                                    finish()
                                                }
                                            )
                                    }
                                )

                        }
                    }

                    // 연결된 카메라가 없을 때
                    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                        shownDialogInfoVOMbr =
                            DialogConfirm.DialogInfoVO(
                                true,
                                "카메라 장치가 없습니다.",
                                "카메라 장치에 접근할 수 없습니다.\n화면을 종료합니다.",
                                null,
                                onCheckBtnClicked = {
                                    shownDialogInfoVOMbr =
                                        null
                                    finish()
                                },
                                onCanceled = {
                                    shownDialogInfoVOMbr =
                                        null
                                    finish()
                                }
                            )
                    } else {
                        // 권한 요청
                        permissionRequestMbr.launch(activityPermissionArrayMbr)
                    }
                } else { // 현재 권한 요청중
                    permissionRequestOnProgressSemaphoreMbr.release()
                }
            }
        }
    }

    override fun onPause() {
        if (cameraObjMbr.mediaRecorderStatusCodeMbr == 3 ||
            cameraObjMbr.mediaRecorderStatusCodeMbr == 4
        ) { // 레코딩 중이라면 기존 레코딩 세션 중지 후 녹화 파일 제거 하고 프리뷰 세션 준비상태로 전환
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

    override fun onDestroy() {
        // 다이얼로그 객체 해소
        dialogMbr?.dismiss()

        // 카메라 해소
        cameraObjMbr.destroyCameraObject(onComplete = {})

        // 랜더 스크립트 객체 해소
        scriptCCropMbr.destroy()
        scriptIntrinsicResizeMbr.destroy()
        scriptIntrinsicYuvToRGBMbr.destroy()
        scriptCRotatorMbr.destroy()
        renderScriptMbr.finish()
        renderScriptMbr.destroy()

        super.onDestroy()
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    // : 해당 이벤트 발생시 처리할 로직을 작성
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 화면회전 landscape

        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // 화면회전 portrait

        }
    }

    // (뒤로가기를 눌렀을 때)
    override fun onBackPressed() {
        imageProcessingPauseMbr = true
        shownDialogInfoVOMbr =
            DialogBinaryChoose.DialogInfoVO(
                true,
                "카메라 종료",
                "카메라를 종료하시겠습니까?",
                "종료",
                "취소",
                onPosBtnClicked = {
                    shownDialogInfoVOMbr =
                        null
                    finish()
                },
                onNegBtnClicked = {
                    shownDialogInfoVOMbr =
                        null
                    imageProcessingPauseMbr = false
                },
                onCanceled = {
                    shownDialogInfoVOMbr =
                        null
                    imageProcessingPauseMbr = false
                }
            )
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (초기 객체 생성)
    // : 클래스에서 사용할 객체를 초기 생성
    private fun onCreateInitObject() {
        // 뷰 객체
        bindingMbr = ActivityBasicCamera2ApiSampleBinding.inflate(layoutInflater)
        // 뷰 객체 바인딩
        setContentView(bindingMbr.root)

        // 레포지토리 객체
        repositorySetMbr = RepositorySet.getInstance(application)

        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)

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

        // 랜더 스크립트 관련 객체 생성
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

        classSpwMbr = ClassSpw(application)

        // (최초 사용 카메라 객체 생성)
        val cameraInfoList = CameraObj.getAllSupportedCameraInfoList(this)

        if (cameraInfoList.size == 0) {
            // 지원하는 카메라가 없음
            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                true,
                "에러",
                "지원되는 카메라를 찾을 수 없습니다.",
                null,
                onCheckBtnClicked = {
                    shownDialogInfoVOMbr = null
                    finish()
                },
                onCanceled = {
                    shownDialogInfoVOMbr = null
                    finish()
                }
            )
        }

        // 초기 카메라 아이디 선정
        val cameraId: String =
            if (classSpwMbr.currentCameraId != null) { // 기존 아이디가 있을 때

                // 기존 아이디가 현재 지원 카메라 리스트에 있는지를 확인
                val lastCameraIdIdx = cameraInfoList.indexOfFirst {
                    it.cameraId == classSpwMbr.currentCameraId
                }

                if (lastCameraIdIdx != -1) {
                    // 기존 아이디가 제공 아이디 리스트에 있을 때
                    classSpwMbr.currentCameraId!!
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

        classSpwMbr.currentCameraId = cameraId
    }

    // (초기 뷰 설정)
    // : 뷰 리스너 바인딩, 초기 뷰 사이즈, 위치 조정 등
    private fun onCreateInitView() {
        // (화면을 꺼지지 않도록 하는 플래그)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (화면 방향에 따른 뷰 마진 설정)
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

        // (캡쳐)
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

        // (카메라 전환)
        bindingMbr.cameraChangeBtn.setOnClickListener {
            val cameraInfoList = CameraObj.getAllSupportedCameraInfoList(this)
            val cameraItemList = ArrayList<String>()

            for (id in cameraInfoList) {
                cameraItemList.add("Camera ${id.cameraId}")
            }

            var checkedIdx = cameraInfoList.indexOfFirst {
                it.cameraId == classSpwMbr.currentCameraId
            }

            if (checkedIdx == -1) {
                checkedIdx = 0
            }

            imageProcessingPauseMbr = true
            shownDialogInfoVOMbr =
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
                        shownDialogInfoVOMbr = null
                        imageProcessingPauseMbr = false
                    },
                    onSelectBtnClicked = {
                        val checkedCameraId = cameraInfoList[it].cameraId

                        if (checkedCameraId != classSpwMbr.currentCameraId) {
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

                                classSpwMbr.currentCameraId =
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
                        shownDialogInfoVOMbr = null
                        imageProcessingPauseMbr = false
                    },
                    onCanceled = {
                        shownDialogInfoVOMbr = null
                        imageProcessingPauseMbr = false
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

    // (액티비티 진입 권한이 클리어 된 시점)
    // : 실질적인 액티비티 로직 실행구역
    private var doItAlreadyMbr = false
    private var currentUserUidMbr: String? = null
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (권한이 충족된 onCreate)
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.userUid
            getScreenDataAndShow()

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val userUid = currentLoginSessionInfoSpwMbr.userUid
            if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = userUid

                // (데이터 수집)
                getScreenDataAndShow()
            }

        }

        // (onResume)


        // (onResume 로직)
        // (알고리즘)
        // (뷰 데이터 로딩)
        // : 데이터 갱신은 유저 정보가 변경된 것을 기준으로 함.
        val userUid = currentLoginSessionInfoSpwMbr.userUid
        if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
            // 진입 플래그 변경
            currentUserUidMbr = userUid

            // (데이터 수집)

            // (알고리즘)
        }

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

        if (cameraObjMbr.cameraStatusCodeMbr == 1) {
            cameraObjMbr.repeatingRequestOnTemplate(
                forPreview = true,
                forMediaRecorder = false,
                forAnalysisImageReader = true,
                onComplete = {

                },
                onError = {

                }
            )
        } else {
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

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    private fun getScreenDataAndShow() {

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
                imageProcessingPauseMbr || // imageProcessing 정지 신호
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
            executorServiceMbr.execute {

                // 조기 종료 확인
                asyncImageProcessingOnProgressSemaphoreMbr.acquire()
                if (asyncImageProcessingOnProgressMbr || // 현재 비동기 이미지 프로세싱 중일 때
                    cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                    imageProcessingPauseMbr || // imageProcessing 정지 신호
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
                        renderScriptMbr,
                        scriptCRotatorMbr,
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
                    renderScriptMbr,
                    scriptIntrinsicResizeMbr,
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

                executorServiceMbr.execute {
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
            renderScriptMbr,
            scriptIntrinsicYuvToRGBMbr,
            imageWidth,
            imageHeight,
            yuvByteArray
        )
        gpuSemaphoreMbr.release()

        return result
    }

    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    class ClassSpw(application: Application) {
        // <멤버 변수 공간>
        // SharedPreference 접근 객체
        private val spMbr = application.getSharedPreferences(
            "ActivityBasicCamera2ApiSampleSpw",
            Context.MODE_PRIVATE
        )

        var currentCameraId: String?
            get() {
                return spMbr.getString(
                    "currentCameraId",
                    null
                )
            }
            set(value) {
                with(spMbr.edit()) {
                    putString(
                        "currentCameraId",
                        value
                    )
                    apply()
                }
            }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>

    }

//    data class ImageDataVo(
//        val imageTimeMs: Long,
//        val imageWidth: Int,
//        val imageHeight: Int,
//        val pixelCount: Int,
//        val rowStride0: Int,
//        val pixelStride0: Int,
//        val planeBuffer0: ByteBuffer,
//        val rowStride1: Int,
//        val pixelStride1: Int,
//        val planeBuffer1: ByteBuffer,
//        val rowStride2: Int,
//        val pixelStride2: Int,
//        val planeBuffer2: ByteBuffer
//    )

}