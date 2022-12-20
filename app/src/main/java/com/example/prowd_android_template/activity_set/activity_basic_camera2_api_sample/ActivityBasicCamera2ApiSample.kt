package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.example.prowd_android_template.R
import com.example.prowd_android_template.ScriptC_rotator
import com.example.prowd_android_template.abstract_class.AbstractProwdRecyclerViewAdapter
import com.example.prowd_android_template.abstract_class.InterfaceDialogInfoVO
import com.example.prowd_android_template.common_shared_preference_wrapper.CurrentLoginSessionInfoSpw
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.custom_view.DialogRadioButtonChoose
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.repository.RepositorySet
import com.example.prowd_android_template.repository.database_room.tables.ActivityBasicCamera2ApiSampleCameraConfigTable
import com.example.prowd_android_template.util_class.Camera2Obj
import com.example.prowd_android_template.util_class.ThreadConfluenceObj
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil
import com.xxx.yyy.ScriptC_crop
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

// todo : 신코드 적용
// todo : 이미지 처리 프레임 조절 기능
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

    // (어뎁터 객체)
    lateinit var adapterSetMbr: ActivityBasicCamera2ApiSampleAdapterSet

    // (SharedPreference 객체)
    // 클래스 비휘발성 저장객체
    lateinit var classSpwMbr: ActivityBasicCamera2ApiSampleSpw

    // 현 로그인 정보 접근 객체
    lateinit var currentLoginSessionInfoSpwMbr: CurrentLoginSessionInfoSpw

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
    var permissionRequestOnProgressMbr = false
    val permissionRequestOnProgressSemaphoreMbr = Semaphore(1)

    // (ActivityResultLauncher 객체)
    // : 액티비티 결과 받아오기 객체. 사용법은 permissionRequestMbr 와 동일
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

    // (데이터)
    // 카메라 실행 객체
    private lateinit var cameraObjMbr: Camera2Obj

    // 이미지 처리용 랜더 스크립트
    private lateinit var renderScriptMbr: RenderScript

    // intrinsic yuv to rgb
    private lateinit var scriptIntrinsicYuvToRGBMbr: ScriptIntrinsicYuvToRGB

    // image rotate
    private lateinit var scriptCRotatorMbr: ScriptC_rotator

    // image crop
    private lateinit var scriptCCropMbr: ScriptC_crop

    // image resize
    private lateinit var scriptIntrinsicResizeMbr: ScriptIntrinsicResize

    // (카메라 정보)
    // 영상 분석을 할지 여부 설정
    private val isCameraImageAnalysisMbr = true

    // 현재 카메라 모드
    // 1 : 사진
    // 2 : 동영상
    private var currentCameraModeMbr: Int = -1

    // 플래쉬 모드
    // 0 : 안함
    // 1 : 촬영시
    // 2 : 항상
    private var flashModeMbr: Int = 0

    private var timerMbr: Int = 0

    private var cameraOrientSurfaceSizeMbr: Size? = null

    // 이미지 리더 프로세싱 일시정지 여부
    var imageProcessingPauseMbr = false


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

        // (필요 디바이스 확인)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            shownDialogInfoVOMbr = DialogConfirm.DialogInfoVO(
                true,
                "카메라 장치가 없습니다.",
                "카메라 장치가 발견되지 않습니다.\n화면을 종료합니다.",
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
            return
        }

        // (권한 체크 후 함수 실행)
        // : requestPermission 시에 onPause 되고, onResume 이 다시 실행되므로 리퀘스트 복귀 시엔 여기를 지나게 되어있음
        var isPermissionAllGranted = true
        for (activityPermission in activityPermissionArrayMbr) {
            if (checkSelfPermission(activityPermission)
                == PackageManager.PERMISSION_DENIED
            ) { // 거부된 필수 권한이 존재
                // 권한 클리어 플래그를 변경하고 break
                isPermissionAllGranted = false
                break
            }
        }

        if (isPermissionAllGranted) { // 모든 권한이 클리어된 상황
            allPermissionsGranted()
            return
        }

        // (권한 비충족으로 인한 권한 요청)
        // : 권한 요청시엔 onPause 되었다가 다시 onResume 으로 복귀함
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
                        var isPermissionAllGranted1 = true
                        var neverAskAgain = false
                        for (activityPermission in activityPermissionArrayMbr) {
                            if (!permissions[activityPermission]!!) { // 거부된 필수 권한이 존재
                                // 권한 클리어 플래그를 변경하고 break
                                neverAskAgain =
                                    !shouldShowRequestPermissionRationale(activityPermission)
                                isPermissionAllGranted1 = false
                                break
                            }
                        }

                        if (isPermissionAllGranted1) { // 모든 권한이 클리어된 상황
                            permissionRequestOnProgressSemaphoreMbr.acquire()
                            permissionRequestOnProgressMbr = false
                            permissionRequestOnProgressSemaphoreMbr.release()

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
                                            var isPermissionAllGranted2 = true
                                            for (activityPermission in activityPermissionArrayMbr) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        this,
                                                        activityPermission
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) { // 거부된 필수 권한이 존재
                                                    // 권한 클리어 플래그를 변경하고 break
                                                    isPermissionAllGranted2 = false
                                                    break
                                                }
                                            }

                                            if (isPermissionAllGranted2) { // 권한 승인
                                                permissionRequestOnProgressSemaphoreMbr.acquire()
                                                permissionRequestOnProgressMbr = false
                                                permissionRequestOnProgressSemaphoreMbr.release()

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

                    // 권한 요청
                    permissionRequestMbr.launch(activityPermissionArrayMbr)
                } else { // 현재 권한 요청중
                    permissionRequestOnProgressSemaphoreMbr.release()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        permissionRequestOnProgressSemaphoreMbr.acquire()
        if (permissionRequestOnProgressMbr) {
            permissionRequestOnProgressSemaphoreMbr.release()
            // 권한 요청중엔 onPause 가 실행될 수 있기에 아래에 위치할 정상 pause 로직 도달 방지
            return
        }
        permissionRequestOnProgressSemaphoreMbr.release()

        // (onPause 알고리즘)
        // todo : 카메라 일시정지
        imageProcessingPauseMbr = true

        if (cameraObjMbr.mediaRecorderStatusCodeMbr == 3 ||
            cameraObjMbr.mediaRecorderStatusCodeMbr == 4
        ) { // 레코딩 중이라면 기존 레코딩 세션을 제거 후 프리뷰 세션으로 전환
            // 기존 저장 폴더 백업
            // todo
//            val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File
//
//            // (카메라를 서페이스 까지 초기화)
//            cameraObjMbr.unsetCameraOutputSurfaces(
//                onComplete = {
//                    // 기존 저장 폴더 삭제
//                    videoFile.delete()
//
//                    // 미디어 레코드를 제외한 카메라 세션 준비
//                    // 지원 사이즈 탐지
//                    val chosenPreviewSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            3,
//                            Long.MAX_VALUE,
//                            3.0 / 2.0
//                        )!!
//
//                    val previewConfigVo =
//                        // 설정 객체 반환
//                        arrayListOf(
//                            CameraObj.PreviewConfigVo(
//                                chosenPreviewSurfaceSize,
//                                bindingMbr.cameraPreviewAutoFitTexture
//                            )
//                        )
//
//                    // 지원 사이즈 탐지
//                    val chosenAnalysisImageReaderSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            2,
//                            500 * 500,
//                            3.0 / 2.0
//                        )!!
//
//                    val analysisImageReaderConfigVo =
//                        // 설정 객체 반환
//                        CameraObj.ImageReaderConfigVo(
//                            chosenAnalysisImageReaderSurfaceSize,
//                            imageReaderCallback = { reader ->
//                                analyzeImage(reader)
//                            }
//                        )
//
//                    val chosenCaptureImageReaderSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            2,
//                            Long.MAX_VALUE,
//                            3.0 / 2.0
//                        )!!
//
//                    val captureImageReaderConfigVo =
//                        // 설정 객체 반환
//                        CameraObj.ImageReaderConfigVo(
//                            chosenCaptureImageReaderSurfaceSize,
//                            imageReaderCallback = { reader ->
//                                captureImage(reader)
//                            }
//                        )
//
//                    // (카메라 변수 설정)
//                    // (카메라 서페이스 설정)
//                    cameraObjMbr.setCameraOutputSurfaces(
//                        previewConfigVo,
//                        captureImageReaderConfigVo,
//                        null,
//                        analysisImageReaderConfigVo,
//                        onComplete = {
//                            // (카메라 리퀘스트 설정)
//                            cameraObjMbr.repeatingRequestOnTemplate(
//                                forPreview = true,
//                                forMediaRecorder = false,
//                                forAnalysisImageReader = true,
//                                onComplete = {
//
//                                },
//                                onError = {
//
//                                }
//                            )
//                        },
//                        onError = {
//
//                        }
//                    )
//                }
//            )
        } else {
            cameraObjMbr.stopRepeatingRequest(onCameraPause = {}, onError = {})
        }
    }

    override fun onDestroy() {
        super.onDestroy()

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
    }

    // (AndroidManifest.xml 에서 configChanges 에 설정된 요소에 변경 사항이 존재할 때 실행되는 콜백)
    // : 해당 이벤트 발생시 처리할 로직을 작성

    // 화면 회전시 복구를 위해 화면 방향별 뷰 변화 정도를 저장
    var galleryBtnPlusYMbr = 0 // 증가된 Y
    var recordOrCaptureBtnPlusYMbr = 0
    var cameraChangeBtnPlusYMbr = 0
    var captureModeBtnMbr = 0
    var recordModeBtnMbr = 0

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // (화면 방향에 따른 뷰 마진 설정)
        // : 소프트 네비게이션을 투명으로 뒀기에, 화면 극단에 위치한 뷰들은 바와 겹치지 않도록 방향에 따라 마진을 설정해야함
        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display!!.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        when (deviceOrientation) {
            Surface.ROTATION_0 -> {
                bindingMbr.galleryBtn.y =
                    bindingMbr.galleryBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.recordOrCaptureBtn.y =
                    bindingMbr.recordOrCaptureBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(
                        this
                    )
                bindingMbr.cameraChangeBtn.y =
                    bindingMbr.cameraChangeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.recordModeBtn.y =
                    bindingMbr.recordModeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.captureModeBtn.y =
                    bindingMbr.captureModeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)

                galleryBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                recordOrCaptureBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                cameraChangeBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                captureModeBtnMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                recordModeBtnMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
            }
            else -> {
                bindingMbr.galleryBtn.y = bindingMbr.galleryBtn.y - galleryBtnPlusYMbr
                bindingMbr.recordOrCaptureBtn.y =
                    bindingMbr.recordOrCaptureBtn.y - recordOrCaptureBtnPlusYMbr
                bindingMbr.cameraChangeBtn.y =
                    bindingMbr.cameraChangeBtn.y - cameraChangeBtnPlusYMbr
                bindingMbr.recordModeBtn.y = bindingMbr.recordModeBtn.y - recordModeBtnMbr
                bindingMbr.captureModeBtn.y = bindingMbr.captureModeBtn.y - captureModeBtnMbr

                galleryBtnPlusYMbr = 0
                recordOrCaptureBtnPlusYMbr = 0
                cameraChangeBtnPlusYMbr = 0
                recordModeBtnMbr = 0
                captureModeBtnMbr = 0
            }
        }
    }

    override fun onBackPressed() {
        imageProcessingPauseMbr = true
        shownDialogInfoVOMbr = DialogBinaryChoose.DialogInfoVO(
            true,
            "카메라 종료",
            "카메라를 종료하시겠습니까?",
            "종료",
            "취소",
            onPosBtnClicked = {
                shownDialogInfoVOMbr = null

                finish()
            },
            onNegBtnClicked = {
                shownDialogInfoVOMbr = null

                imageProcessingPauseMbr = false
            },
            onCanceled = {
                shownDialogInfoVOMbr = null

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

        // 어뎁터 셋 객체 생성 (어뎁터 내부 데이터가 포함된 객체)
        adapterSetMbr = ActivityBasicCamera2ApiSampleAdapterSet()

        // SPW 객체 생성
        classSpwMbr = ActivityBasicCamera2ApiSampleSpw(application)
        currentLoginSessionInfoSpwMbr = CurrentLoginSessionInfoSpw(application)

        // 권한 요청 객체 생성
        permissionRequestMbr =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionRequestCallbackMbr?.let { it1 -> it1(it) }
            }

        // ActivityResultLauncher 생성
        resultLauncherMbr = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            resultLauncherCallbackMbr?.let { it1 -> it1(it) }
        }

        // (최초 사용 카메라 객체 생성)
        // 사용 가능한 카메라 리스트
        val availableCameraIdList = Camera2Obj.getAllAvailableCameraIdList(this)

        if (availableCameraIdList.size == 0) {
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
            return
        }

        // 초기 카메라 아이디 선정
        val cameraId: String =
            if (classSpwMbr.currentCameraId != null) { // 기존 아이디가 있을 때

                // 기존 아이디가 현재 지원 카메라 리스트에 있는지를 확인
                val lastCameraIdIdx = availableCameraIdList.indexOfFirst {
                    it == classSpwMbr.currentCameraId
                }

                if (lastCameraIdIdx != -1) {
                    // 기존 아이디가 제공 아이디 리스트에 있을 때
                    classSpwMbr.currentCameraId!!
                } else {
                    // 기존 아이디가 제공 아이디 리스트에 없을 때 = 0번 카메라를 먼저 적용
                    availableCameraIdList[0]
                }
            } else { // 기존 아이디가 없을 때 = 0번 카메라를 먼저 적용
                availableCameraIdList[0]
            }

        val cameraObj = Camera2Obj.getInstance(
            this,
            cameraId,
            onCameraDisconnectedAndClearCamera = {
                // todo 디버그 후 결정
            }
        )

        if (cameraObj == null) {
            // 위에서 적합성 검증을 끝냈지만 찰나의 에러에 대비
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
            return
        }

        cameraObjMbr = cameraObj
        classSpwMbr.currentCameraId = cameraId

        // (랜더 스크립트 객체 생성)
        renderScriptMbr = RenderScript.create(application)
        scriptIntrinsicYuvToRGBMbr =
            ScriptIntrinsicYuvToRGB.create(
                renderScriptMbr,
                Element.U8_4(renderScriptMbr)
            )
        scriptCRotatorMbr = ScriptC_rotator(renderScriptMbr)

        scriptCCropMbr = ScriptC_crop(renderScriptMbr)

        scriptIntrinsicResizeMbr = ScriptIntrinsicResize.create(renderScriptMbr)

    }

    // (초기 뷰 설정)
    // : 뷰 리스너 바인딩, 초기 뷰 사이즈, 위치 조정 등
    private fun onCreateInitView() {
        // (화면 자동꺼짐 방지 플래그)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (화면 방향에 따른 뷰 마진 설정)
        // : 소프트 네비게이션을 투명으로 뒀기에, 화면 극단에 위치한 뷰들은 바와 겹치지 않도록 방향에 따라 마진을 설정해야함
        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display!!.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        when (deviceOrientation) {
            Surface.ROTATION_0 -> {
                bindingMbr.galleryBtn.y =
                    bindingMbr.galleryBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.recordOrCaptureBtn.y =
                    bindingMbr.recordOrCaptureBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(
                        this
                    )
                bindingMbr.cameraChangeBtn.y =
                    bindingMbr.cameraChangeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.recordModeBtn.y =
                    bindingMbr.recordModeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)
                bindingMbr.captureModeBtn.y =
                    bindingMbr.captureModeBtn.y - CustomUtil.getSoftNavigationBarHeightPixel(this)

                galleryBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                recordOrCaptureBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                cameraChangeBtnPlusYMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                captureModeBtnMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
                recordModeBtnMbr = -CustomUtil.getSoftNavigationBarHeightPixel(this)
            }
            else -> {
                bindingMbr.galleryBtn.y = bindingMbr.galleryBtn.y - galleryBtnPlusYMbr
                bindingMbr.recordOrCaptureBtn.y =
                    bindingMbr.recordOrCaptureBtn.y - recordOrCaptureBtnPlusYMbr
                bindingMbr.cameraChangeBtn.y =
                    bindingMbr.cameraChangeBtn.y - cameraChangeBtnPlusYMbr
                bindingMbr.recordModeBtn.y = bindingMbr.recordModeBtn.y - recordModeBtnMbr
                bindingMbr.captureModeBtn.y = bindingMbr.captureModeBtn.y - captureModeBtnMbr

                galleryBtnPlusYMbr = 0
                recordOrCaptureBtnPlusYMbr = 0
                cameraChangeBtnPlusYMbr = 0
                recordModeBtnMbr = 0
                captureModeBtnMbr = 0
            }
        }

        // (카메라 지원에 따른 모드 버튼 처리)
        if (cameraObjMbr.isThisCameraSupportedForMode(
                if (isCameraImageAnalysisMbr) {// preview, capture, analysis 가능
                    13
                } else {// preview, capture 가능
                    6
                }
            )
        ) { // 사진모드
            bindingMbr.captureModeBtn.visibility = View.VISIBLE
        } else {
            bindingMbr.captureModeBtn.visibility = View.GONE
        }

        if (cameraObjMbr.isThisCameraSupportedForMode(
                if (isCameraImageAnalysisMbr) { // preview, mediaRecord, analysis 가능
                    14
                } else { // preview, mediaRecord 가능
                    7
                }
            )
        ) { // 동영상 모드
            bindingMbr.recordModeBtn.visibility = View.VISIBLE
        } else {
            bindingMbr.recordModeBtn.visibility = View.GONE
        }

        // 카메라 모드를 모두 지원하지 않을 때
        if (!cameraObjMbr.isThisCameraSupportedForMode(
                if (isCameraImageAnalysisMbr) {// preview, capture, analysis 가능
                    13
                } else {// preview, capture 가능
                    6
                }
            ) &&
            !cameraObjMbr.isThisCameraSupportedForMode(
                if (isCameraImageAnalysisMbr) { // preview, mediaRecord, analysis 가능
                    14
                } else { // preview, mediaRecord 가능
                    7
                }
            )
        ) {
            // 위에서 적합성 검증을 끝냈지만 찰나의 에러에 대비
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
            return
        }

        if (classSpwMbr.currentCameraMode == -1) { // 기존 설정이 없을 때
            if (bindingMbr.captureModeBtn.visibility == View.VISIBLE) { // 사진모드 가능
                // 사진 모드 설정
                currentCameraModeMbr = 1
                classSpwMbr.currentCameraMode = 1

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_capture)

            } else if (bindingMbr.recordModeBtn.visibility == View.VISIBLE) { // 동영상 모드 가능
                // 동영상 모드 설정
                currentCameraModeMbr = 2
                classSpwMbr.currentCameraMode = 2

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_record)

            }
        } else if (classSpwMbr.currentCameraMode == 1) { // 기존 설정이 사진 모드
            if (bindingMbr.captureModeBtn.visibility == View.VISIBLE) { // 사진모드 가능
                // 사진 모드 설정
                currentCameraModeMbr = 1

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_capture)

            } else if (bindingMbr.recordModeBtn.visibility == View.VISIBLE) { // 동영상 모드 가능
                // 동영상 모드 설정
                currentCameraModeMbr = 2
                classSpwMbr.currentCameraMode = 2

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_record)

            }
        } else if (classSpwMbr.currentCameraMode == 2) { // 기존 설정이 동영상 모드
            if (bindingMbr.recordModeBtn.visibility == View.VISIBLE) { // 동영상 모드 가능
                // 동영상 모드 설정
                currentCameraModeMbr = 2

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_record)

            } else if (bindingMbr.captureModeBtn.visibility == View.VISIBLE) { // 사진모드 가능
                // 사진 모드 설정
                currentCameraModeMbr = 1
                classSpwMbr.currentCameraMode = 1

                bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
                bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
                bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_capture)

            }
        } else { // 코드 에러
            throw Exception("Code Error : 지원하지 않는 모드")
        }

        bindingMbr.captureModeBtn.setOnClickListener {
            if (currentCameraModeMbr == 1) {
                return@setOnClickListener
            }

            // 사진 모드 설정
            currentCameraModeMbr = 1
            classSpwMbr.currentCameraMode = 1

            bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
            bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
            bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_capture)

            // todo 기존 카메라 중단 onPause 와 동일
            // (초기 카메라 정보 설정 및 실행)
            getCameraConfig()
        }

        bindingMbr.recordModeBtn.setOnClickListener {
            if (currentCameraModeMbr == 2) {
                return@setOnClickListener
            }

            // 동영상 모드 설정
            currentCameraModeMbr = 2
            classSpwMbr.currentCameraMode = 2

            bindingMbr.captureModeBtn.setTextColor(Color.parseColor("#FFFFFFFF"))
            bindingMbr.recordModeBtn.setTextColor(Color.parseColor("#FF3D7DDC"))
            bindingMbr.recordOrCaptureBtn.setImageResource(R.drawable.img_layout_activity_basic_camera2_api_sample_record)

            // todo 기존 카메라 중단 onPause 와 동일
            // (초기 카메라 정보 설정 및 실행)
            getCameraConfig()
        }

        bindingMbr.recordOrCaptureBtn.setOnClickListener {
            if (currentCameraModeMbr == 1) { // 사진 캡쳐 모드
                // todo
                cameraObjMbr.captureRequest(null, onError = {})

            } else { // 동영상 촬영 모드
                // todo
                // todo 촬영중 모드 변경 버튼 숨기기
                //     pause 시에는 영상 멈추고 새로운 서페이스
                //

//                cameraObjMbr.repeatingRequestOnTemplate(
//                    forPreview = true,
//                    forMediaRecorder = true,
//                    forAnalysisImageReader = false,
//                    onComplete = {
//                    },
//                    onError = {
//
//                    }
//                )
            }
        }

        // todo : 각 버튼 클릭 리스너 처리


        cameraObjMbr.setCameraPinchZoomTouchListener(bindingMbr.cameraPreviewAutoFitTexture)
        // 카메라 전환
