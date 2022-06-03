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
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
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
import kotlin.math.abs
import kotlin.math.max

// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용
// Output Surface 에서 프리뷰는 복수 설정이 가능, 이미지 리더와 미디어 리코더는 1개만 설정 가능

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

// todo : 미디어 리코드 세션, 캡쳐, 설정 변경 함수
class CameraObj private constructor(
    private val parentActivityMbr: Activity
) {
    // [카메라 기본 생성 객체] : 카메라 객체 생성시 생성
    // 카메라 아이디
    lateinit var cameraIdMbr: String

    // 카메라 총괄 빌더
    private val cameraManagerMbr: CameraManager =
        parentActivityMbr.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // 카메라 정보 객체
    lateinit var cameraCharacteristicsMbr: CameraCharacteristics

    // 카메라 지원 사이즈 반환 객체
    private lateinit var streamConfigurationMapMbr: StreamConfigurationMap

    // 카메라 기본 방향 정보
    var sensorOrientationMbr: Int = 0

    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // Camera2 api 핸들러 스레드
    private val cameraHandlerThreadMbr = HandlerThreadObj("camera").apply {
        this.startHandlerThread()
    }

    // 이미지 리더 핸들러 스레드
    private val imageReaderHandlerThreadMbr = HandlerThreadObj("imageReader").apply {
        this.startHandlerThread()
    }

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
        // (가용 카메라 리스트 반환)
        fun getCameraInfoList(parentActivity: Activity): ArrayList<CameraInfo> {
            val cameraInfoList: ArrayList<CameraInfo> = ArrayList()

            val cameraManager: CameraManager =
                parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            cameraManager.cameraIdList.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)

                val cameraConfig = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )!!

                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )!!

                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)!!

                val previewInfoList = ArrayList<CameraInfo.DeviceInfo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    cameraConfig.getOutputSizes(SurfaceTexture::class.java).forEach { size ->
                        val secondsPerFrame =
                            cameraConfig.getOutputMinFrameDuration(
                                SurfaceTexture::class.java,
                                size
                            ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        previewInfoList.add(
                            CameraInfo.DeviceInfo(
                                size, fps
                            )
                        )
                    }
                }

                val imageReaderInfoList = ArrayList<CameraInfo.DeviceInfo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    cameraConfig.getOutputSizes(ImageFormat.YUV_420_888).forEach { size ->
                        val secondsPerFrame =
                            cameraConfig.getOutputMinFrameDuration(
                                ImageFormat.YUV_420_888,
                                size
                            ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        imageReaderInfoList.add(
                            CameraInfo.DeviceInfo(
                                size, fps
                            )
                        )
                    }
                }

                val mediaRecorderInfoList = ArrayList<CameraInfo.DeviceInfo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    cameraConfig.getOutputSizes(MediaRecorder::class.java).forEach { size ->
                        val secondsPerFrame =
                            cameraConfig.getOutputMinFrameDuration(
                                MediaRecorder::class.java,
                                size
                            ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        mediaRecorderInfoList.add(
                            CameraInfo.DeviceInfo(
                                size, fps
                            )
                        )
                    }
                }

                val highSpeedInfoList = ArrayList<CameraInfo.DeviceInfo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
                    )
                ) {
                    cameraConfig.highSpeedVideoSizes.forEach { size ->
                        cameraConfig.getHighSpeedVideoFpsRangesFor(size).forEach { fpsRange ->
                            val fps = fpsRange.upper
                            highSpeedInfoList.add(
                                CameraInfo.DeviceInfo(
                                    size, fps
                                )
                            )
                        }
                    }
                }

                cameraInfoList.add(
                    CameraInfo(
                        id,
                        facing,
                        previewInfoList,
                        imageReaderInfoList,
                        mediaRecorderInfoList,
                        highSpeedInfoList
                    )
                )
            }

            return cameraInfoList
        }

        // (카메라 아이디를 반환하는 스태틱 함수)
        // CameraCharacteristics.LENS_FACING_FRONT: 전면 카메라. value : 0
        // CameraCharacteristics.LENS_FACING_BACK: 후면 카메라. value : 1
        // CameraCharacteristics.LENS_FACING_EXTERNAL: 기타 카메라. value : 2
        fun chooseCameraId(parentActivity: Activity, lensFacing: Int): String? {
            var result: String? = null

            val cameraManager: CameraManager =
                parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            for (cameraId in cameraManager.cameraIdList) { // 존재하는 cameraId 를 순회
                // 카메라 정보 반환
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

                // 카메라 Facing 반환
                val deviceLensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

                if (null != deviceLensFacing) { // 카메라 Facing null 체크
                    if (lensFacing == deviceLensFacing) { // 해당 카메라 facing 이 원하는 facing 일 경우
                        val map =
                            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        if (null != map) { // 현 id 에서 제공해주는 map 이 존재할 때에 제대로된 카메라 센서로 반환
                            result = cameraId
                            break
                        }
                    }
                }
            }

            return result
        }

        // (객체 생성 함수 = 조건에 맞지 않으면 null 반환)
        // 조작하길 원하는 카메라 ID 를 설정하여 해당 카메라 정보를 생성
        fun getInstance(
            parentActivity: Activity,
            cameraId: String
        ): CameraObj? {
            // 카메라 객체 생성
            val cameraObj = CameraObj(parentActivity)

            // 카메라 id 멤버 변수 세팅
            cameraObj.cameraIdMbr = cameraId

            // 카메라 Id에 해당하는 카메라 정보 가져오기
            cameraObj.cameraCharacteristicsMbr =
                cameraObj.cameraManagerMbr.getCameraCharacteristics(cameraId)

            // 필수 정보 확인
            val streamConfigurationMap: StreamConfigurationMap? =
                cameraObj.cameraCharacteristicsMbr.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val sensorOrientationMbr: Int? =
                cameraObj.cameraCharacteristicsMbr.get(CameraCharacteristics.SENSOR_ORIENTATION)

            return if (null == streamConfigurationMap || null == sensorOrientationMbr) {
                // 필수 정보가 하나라도 없으면 null 반환
                null
            } else {
                // 각 정보 객체 저장 후 객체 반환
                cameraObj.streamConfigurationMapMbr = streamConfigurationMap
                cameraObj.sensorOrientationMbr = sensorOrientationMbr

                cameraObj
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (카메라 세션 실행)
    // 프리뷰 or 이미지 리더 or (프리뷰 and 이미지 리더)
    fun startCameraSessionAsync(
        cameraOutputSurfaceWhRatio: Double,
        imageReaderConfigVo: ImageReaderConfigVo?,
        previewConfigList: ArrayList<PreviewConfigVo>?,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (카메라 상태 초기화)
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
                            streamConfigurationMapMbr.getOutputSizes(imageReaderConfigVo.imageFormat)

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
                                imageReaderConfigVo.imageFormat,
                                imageReaderConfigVo.imageReaderMaxImage
                            ).apply {
                                setOnImageAvailableListener(
                                    imageReaderConfigVo.imageReaderCallback,
                                    imageReaderHandlerThreadMbr.handler
                                )
                            }

                            imageReaderMbr = imageReader
                            imageReaderInfoVoMbr = ImageReaderInfoVo(chosenImageReaderSize)
                        }
                    }

                    // (프리뷰 서페이스 설정)
                    if ((previewConfigList != null &&
                                previewConfigList.isNotEmpty())
                    ) {
                        setPreviewSurfaceListAsync(
                            previewConfigList,
                            onPreviewSurfaceReady = {
                                // (생성 서페이스 검사)
                                if (previewInfoListMbr.isEmpty() &&
                                    imageReaderInfoVoMbr == null
                                ) { // 생성 서페이스가 하나도 존재하지 않으면,
                                    cameraSessionSemaphoreMbr.release()
                                    onError(8)
                                    return@setPreviewSurfaceListAsync
                                }

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
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )

                                // (카메라 세션 객체 생성)
                                createCameraSessionAsync(
                                    onCaptureSessionCreated = {
                                        // (카메라 실행)
                                        cameraCaptureSessionMbr!!.setRepeatingRequest(
                                            captureRequestBuilderMbr!!.build(),
                                            object : CameraCaptureSession.CaptureCallback() {},
                                            cameraHandlerThreadMbr.handler
                                        )

                                        cameraSessionSemaphoreMbr.release()
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

    fun clearCameraSession() {
        cameraSessionSemaphoreMbr.acquire()

        mediaRecorderMbr?.stop()
        mediaRecorderMbr?.release()
        mediaRecorderMbr = null
        mediaRecorderSurfaceMbr?.release()
        mediaRecorderSurfaceMbr = null
        videoFpsMbr = null

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
    // 1. 영상 저장 서페이스 생성
    // 영상 저장 스트림은 한개만 생성 가능
    // todo: 파라미터 확인, 녹음 권한
    private var mediaRecorderSurfaceMbr: Surface? = null
    private var mediaRecorderMbr: MediaRecorder? = null
    private var videoFpsMbr: Int? = null
    fun setVideoRecordingSurface(videoRecorderConfigVo: VideoRecorderConfigVo): VideoRecorderInfoVO {
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
        val secondsPerFrame =
            streamConfigurationMapMbr.getOutputMinFrameDuration(
                MediaRecorder::class.java,
                chosenVideoSize
            ) / 1_000_000_000.0
        videoFpsMbr = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0

        // 카메라 방향 정보
        val rotation: Int = parentActivityMbr.windowManager.defaultDisplay.rotation

        val defaultOrientation = SparseIntArray()
        defaultOrientation.append(Surface.ROTATION_90, 0)
        defaultOrientation.append(Surface.ROTATION_0, 90)
        defaultOrientation.append(Surface.ROTATION_270, 180)
        defaultOrientation.append(Surface.ROTATION_180, 270)

        val inverseOrientation = SparseIntArray()
        inverseOrientation.append(Surface.ROTATION_270, 0)
        inverseOrientation.append(Surface.ROTATION_180, 90)
        inverseOrientation.append(Surface.ROTATION_90, 180)
        inverseOrientation.append(Surface.ROTATION_0, 270)

        // (할당용 미디어 리코더 준비)
        val preMediaRecorder = MediaRecorder()

        if (videoRecorderConfigVo.isRecordAudio) {
            preMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        preMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        preMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        preMediaRecorder.setOutputFile(videoRecorderConfigVo.saveFile.absolutePath)
        preMediaRecorder.setVideoSize(chosenVideoSize.width, chosenVideoSize.height)
        preMediaRecorder.setVideoFrameRate(videoFpsMbr!!)
        preMediaRecorder.setVideoEncodingBitRate(chosenVideoSize.width * chosenVideoSize.height * videoFpsMbr!!)
        preMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (videoRecorderConfigVo.isRecordAudio) {
            preMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        when (sensorOrientationMbr) {
            90 ->
                preMediaRecorder.setOrientationHint(
                    defaultOrientation.get(rotation)
                )
            270 ->
                preMediaRecorder.setOrientationHint(
                    inverseOrientation.get(rotation)
                )
        }
        preMediaRecorder.setInputSurface(mediaRecorderSurfaceMbr!!)

        // 카메라 스트림 지원을 위해 미디어 리코더를 만들었다가 바로 해제
        preMediaRecorder.prepare()
        preMediaRecorder.release()

        // (실사용 미디어 리코더 생성)
        val mediaRecorder = MediaRecorder()

        if (videoRecorderConfigVo.isRecordAudio) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(videoRecorderConfigVo.saveFile.absolutePath)
        mediaRecorder.setVideoSize(chosenVideoSize.width, chosenVideoSize.height)
        mediaRecorder.setVideoFrameRate(videoFpsMbr!!)
        mediaRecorder.setVideoEncodingBitRate(chosenVideoSize.width * chosenVideoSize.height * videoFpsMbr!!)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        if (videoRecorderConfigVo.isRecordAudio) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        when (sensorOrientationMbr) {
            90 ->
                mediaRecorder.setOrientationHint(
                    defaultOrientation.get(rotation)
                )
            270 ->
                mediaRecorder.setOrientationHint(
                    inverseOrientation.get(rotation)
                )
        }
        mediaRecorder.setInputSurface(mediaRecorderSurfaceMbr!!)
        mediaRecorder.prepare()

        mediaRecorderMbr = mediaRecorder

        return VideoRecorderInfoVO(mediaRecorder, videoFpsMbr!!, chosenVideoSize)
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
                Range(videoFpsMbr!!, videoFpsMbr!!)
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
            cameraHandlerThreadMbr.handler
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

    // todo : 변환했을 때 기존 상태를 복원
    fun destroyCamera() {
        imageReaderHandlerThreadMbr.stopHandlerThread()
        cameraHandlerThreadMbr.stopHandlerThread()
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (cameraSizes 들 중에 preferredArea 와 가장 유사한 것을 선택하고, 그 중에서도 preferredWHRatio 가 유사한 것을 선택)
    // preferredArea 0 은 최소, Long.MAX_VALUE 는 최대
    // preferredWHRatio 0 이하면 비율을 신경쓰지 않고 넓이만으로 비교
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
        viewWidth: Int,
        viewHeight: Int,
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
            viewWidth.toFloat(),
            viewHeight.toFloat()
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
                viewHeight.toFloat() / chosenHeight,
                viewWidth.toFloat() / chosenWidth
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
                }, cameraHandlerThreadMbr.handler
            )
        }
    }

    // 프리뷰 서페이스 리스트 생성
    // todo : 실제 변형 확인
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
                        surfaceTexture.setDefaultBufferSize(
                            chosenPreviewSize.width,
                            chosenPreviewSize.height
                        )

                        val previewInfoVO = PreviewInfoVO(
                            previewIdx,
                            chosenPreviewSize
                        )

                        previewInfoListMbr.add(previewInfoVO)
                        previewSurfaceListMbr.add(Surface(surfaceTexture))

                        // (텍스쳐 뷰 비율 변경)
                        if (Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) {
                            // 현 디바이스가 가로모드일 때
                            parentActivityMbr.runOnUiThread {
                                cameraPreview.setAspectRatio(
                                    chosenPreviewSize.width,
                                    chosenPreviewSize.height
                                )
                            }
                        } else {
                            // 현 디바이스가 세로모드일 때
                            parentActivityMbr.runOnUiThread {
                                cameraPreview.setAspectRatio(
                                    chosenPreviewSize.height,
                                    chosenPreviewSize.width
                                )
                            }
                        }

                        configureTransform(
                            cameraPreview.width,
                            cameraPreview.height,
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
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                val surfaceTexture = cameraPreview.surfaceTexture

                                if (null != surfaceTexture) {
                                    surfaceTexture.setDefaultBufferSize(
                                        chosenPreviewSize.width,
                                        chosenPreviewSize.height
                                    )

                                    val previewInfoVO = PreviewInfoVO(
                                        previewIdx,
                                        chosenPreviewSize
                                    )

                                    previewInfoListMbr.add(previewInfoVO)
                                    previewSurfaceListMbr.add(Surface(surfaceTexture))

                                    // (텍스쳐 뷰 비율 변경)
                                    if (Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) {
                                        // 현 디바이스가 가로모드일 때
                                        parentActivityMbr.runOnUiThread {
                                            cameraPreview.setAspectRatio(
                                                chosenPreviewSize.width,
                                                chosenPreviewSize.height
                                            )
                                        }
                                    } else {
                                        // 현 디바이스가 세로모드일 때
                                        parentActivityMbr.runOnUiThread {
                                            cameraPreview.setAspectRatio(
                                                chosenPreviewSize.height,
                                                chosenPreviewSize.width
                                            )
                                        }
                                    }

                                    configureTransform(
                                        width,
                                        height,
                                        chosenPreviewSize.width,
                                        chosenPreviewSize.height,
                                        cameraPreview
                                    )
                                }

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
                                    width,
                                    height,
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
                    HandlerExecutor(cameraHandlerThreadMbr.handler!!.looper),
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
                    }, cameraHandlerThreadMbr.handler
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
        // ex : ImageFormat.YUV_420_888
        val imageFormat: Int,
        val imageReaderMaxImage: Int,
        // 원하는 이미지 넓이
        // width * height
        // 0L 이하면 최소, Long.MAX_VALUE 이면 최대
        val preferredImageReaderArea: Long,

        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class ImageReaderInfoVo(
        val chosenSize: Size
    )

    data class PreviewConfigVo(
        val autoFitTextureView: AutoFitTextureView
    )

    data class PreviewInfoVO(
        val idx: Int,
        // 계산된 카메라 프리뷰 사이즈
        val chosenPreviewSize: Size
    )

    data class VideoRecorderConfigVo(
        // 원하는 이미지 넓이
        // width * height
        // 0L 이하면 최소, Long.MAX_VALUE 이면 최대
        val preferredImageReaderArea: Long,

        val isRecordAudio: Boolean,

        val saveFile: File
    )

    data class VideoRecorderInfoVO(
        val mediaRecorder: MediaRecorder,
        val videoFps: Int,
        val chosenPreviewSize: Size
    )

    // facing
    // 전면 카메라. value : 0
    // 후면 카메라. value : 1
    // 기타 카메라. value : 2
    // image reader format : YUV 420 888 을 사용
    data class CameraInfo(
        val cameraId: String,
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