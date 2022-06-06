package com.example.prowd_android_template.util_class

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.custom_view.AutoFitTextureView
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max

// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용
// Output Surface 에서 프리뷰는 복수 설정이 가능, 이미지 리더와 미디어 리코더는 1개만 설정 가능

// todo : 캡쳐, 설정 변경 함수
class CameraObj private constructor(
    private val parentActivityMbr: Activity,
    val cameraIdMbr: String,
    private val cameraHandlerMbr: Handler,
    private val cameraManagerMbr: CameraManager,
    private val cameraCharacteristicsMbr: CameraCharacteristics,
    private val streamConfigurationMapMbr: StreamConfigurationMap,
    var sensorOrientationMbr: Int = 0
) {
    // [카메라 기본 생성 객체] : 카메라 객체 생성시 생성
    // (스레드 풀)
    private var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (카메라 부산 데이터)
    private val cameraSessionSemaphoreMbr = Semaphore(1)
    private var cameraDeviceMbr: CameraDevice? = null

    // 카메라 출력 비율
    // 원하는 이미지 비율 (기본 비율은 1 : 1)
    // 0 이하라면 비율은 보지 않음
    // width / height 으로 계산
    private var cameraOutputSurfaceWhRatioMbr: Double = 1.0

    // 이미지 리더 세팅 부산물
    private var imageReaderMbr: ImageReader? = null
    private var imageReaderInfoVoMbr: ImageReaderInfoVo? = null

    // 미디어 리코더 세팅 부산물
    private var mediaRecorderSurfaceMbr: Surface? = null
    private var mediaRecorderInfoVOMbr: VideoRecorderInfoVO? = null
    private var mediaRecorderFpsMbr: Int? = null

    // 프리뷰 세팅 부산물
    private val previewSurfaceListMbr: ArrayList<Surface> = ArrayList()
    private val previewInfoListMbr: ArrayList<PreviewInfoVO> = ArrayList()

    // 카메라 리퀘스트 빌더
    private var captureRequestBuilderMbr: CaptureRequest.Builder? = null

    // 카메라 세션 객체
    private var cameraCaptureSessionMbr: CameraCaptureSession? = null


    // ---------------------------------------------------------------------------------------------
    // <스태틱 메소드 공간>
    companion object {
        // (객체 생성 함수 = 조건에 맞지 않으면 null 반환)
        // 조작하길 원하는 카메라 ID 를 설정하여 해당 카메라 정보를 생성
        fun getInstance(
            parentActivity: Activity,
            cameraId: String,
            cameraHandler: Handler,
        ): CameraObj? {
            // 카메라 총괄 빌더
            val cameraManager: CameraManager =
                parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // 카메라 Id에 해당하는 카메라 정보 가져오기
            val cameraCharacteristics =
                cameraManager.getCameraCharacteristics(cameraId)

            // 필수 정보 확인
            val streamConfigurationMap: StreamConfigurationMap? =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val sensorOrientationMbr: Int? =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            return if (null == streamConfigurationMap || null == sensorOrientationMbr) {
                // 필수 정보가 하나라도 없으면 null 반환
                null
            } else {
                return CameraObj(
                    parentActivity,
                    cameraId,
                    cameraHandler,
                    cameraManager,
                    cameraCharacteristics,
                    streamConfigurationMap,
                    sensorOrientationMbr
                )
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>

    // (카메라 세션 생성 함수)
    // 카메라 세션에서 사용할 서페이스를 설정하고, 카메라 세션을 생성하는 함수
    // 서페이스 재설정시 이것을 사용

    // API 에러 코드 :
    // 0 : 함수 파라미터 출력 서페이스가 하나도 입력되어 있지 않음
    // 1 : 카메라 장치가 탐지되지 않음
    // 2 : 카메라 권한이 없음
    // 3 : CameraDevice.StateCallback.ERROR_CAMERA_DISABLED (권한 등으로 인해 사용이 불가능)
    // 4 : CameraDevice.StateCallback.ERROR_CAMERA_IN_USE (해당 카메라가 이미 사용중)
    // 5 : CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE (시스템에서 허용한 카메라 동시 사용을 초과)
    // 6 : CameraDevice.StateCallback.ERROR_CAMERA_DEVICE (카메라 디바이스 자체적인 문제)
    // 7 : CameraDevice.StateCallback.ERROR_CAMERA_SERVICE (안드로이드 시스템 문제)
    // 8 : 생성된 서페이스가 존재하지 않음
    // 9 : 카메라 세션 생성 실패
    fun setCameraSurfaceAsync(
        cameraOutputSurfaceWhRatio: Double,
        imageReaderConfigVo: ImageReaderConfigVo?,
        videoRecorderConfigVo: VideoRecorderConfigVo?,
        previewConfigList: ArrayList<PreviewConfigVo>?,
        onCameraSessionReady: () -> Unit,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (카메라 상태 초기화)
            mediaRecorderSurfaceMbr?.release()
            mediaRecorderSurfaceMbr = null
            mediaRecorderFpsMbr = null
            mediaRecorderInfoVOMbr = null

            imageReaderMbr?.setOnImageAvailableListener(null, null)
            imageReaderMbr?.close()
            imageReaderMbr = null
            imageReaderInfoVoMbr = null

            cameraCaptureSessionMbr?.stopRepeating()
            cameraCaptureSessionMbr?.close()
            cameraCaptureSessionMbr = null

            captureRequestBuilderMbr = null

            previewInfoListMbr.clear()
            previewSurfaceListMbr.clear()

            cameraOutputSurfaceWhRatioMbr = 1.0

            // (파라미터 검사)
            if (imageReaderConfigVo == null &&
                videoRecorderConfigVo == null &&
                (previewConfigList == null ||
                        previewConfigList.isEmpty())
            ) {
                cameraSessionSemaphoreMbr.release()
                onError(0)
                return@execute
            }

            // (카메라 장치 검사)
            if (!parentActivityMbr.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                cameraSessionSemaphoreMbr.release()
                onError(1)
                return@execute
            }

            // (카메라 권한 검사)
            if (ActivityCompat.checkSelfPermission(
                    parentActivityMbr,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                cameraSessionSemaphoreMbr.release()
                onError(2)
                return@execute
            }

            // (카메라 디바이스 열기)
            openCameraDevice(
                onCameraDeviceReady = {
                    // (카메라 출력 비율 설정)
                    cameraOutputSurfaceWhRatioMbr = cameraOutputSurfaceWhRatio

                    // (이미지 리더 서페이스 설정)
                    if (imageReaderConfigVo != null) {
                        // 카메라 디바이스에서 지원되는 이미지 사이즈 리스트
                        val imageReaderSizes =
                            streamConfigurationMapMbr.getOutputSizes(ImageFormat.YUV_420_888)

                        if (imageReaderSizes.isNotEmpty()) {
                            // 원하는 사이즈에 유사한 사이즈를 선정
                            val chosenImageReaderSize = chooseCameraSize(
                                imageReaderSizes,
                                imageReaderConfigVo.preferredImageReaderArea,
                                cameraOutputSurfaceWhRatio
                            )

                            val imageReader = ImageReader.newInstance(
                                chosenImageReaderSize.width,
                                chosenImageReaderSize.height,
                                ImageFormat.YUV_420_888,
                                2
                            ).apply {
                                setOnImageAvailableListener(
                                    imageReaderConfigVo.imageReaderCallback,
                                    imageReaderConfigVo.imageReaderHandler
                                )
                            }

                            imageReaderMbr = imageReader
                            imageReaderInfoVoMbr = ImageReaderInfoVo(chosenImageReaderSize)
                        }
                    }

                    // (미디어 리코더 서페이스 설정)
                    if (videoRecorderConfigVo != null) {
                        // 코덱 서페이스 생성
                        mediaRecorderSurfaceMbr = MediaCodec.createPersistentInputSurface()

                        // (리코더 정보 가져오기)
                        // 원하는 사이즈에 유사한 사이즈를 선정
                        val chosenVideoSize = chooseCameraSize(
                            streamConfigurationMapMbr.getOutputSizes(MediaRecorder::class.java),
                            videoRecorderConfigVo.preferredImageReaderArea,
                            cameraOutputSurfaceWhRatioMbr
                        )

                        // 비디오 FPS
                        val spf = (streamConfigurationMapMbr.getOutputMinFrameDuration(
                            MediaRecorder::class.java,
                            chosenVideoSize
                        ) / 1_000_000_000.0)
                        mediaRecorderFpsMbr = if (spf > 0) (1.0 / spf).toInt() else 0

                        // (할당용 미디어 리코더 준비)
                        val preMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(parentActivityMbr)
                        } else {
                            MediaRecorder()
                        }

                        val tempFile = File(parentActivityMbr.filesDir, "temp.mp4")

                        preMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                        preMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        preMediaRecorder.setOutputFile(tempFile.absolutePath)
                        preMediaRecorder.setVideoSize(chosenVideoSize.width, chosenVideoSize.height)
                        preMediaRecorder.setVideoFrameRate(mediaRecorderFpsMbr!!)
                        preMediaRecorder.setVideoEncodingBitRate(chosenVideoSize.width * chosenVideoSize.height * mediaRecorderFpsMbr!!)
                        preMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                        preMediaRecorder.setInputSurface(mediaRecorderSurfaceMbr!!)

                        // 카메라 스트림 지원을 위해 미디어 리코더를 만들었다가 바로 해제
                        preMediaRecorder.prepare()
                        preMediaRecorder.release()

                        mediaRecorderInfoVOMbr =
                            VideoRecorderInfoVO(
                                mediaRecorderFpsMbr!!,
                                chosenVideoSize,
                                sensorOrientationMbr,
                                mediaRecorderSurfaceMbr!!
                            )
                    }

                    // (프리뷰 서페이스 설정)
                    if ((previewConfigList != null &&
                                previewConfigList.isNotEmpty())
                    ) {
                        setPreviewSurfaceListAsync(
                            previewConfigList,
                            onPreviewSurfaceReady = {
                                if (previewInfoListMbr.isEmpty() &&
                                    mediaRecorderInfoVOMbr == null &&
                                    imageReaderInfoVoMbr == null
                                ) { // 생성 서페이스가 하나도 존재하지 않으면,
                                    cameraSessionSemaphoreMbr.release()
                                    onError(8)
                                    return@setPreviewSurfaceListAsync
                                }

                                // (카메라 세션 생성)
                                createCameraSessionAsync(
                                    onCaptureSessionCreated = {
                                        cameraSessionSemaphoreMbr.release()
                                        onCameraSessionReady()
                                    },
                                    onError = {
                                        cameraSessionSemaphoreMbr.release()
                                        onError(it)
                                    }
                                )
                            }
                        )
                    }
                },
                onCameraDisconnected = {
                    cameraSessionSemaphoreMbr.release()
                    onCameraDisconnected()
                },
                onError = {
                    cameraSessionSemaphoreMbr.release()
                    onError(it)
                }
            )
        }
    }

    // (프리뷰 세션을 실행하는 함수)
    // API 에러 코드 :
    // 0 : 적당한 출력 서페이스가 하나도 세팅되어 있지 않음
    // 1 : 카메라 세션이 생성되지 않음
    fun startPreviewSessionAsync(onError: (Int) -> Unit) {
        executorServiceMbr?.execute {
            cameraSessionSemaphoreMbr.acquire()
            // (생성 서페이스 검사)
            if (previewInfoListMbr.isEmpty() &&
                imageReaderInfoVoMbr == null
            ) { // 생성 서페이스가 하나도 존재하지 않으면,
                cameraSessionSemaphoreMbr.release()
                onError(0)
                return@execute
            }

            // (세션 객체 검사)
            if (cameraCaptureSessionMbr == null) {
                cameraSessionSemaphoreMbr.release()
                onError(1)
                return@execute
            }

            // (기존 실행 세션 정리)
            captureRequestBuilderMbr = null
            cameraCaptureSessionMbr!!.stopRepeating()

            // (리퀘스트 빌더 생성)
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            // 서페이스 주입
            imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }
            for (previewSurface in previewSurfaceListMbr) {
                captureRequestBuilderMbr!!.addTarget(previewSurface)
            }

            // 리퀘스트 빌더 설정
            captureRequestBuilderMbr!!.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            // (카메라 실행)
            cameraCaptureSessionMbr!!.setRepeatingRequest(
                captureRequestBuilderMbr!!.build(),
                object : CameraCaptureSession.CaptureCallback() {},
                cameraHandlerMbr
            )

            cameraSessionSemaphoreMbr.release()
        }
    }

    // (녹화 세션을 실행하는 함수)
    // API 에러 코드 :
    // 0 : 미디어 리코더 서페이스가 세팅되어 있지 않음
    // 1 : 카메라 세션이 생성되지 않음
    fun startMediaRecorderSessionAsync(
        onMediaRecordingSessionStart: (VideoRecorderInfoVO) -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            cameraSessionSemaphoreMbr.acquire()
            // (생성 서페이스 검사)
            if (mediaRecorderInfoVOMbr == null
            ) { // 서페이스가 존재하지 않으면,
                cameraSessionSemaphoreMbr.release()
                onError(0)
                return@execute
            }

            // (세션 객체 검사)
            if (cameraCaptureSessionMbr == null) {
                cameraSessionSemaphoreMbr.release()
                onError(1)
                return@execute
            }

            // (기존 실행 세션 정리)
            captureRequestBuilderMbr = null
            cameraCaptureSessionMbr!!.stopRepeating()

            // (리퀘스트 빌더 생성)
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            // 서페이스 주입
            imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }
            for (previewSurface in previewSurfaceListMbr) {
                captureRequestBuilderMbr!!.addTarget(previewSurface)
            }
            captureRequestBuilderMbr!!.addTarget(mediaRecorderSurfaceMbr!!)

            // 리퀘스트 빌더 설정
            captureRequestBuilderMbr!!.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            captureRequestBuilderMbr!!.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(mediaRecorderFpsMbr!!, mediaRecorderFpsMbr!!)
            )

            // (카메라 실행)
            cameraCaptureSessionMbr!!.setRepeatingRequest(
                captureRequestBuilderMbr!!.build(),
                object : CameraCaptureSession.CaptureCallback() {},
                cameraHandlerMbr
            )

            onMediaRecordingSessionStart(mediaRecorderInfoVOMbr!!)

            cameraSessionSemaphoreMbr.release()
        }
    }

    fun clearCameraSession() {
        cameraSessionSemaphoreMbr.acquire()

        mediaRecorderSurfaceMbr?.release()
        mediaRecorderSurfaceMbr = null
        mediaRecorderFpsMbr = null
        mediaRecorderInfoVOMbr = null

        imageReaderMbr?.setOnImageAvailableListener(null, null)
        imageReaderMbr?.close()
        imageReaderMbr = null
        imageReaderInfoVoMbr = null

        cameraCaptureSessionMbr?.stopRepeating()
        cameraCaptureSessionMbr?.close()
        cameraCaptureSessionMbr = null

        captureRequestBuilderMbr = null

        previewInfoListMbr.clear()
        previewSurfaceListMbr.clear()

        cameraOutputSurfaceWhRatioMbr = 1.0

        cameraDeviceMbr?.close()
        cameraDeviceMbr = null

        cameraSessionSemaphoreMbr.release()
    }


    // todo : 변환했을 때 기존 상태를 복원
    // 2. 카메라 세션 리퀘스트 빌더 생성
    // 카메라 디바이스 및 출력 서페이스 필요
    // todo : manual change 기능 추가
    fun createCameraCaptureRequest() {
        // 필요 사항 준비 여부 (cameraDevice 준비 및 서페이스 준비)
        if (null == cameraDeviceMbr) {
            // 빌더 생성용 카메라 객체
            return
        } else if ((null == imageReaderMbr &&
                    previewSurfaceListMbr.isEmpty() &&
                    mediaRecorderSurfaceMbr == null)
        ) {
            // 출력용 서페이스가 하나도 없을 때
            return
        }

        // 리퀘스트 빌더 생성
        if (mediaRecorderSurfaceMbr == null) {
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } else {
            // 비디오 서페이스 있으면 CameraDevice.TEMPLATE_RECORD
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilderMbr!!.addTarget(mediaRecorderSurfaceMbr!!)
            captureRequestBuilderMbr!!.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(mediaRecorderFpsMbr!!, mediaRecorderFpsMbr!!)
            )
        }

        // 서페이스 주입
        imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }

        for (previewSurface in previewSurfaceListMbr) {
            captureRequestBuilderMbr!!.addTarget(previewSurface)
        }

        // 리퀘스트 빌더 설정
        // todo : captureRequestBuilderMbr 설정 변경 가능하도록(CameraConfigVo 객체 사용 )
        // todo : cameraConfigVOMbr 를 이용하여 리퀘스트 설정 변경
        captureRequestBuilderMbr!!.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )

        chooseStabilizationMode(captureRequestBuilderMbr)
    }

    // todo : 변환했을 때 기존 상태를 복원
    fun deleteCameraCaptureRequest() {
        captureRequestBuilderMbr = null
    }

    // todo : 변환했을 때 기존 상태를 복원
    // 3. 카메라 세션 실행
    fun runCameraCaptureSession(onError: (Throwable) -> Unit) {
        if (null == cameraCaptureSessionMbr) {
            onError(RuntimeException("need to create cameraCaptureSession before"))
            return
        } else if (null == captureRequestBuilderMbr) {
            onError(RuntimeException("need to create captureRequestBuilder before"))
            return
        }

        cameraCaptureSessionMbr!!.setRepeatingRequest(
            captureRequestBuilderMbr!!.build(),
            object : CameraCaptureSession.CaptureCallback() {},
            cameraHandlerMbr
        )
    }

    // todo : 변환했을 때 기존 상태를 복원
    fun stopCameraCaptureSession() {
        cameraCaptureSessionMbr?.stopRepeating()
    }

    // 카메라와 폰 디바이스 width height 개념이 같은지 여부(90도 회전되었다면 width, height 가 서로 바뀜)
    fun isCameraAndScreenWidthHeightDifferent(): Boolean {
        // 카메라 디바이스와 휴대폰 rotation 이 서로 다른지를 확인
        var isCameraDeviceAndMobileRotationDifferent = false

        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parentActivityMbr.display!!.rotation
        } else {
            parentActivityMbr.windowManager.defaultDisplay.rotation
        }

        // width, height 가 서로 달라지는지를 확인하는 것이므로 90도 단위 변경만을 캐치
        when (deviceOrientation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientationMbr == 90 || sensorOrientationMbr == 270) {
                    isCameraDeviceAndMobileRotationDifferent = true
                }
            }

            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) {
                    isCameraDeviceAndMobileRotationDifferent = true
                }
            }
        }

        return isCameraDeviceAndMobileRotationDifferent
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (cameraSizes 들 중에 preferredArea 와 가장 유사한 것을 선택하고, 그 중에서도 preferredWHRatio 가 유사한 것을 선택)
    // preferredArea 0 은 최소, Long.MAX_VALUE 는 최대
    // preferredWHRatio 0 이하면 비율을 신경쓰지 않고 넓이만으로 비교
    // 반환 사이즈의 방향은 카메라 방향
    private fun chooseCameraSize(
        offeredCameraSizes: Array<Size>,
        preferredArea: Long,
        preferredWHRatio: Double
    ): Size {
        if (0 >= preferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
            // 넓이 비슷한 것을 선정
            var smallestAreaDiff: Long = Long.MAX_VALUE
            var resultIndex = 0

            for ((index, value) in offeredCameraSizes.withIndex()) {
                val area = value.width.toLong() * value.height.toLong()
                val areaDiff = abs(area - preferredArea)
                if (areaDiff < smallestAreaDiff) {
                    smallestAreaDiff = areaDiff
                    resultIndex = index
                }
            }

            return offeredCameraSizes[resultIndex]
        } else { // 비율을 먼저 보고, 이후 넓이로 비교
            // 카메라 디바이스와 휴대폰 rotation 이 서로 다른지를 확인
            var isCameraDeviceAndMobileRotationDifferent = false

            val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivityMbr.display!!.rotation
            } else {
                parentActivityMbr.windowManager.defaultDisplay.rotation
            }

            // width, height 가 서로 달라지는지를 확인하는 것이므로 90도 단위 변경만을 캐치
            when (deviceOrientation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    if (sensorOrientationMbr == 90 || sensorOrientationMbr == 270) {
                        isCameraDeviceAndMobileRotationDifferent = true
                    }
                }

                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    if (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) {
                        isCameraDeviceAndMobileRotationDifferent = true
                    }
                }
            }

            // 비율 비슷한 것을 선정
            var mostSameWhRatio = 0.0
            var smallestWhRatioDiff: Double = Double.MAX_VALUE

            for (value in offeredCameraSizes) {
                val whRatio: Double = if (isCameraDeviceAndMobileRotationDifferent) {
                    value.height.toDouble() / value.width.toDouble()
                } else {
                    value.width.toDouble() / value.height.toDouble()
                }

                val whRatioDiff = abs(whRatio - preferredWHRatio)
                if (whRatioDiff < smallestWhRatioDiff) {
                    smallestWhRatioDiff = whRatioDiff
                    mostSameWhRatio = whRatio
                }
            }

            // 넓이 비슷한 것을 선정
            var resultSizeIndex = 0
            var smallestAreaDiff: Long = Long.MAX_VALUE
            // 비슷한 비율중 가장 비슷한 넓이를 선정
            for ((index, value) in offeredCameraSizes.withIndex()) {
                val whRatio: Double = if (isCameraDeviceAndMobileRotationDifferent) {
                    value.height.toDouble() / value.width.toDouble()
                } else {
                    value.width.toDouble() / value.height.toDouble()
                }

                if (mostSameWhRatio == whRatio) {
                    val area = value.width.toLong() * value.height.toLong()
                    val areaDiff = abs(area - preferredArea)
                    if (areaDiff < smallestAreaDiff) {
                        smallestAreaDiff = areaDiff
                        resultSizeIndex = index
                    }
                }
            }
            return offeredCameraSizes[resultSizeIndex]
        }
    }

    private fun configureTransform(
        chosenWidth: Int,
        chosenHeight: Int,
        cameraPreview: AutoFitTextureView
    ) {
        val rotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivityMbr.display!!.rotation
            } else {
                parentActivityMbr.windowManager.defaultDisplay.rotation
            }

        val matrix = Matrix()
        val viewRect = RectF(
            0f,
            0f,
            cameraPreview.width.toFloat(),
            cameraPreview.height.toFloat()
        )
        val bufferRect = RectF(
            0f,
            0f,
            chosenHeight.toFloat(),
            chosenWidth.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(
                centerX - bufferRect.centerX(),
                centerY - bufferRect.centerY()
            )
            val scale = max(
                cameraPreview.height.toFloat() / chosenHeight,
                cameraPreview.width.toFloat() / chosenWidth
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate(
                    (90 * (rotation - 2)).toFloat(),
                    centerX,
                    centerY
                )
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        cameraPreview.setTransform(matrix)
    }

    // 카메라 디바이스 생성
    @RequiresPermission(Manifest.permission.CAMERA)
    private fun openCameraDevice(
        onCameraDeviceReady: () -> Unit,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        if (cameraDeviceMbr != null) {
            onCameraDeviceReady()
        } else {
            // 카메라 디바이스가 존재하지 않는다면 생성
            cameraManagerMbr.openCamera(
                cameraIdMbr,
                object : CameraDevice.StateCallback() {
                    // 카메라 디바이스 연결
                    override fun onOpened(camera: CameraDevice) {
                        // cameraDevice 가 열리면,
                        // 객체 저장
                        cameraDeviceMbr = camera
                        onCameraDeviceReady()
                    }

                    // 카메라 디바이스 연결 끊김 : 물리적 연결 종료, 혹은 권한이 높은 다른 앱에서 해당 카메라를 캐치한 경우
                    override fun onDisconnected(camera: CameraDevice) {
                        cameraDeviceMbr = null

                        onCameraDisconnected()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        cameraDeviceMbr = null

                        when (error) {
                            ERROR_CAMERA_DISABLED -> {
                                onError(3)
                            }
                            ERROR_CAMERA_IN_USE -> {
                                onError(4)
                            }
                            ERROR_MAX_CAMERAS_IN_USE -> {
                                onError(5)
                            }
                            ERROR_CAMERA_DEVICE -> {
                                onError(6)
                            }
                            ERROR_CAMERA_SERVICE -> {
                                onError(7)
                            }
                        }
                    }
                }, cameraHandlerMbr
            )
        }
    }

    // 프리뷰 서페이스 리스트 생성
    private fun setPreviewSurfaceListAsync(
        previewConfigList: ArrayList<PreviewConfigVo>,
        onPreviewSurfaceReady: () -> Unit
    ) {
        executorServiceMbr?.execute {
            // 지원되는 카메라 사이즈 배열 가져오기
            val cameraSizes =
                streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

            if (cameraSizes.isEmpty()) {
                onPreviewSurfaceReady()
                return@execute
            }

            for (previewIdx in 0 until previewConfigList.size) {
                val previewConfigVo = previewConfigList[previewIdx]
                val cameraPreview = previewConfigVo.autoFitTextureView
                var chosenPreviewSize: Size

                // (카메라 프리뷰 사이즈 계산)
                // 실제 지원하는 카메라 사이즈들 중 설정에 가까운 것을 선택
                // 원하는 크기 (= 디바이스 스크린 크기와 동일하게 설정)
                val preferredPreviewArea: Long =
                    parentActivityMbr.resources.displayMetrics.widthPixels.toLong() *
                            parentActivityMbr.resources.displayMetrics.heightPixels.toLong()

                chosenPreviewSize = chooseCameraSize(
                    cameraSizes,
                    preferredPreviewArea,
                    cameraOutputSurfaceWhRatioMbr
                )

                if (cameraPreview.isAvailable) {
                    val surfaceTexture = cameraPreview.surfaceTexture

                    if (null != surfaceTexture) {
                        // 서페이스 버퍼 설정
                        surfaceTexture.setDefaultBufferSize(
                            chosenPreviewSize.width,
                            chosenPreviewSize.height
                        )

                        val previewInfoVO = PreviewInfoVO(
                            chosenPreviewSize
                        )

                        previewInfoListMbr.add(previewInfoVO)
                        previewSurfaceListMbr.add(Surface(surfaceTexture))

                        // (텍스쳐 뷰 비율 변경)
                        var viewWidth = chosenPreviewSize.width
                        var viewHeight = chosenPreviewSize.height

                        if (((sensorOrientationMbr == 0 || sensorOrientationMbr == 180) &&
                                    Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) ||
                            ((sensorOrientationMbr == 90 || sensorOrientationMbr == 270) &&
                                    Configuration.ORIENTATION_PORTRAIT == parentActivityMbr.resources.configuration.orientation)
                        ) {
                            viewWidth = chosenPreviewSize.height
                            viewHeight = chosenPreviewSize.width
                        }

                        parentActivityMbr.runOnUiThread {
                            cameraPreview.setAspectRatio(
                                viewWidth,
                                viewHeight
                            )
                        }

                        configureTransform(
                            chosenPreviewSize.width,
                            chosenPreviewSize.height,
                            cameraPreview
                        )
                    }

                    if (previewIdx == previewConfigList.size - 1) {
                        // 마지막 작업일 때
                        onPreviewSurfaceReady()
                    }
                } else {
                    cameraPreview.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                // 서페이스 버퍼 설정
                                surfaceTexture.setDefaultBufferSize(
                                    chosenPreviewSize.width,
                                    chosenPreviewSize.height
                                )

                                val previewInfoVO = PreviewInfoVO(
                                    chosenPreviewSize
                                )

                                previewInfoListMbr.add(previewInfoVO)
                                previewSurfaceListMbr.add(Surface(surfaceTexture))

                                // (텍스쳐 뷰 비율 변경)
                                var viewWidth = chosenPreviewSize.width
                                var viewHeight = chosenPreviewSize.height

                                if (((sensorOrientationMbr == 0 || sensorOrientationMbr == 180) &&
                                            Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) ||
                                    ((sensorOrientationMbr == 90 || sensorOrientationMbr == 270) &&
                                            Configuration.ORIENTATION_PORTRAIT == parentActivityMbr.resources.configuration.orientation)
                                ) {
                                    viewWidth = chosenPreviewSize.height
                                    viewHeight = chosenPreviewSize.width
                                }

                                parentActivityMbr.runOnUiThread {
                                    cameraPreview.setAspectRatio(
                                        viewWidth,
                                        viewHeight
                                    )
                                }

                                configureTransform(
                                    chosenPreviewSize.width,
                                    chosenPreviewSize.height,
                                    cameraPreview
                                )

                                if (previewIdx == previewConfigList.size - 1) {
                                    // 마지막 작업일 때
                                    onPreviewSurfaceReady()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                configureTransform(
                                    chosenPreviewSize.width,
                                    chosenPreviewSize.height,
                                    cameraPreview
                                )
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                }
            }
        }
    }

    private fun createCameraSessionAsync(
        onCaptureSessionCreated: () -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            // api 28 이상 / 미만의 요청 방식이 다름
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // api 28 이상
                // 출력 설정 객체 리스트
                val outputConfigurationList = ArrayList<OutputConfiguration>()

                // 프리뷰 서페이스 주입
                for (previewSurface in previewSurfaceListMbr) {
                    outputConfigurationList.add(OutputConfiguration(previewSurface))
                }

                // 이미지 리더 서페이스 주입
                if (null != imageReaderMbr) {
                    outputConfigurationList.add(
                        OutputConfiguration(
                            imageReaderMbr!!.surface
                        )
                    )
                }

                // 미디어 리코더 서페이스 주입
                if (null != mediaRecorderSurfaceMbr) {
                    outputConfigurationList.add(
                        OutputConfiguration(
                            mediaRecorderSurfaceMbr!!
                        )
                    )
                }

                cameraDeviceMbr?.createCaptureSession(SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputConfigurationList,
                    HandlerExecutor(cameraHandlerMbr.looper),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSessionMbr = session

                            onCaptureSessionCreated()
                        }

                        // 세션 생성 실패
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            cameraCaptureSessionMbr = null

                            onError(9)
                        }
                    }
                ))
            } else {
                // api 28 미만
                // 출력 서페이스 리스트
                val surfaces = ArrayList<Surface>()

                // 프리뷰 서페이스 주입
                for (previewSurface in previewSurfaceListMbr) {
                    surfaces.add(previewSurface)
                }

                // 이미지 리더 서페이스 주입
                if (null != imageReaderMbr) {
                    val surface = imageReaderMbr!!.surface
                    surfaces.add(surface)
                }

                // 비디오 리코더 서페이스 주입
                if (null != mediaRecorderSurfaceMbr) {
                    val surface = mediaRecorderSurfaceMbr!!
                    surfaces.add(surface)
                }

                cameraDeviceMbr?.createCaptureSession(
                    surfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSessionMbr = session

                            onCaptureSessionCreated()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            cameraCaptureSessionMbr = null

                            onError(9)
                        }
                    }, cameraHandlerMbr
                )
            }
        }
    }

    // 손떨림 방지
    private fun chooseStabilizationMode(builder: CaptureRequest.Builder?) {
        val availableOpticalStabilization =
            cameraCharacteristicsMbr.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        if (availableOpticalStabilization != null) {
            for (mode in availableOpticalStabilization) {
                if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    builder?.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                    builder?.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
                    return
                }
            }
        }

        // If no optical mode is available, try software.
        val availableVideoStabilization =
            cameraCharacteristicsMbr.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
        if (availableVideoStabilization != null) {
            for (mode in availableVideoStabilization) {
                if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    builder?.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                    builder?.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    )
                    return
                }
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <내부 클래스 공간>
    data class ImageReaderConfigVo(
        // 원하는 이미지 넓이
        // width * height
        // 0L 이하면 최소, Long.MAX_VALUE 이면 최대
        val preferredImageReaderArea: Long,
        val imageReaderHandler: Handler,
        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class ImageReaderInfoVo(
        val chosenSize: Size
    )

    data class PreviewConfigVo(
        val autoFitTextureView: AutoFitTextureView
    )

    data class PreviewInfoVO(
        // 계산된 카메라 프리뷰 사이즈
        val chosenPreviewSize: Size
    )

    data class VideoRecorderConfigVo(
        // 원하는 이미지 넓이
        // width * height
        // 0L 이하면 최소, Long.MAX_VALUE 이면 최대
        val preferredImageReaderArea: Long
    )

    data class VideoRecorderInfoVO(
        val mediaRecorderFps: Int,
        val chosenVideoSize: Size,
        val sensorOrientation: Int,
        val inputSurface: Surface
    )

    // image reader format : YUV 420 888 을 사용
    data class CameraInfo(
        val cameraId: String,
        // facing
        // 전면 카메라. value : 0
        // 후면 카메라. value : 1
        // 기타 카메라. value : 2
        val facing: Int,
        val previewInfoList: ArrayList<DeviceInfo>,
        val imageReaderInfoList: ArrayList<DeviceInfo>,
        val mediaRecorderInfoList: ArrayList<DeviceInfo>,
        val highSpeedInfoList: ArrayList<DeviceInfo>
    ) {
        data class DeviceInfo(
            val size: Size,
            val fps: Int
        )
    }
}