//        bindingMbr.cameraChangeBtn.setOnClickListener {
//            val cameraInfoList = CameraObj.getAllSupportedCameraInfoList(this)
//            val cameraItemList = ArrayList<String>()
//
//            for (id in cameraInfoList) {
//                cameraItemList.add("Camera ${id.cameraId}")
//            }
//
//            var checkedIdx = cameraInfoList.indexOfFirst {
//                it.cameraId == classSpwMbr.currentCameraId
//            }
//
//            if (checkedIdx == -1) {
//                checkedIdx = 0
//            }
//
//            imageProcessingPauseMbr = true
//            shownDialogInfoVOMbr =
//                DialogRadioButtonChoose.DialogInfoVO(
//                    isCancelable = true,
//                    title = "카메라 선택",
//                    contentMsg = null,
//                    radioButtonContentList = cameraItemList,
//                    checkedItemIdx = checkedIdx,
//                    cancelBtnTxt = null,
//                    onRadioItemClicked = {
//                    },
//                    onCancelBtnClicked = {
//                        shownDialogInfoVOMbr = null
//                        imageProcessingPauseMbr = false
//                    },
//                    onSelectBtnClicked = {
//                        val checkedCameraId = cameraInfoList[it].cameraId
//
//                        if (checkedCameraId != classSpwMbr.currentCameraId) {
//                            // 기존 저장 폴더 백업
//                            val videoFile =
//                                if (cameraObjMbr.mediaRecorderStatusCodeMbr == 3 || cameraObjMbr.mediaRecorderStatusCodeMbr == 4) {
//                                    cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File
//                                } else {
//                                    null
//                                }
//
//                            cameraObjMbr.destroyCameraObject {
//                                videoFile?.delete()
//
//                                val cameraObj = CameraObj.getInstance(
//                                    this,
//                                    checkedCameraId,
//                                    onCameraDisconnectedAndClearCamera = {
//
//                                    }
//                                )!!
//
//                                cameraObjMbr = cameraObj
//
//                                // 핀치 줌 설정
//                                cameraObjMbr.setCameraPinchZoomTouchListener(bindingMbr.cameraPreviewAutoFitTexture)
//
//                                classSpwMbr.currentCameraId =
//                                    checkedCameraId
//
//                                // (카메라 실행)
//                                // 지원 사이즈 탐지
//                                val chosenPreviewSurfaceSize =
//                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
//                                        1,
//                                        Long.MAX_VALUE,
//                                        3.0 / 2.0
//                                    )!!
//
//                                val previewConfigVo =
//                                    // 설정 객체 반환
//                                    arrayListOf(
//                                        CameraObj.PreviewConfigVo(
//                                            chosenPreviewSurfaceSize,
//                                            bindingMbr.cameraPreviewAutoFitTexture
//                                        )
//                                    )
//
//                                // 지원 사이즈 탐지
//                                val chosenImageReaderSurfaceSize =
//                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
//                                        2,
//                                        500 * 500,
//                                        3.0 / 2.0
//                                    )!!
//
//                                val analysisImageReaderConfigVo =
//                                    // 설정 객체 반환
//                                    CameraObj.ImageReaderConfigVo(
//                                        chosenImageReaderSurfaceSize,
//                                        imageReaderCallback = { reader ->
//                                            analyzeImage(reader)
//                                        }
//                                    )
//
//                                val chosenCaptureImageReaderSurfaceSize =
//                                    cameraObjMbr.getNearestSupportedCameraOutputSize(
//                                        2,
//                                        Long.MAX_VALUE,
//                                        3.0 / 2.0
//                                    )!!
//
//                                val captureImageReaderConfigVo =
//                                    // 설정 객체 반환
//                                    CameraObj.ImageReaderConfigVo(
//                                        chosenCaptureImageReaderSurfaceSize,
//                                        imageReaderCallback = { reader ->
//                                            captureImage(reader)
//                                        }
//                                    )
//
//                                // (카메라 변수 설정)
//                                // todo : 세팅 함수는 그대로 두되, setCameraRequest 에서 한번에 설정하도록
//                                // 떨림 보정
//                                cameraObjMbr.setCameraStabilization(
//                                    true,
//                                    onComplete = {},
//                                    onError = {})
//
//                                // (카메라 서페이스 설정)
//                                cameraObjMbr.setCameraOutputSurfaces(
//                                    previewConfigVo,
//                                    captureImageReaderConfigVo,
//                                    null,
//                                    analysisImageReaderConfigVo,
//                                    onComplete = {
//                                        // (카메라 리퀘스트 설정)
//                                        cameraObjMbr.repeatingRequestOnTemplate(
//                                            forPreview = true,
//                                            forMediaRecorder = false,
//                                            forAnalysisImageReader = true,
//                                            onComplete = {
//
//                                            },
//                                            onError = {
//
//                                            }
//                                        )
//                                    },
//                                    onError = {
//
//                                    }
//                                )
//                            }
//                        }
//                        shownDialogInfoVOMbr = null
//                        imageProcessingPauseMbr = false
//                    },
//                    onCanceled = {
//                        shownDialogInfoVOMbr = null
//                        imageProcessingPauseMbr = false
//                    }
//                )
//        }
//
//        // todo 녹화중 화면 효과
//        // todo 방해 금지 모드로 회전 및 pause 가 불가능하도록 처리
//        bindingMbr.btn1.setOnClickListener {
//            // 처리 완료까지 중복 클릭 방지
//            bindingMbr.btn1.isEnabled = false
//
//            if (cameraObjMbr.mediaRecorderStatusCodeMbr != 3 && cameraObjMbr.mediaRecorderStatusCodeMbr != 4) { // 현재 레코딩 중이 아닐 때
//                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
//
//                cameraObjMbr.unsetCameraOutputSurfaces {
//                    // 지원 사이즈 탐지
//                    val chosenPreviewSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            1,
//                            Long.MAX_VALUE,
//                            3.0 / 2.0
//                        )!!
//
//                    val previewConfigVo =
//                        // 설정 객체 반환
//                        arrayListOf(
//                            CameraObj.PreviewConfigVo(
//                                chosenPreviewSurfaceSize,
//                                bindingMbr.cameraPreviewAutoFitTexture
//                            )
//                        )
//
//                    // 지원 사이즈 탐지
//                    val chosenImageReaderSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            2,
//                            500 * 500,
//                            3.0 / 2.0
//                        )!!
//
//                    val analysisImageReaderConfigVo =
//                        // 설정 객체 반환
//                        CameraObj.ImageReaderConfigVo(
//                            chosenImageReaderSurfaceSize,
//                            imageReaderCallback = { reader ->
//                                analyzeImage(reader)
//                            }
//                        )
//
//                    // 지원 사이즈 탐지
//                    val chosenSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            3,
//                            Long.MAX_VALUE,
//                            3.0 / 2.0
//                        )!!
//
//                    val mediaRecorderConfigVo =
//                        // 설정 객체 반환
//                        CameraObj.MediaRecorderConfigVo(
//                            chosenSurfaceSize,
//                            File("${this.filesDir.absolutePath}/${System.currentTimeMillis()}.mp4"),
//                            true,
//                            Int.MAX_VALUE,
//                            Int.MAX_VALUE,
//                            Int.MAX_VALUE
//                        )
//
//                    val chosenCaptureImageReaderSurfaceSize =
//                        cameraObjMbr.getNearestSupportedCameraOutputSize(
//                            2,
//                            Long.MAX_VALUE,
//                            3.0 / 2.0
//                        )!!
//
//                    val captureImageReaderConfigVo =
//                        // 설정 객체 반환
//                        CameraObj.ImageReaderConfigVo(
//                            chosenCaptureImageReaderSurfaceSize,
//                            imageReaderCallback = { reader ->
//                                captureImage(reader)
//                            }
//                        )
//
//                    // (카메라 변수 설정)
//                    // 떨림 보정
//                    cameraObjMbr.setCameraStabilization(
//                        true,
//                        onComplete = {},
//                        onError = {})
//
//                    // (카메라 서페이스 설정)
//                    cameraObjMbr.setCameraOutputSurfaces(
//                        previewConfigVo,
//                        captureImageReaderConfigVo,
//                        mediaRecorderConfigVo,
//                        null,
//                        onComplete = {
//                            // (카메라 리퀘스트 설정)
//                            cameraObjMbr.repeatingRequestOnTemplate(
//                                forPreview = true,
//                                forMediaRecorder = true,
//                                forAnalysisImageReader = false,
//                                onComplete = {ㅋㅇ
//                                },
//                                onError = {
//
//                                }
//                            )
//                        },
//                        onError = {
//                            runOnUiThread {
//                                bindingMbr.btn1.isEnabled = true
//                            }
//                        }
//                    )
//                }
//
//            } else { // 레코딩 중일때
//                // 화면 고정 풀기
//                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//
//                // 기존 저장 폴더 백업
//                val videoFile = cameraObjMbr.mediaRecorderConfigVoMbr!!.mediaRecordingMp4File
//
//                // 카메라 초기화
//                cameraObjMbr.unsetCameraOutputSurfaces(
//                    onComplete = {
//                        runOnUiThread {
//                            bindingMbr.btn1.isEnabled = true
//
//                            // (결과물 감상)
//                            val mediaPlayerIntent = Intent()
//                            mediaPlayerIntent.action = Intent.ACTION_VIEW
//                            mediaPlayerIntent.setDataAndType(
//                                FileProvider.getUriForFile(
//                                    this@ActivityBasicCamera2ApiSample,
//                                    "${BuildConfig.APPLICATION_ID}.provider",
//                                    videoFile
//                                ), MimeTypeMap.getSingleton()
//                                    .getMimeTypeFromExtension(videoFile.extension)
//                            )
//                            mediaPlayerIntent.flags =
//                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
//                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
//
//                            resultLauncherCallbackMbr = {
//                                videoFile.delete()
//                                finish()
//                            }
//                            resultLauncherMbr.launch(mediaPlayerIntent)
//                        }
//                    }
//                )
//            }
//        }
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
    }

    // (액티비티 진입 권한이 클리어 된 시점)
    // : 실질적인 액티비티 로직 실행구역
    private var doItAlreadyMbr = false
    private var currentUserUidMbr: String? = null // 유저 식별가능 정보 - null 이라면 비회원
    private fun allPermissionsGranted() {
        if (!doItAlreadyMbr) {
            // (권한이 충족된 onCreate)
            doItAlreadyMbr = true

            // (초기 데이터 수집)
            currentUserUidMbr = currentLoginSessionInfoSpwMbr.userUid
            refreshWholeScreenData(onComplete = {})

            // (초기 카메라 정보 설정 및 실행)
            getCameraConfig()

        } else {
            // (onResume - (권한이 충족된 onCreate))

            // (유저별 데이터 갱신)
            // : 유저 정보가 갱신된 상태에서 다시 현 액티비티로 복귀하면 자동으로 데이터를 다시 갱신합니다.
            val userUid = currentLoginSessionInfoSpwMbr.userUid
            if (userUid != currentUserUidMbr) { // 액티비티 유저와 세션 유저가 다를 때
                // 진입 플래그 변경
                currentUserUidMbr = userUid

                // (데이터 수집)
                refreshWholeScreenData(onComplete = {})
            }

            // (카메라 실행)
            // todo 카메라 재시작
            imageProcessingPauseMbr = false
            cameraObjMbr.repeatingRequestOnTemplate(
                forPreview = true,
                forMediaRecorder = false,
                forAnalysisImageReader = isCameraImageAnalysisMbr,
                onComplete = {

                },
                onError = {

                }
            )

        }

        // (onResume)
    }

    // 화면 데이터 갱신관련 세마포어
    private val screenDataSemaphoreMbr = Semaphore(1)

    // (화면 구성용 데이터를 가져오기)
    // : 네트워크 등 레포지토리에서 데이터를 가져오고 이를 뷰에 반영
    //     onComplete = 네트워크 실패든 성공이든 데이터 요청 후 응답을 받아와 해당 상태에 따라 스크린 뷰 처리를 완료한 시점
    //     'c숫자' 로 표기된 부분은 원하는대로 커스텀
    private fun refreshWholeScreenData(onComplete: () -> Unit) {
        executorServiceMbr.execute {
            screenDataSemaphoreMbr.acquire()

            runOnUiThread {
                // (c1. 리스트 초기화)

                // (c2. 로더 추가)
            }

            // (스레드 합류 객체 생성)
            // : 헤더, 푸터, 아이템 리스트의 각 데이터를 비동기적으로 요청했을 때, 그 합류용으로 사용되는 객체
            //     numberOfThreadsBeingJoinedMbr 에 비동기 처리 개수를 적고,
            //     각 처리 완료시마다 threadComplete 를 호출하면 됨
            val threadConfluenceObj =
                ThreadConfluenceObj(
                    3,
                    onComplete = {
                        screenDataSemaphoreMbr.release()
                        onComplete()
                    }
                )

            // (정보 요청 콜백)
            // 아이템 리스트
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 itemList 는 null
            val getItemListOnComplete: (statusCode: Int, itemList: ArrayList<AbstractProwdRecyclerViewAdapter.AdapterItemAbstractVO>?) -> Unit =
                { statusCode, itemList ->
                    runOnUiThread {
                        // (c3. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            if (itemList!!.isEmpty()) { // 받아온 리스트가 비어있을 때
                                // (c4. 빈 리스트 처리)

                                threadConfluenceObj.threadComplete()
                            } else {
                                runOnUiThread {
                                    // (c5. 받아온 아이템 추가)

                                    // (c6. 스크롤을 가장 앞으로 이동)
                                }

                                threadConfluenceObj.threadComplete()
                            }
                        }
                        -1 -> { // 네트워크 에러
                            // (c7. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러그외 서버 에러
                            // (c8. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 헤더 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getHeaderItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterHeaderAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c9. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> { // 완료
                            runOnUiThread {
                                // (c10. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c11. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c12. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // 푸터 아이템
            // : statusCode
            //     서버 반환 상태값. 1이라면 정상동작, -1 이라면 타임아웃, 2 이상 값들 중 서버에서 정한 상태값 처리, 그외엔 서버 에러
            //     1 이외의 상태값에서 item 은 null
            val getFooterItemOnComplete: (statusCode: Int, item: AbstractProwdRecyclerViewAdapter.AdapterFooterAbstractVO?) -> Unit =
                { statusCode, item ->
                    runOnUiThread {
                        // (c13. 로더 제거)
                    }

                    when (statusCode) {
                        1 -> {// 완료
                            runOnUiThread {
                                // (c14. 받아온 아이템 추가)
                            }

                            threadConfluenceObj.threadComplete()
                        }
                        -1 -> { // 네트워크 에러
                            // (c15. 네트워크 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                        else -> { // 그외 서버 에러
                            // (c16. 그외 서버 에러 처리)

                            threadConfluenceObj.threadComplete()
                        }
                    }
                }

            // (네트워크 요청)
            // (c17. 아이템 리스트 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getItemListOnComplete(-2, null)
            }

            // (c18. 헤더 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getHeaderItemOnComplete(-2, null)
            }

            // (c19. 푸터 데이터 가져오기)
            // : lastItemUid 등의 인자값을 네트워크 요청으로 넣어주고 데이터를 받아와서 onComplete 실행
            //     데이터 요청 API 는 정렬기준, 마지막 uid, 요청 아이템 개수 등을 입력하여 데이터 리스트를 반환받음
            executorServiceMbr.execute {
                getFooterItemOnComplete(-2, null)
            }

            // (c20. 그외 스크린 데이터 가져오기)

        }
    }


    // todo 다시보기
    // (초기 카메라 정보 설정 및 실행)
    // : DB 에 저장된 기존 카메라 정보 확인
    //     카메라 앱 구조는 ID 안에 모드가 있고, 모드 안에 설정 정보가 있음
    //     여기까지 오면 "초기 카메라 id", "카메라 모드"는 결정된 상황
    private fun getCameraConfig() {
        executorServiceMbr.execute {
            // 기존 저장된 카메라 설정 가져오기
            val cameraInfo =
                repositorySetMbr.databaseRoomMbr.appDatabaseMbr.activityBasicCamera2ApiSampleCameraConfigTableDao()
                    .getCameraModeConfig(
                        cameraObjMbr.cameraInfoVoMbr.cameraId,
                        currentCameraModeMbr
                    )

            if (cameraInfo == null) {
                // 모니터에서 가장 많이 사용되는 16:9 비율과 비슷한 크기를 먼저 선정
                val cameraOrientSupportedUnitSizeList =
                    cameraObjMbr.getSupportedCameraOrientSurfaceUnitSize(
                        if (currentCameraModeMbr == 1) { // 사진
                            if (isCameraImageAnalysisMbr) { // 이미지 분석
                                13
                            } else {
                                6
                            }
                        } else { // 동영상
                            if (isCameraImageAnalysisMbr) { // 이미지 분석
                                14
                            } else {
                                7
                            }
                        }
                    )

                val cameraOrientSupportedUnitSize = cameraObjMbr.getNearestSize(
                    cameraOrientSupportedUnitSizeList.toList(),
                    Long.MAX_VALUE,
                    16.0 / 9.0
                )

                // 실제로 선정된 비율값
                val selectedWhRatio =
                    cameraOrientSupportedUnitSize.width.toDouble() / cameraOrientSupportedUnitSize.height.toDouble()

                // 위 비율에 대한 지원 해상도 목록 선정
                val surfaceSizeList =
                    cameraObjMbr.getSizeListsForWhRatio(selectedWhRatio)
                // 가장 큰 해상도 선정
                val cameraOrientSupportedSize = if (currentCameraModeMbr == 1) {
                    cameraObjMbr.getNearestSize(
                        surfaceSizeList.captureImageReaderSizeList,
                        Long.MAX_VALUE,
                        selectedWhRatio
                    )
                } else {
                    cameraObjMbr.getNearestSize(
                        surfaceSizeList.mediaRecorderSizeList,
                        Long.MAX_VALUE,
                        selectedWhRatio
                    )
                }

                // 화면 반영
                runOnUiThread {
                    bindingMbr.flashModeValue.text = "off"
                    bindingMbr.timerValue.text = "0"
                    bindingMbr.surfaceRatioValue.text =
                        if (cameraObjMbr.cameraSensorOrientationAndDeviceAreSameWh()) {
                            "${cameraOrientSupportedUnitSize.width}:${cameraOrientSupportedUnitSize.height}"
                        } else {
                            "${cameraOrientSupportedUnitSize.height}:${cameraOrientSupportedUnitSize.width}"
                        }
                    bindingMbr.surfaceSizeValue.text =
                        if (cameraObjMbr.cameraSensorOrientationAndDeviceAreSameWh()) {
                            "${cameraOrientSupportedSize.width}:${cameraOrientSupportedSize.height}"
                        } else {
                            "${cameraOrientSupportedSize.height}:${cameraOrientSupportedSize.width}"
                        }
                }

                repositorySetMbr.databaseRoomMbr.appDatabaseMbr.activityBasicCamera2ApiSampleCameraConfigTableDao()
                    .insert(
                        ActivityBasicCamera2ApiSampleCameraConfigTable.TableVo(
                            cameraObjMbr.cameraInfoVoMbr.cameraId,
                            currentCameraModeMbr,
                            0, // 기본 플래시 0
                            0, // 기본 타이머 0
                            "${cameraOrientSupportedUnitSize.width}:${cameraOrientSupportedUnitSize.height}",
                            "${cameraOrientSupportedSize.width}:${cameraOrientSupportedSize.height}"
                        )
                    )

                flashModeMbr = 0
                timerMbr = 0
                cameraOrientSurfaceSizeMbr = cameraOrientSupportedSize

                startCamera()
            } else { // 기존 저장 카메라 정보가 있을 때
                // (카메라 정보 검증)
                // 기존 플래시
                var flashMode = cameraInfo.flashMode
                // 플래시 검증
                flashMode =
                    if (flashMode != 0 &&  // 플래시 사용 설정 and
                        cameraObjMbr.cameraInfoVoMbr.flashSupported // 플래시 지원
                    ) {
                        flashMode
                    } else { // 플래시 비사용 설정 or 플래시 미지원
                        0
                    }

                // 기존 타이머 (검증 필요없음)
                val timer = cameraInfo.timerSec

                // 기존 단위 사이즈
                val cameraOrientSupportedUnitArray = cameraInfo.cameraOrientSurfaceRatio.split(":")
                var cameraOrientSupportedUnitSize = Size(
                    cameraOrientSupportedUnitArray[0].toInt(),
                    cameraOrientSupportedUnitArray[1].toInt()
                )
                // 단위 사이즈 검증
                val cameraOrientSupportedUnitSizeList =
                    cameraObjMbr.getSupportedCameraOrientSurfaceUnitSize(
                        if (currentCameraModeMbr == 1) { // 사진
                            if (isCameraImageAnalysisMbr) { // 이미지 분석
                                13
                            } else {
                                6
                            }
                        } else { // 동영상
                            if (isCameraImageAnalysisMbr) { // 이미지 분석
                                14
                            } else {
                                7
                            }
                        }
                    )
                val findSameUnitSizeIdx = cameraOrientSupportedUnitSizeList.indexOfFirst {
                    it.width == cameraOrientSupportedUnitSize.width &&
                            it.height == cameraOrientSupportedUnitSize.height
                }

                var cameraOrientSupportedSize: Size? = null
                if (findSameUnitSizeIdx == -1) { // 기존 단위 사이즈가 존재하지 않을 때
                    // 기존 해상도도 쓸모가 없어지기에 단위사이즈서부터 다시 생성

                    // 모니터에서 가장 많이 사용되는 16:9 비율과 비슷한 크기를 먼저 선정
                    val cameraOrientSupportedUnitSizeList1 =
                        cameraObjMbr.getSupportedCameraOrientSurfaceUnitSize(
                            if (currentCameraModeMbr == 1) { // 사진
                                if (isCameraImageAnalysisMbr) { // 이미지 분석
                                    13
                                } else {
                                    6
                                }
                            } else { // 동영상
                                if (isCameraImageAnalysisMbr) { // 이미지 분석
                                    14
                                } else {
                                    7
                                }
                            }
                        )

                    cameraOrientSupportedUnitSize = cameraObjMbr.getNearestSize(
                        cameraOrientSupportedUnitSizeList1.toList(),
                        Long.MAX_VALUE, 16.0 / 9.0
                    )

                    val selectedWhRatio =
                        cameraOrientSupportedUnitSize.width.toDouble() / cameraOrientSupportedUnitSize.height.toDouble()

                    // 위 비율에 대한 지원 해상도 목록 선정
                    val surfaceSizeList = cameraObjMbr.getSizeListsForWhRatio(selectedWhRatio)
                    // 가장 큰 해상도 선정
                    cameraOrientSupportedSize = if (currentCameraModeMbr == 1) {
                        cameraObjMbr.getNearestSize(
                            surfaceSizeList.captureImageReaderSizeList,
                            Long.MAX_VALUE,
                            selectedWhRatio
                        )
                    } else {
                        cameraObjMbr.getNearestSize(
                            surfaceSizeList.mediaRecorderSizeList,
                            Long.MAX_VALUE,
                            selectedWhRatio
                        )
                    }

                } else { // 기존 단위 사이즈가 실존할 때
                    // 기존 해상도
                    val cameraOrientSupportedSizeArray =
                        cameraInfo.cameraOrientSurfaceSize.split(":")
                    cameraOrientSupportedSize = Size(
                        cameraOrientSupportedSizeArray[0].toInt(),
                        cameraOrientSupportedSizeArray[1].toInt()
                    )

                    // 해상도 존재 검증
                    val surfaceSizeLists =
                        cameraObjMbr.getSizeListsForWhRatio(
                            cameraOrientSupportedUnitSize.width.toDouble() /
                                    cameraOrientSupportedUnitSize.height.toDouble()
                        )

                    val surfaceSizeList = if (currentCameraModeMbr == 1) {
                        surfaceSizeLists.captureImageReaderSizeList
                    } else {
                        surfaceSizeLists.mediaRecorderSizeList
                    }

                    val findSameSizeIdx = surfaceSizeList.indexOfFirst {
                        it.width == cameraOrientSupportedSize!!.width &&
                                it.height == cameraOrientSupportedSize!!.height
                    }

                    if (findSameSizeIdx == -1) { // 동일 해상도를 찾지 못했을 때
                        // 존재하는 리스트에서 가장 큰 해상도를 선택
                        cameraOrientSupportedSize = cameraObjMbr.getNearestSize(
                            surfaceSizeList,
                            Long.MAX_VALUE,
                            cameraOrientSupportedUnitSize.width.toDouble() /
                                    cameraOrientSupportedUnitSize.height.toDouble()
                        )
                    }
                }

                runOnUiThread {
                    bindingMbr.flashModeValue.text = when (flashMode) {
                        0 -> "off"
                        1 -> "on"
                        2 -> "always"
                        else -> throw Exception("flashMode parameter error")
                    }
                    bindingMbr.timerValue.text = timer.toString()
                    bindingMbr.surfaceRatioValue.text =
                        if (cameraObjMbr.cameraSensorOrientationAndDeviceAreSameWh()) {
                            "${cameraOrientSupportedUnitSize.width}:${cameraOrientSupportedUnitSize.height}"
                        } else {
                            "${cameraOrientSupportedUnitSize.height}:${cameraOrientSupportedUnitSize.width}"
                        }
                    bindingMbr.surfaceSizeValue.text =
                        if (cameraObjMbr.cameraSensorOrientationAndDeviceAreSameWh()) {
                            "${cameraOrientSupportedSize.width}:${cameraOrientSupportedSize.height}"
                        } else {
                            "${cameraOrientSupportedSize.height}:${cameraOrientSupportedSize.width}"
                        }
                }

                repositorySetMbr.databaseRoomMbr.appDatabaseMbr.activityBasicCamera2ApiSampleCameraConfigTableDao()
                    .insert(
                        ActivityBasicCamera2ApiSampleCameraConfigTable.TableVo(
                            cameraObjMbr.cameraInfoVoMbr.cameraId,
                            currentCameraModeMbr,
                            flashMode,
                            timer,
                            "${cameraOrientSupportedUnitSize.width}:${cameraOrientSupportedUnitSize.height}",
                            "${cameraOrientSupportedSize.width}:${cameraOrientSupportedSize.height}"
                        )
                    )

                flashModeMbr = flashMode
                timerMbr = timer
                cameraOrientSurfaceSizeMbr = cameraOrientSupportedSize

                startCamera()
            }

        }
    }

    // (카메라 실행 함수)
    // : 여기까지 오면 카메라 실행에 필요한 정보들이 결정된 상태
    //     멤버변수에 존재하는
    //     카메라 id : cameraObjMbr.cameraInfoVoMbr.cameraId,
    //     카메라 mode : currentCameraModeMbr,
    //     플래시 모드 : flashModeMbr,
    //     타이머 : timerMbr,
    //     카메라 사이즈 : cameraOrientSupportedSize
    //     정보를 토대로 실행
    private fun startCamera() {
        val previewConfigVo =
            // 설정 객체 반환
            arrayListOf(
                Camera2Obj.PreviewConfigVo(
                    cameraOrientSurfaceSizeMbr!!,
                    bindingMbr.cameraPreviewAutoFitTexture
                )
            )

        val captureImageReaderConfigVo = if (currentCameraModeMbr == 1) { // 사진
            Camera2Obj.ImageReaderConfigVo(
                cameraOrientSurfaceSizeMbr!!,
                imageReaderCallback = { reader ->
                    captureImage(reader)
                }
            )
        } else { // 동영상
            null
        }

        // todo
        val mediaRecorderConfigVo = if (currentCameraModeMbr == 1) { // 사진
            null
        } else { // 동영상
            null
        }

        val analysisImageReaderConfigVo = if (isCameraImageAnalysisMbr) {
            Camera2Obj.ImageReaderConfigVo(
                cameraOrientSurfaceSizeMbr!!,
                imageReaderCallback = { reader ->
                    analyzeImage(reader)
                }
            )
        } else {
            null
        }


        cameraObjMbr.setCameraOutputSurfaces(
            previewConfigVo,
            captureImageReaderConfigVo,
            mediaRecorderConfigVo,
            analysisImageReaderConfigVo,
            onComplete = {
                // (카메라 리퀘스트 설정)
                cameraObjMbr.repeatingRequestOnTemplate(
                    forPreview = true,
                    forMediaRecorder = currentCameraModeMbr == 2,
                    forAnalysisImageReader = isCameraImageAnalysisMbr,
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
            executorServiceMbr?.execute {

                // 조기 종료 확인
                asyncImageProcessingOnProgressSemaphoreMbr.acquire()
                if (asyncImageProcessingOnProgressMbr || // 현재 비동기 이미지 프로세싱 중일 때
                    cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                    imageProcessingPauseMbr || // imageProcessing 정지 신호
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

    // (카메라 이미지 실시간 처리 콜백)
    // 카메라에서 이미지 프레임을 받아올 때마다 이것이 실행됨
    private fun captureImage(reader: ImageReader) {
        try {
            val file = File("${this.filesDir.absolutePath}/${System.currentTimeMillis()}.jpg")

            // 프레임 이미지 객체
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // 조기 종료 플래그
            if (cameraObjMbr.cameraStatusCodeMbr != 2 || // repeating 상태가 아닐 경우
                isDestroyed // 액티비티 자체가 종료
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
                if (!isDestroyed) {
                    Glide.with(this)
                        .load(file)
                        .transform(FitCenter())
                        .into(bindingMbr.captureImg)
                }

                executorServiceMbr?.execute {
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
    // (클래스 비휘발 저장 객체)
    class ActivityBasicCamera2ApiSampleSpw(application: Application) {
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

        var currentCameraMode: Int
            get() {
                return spMbr.getInt(
                    "currentCameraMode",
                    -1
                )
            }
            set(value) {
                with(spMbr.edit()) {
                    putInt(
                        "currentCameraMode",
                        value
                    )
                    apply()
                }
            }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>

    }

    // (액티비티 내 사용 어뎁터 모음)
    // : 액티비티 내 사용할 어뎁터가 있다면 본문에 클래스 추가 후 인자로 해당 클래스의 인스턴스를 받도록 하기
    class ActivityBasicCamera2ApiSampleAdapterSet {
        // 어뎁터 #1

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