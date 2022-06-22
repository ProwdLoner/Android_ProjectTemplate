package com.example.prowd_android_template.util_class

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.custom_view.AutoFitTextureView
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.math.sqrt


// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용
// Output Surface 에서 프리뷰는 복수 설정이 가능, 이미지 리더와 미디어 리코더는 1개만 설정 가능
// 내부 제공 함수들은 대다수 비동기 동작을 수행합니다. 시점을 맞추기 위해선 제공되는 콜백을 사용하면 됩니다.
// 카메라 동작 관련 함수들 모두 세마포어로 뮤텍스가 되어있으므로 이 경우 꼭 완료 콜백을 통하지 않아도 선행 후행의 싱크가 어긋나지 않습니다.

// todo : 이미지 리더 최대 크기일 때 불안정 해결
// todo : 180 도 회전시 프리뷰 거꾸로 나오는 문제(restart 가 되지 않고 있음)
// todo : 사진 찍기 기능 검증
// todo : 서페이스 각자 세팅 기능 오버로딩(request setting callback 을 제거)
// todo : exposure, whitebalance 등을 내부 멤버변수로 두고 자동, 수동 모드 변경 및 수동 수치 조작 가능하게
// todo : 클릭 exposure, whitebalance 등
// todo : 전체 검증 : 특히 setSurface 의 디바이스 방향
// todo : 다시 시작 스레드 관련 불안정
class CameraObj private constructor(
    private val parentActivityMbr: Activity,
    val cameraIdMbr: String,
    private val cameraManagerMbr: CameraManager,
    cameraCharacteristicsMbr: CameraCharacteristics,
    private val streamConfigurationMapMbr: StreamConfigurationMap,
    val previewSurfaceSupportedSizeListMbr: Array<Size>?,
    val imageReaderSurfaceSupportedSizeListMbr: Array<Size>?,
    val mediaRecorderSurfaceSupportedSizeListMbr: Array<Size>?,
    val sensorOrientationMbr: Int, // 카메라 방향이 시계방향으로 얼마나 돌려야 디바이스 방향과 일치하는지에 대한 각도
    private val onCameraDisconnectedMbr: (() -> Unit)
) {
    // <멤버 변수 공간>
    // (스레드 풀)
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // Camera2 api 핸들러 스레드
    private val cameraApiHandlerThreadObjMbr: HandlerThreadObj =
        HandlerThreadObj(
            publishCameraThreadName()
        ).apply {
            this.startHandlerThread()
        }

    // 이미지 리더 핸들러 스레드
    private val imageReaderHandlerThreadObjMbr =
        HandlerThreadObj(
            publishImageReaderThreadName()
        ).apply {
            this.startHandlerThread()
        }

    // (카메라 정보)
    // 떨림 보정 기능 가능 여부 (기계적) : stabilization 설정을 할 때 우선 적용
    var isOpticalStabilizationAvailableMbr: Boolean = false
        private set

    // 떨림 보정 기능 가능 여부 (소프트웨어적) : stabilization 설정을 할 때 차선 적용
    var isVideoStabilizationAvailableMbr: Boolean = false
        private set

    // 떨림 보정 기능 적용 여부 :
    // isOpticalStabilizationAvailableMbr 가 true 라면 이를 적용,
    // 아니라면 isVideoStabilizationAvailableMbr 를 적용,
    // 둘 다 지원 불가라면 보정 불가
    var isCameraStabilizationSetMbr: Boolean = false
        private set

    // 센서 사이즈
    val sensorSize =
        cameraCharacteristicsMbr.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

    // 카메라 최대 줌 배수
    // maxZoom 이 1.0 이라는 것은 줌이 불가능하다는 의미
    var maxZoomMbr = 1.0f
        private set

    // 카메라 현재 줌 배수
    var currentCameraZoomFactorMbr = 1.0f
        private set

    // 카메라가 현재 리퀘스트 반복 처리중인지
    var isRepeatingMbr: Boolean = false
        private set

    // 카메라가 현재 미디어 레코딩 중인지
    var isRecordingMbr = false
        private set


    // [카메라 기본 생성 객체] : 카메라 객체 생성시 생성 - 상태 변화에 따라 초기화
    // (카메라 부산 데이터)
    private val cameraSessionSemaphoreMbr = Semaphore(1)
    private var cameraDeviceMbr: CameraDevice? = null

    // 이미지 리더 세팅 부산물
    var imageReaderConfigVoMbr: ImageReaderConfigVo? = null
    private var imageReaderMbr: ImageReader? = null

    // 미디어 리코더 세팅 부산물
    var mediaRecorderConfigVoMbr: MediaRecorderConfigVo? = null
    private var mediaRecorderMbr: MediaRecorder? = null
    private var mediaCodecSurfaceMbr: Surface? = null

    // 프리뷰 세팅 부산물
    val previewConfigVoListMbr: ArrayList<PreviewConfigVo> = ArrayList()
    private val previewSurfaceListMbr: ArrayList<Surface> = ArrayList()

    // 카메라 리퀘스트 빌더
    private var captureRequestBuilderMbr: CaptureRequest.Builder? = null

    // 카메라 세션 객체
    private var cameraCaptureSessionMbr: CameraCaptureSession? = null


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    init {
        // (max zoom 정보)
        maxZoomMbr = if (sensorSize == null) {
            1.0f
        } else {
            val maxZoom =
                cameraCharacteristicsMbr.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

            if (maxZoom == null) {
                1.0f
            } else {
                if (maxZoom < 1.0f) {
                    1.0f
                } else {
                    maxZoom
                }
            }
        }

        // (기계적 떨림 보정 정보)
        val availableOpticalStabilization =
            cameraCharacteristicsMbr.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)

        if (availableOpticalStabilization != null) {
            for (mode in availableOpticalStabilization) {
                if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    isOpticalStabilizationAvailableMbr = true
                }
            }
        }

        // (소프트웨어 떨림 보정 정보)
        val availableVideoStabilization =
            cameraCharacteristicsMbr.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)

        if (availableVideoStabilization != null) {
            for (mode in availableVideoStabilization) {
                if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    isVideoStabilizationAvailableMbr = true
                }
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <스태틱 공간>
    companion object {
        // (객체 생성 함수 = 조건에 맞지 않으면 null 반환)
        // 조작하길 원하는 카메라 ID 를 설정하여 해당 카메라 정보를 생성
        fun getInstance(
            parentActivity: Activity,
            cameraId: String,
            onCameraDisconnected: (() -> Unit)
        ): CameraObj? {
            if (!parentActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                // 카메라 장치가 없다면 null 반환
                return null
            }

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

            if (null == streamConfigurationMap || null == sensorOrientationMbr) {
                // 필수 정보가 하나라도 없으면 null 반환
                return null
            }

            // 서페이스 출력 지원 사이즈 리스트
            val previewSurfaceSupportedSizeList =
                streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)
            val imageReaderSurfaceSupportedSizeList =
                streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)
            val mediaRecorderSurfaceSupportedSizeList =
                streamConfigurationMap.getOutputSizes(MediaRecorder::class.java)

            // 출력 지원 사이즈가 하나도 없다면 null 반환
            if (previewSurfaceSupportedSizeList == null &&
                imageReaderSurfaceSupportedSizeList == null &&
                mediaRecorderSurfaceSupportedSizeList == null
            ) {
                return null
            }

            return CameraObj(
                parentActivity,
                cameraId,
                cameraManager,
                cameraCharacteristics,
                streamConfigurationMap,
                previewSurfaceSupportedSizeList,
                imageReaderSurfaceSupportedSizeList,
                mediaRecorderSurfaceSupportedSizeList,
                sensorOrientationMbr,
                onCameraDisconnected
            )
        }

        // (카메라 아이디를 반환하는 스태틱 함수)
        // CameraCharacteristics.LENS_FACING_FRONT: 전면 카메라. value : 0
        // CameraCharacteristics.LENS_FACING_BACK: 후면 카메라. value : 1
        // CameraCharacteristics.LENS_FACING_EXTERNAL: 기타 카메라. value : 2
        fun getCameraIdFromFacing(parentActivity: Activity, lensFacing: Int): String? {
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


        private var cameraThreadNameSemaphore = Semaphore(1)
        private val cameraThreadNameList = ArrayList<String>()
        private fun publishCameraThreadName(): String {
            cameraThreadNameSemaphore.acquire()
            if (cameraThreadNameList.size == 0) {
                val name = "camera_thread_0"
                cameraThreadNameList.add(name)
                cameraThreadNameSemaphore.release()
                return name
            } else {
                var curNameIdx = 0
                while (true) {
                    val idx = cameraThreadNameList.indexOfFirst {
                        it.split("_").last().toInt() == curNameIdx
                    }

                    if (idx == -1) {
                        break
                    }

                    curNameIdx++
                }

                val name = "camera_thread_${curNameIdx}"
                cameraThreadNameList.add(name)
                cameraThreadNameSemaphore.release()
                return name
            }
        }

        private fun deleteCameraThreadName(name: String) {
            cameraThreadNameSemaphore.acquire()
            val idx = cameraThreadNameList.indexOfLast { it == name }
            if (idx != -1) {
                cameraThreadNameList.removeAt(idx)
            }
            cameraThreadNameSemaphore.release()
        }

        private var imageReaderThreadNameSemaphore = Semaphore(1)
        private val imageReaderThreadNameList = ArrayList<String>()
        private fun publishImageReaderThreadName(): String {
            imageReaderThreadNameSemaphore.acquire()
            if (imageReaderThreadNameList.size == 0) {
                val name = "image_reader_thread_0"
                imageReaderThreadNameList.add(name)
                imageReaderThreadNameSemaphore.release()
                return name
            } else {
                var curNameIdx = 0
                while (true) {
                    val idx = imageReaderThreadNameList.indexOfFirst {
                        it.split("_").last().toInt() == curNameIdx
                    }

                    if (idx == -1) {
                        break
                    }

                    curNameIdx++
                }

                val name = "image_reader_thread_${curNameIdx}"
                imageReaderThreadNameList.add(name)
                imageReaderThreadNameSemaphore.release()
                return name
            }
        }

        private fun deleteImageReaderThreadName(name: String) {
            imageReaderThreadNameSemaphore.acquire()
            val idx = imageReaderThreadNameList.indexOfLast { it == name }
            if (idx != -1) {
                imageReaderThreadNameList.removeAt(idx)
            }
            imageReaderThreadNameSemaphore.release()
        }

        // todo : 카메라 id 별 세마포어 생성 및 제공
    }

    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>

    // (출력 서페이스 설정 함수)
    // 서페이스 설정 검증 및 생성, 이후 CameraDevice, CameraCaptureSession 생성까지를 수행
    // 사용할 서페이스 사이즈 및 종류를 결정하는 것이 주요 기능
    // 실행시 카메라 초기화를 먼저 실행 (이미 생성된 CameraDevice 만 놔두고 모든것을 초기화)

    // onError 에러 코드 :
    // 1 : 출력 서페이스가 하나도 입력되어 있지 않음
    // 2 : 해당 사이즈 이미지 리더를 지원하지 않음
    // 3 : 해당 사이즈 미디어 리코더를 지원하지 않음
    // 4 : 해당 사이즈 프리뷰를 지원하지 않음
    // 5 : 미디어 레코더 오디오 녹음 설정 권한 비충족
    // 6 : 생성된 서페이스가 존재하지 않음
    // 7 : 카메라 권한이 없음
    // 8 : CameraDevice.StateCallback.ERROR_CAMERA_DISABLED (권한 등으로 인해 사용이 불가능)
    // 9 : CameraDevice.StateCallback.ERROR_CAMERA_IN_USE (해당 카메라가 이미 사용중)
    // 10 : CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE (시스템에서 허용한 카메라 동시 사용을 초과)
    // 11 : CameraDevice.StateCallback.ERROR_CAMERA_DEVICE (카메라 디바이스 자체적인 문제)
    // 12 : CameraDevice.StateCallback.ERROR_CAMERA_SERVICE (안드로이드 시스템 문제)
    // 13 : 카메라 세션 생성 실패
    // 14 : 녹화 파일 확장자가 mp4 가 아님
    fun setCameraOutputSurfaces(
        previewConfigVoList: ArrayList<PreviewConfigVo>?,
        imageReaderConfigVo: ImageReaderConfigVo?,
        mediaRecorderConfigVo: MediaRecorderConfigVo?,
        executorOnSurfaceAllReady: () -> Unit,
        executorOnError: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (서페이스 설정 파라미터 개수 검사)
            if ((imageReaderConfigVo == null &&
                        mediaRecorderConfigVo == null &&
                        (previewConfigVoList == null ||
                                previewConfigVoList.isEmpty()))
            ) {
                cameraSessionSemaphoreMbr.release()
                executorOnError(1)
                return@execute
            }

            // (서페이스 사이즈 검사)
            if (imageReaderConfigVo != null) {
                val cameraSizes =
                    streamConfigurationMapMbr.getOutputSizes(ImageFormat.YUV_420_888)

                // 이미지 리더 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes == null ||
                    cameraSizes.isEmpty() ||
                    cameraSizes.indexOfFirst {
                        it.width == imageReaderConfigVo.cameraOrientSurfaceSize.width &&
                                it.height == imageReaderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(2)
                    return@execute
                }
            }

            // 프리뷰 리스트 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
            if (previewConfigVoList != null && previewConfigVoList.isNotEmpty()) {
                val cameraSizes =
                    streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

                // 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes == null ||
                    cameraSizes.isEmpty()
                ) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(4)
                    return@execute
                } else {
                    for (previewConfig in previewConfigVoList) {
                        if (cameraSizes.indexOfFirst {
                                it.width == previewConfig.cameraOrientSurfaceSize.width &&
                                        it.height == previewConfig.cameraOrientSurfaceSize.height
                            } == -1) {
                            cameraSessionSemaphoreMbr.release()
                            executorOnError(4)
                            return@execute
                        }
                    }
                }
            }

            // (카메라 상태 초기화)
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            for (previewConfigVo in previewConfigVoListMbr) {
                previewConfigVo.autoFitTextureView.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                            Unit
                    }
            }

            mediaRecorderMbr?.release()
            mediaRecorderMbr = null
            mediaCodecSurfaceMbr?.release()
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null

            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()

            imageReaderMbr?.close()
            imageReaderMbr = null
            imageReaderConfigVoMbr = null

            cameraCaptureSessionMbr?.close()
            cameraCaptureSessionMbr = null

            captureRequestBuilderMbr = null

            // (이미지 리더 서페이스 준비)
            if (imageReaderConfigVo != null) {
                imageReaderConfigVoMbr = imageReaderConfigVo

                imageReaderMbr = ImageReader.newInstance(
                    imageReaderConfigVo.cameraOrientSurfaceSize.width,
                    imageReaderConfigVo.cameraOrientSurfaceSize.height,
                    ImageFormat.YUV_420_888,
                    2
                ).apply {
                    setOnImageAvailableListener(
                        imageReaderConfigVo.imageReaderCallback,
                        imageReaderHandlerThreadObjMbr.handler
                    )
                }
            }

            // (미디어 레코더 서페이스 준비)
            if (mediaRecorderConfigVo != null) {
                mediaRecorderConfigVoMbr = mediaRecorderConfigVo

                // 오디오 권한 확인
                // 녹음 설정을 했는데 권한이 없을 때, 에러
                if (mediaRecorderConfigVo.isAudioRecording &&
                    ActivityCompat.checkSelfPermission(
                        parentActivityMbr,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(5)
                    return@execute
                }

                // (레코더 객체 생성)
                mediaCodecSurfaceMbr = MediaCodec.createPersistentInputSurface()

                mediaRecorderMbr =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(parentActivityMbr)
                    } else {
                        MediaRecorder()
                    }

                // (레코더 정보 생성)
                // 카메라 방향 정보
                val rotation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    parentActivityMbr.display!!.rotation
                } else {
                    parentActivityMbr.windowManager.defaultDisplay.rotation
                }

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

                // 비디오 FPS
                val spf = (streamConfigurationMapMbr.getOutputMinFrameDuration(
                    MediaRecorder::class.java,
                    mediaRecorderConfigVo.cameraOrientSurfaceSize
                ) / 1_000_000_000.0)
                val mediaRecorderFps = if (spf > 0) (1.0 / spf).toInt() else 0

                // 파일 확장자 검증
                if (mediaRecorderConfigVo.mediaRecordingMp4File.extension != "mp4") {
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(14)
                    return@execute
                }

                // (미디어 레코더 설정)
                // 서페이스 소스 설정
                if (mediaRecorderConfigVo.isAudioRecording) {
                    mediaRecorderMbr!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                mediaRecorderMbr!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)

                // 파일 포멧 설정
                mediaRecorderMbr!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // 파일 경로 설정
                // 만일 같은 파일을 넣으면 새로써짐
                if (mediaRecorderConfigVo.mediaRecordingMp4File.exists()) {
                    mediaRecorderConfigVo.mediaRecordingMp4File.delete()
                }
                mediaRecorderConfigVo.mediaRecordingMp4File.createNewFile()
                mediaRecorderMbr!!.setOutputFile(mediaRecorderConfigVo.mediaRecordingMp4File.absolutePath)

                // 데이터 저장 퀄리티 설정
                if (mediaRecorderConfigVo.videoEncodingBitrate == null) {
                    // todo : setAudioEncoding 도 적용
                    // todo : 최적값 찾기 videoEncodingBitrate calc
                    mediaRecorderMbr!!.setVideoEncodingBitRate(
                        720000000
                    )
                } else {
                    mediaRecorderMbr!!.setVideoEncodingBitRate(
                        mediaRecorderConfigVo.videoEncodingBitrate
                    )
                }

                // 데이터 저장 프레임 설정
                if (mediaRecorderConfigVo.recordingFps == null) {
                    mediaRecorderMbr!!.setVideoFrameRate(mediaRecorderFps)
                } else {
                    mediaRecorderMbr!!.setVideoFrameRate(mediaRecorderConfigVo.recordingFps)
                }

                // 서페이스 사이즈 설정
                mediaRecorderMbr!!.setVideoSize(
                    mediaRecorderConfigVo.cameraOrientSurfaceSize.width,
                    mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                )

                // 서페이스 방향 설정
                when (sensorOrientationMbr) {
                    90 ->
                        mediaRecorderMbr!!.setOrientationHint(
                            defaultOrientation.get(rotation)
                        )
                    270 ->
                        mediaRecorderMbr!!.setOrientationHint(
                            inverseOrientation.get(rotation)
                        )
                }

                // 인코딩 타입 설정
                mediaRecorderMbr!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (mediaRecorderConfigVo.isAudioRecording) {
                    mediaRecorderMbr!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }

                mediaRecorderMbr!!.setInputSurface(mediaCodecSurfaceMbr!!)

                mediaRecorderMbr!!.prepare()
            }

            // (프리뷰 서페이스 준비)
            if (previewConfigVoList != null &&
                previewConfigVoList.isNotEmpty()
            ) {
                previewConfigVoListMbr.addAll(previewConfigVoList)

                val previewListSize = previewConfigVoList.size
                var checkedPreviewCount = 0
                val checkedPreviewCountSemaphore = Semaphore(1)

                for (previewIdx in 0 until previewListSize) {
                    val previewObj = previewConfigVoList[previewIdx].autoFitTextureView
                    val surfaceSize = previewConfigVoList[previewIdx].cameraOrientSurfaceSize

                    previewObj.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) = Unit

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                configureTransform(
                                    previewObj,
                                    surfaceSize.width,
                                    surfaceSize.height,
                                    width,
                                    height
                                )
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                true

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                Unit
                        }

                    parentActivityMbr.runOnUiThread {
                        if (previewObj.isAvailable) {
                            // (텍스쳐 뷰 비율 변경)
                            if (parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                && (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) ||
                                parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                                && (sensorOrientationMbr == 90 || sensorOrientationMbr == 270)
                            ) {
                                previewObj.setAspectRatio(
                                    surfaceSize.height,
                                    surfaceSize.width
                                )
                            } else {
                                previewObj.setAspectRatio(
                                    surfaceSize.width,
                                    surfaceSize.height
                                )
                            }

                            configureTransform(
                                previewObj,
                                surfaceSize.width,
                                surfaceSize.height,
                                previewObj.width,
                                previewObj.height
                            )

                            executorServiceMbr.execute {
                                checkedPreviewCountSemaphore.acquire()
                                if (++checkedPreviewCount == previewListSize) {
                                    // 마지막 작업일 때
                                    checkedPreviewCountSemaphore.release()

                                    onSurfacesAllChecked(
                                        executorOnSurfaceAllReady,
                                        executorOnError
                                    )
                                } else {
                                    checkedPreviewCountSemaphore.release()
                                }
                            }
                        } else {
                            previewObj.surfaceTextureListener =
                                object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(
                                        surface: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) {
                                        // (텍스쳐 뷰 비율 변경)
                                        if (parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                            && (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) ||
                                            parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                                            && (sensorOrientationMbr == 90 || sensorOrientationMbr == 270)
                                        ) {
                                            previewObj.setAspectRatio(
                                                surfaceSize.height,
                                                surfaceSize.width
                                            )
                                        } else {
                                            previewObj.setAspectRatio(
                                                surfaceSize.width,
                                                surfaceSize.height
                                            )
                                        }

                                        configureTransform(
                                            previewObj,
                                            surfaceSize.width,
                                            surfaceSize.height,
                                            width,
                                            height
                                        )

                                        executorServiceMbr.execute {
                                            checkedPreviewCountSemaphore.acquire()
                                            if (++checkedPreviewCount == previewListSize) {
                                                // 마지막 작업일 때
                                                checkedPreviewCountSemaphore.release()

                                                onSurfacesAllChecked(
                                                    executorOnSurfaceAllReady,
                                                    executorOnError
                                                )
                                            } else {
                                                checkedPreviewCountSemaphore.release()
                                            }
                                        }
                                    }

                                    override fun onSurfaceTextureSizeChanged(
                                        surface: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) {
                                        configureTransform(
                                            previewObj,
                                            surfaceSize.width,
                                            surfaceSize.height,
                                            width,
                                            height
                                        )
                                    }

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                        true

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                        Unit
                                }
                        }
                    }
                }
            } else {
                onSurfacesAllChecked(
                    executorOnSurfaceAllReady,
                    executorOnError
                )
            }
        }
    }

    // (카메라 리퀘스트 빌더 설정 함수)
    // 리퀘스트 모드 (ex : CameraDevice.TEMPLATE_PREVIEW) 및 리퀘스트로 사용할 서페이스 결정
    // onPreview, onImageReader, onMediaRecorder -> 어느 서페이스를 사용할지를 결정
    // onCameraRequestSettingTime 콜백으로 빌더를 빌려와 원하는 리퀘스트를 세팅 가능
    // 현 세션이 실행중이라면 이 설정으로 실행됨

    // onError 에러 코드 :
    // 1 : CameraDevice 객체가 아직 생성되지 않은 경우
    // 2 : 서페이스가 하나도 생성되지 않은 경우
    // 3 : preview 설정이지만 preview 서페이스가 없을 때
    // 4 : imageReader 설정이지만 imageReader 서페이스가 없을 때
    // 5 : mediaRecorder 설정이지만 mediaRecorder 서페이스가 없을 때
    fun setCameraRequest(
        onPreview: Boolean,
        onImageReader: Boolean,
        onMediaRecorder: Boolean,
        cameraRequestMode: Int,
        executorOnCameraRequestSettingTime: ((CaptureRequest.Builder) -> Unit),
        executorOnCameraRequestBuilderSet: () -> Unit,
        executorOnError: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            if (cameraDeviceMbr == null) {
                cameraSessionSemaphoreMbr.release()
                executorOnError(1)
                return@execute
            }

            if (previewConfigVoListMbr.isEmpty() &&
                imageReaderMbr == null &&
                mediaCodecSurfaceMbr == null
            ) { // 생성 서페이스가 하나도 존재하지 않으면,
                cameraSessionSemaphoreMbr.release()
                executorOnError(2)
                return@execute
            }

            // (리퀘스트 빌더 생성)
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(cameraRequestMode)

            if (onPreview) {
                if (previewConfigVoListMbr.isEmpty()) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(3)
                    return@execute
                } else {
                    for (previewSurface in previewSurfaceListMbr) {
                        captureRequestBuilderMbr!!.addTarget(previewSurface)
                    }
                }
            }

            if (onImageReader) {
                if (imageReaderMbr == null) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(4)
                    return@execute
                } else {
                    captureRequestBuilderMbr!!.addTarget(imageReaderMbr!!.surface)
                }
            }

            if (onMediaRecorder) {
                if (mediaCodecSurfaceMbr == null) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(5)
                    return@execute
                } else {
                    captureRequestBuilderMbr!!.addTarget(mediaCodecSurfaceMbr!!)
                }
            }

            // (사용자에게 설정 정보를 받아오는 공간 제공)
            executorOnCameraRequestSettingTime(captureRequestBuilderMbr!!)

            // (카메라 오브젝트 내부 정보를 최종적으로 채택)
            // 오브젝트 내부 줌 정보를 설정
            val zoom = if (maxZoomMbr < currentCameraZoomFactorMbr) {
                // 가용 줌 최대치에 설정을 맞추기
                maxZoomMbr
            } else {
                currentCameraZoomFactorMbr
            }

            if (zoom != 1.0f) {
                val centerX =
                    sensorSize!!.width() / 2
                val centerY =
                    sensorSize.height() / 2
                val deltaX =
                    ((0.5f * sensorSize.width()) / zoom).toInt()
                val deltaY =
                    ((0.5f * sensorSize.height()) / zoom).toInt()

                val mCropRegion = Rect().apply {
                    set(
                        centerX - deltaX,
                        centerY - deltaY,
                        centerX + deltaX,
                        centerY + deltaY
                    )
                }

                captureRequestBuilderMbr!!.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)
            }

            // 카메라 떨림 보정 여부 반영
            if (isCameraStabilizationSetMbr) { // 떨림 보정 on
                // 기계 보정이 가능하면 사용하고, 아니라면 소프트웨어 보정을 적용
                if (isOpticalStabilizationAvailableMbr) {
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
                } else if (isVideoStabilizationAvailableMbr) {
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    )
                }
            } else { // 떨림 보정 off
                captureRequestBuilderMbr!!.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
                captureRequestBuilderMbr!!.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
            }

            // 기존 세션이 실행중이라면 곧바로 설정 적용
            if (isRepeatingMbr) {
                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraApiHandlerThreadObjMbr.handler
                )
            }

            cameraSessionSemaphoreMbr.release()
            executorOnCameraRequestBuilderSet()
        }
    }

    // (준비된 카메라 리퀘스트 실행 함수)
    // isRepeating 인자가 true 라면 프리뷰와 같은 지속적 요청, false 라면 capture

    // onError 에러 코드 :
    // 1 : 카메라 세션이 생성되지 않음
    // 2 : 카메라 리퀘스트 빌더가 생성되지 않음
    // 3 : capture 설정으로 했으면서 imageReader 서페이스가 없는 경우
    fun runCameraRequest(
        isRepeating: Boolean,
        captureCallback: CameraCaptureSession.CaptureCallback?,
        executorOnRequestComplete: () -> Unit,
        executorOnError: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            if (cameraCaptureSessionMbr == null) {
                cameraSessionSemaphoreMbr.release()
                executorOnError(1)
                return@execute
            }

            if (captureRequestBuilderMbr == null) {
                cameraSessionSemaphoreMbr.release()
                executorOnError(2)
                return@execute
            }

            if (isRepeating) {
                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraApiHandlerThreadObjMbr.handler
                )
                isRepeatingMbr = true

                cameraSessionSemaphoreMbr.release()
                executorOnRequestComplete()
            } else {
                if (null == imageReaderMbr) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnError(3)
                    return@execute
                }

                cameraCaptureSessionMbr!!.capture(
                    captureRequestBuilderMbr!!.build(),
                    captureCallback,
                    cameraApiHandlerThreadObjMbr.handler
                )

                isRepeatingMbr = false

                cameraSessionSemaphoreMbr.release()
                executorOnRequestComplete()
            }
        }
    }

    // (CameraSession 을 대기 상태로 만드는 함수)
    // 현재 세션이 repeating 이라면 이를 중단함.
    // 기존 설정을 모두 유지하는 중이라 다시 runCameraRequest 을 하면 기존 설정으로 실행됨
    fun pauseCameraSession(
        executorOnCameraPause: () -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()
            if (isRepeatingMbr) {
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            cameraSessionSemaphoreMbr.release()

            executorOnCameraPause()
        }
    }

    // (카메라 세션을 멈추는 함수)
    // 카메라 디바이스를 제외한 나머지 초기화 (= 서페이스 설정하기 이전 상태로 되돌리기)
    fun stopCameraObject(executorOnCameraStop: () -> Unit) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (카메라 상태 초기화)
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            for (previewConfigVo in previewConfigVoListMbr) {
                previewConfigVo.autoFitTextureView.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                            Unit
                    }
            }

            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            mediaRecorderMbr = null

            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()

            imageReaderMbr?.close()
            imageReaderMbr = null
            imageReaderConfigVoMbr = null

            cameraCaptureSessionMbr?.close()
            cameraCaptureSessionMbr = null

            captureRequestBuilderMbr = null

            cameraSessionSemaphoreMbr.release()

            executorOnCameraStop()
        }
    }

    // (카메라 객체를 초기화하는 함수)
    // 카메라 디바이스 까지 닫기
    fun clearCameraObject(executorOnCameraClear: () -> Unit) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (카메라 상태 초기화)
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            for (previewConfigVo in previewConfigVoListMbr) {
                previewConfigVo.autoFitTextureView.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                            Unit
                    }
            }

            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            mediaRecorderMbr = null

            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()

            imageReaderMbr?.close()
            imageReaderMbr = null
            imageReaderConfigVoMbr = null

            cameraCaptureSessionMbr?.close()
            cameraCaptureSessionMbr = null

            captureRequestBuilderMbr = null

            cameraDeviceMbr?.close()
            cameraDeviceMbr = null

            deleteCameraThreadName(cameraApiHandlerThreadObjMbr.threadName)
            deleteImageReaderThreadName(imageReaderHandlerThreadObjMbr.threadName)

            cameraApiHandlerThreadObjMbr.stopHandlerThread()
            imageReaderHandlerThreadObjMbr.stopHandlerThread()

            cameraSessionSemaphoreMbr.release()

            executorOnCameraClear()
        }
    }

    // (뷰 핀치 동작에 따른 줌 변경 리스너 주입 함수)
    // 뷰를 주입하면 해당 뷰를 핀칭할 때에 줌을 변경할수 있도록 리스너를 주입
    // 뷰를 여러번 넣으면 각각의 뷰에 핀칭을 할 때마다 줌을 변경
    // delta : 단위 핀치 이벤트에 따른 줌 변화량 = 높을수록 민감
    var beforePinchSpacingMbr: Float? = null
    fun setCameraPinchZoomTouchListener(view: View, delta: Float = 0.05f) {
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                // 형식 맞추기 코드
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {}
                    MotionEvent.ACTION_UP -> {
                        v!!.performClick()

                        // 손가락을 떼면 기존 핀치 너비 비우기
                        beforePinchSpacingMbr = null
                    }
                    else -> {}
                }

                if (event.pointerCount == 2) { // 핀치를 위한 더블 터치일 경우
                    // 현재 핀치 넓이 구하기
                    val currentFingerSpacing: Float
                    val x = event.getX(0) - event.getX(1)
                    val y = event.getY(0) - event.getY(1)
                    currentFingerSpacing = sqrt((x * x + y * y).toDouble()).toFloat()

                    if (beforePinchSpacingMbr != null) {
                        if (currentFingerSpacing > beforePinchSpacingMbr!!) { // 손가락을 벌린 경우
                            val zoom =
                                if ((currentCameraZoomFactorMbr + delta) > maxZoomMbr) {
                                    maxZoomMbr
                                } else {
                                    currentCameraZoomFactorMbr + delta
                                }
                            setZoomFactor(
                                zoom,
                                executorOnZoomSettingComplete = {})
                        } else if (currentFingerSpacing < beforePinchSpacingMbr!!) { // 손가락을 좁힌 경우
                            val zoom =
                                if ((currentCameraZoomFactorMbr - delta) < 1.0f) {
                                    1.0f
                                } else {
                                    currentCameraZoomFactorMbr - delta
                                }
                            setZoomFactor(
                                zoom,
                                executorOnZoomSettingComplete = {})
                        }
                    }

                    // 핀치 너비를 갱신
                    beforePinchSpacingMbr = currentFingerSpacing

                    return true
                } else {
                    return true
                }
            }
        })
    }

    // [미디어 레코더 변경 함수 모음]
    // prepare 는 서페이스 설정시 자동 실행
    // stop 은 cameraObject 를 stop 시킬 때, 혹은 setSurface 의 초기화 로직에서 자동으로 실행됨
    // 이외에 start, pause, resume 은 내부적으로 전혀 손대지 않기에 아래 함수로 사용자가 적절히 사용할 것.
    // camera pause 시 아직 mediaRecorder 가 녹화중이라면 화면은 움직이지 않고 계속 시간에 따라 녹화가 진행됨.
    // 고로 단순히 pause 후 연속 녹화를 하려면 외부에서 camera pause 이전에 mediaRecorder pause 를 먼저 해주는 것을 추천

    // (미디어 레코딩 시작)
    // 결과 코드 :
    // 0 : 정상 동작
    // 1 : 미디어 레코딩 서페이스가 없음
    // 2 : 현재 카메라가 동작중이 아님
    fun startMediaRecording(): Int {
        if (null == mediaCodecSurfaceMbr) {
            return 1
        }

        if (!isRepeatingMbr) {
            return 2
        }

        if (isRecordingMbr) {
            return 0
        }

        mediaRecorderMbr!!.start()

        isRecordingMbr = true

        return 0
    }

    // (미디어 레코딩 재시작)
    // 결과 코드 :
    // 0 : 정상 동작
    // 1 : 미디어 레코딩 서페이스가 없음
    // 2 : 현재 카메라가 동작중이 아님
    fun resumeMediaRecording(): Int {
        if (null == mediaCodecSurfaceMbr) {
            return 1
        }

        if (!isRepeatingMbr) {
            return 2
        }

        if (isRecordingMbr) {
            return 0
        }

        mediaRecorderMbr!!.resume()

        isRecordingMbr = true

        return 0
    }

    // (미디어 레코딩 일시정지)
    // 결과 코드 :
    // 0 : 정상 동작
    // 1 : 미디어 레코딩 중이 아님
    fun pauseMediaRecording(): Int {
        return if (isRecordingMbr) {
            mediaRecorderMbr?.pause()
            isRecordingMbr = false
            0
        } else {
            1
        }
    }


    // [카메라 상태 변경 함수 모음]
    // CameraObj 객체 안의 상태 멤버변수를 조절하며 실제 세션 설정에 반영하는 함수

    // (카메라 줌 비율을 변경하는 함수)
    // 카메라가 실행중 상태라면 즉시 반영
    // onZoomSettingComplete : 반환값 = 적용된 줌 펙터 값
    fun setZoomFactor(
        zoomFactor: Float,
        executorOnZoomSettingComplete: (Float) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()
            val zoom = if (maxZoomMbr < zoomFactor) {
                // 가용 줌 최대치에 설정을 맞추기
                maxZoomMbr
            } else {
                zoomFactor
            }

            currentCameraZoomFactorMbr = zoom

            if (captureRequestBuilderMbr != null) {
                val centerX =
                    sensorSize!!.width() / 2
                val centerY =
                    sensorSize.height() / 2
                val deltaX =
                    ((0.5f * sensorSize.width()) / zoom).toInt()
                val deltaY =
                    ((0.5f * sensorSize.height()) / zoom).toInt()

                val mCropRegion = Rect().apply {
                    set(
                        centerX - deltaX,
                        centerY - deltaY,
                        centerX + deltaX,
                        centerY + deltaY
                    )
                }

                captureRequestBuilderMbr!!.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)

                if (isRepeatingMbr) {
                    cameraCaptureSessionMbr!!.setRepeatingRequest(
                        captureRequestBuilderMbr!!.build(),
                        null,
                        cameraApiHandlerThreadObjMbr.handler
                    )
                    isRepeatingMbr = true

                    cameraSessionSemaphoreMbr.release()
                    executorOnZoomSettingComplete(zoom)
                } else {
                    cameraSessionSemaphoreMbr.release()
                    executorOnZoomSettingComplete(zoom)
                }
            } else {
                cameraSessionSemaphoreMbr.release()
                executorOnZoomSettingComplete(zoom)
            }
        }
    }

    // (손떨림 방지 설정)
    fun setCameraStabilization(
        stabilizationOn: Boolean,
        executorOnCameraStabilizationSettingComplete: () -> Unit
    ) {
        executorServiceMbr.execute {
            cameraSessionSemaphoreMbr.acquire()

            if (stabilizationOn) {
                // 보정 기능이 하나도 제공되지 않는 경우
                if (!isVideoStabilizationAvailableMbr &&
                    !isOpticalStabilizationAvailableMbr
                ) {
                    isCameraStabilizationSetMbr = false
                    cameraSessionSemaphoreMbr.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }
                isCameraStabilizationSetMbr = true

                if (captureRequestBuilderMbr == null) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                // 기계 보정이 가능하면 사용하고, 아니라면 소프트웨어 보정을 적용
                if (isOpticalStabilizationAvailableMbr) {
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
                } else if (isVideoStabilizationAvailableMbr) {
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    )
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    )
                }

                if (!isRepeatingMbr) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraApiHandlerThreadObjMbr.handler
                )

                cameraSessionSemaphoreMbr.release()
                executorOnCameraStabilizationSettingComplete()
            } else {
                isCameraStabilizationSetMbr = false

                if (captureRequestBuilderMbr == null) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                captureRequestBuilderMbr!!.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
                captureRequestBuilderMbr!!.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )

                if (!isRepeatingMbr) {
                    cameraSessionSemaphoreMbr.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraApiHandlerThreadObjMbr.handler
                )

                cameraSessionSemaphoreMbr.release()
                executorOnCameraStabilizationSettingComplete()
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (startCamera 함수 서페이스 준비가 끝난 시점의 처리 함수)
    private fun onSurfacesAllChecked(
        onSurfaceAllReady: () -> Unit,
        onError: (Int) -> Unit
    ) {
        if (previewConfigVoListMbr.isEmpty() &&
            imageReaderMbr == null &&
            mediaCodecSurfaceMbr == null
        ) { // 생성 서페이스가 하나도 존재하지 않으면,
            cameraSessionSemaphoreMbr.release()
            onError(6)
            return
        }

        // (카메라 디바이스 열기)
        openCameraDevice(
            onCameraDeviceReady = {
                for (previewConfigVo in previewConfigVoListMbr) {
                    val surfaceTexture = previewConfigVo.autoFitTextureView.surfaceTexture!!
                    surfaceTexture.setDefaultBufferSize(
                        previewConfigVo.cameraOrientSurfaceSize.width,
                        previewConfigVo.cameraOrientSurfaceSize.height
                    )

                    previewSurfaceListMbr.add(Surface(surfaceTexture))
                }

                // (카메라 세션 생성)
                createCameraSessionAsync(
                    onCaptureSessionCreated = {
                        cameraSessionSemaphoreMbr.release()
                        onSurfaceAllReady()
                    },
                    onError = { errorCode, cameraCaptureSession ->
                        // (카메라 상태 초기화)
                        imageReaderMbr?.setOnImageAvailableListener(null, null)

                        if (isRecordingMbr) {
                            mediaRecorderMbr?.stop()
                            mediaRecorderMbr?.reset()
                            isRecordingMbr = false

                            cameraCaptureSessionMbr?.stopRepeating()
                            isRepeatingMbr = false
                        } else if (isRepeatingMbr) {
                            cameraCaptureSessionMbr?.stopRepeating()
                            isRepeatingMbr = false
                        }

                        for (previewConfigVo in previewConfigVoListMbr) {
                            previewConfigVo.autoFitTextureView.surfaceTextureListener =
                                object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(
                                        surface: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) = Unit

                                    override fun onSurfaceTextureSizeChanged(
                                        surface: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) = Unit

                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                        true

                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                        Unit
                                }
                        }

                        mediaRecorderMbr?.release()
                        mediaCodecSurfaceMbr?.release()
                        mediaCodecSurfaceMbr = null
                        mediaRecorderConfigVoMbr = null
                        mediaRecorderMbr = null

                        previewConfigVoListMbr.clear()
                        previewSurfaceListMbr.clear()

                        imageReaderMbr?.close()
                        imageReaderMbr = null
                        imageReaderConfigVoMbr = null

                        cameraCaptureSession.close()
                        cameraCaptureSessionMbr = null

                        captureRequestBuilderMbr = null

                        cameraSessionSemaphoreMbr.release()
                        onError(errorCode)
                    }
                )
            },
            onCameraDisconnected = {
                // (카메라 상태 초기화)
                cameraSessionSemaphoreMbr.release()
                onCameraDisconnectedMbr()
            },
            onError = { errorCode ->
                cameraSessionSemaphoreMbr.release()
                onError(errorCode)
            }
        )
    }

    // 카메라 디바이스 생성
    private fun openCameraDevice(
        onCameraDeviceReady: () -> Unit,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        if (cameraDeviceMbr != null) {
            onCameraDeviceReady()
            return
        }

        // (카메라 권한 검사)
        if (ActivityCompat.checkSelfPermission(
                parentActivityMbr,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onError(7)
            return
        }

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
                    // (카메라 상태 초기화)
                    imageReaderMbr?.setOnImageAvailableListener(null, null)

                    if (isRecordingMbr) {
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        isRecordingMbr = false

                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    } else if (isRepeatingMbr) {
                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    }

                    for (previewConfigVo in previewConfigVoListMbr) {
                        previewConfigVo.autoFitTextureView.surfaceTextureListener =
                            object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) = Unit

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) = Unit

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                    true

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                    Unit
                            }
                    }

                    mediaRecorderMbr?.release()
                    mediaCodecSurfaceMbr?.release()
                    mediaCodecSurfaceMbr = null
                    mediaRecorderConfigVoMbr = null
                    mediaRecorderMbr = null

                    previewConfigVoListMbr.clear()
                    previewSurfaceListMbr.clear()

                    imageReaderMbr?.close()
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null

                    cameraCaptureSessionMbr?.close()
                    cameraCaptureSessionMbr = null

                    captureRequestBuilderMbr = null

                    camera.close()
                    cameraDeviceMbr = null

                    onCameraDisconnected()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    // (카메라 상태 초기화)
                    imageReaderMbr?.setOnImageAvailableListener(null, null)

                    if (isRecordingMbr) {
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        isRecordingMbr = false

                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    } else if (isRepeatingMbr) {
                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    }

                    for (previewConfigVo in previewConfigVoListMbr) {
                        previewConfigVo.autoFitTextureView.surfaceTextureListener =
                            object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) = Unit

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) = Unit

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                    true

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                    Unit
                            }
                    }

                    mediaRecorderMbr?.release()
                    mediaCodecSurfaceMbr?.release()
                    mediaCodecSurfaceMbr = null
                    mediaRecorderConfigVoMbr = null
                    mediaRecorderMbr = null

                    previewConfigVoListMbr.clear()
                    previewSurfaceListMbr.clear()

                    imageReaderMbr?.close()
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null

                    cameraCaptureSessionMbr?.close()
                    cameraCaptureSessionMbr = null

                    captureRequestBuilderMbr = null

                    camera.close()
                    cameraDeviceMbr = null

                    when (error) {
                        ERROR_CAMERA_DISABLED -> {
                            onError(8)
                        }
                        ERROR_CAMERA_IN_USE -> {
                            onError(9)
                        }
                        ERROR_MAX_CAMERAS_IN_USE -> {
                            onError(10)
                        }
                        ERROR_CAMERA_DEVICE -> {
                            onError(11)
                        }
                        ERROR_CAMERA_SERVICE -> {
                            onError(12)
                        }
                    }
                }
            }, cameraApiHandlerThreadObjMbr.handler
        )
    }

    private fun configureTransform(
        autoFitTextureView: AutoFitTextureView,
        surfaceWidth: Int,
        surfaceHeight: Int,
        viewWidth: Int,
        viewHeight: Int
    ) {
        val matrix = Matrix()
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parentActivityMbr.display!!.rotation
        } else {
            parentActivityMbr.windowManager.defaultDisplay.rotation
        }
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0f,
            0f,
            surfaceHeight.toFloat(),
            surfaceWidth.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale =
                (viewHeight.toFloat() / viewHeight).coerceAtLeast(viewWidth.toFloat() / surfaceWidth)
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }
        autoFitTextureView.setTransform(matrix)
    }

    private fun createCameraSessionAsync(
        onCaptureSessionCreated: () -> Unit,
        onError: (Int, CameraCaptureSession) -> Unit
    ) {
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
            if (null != mediaCodecSurfaceMbr) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        mediaCodecSurfaceMbr!!
                    )
                )
            }

            cameraDeviceMbr?.createCaptureSession(SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurationList,
                HandlerExecutor(cameraApiHandlerThreadObjMbr.handler!!.looper),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr = session

                        onCaptureSessionCreated()
                    }

                    // 세션 생성 실패
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError(13, session)
                    }
                }
            ))
        } else {
            // api 28 미만
            // 출력 서페이스 리스트
            val surfaces = ArrayList<Surface>()

            // 프리뷰 서페이스 주입
            surfaces.addAll(previewSurfaceListMbr)

            // 이미지 리더 서페이스 주입
            if (null != imageReaderMbr) {
                val surface = imageReaderMbr!!.surface
                surfaces.add(surface)
            }

            // 비디오 리코더 서페이스 주입
            if (null != mediaCodecSurfaceMbr) {
                val surface = mediaCodecSurfaceMbr!!
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
                        onError(13, session)
                    }
                }, cameraApiHandlerThreadObjMbr.handler
            )
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <내부 클래스 공간>
    data class PreviewConfigVo(
        val cameraOrientSurfaceSize: Size,
        val autoFitTextureView: AutoFitTextureView
    )

    data class ImageReaderConfigVo(
        val cameraOrientSurfaceSize: Size,
        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class MediaRecorderConfigVo(
        val cameraOrientSurfaceSize: Size,
        var mediaRecordingMp4File: File,
        val recordingFps: Int?,
        val videoEncodingBitrate: Int?,
        val isAudioRecording: Boolean
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