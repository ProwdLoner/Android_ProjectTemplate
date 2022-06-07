package com.example.prowd_android_template.activity_set.activity_basic_camera2_api_sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.Size
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
import com.example.prowd_android_template.custom_view.DialogBinaryChoose
import com.example.prowd_android_template.custom_view.DialogConfirm
import com.example.prowd_android_template.custom_view.DialogProgressLoading
import com.example.prowd_android_template.databinding.ActivityBasicCamera2ApiSampleBinding
import com.example.prowd_android_template.util_class.CameraObj
import com.example.prowd_android_template.util_object.CameraUtil
import com.example.prowd_android_template.util_object.CustomUtil
import com.example.prowd_android_template.util_object.RenderScriptUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


// todo : 카메라는 screen 회전을 막아둠 (= 카메라 정지를 막기 위하여.) 보다 세련된 방식을 찾기
// todo : onpause - onresume 빠르게 변환시 튕김 (세마포어를 전역으로 놓아서 실행해보기)
// todo : 바로 위 문제는, 전역변수로 두고, 이미지 리더 스레드 완전 종료 후 해제까지 확인하도록 할 것
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

    // 카메라 실행 객체
    var backCameraObjMbr: CameraObj? = null

    // (권한 요청 객체)
    lateinit var permissionRequestMbr: ActivityResultLauncher<Array<String>>
    var permissionRequestCallbackMbr: (((MutableMap<String, Boolean>) -> Unit))? = null

    // (ResultLauncher 객체)
    // 액티비티 이동 복귀 객체
    lateinit var resultLauncherMbr: ActivityResultLauncher<Intent>
    var resultLauncherCallbackMbr: ((ActivityResult) -> Unit)? = null

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

        if (viewModelMbr.isActivityPermissionClearMbr) {
            startCamera()
        }
    }

    override fun onPause() {
        isImageProcessingPause = true
        backCameraObjMbr?.clearCameraObject()

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
    private fun requestActivityPermission() {
        // 카메라 디바이스 사용 가능 여부 확인
        if (this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            permissionRequestCallbackMbr = { permissions ->
                // 카메라 권한
                val isGranted = permissions[Manifest.permission.CAMERA]!!
                val neverAskAgain =
                    !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

                if (isGranted) { // 권한 승인
                    viewModelMbr.isActivityPermissionClearMbr = true
                    startCamera()
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
                                            startCamera()
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
        } else {
            // 디바이스 장치에 카메라가 없는 상태
            viewModelMbr.confirmDialogInfoLiveDataMbr.value = DialogConfirm.DialogInfoVO(
                true,
                "장치 접근",
                "카메라를 사용할 수 없습니다.\n장치 상태를 확인해주세요.",
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
    }

    // 초기 멤버 객체 생성
    private fun createMemberObjects() {
        // 뷰 모델 객체 생성
        viewModelMbr = ViewModelProvider(this)[ActivityBasicCamera2ApiSampleViewModel::class.java]

        // 사용 카메라 객체 생성
        // 후방 카메라
        val backCameraId = CameraUtil.chooseCameraId(this, CameraCharacteristics.LENS_FACING_BACK)
        if (null != backCameraId) {
            backCameraObjMbr =
                CameraObj.getInstance(
                    this,
                    backCameraId,
                    viewModelMbr.cameraHandlerThreadMbr.handler!!
                )
        }

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
    private var videoFileMbr : File? = null
    private fun viewSetting() {
        bindingMbr.logContainer.y =
            bindingMbr.logContainer.y - CustomUtil.getNavigationBarHeightPixel(this)

        bindingMbr.recordBtn.y =
            bindingMbr.recordBtn.y - CustomUtil.getNavigationBarHeightPixel(this)

        bindingMbr.recordBtn.setOnClickListener {
            // todo
            if (backCameraObjMbr != null) {
                if (!(backCameraObjMbr!!.isRecordingMbr)) {
                    videoFileMbr = File(
                        this.filesDir,
                        "VID_${
                            SimpleDateFormat(
                                "yyyy_MM_dd_HH_mm_ss_SSS",
                                Locale.US
                            ).format(Date())
                        }.mp4"
                    )

                    // 녹화 모드 실행
                    // todo : 서페이스 설정과 분리
                    backCameraObjMbr?.startCameraSession(
                        arrayListOf(
                            CameraObj.PreviewConfigVo(
                                Size(4000, 3000),
                                bindingMbr.cameraPreviewAutoFitTexture
                            )
                        ),
                        CameraObj.ImageReaderConfigVo(
                            Size(4000, 3000),
                            viewModelMbr.imageReaderHandlerThreadMbr.handler!!,
                            imageReaderCallback = { reader ->
                                processImage(reader)
                            }
                        ),
                        CameraObj.MediaRecorderConfigVo(
                            Size(4000, 3000),
                            videoFileMbr!!.absolutePath,
                            false
                        ),
                        onCameraSessionStarted = {

                        },
                        onCameraDisconnected = {

                        },
                        onError = {

                        }
                    )
                } else {
                    // 녹화 모드 종료
                    backCameraObjMbr?.startCameraSession(
                        arrayListOf(
                            CameraObj.PreviewConfigVo(
                                Size(4000, 3000),
                                bindingMbr.cameraPreviewAutoFitTexture
                            )
                        ),
                        CameraObj.ImageReaderConfigVo(
                            Size(4000, 3000),
                            viewModelMbr.imageReaderHandlerThreadMbr.handler!!,
                            imageReaderCallback = { reader ->
                                processImage(reader)
                            }
                        ),
                        null,
                        onCameraSessionStarted = {
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                type = MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(videoFileMbr!!.extension)
                                val authority = "${BuildConfig.APPLICATION_ID}.provider"
                                data = FileProvider.getUriForFile(it.context, authority, videoFileMbr!!)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                            })
                        },
                        onCameraDisconnected = {

                        },
                        onError = {

                        }
                    )
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
    }

    private fun startCamera() {
        // 카메라 실행
        isImageProcessingPause = false

        // (카메라 실행)
        // 카메라 세션 실행
        backCameraObjMbr?.startCameraSession(
            arrayListOf(
                CameraObj.PreviewConfigVo(
                    Size(4000, 3000),
                    bindingMbr.cameraPreviewAutoFitTexture
                )
            ),
            CameraObj.ImageReaderConfigVo(
                Size(4000, 3000),
                viewModelMbr.imageReaderHandlerThreadMbr.handler!!,
                imageReaderCallback = { reader ->
                    processImage(reader)
                }
            ),
            null,
            onCameraSessionStarted = {

            },
            onCameraDisconnected = {

            },
            onError = {

            }
        )

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

    // (카메라 이미지 실시간 처리 콜백)
    private var isImageProcessingPause = false
    private fun processImage(reader: ImageReader) {
        try {
            val imageObj: Image = reader.acquireLatestImage() ?: return

            // yuv ByteArray 를 rgb Bitmap 으로 변환 (병렬처리)
            // 반환되는 비트맵 이미지는 카메라 센서 방향에 따라 정방향이 아닐 수 있음.
            yuvByteArrayToArgbBitmapAsync(
                imageObj,
                onConvertingComplete = { yuvByteArrayToArgbBitmapAsyncBitmap ->

                    runOnUiThread {
                        if (!isDestroyed) {
                            Glide.with(this)
                                .load(yuvByteArrayToArgbBitmapAsyncBitmap)
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
    // onConvertingComplete 콜백 반환값 : 변환 완료 Bitmap
    private var yuvByteArrayToArgbBitmapAsyncOnProgressMbr = false
    private fun yuvByteArrayToArgbBitmapAsync(
        imageObj: Image,
        onConvertingComplete: (Bitmap) -> Unit
    ) {
        // 병렬처리 플래그
        if (yuvByteArrayToArgbBitmapAsyncOnProgressMbr || // 작업중
            isImageProcessingPause || // 이미지 프로세싱 중지 상태
            isDestroyed // 액티비티 자체가 종료
        ) {
            // 현재 로테이팅 중 or 액티비티가 종료 or 이미지 수집이 완료
            imageObj.close()
            return
        }

        // (이미지 데이터 복사 = yuv to rgb bitmap 변환 로직 병렬처리를 위한 데이터 백업)
        // 최대한 빨리 imageObj 를 닫기 위하여(= 프레임을 다음으로 넘기기 위하여) imageObj 정보를 ByteArray 로 복사하여 사용
        val imgWidth: Int = imageObj.width
        val imgHeight: Int = imageObj.height

        val yuvByteArray = RenderScriptUtil.yuv420888ImageToByteArray(imageObj)

        // 이미지 데이터가 복사되어 image 객체 해제
        imageObj.close()

        if (yuvByteArray == null) {
            return
        }

        yuvByteArrayToArgbBitmapAsyncOnProgressMbr = true

        viewModelMbr.executorServiceMbr?.execute {
            // 이미지 변환 타이머 스타트
            val logicStartTime = SystemClock.elapsedRealtime()

            // (YUV420 Image to ARGB8888 Bitmap)
            // RenderScript 사용
            val bitmap =
                RenderScriptUtil.yuv420888ToARgb8888BitmapIntrinsic(
                    viewModelMbr.renderScriptMbr,
                    viewModelMbr.scriptIntrinsicYuvToRGBMbr,
                    imgWidth,
                    imgHeight,
                    yuvByteArray
                )

            // 이미지 변환 작업 소요 시간 표시
            runOnUiThread {
                bindingMbr.yuvToRgbMs.text =
                    (SystemClock.elapsedRealtime() - logicStartTime).toString()
            }

            yuvByteArrayToArgbBitmapAsyncOnProgressMbr = false

            onConvertingComplete(bitmap)
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
}