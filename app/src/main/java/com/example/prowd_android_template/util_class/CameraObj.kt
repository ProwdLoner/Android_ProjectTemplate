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

// todo : 180 도 회전시 프리뷰 거꾸로 나오는 문제(restart 가 되지 않고 있음)
// todo : 사진 찍기 기능 검증
// todo : 서페이스 각자 세팅 기능 오버로딩(request setting callback 을 제거)
// todo : exposure, whitebalance 등을 내부 멤버변수로 두고 자동, 수동 모드 변경 및 수동 수치 조작 가능하게
// todo : 클릭 exposure, whitebalance, focus 등 (핀치 줌을 참고)
// todo : 전체 검증 : 특히 setSurface 의 디바이스 방향
// todo : 함수 실행에 카메라 스레드가 아닌 익시큐터 사용때, 빠르게 회전시 멈춤
//  = 익시큐터 내 외부에 카메라 스레드 사용 or 세마포어 락 에러로 보임. 즉, 할수있다면 락 시간을 설정
//    혹은 익시큐터가 소멸하나? 그래서 release 가 되지 않는지도...
//  카메라 스레드만 사용시에는 프리뷰가 일그러짐
//  그러면 현재 사용중인 카메라 스레드를 최외곽에 두고, 이에 세마포어를 먹이고 익시큐터를 실행시키면 어떨까?
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
    // 함수 사용 스레드풀
    private var executorServiceMbr: ExecutorService = Executors.newCachedThreadPool()

    // 카메라 API 사용 스레드 세트
    private val cameraThreadVoMbr: CameraIdThreadVo =
        publishSharedCameraIdThreadVoOnStaticMemory(cameraIdMbr)


    // [카메라 정보]
    // (현 디바이스 방향과 카메라 방향에서 width, height 개념이 같은)
    // 카메라와 디바이스 방향이 90도, 270 도 차이가 난다면 둘의 Width, Height 개념은 상반됨
    var isDeviceAndCameraWhSameMbr: Boolean = false
        private set
        get() {
            // 디바이스 물리적 특정 방향을 0으로 뒀을 때의 현 디바이스가 반시계 방향으로 몇도가 돌아갔는가
            val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivityMbr.display!!.rotation
            } else {
                parentActivityMbr.windowManager.defaultDisplay.rotation
            }

            return when (deviceOrientation) {
                Surface.ROTATION_90, Surface.ROTATION_270 -> {
                    // 디바이스 현 방향이 90도 단위로 기울어졌을 때
                    when (sensorOrientationMbr) {
                        // 센서 방향이 물리적 특정 방향에서 n*90 도 회전 된 것을 기반하여
                        0, 180 -> {
                            false
                        }
                        else -> {
                            true
                        }
                    }
                }

                else -> {
                    // 디바이스 현 방향이 90도 단위로 기울어지지 않았을 때
                    when (sensorOrientationMbr) {
                        // 센서 방향이 물리적 특정 방향에서 n*90 도 회전 된 것을 기반하여
                        0, 180 -> {
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            }
        }

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
    val sensorSizeMbr =
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
    // <스태틱 공간>
    companion object {
        // (카메라 실행 스레드 리스트와 발행 세마포어)
        private val cameraIdThreadVoList: ArrayList<CameraIdThreadVo> = ArrayList()
        private val cameraIdThreadVoListSemaphore: Semaphore = Semaphore(1)

        // (전역에서 해당 카메라 아이디에 공유되는 스레드 객체를 반환)
        // 물리적인 한 카메라는 서로 다른 곳에서 조작되어도 동일한 스레드 공간과 동일한 세마포어를 사용
        // == 한 카메라에 비동기적으로 동시 명령을 금지
        private fun publishSharedCameraIdThreadVoOnStaticMemory(cameraId: String): CameraIdThreadVo {
            cameraIdThreadVoListSemaphore.acquire()

            // 해당 아이디에 기존 발행된 스레드가 존재하는지 찾기
            val listIdx = cameraIdThreadVoList.indexOfFirst {
                it.cameraHandlerThreadObj.threadName == cameraId
            }

            if (-1 == listIdx) { // 기존에 발행된 스레드 객체가 없다면,
                // 새로운 스레드 객체를 발행
                val cameraThreadVo = CameraIdThreadVo(
                    Semaphore(1),
                    HandlerThreadObj(cameraId),
                    HandlerThreadObj(cameraId),
                    1
                )

                // 새로운 핸들러 스레드 실행
                cameraThreadVo.cameraHandlerThreadObj.startHandlerThread()
                cameraThreadVo.imageReaderHandlerThreadObj.startHandlerThread()

                cameraIdThreadVoList.add(
                    cameraThreadVo
                )

                cameraIdThreadVoListSemaphore.release()
                return cameraThreadVo
            } else { // 기존에 발행된 스레드 객체가 있다면,
                // 기존 스레드 객체를 가져오고 publishCount +1
                val cameraThreadVo = cameraIdThreadVoList[listIdx]
                cameraThreadVo.publishCount += 1

                cameraIdThreadVoListSemaphore.release()
                return cameraThreadVo
            }
        }

        // (카메라 스레드를 전역 메모리에서 지우도록 요청)
        // todo : destroy 가 적용 안되는 공간 확인 (camera disconnect 같은 것)
        // publish count 가 1 이하라면 삭제, 2 부터는 -1
        private fun requestForDeleteSharedCameraIdThreadVoOnStaticMemory(cameraId: String) {
            cameraIdThreadVoListSemaphore.acquire()

            // 해당 아이디에 기존 발행된 스레드가 존재하는지 찾기
            val listIdx = cameraIdThreadVoList.indexOfFirst {
                it.cameraHandlerThreadObj.threadName == cameraId
            }

            if (listIdx == -1) { // 발행된적 없는 경우
                // 그냥 종료
                cameraIdThreadVoListSemaphore.release()
                return
            } else { // 발행된 적 있는 경우
                val cameraThreadVo = cameraIdThreadVoList[listIdx]

                if (cameraThreadVo.publishCount <= 1) { // publish count 가 1일 경우(발행이 한번만 된 경우)
                    // 기존 실행 핸들러 스레드 종료
                    cameraThreadVo.cameraHandlerThreadObj.stopHandlerThread()
                    cameraThreadVo.imageReaderHandlerThreadObj.stopHandlerThread()

                    // 기존 발행을 삭제
                    cameraIdThreadVoList.removeAt(listIdx)

                    cameraIdThreadVoListSemaphore.release()
                    return
                } else { // publish count 가 복수개일 경우
                    // 발행 숫자를 낮추기
                    cameraThreadVo.publishCount -= 1

                    cameraIdThreadVoListSemaphore.release()
                    return
                }
            }
        }

        // (객체 생성 함수 = 조건에 맞지 않으면 null 반환)
        // 조작하길 원하는 카메라 ID 를 설정하여 해당 카메라 정보를 생성
        fun getInstance(
            parentActivity: Activity,
            cameraId: String,
            onCameraDisconnectedAndClearCamera: (() -> Unit)
        ): CameraObj? {
            // [카메라 디바이스 유무 검증]
            if (!parentActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                // 카메라 장치가 없다면 null 반환
                return null
            }

            // [카메라 필수 데이터 검증]
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

            val resultCameraObject = CameraObj(
                parentActivity,
                cameraId,
                cameraManager,
                cameraCharacteristics,
                streamConfigurationMap,
                previewSurfaceSupportedSizeList,
                imageReaderSurfaceSupportedSizeList,
                mediaRecorderSurfaceSupportedSizeList,
                sensorOrientationMbr,
                onCameraDisconnectedAndClearCamera
            )

            // [카메라 객체 내부 멤버변수 생성]
            // (max zoom 정보)
            resultCameraObject.maxZoomMbr = if (resultCameraObject.sensorSizeMbr == null) {
                1.0f
            } else {
                val maxZoom =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

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
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)

            if (availableOpticalStabilization != null) {
                for (mode in availableOpticalStabilization) {
                    if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                        resultCameraObject.isOpticalStabilizationAvailableMbr = true
                    }
                }
            }

            // (소프트웨어 떨림 보정 정보)
            val availableVideoStabilization =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)

            if (availableVideoStabilization != null) {
                for (mode in availableVideoStabilization) {
                    if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                        resultCameraObject.isVideoStabilizationAvailableMbr = true
                    }
                }
            }

            return resultCameraObject
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
    // 15 : 프리뷰 서페이스가 비어있음
    fun setCameraOutputSurfaces(
        previewConfigVoList: ArrayList<PreviewConfigVo>?,
        imageReaderConfigVo: ImageReaderConfigVo?,
        mediaRecorderConfigVo: MediaRecorderConfigVo?,
        executorOnSurfaceAllReady: () -> Unit,
        executorOnError: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [조건 검증]
            // (서페이스 설정 파라미터 개수 검사)
            if ((imageReaderConfigVo == null &&
                        mediaRecorderConfigVo == null &&
                        (previewConfigVoList == null ||
                                previewConfigVoList.isEmpty()))
            ) {
                cameraThreadVoMbr.cameraSemaphore.release()
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
                    cameraThreadVoMbr.cameraSemaphore.release()
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
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(4)
                    return@execute
                } else {
                    for (previewConfig in previewConfigVoList) {
                        if (cameraSizes.indexOfFirst {
                                it.width == previewConfig.cameraOrientSurfaceSize.width &&
                                        it.height == previewConfig.cameraOrientSurfaceSize.height
                            } == -1) {
                            cameraThreadVoMbr.cameraSemaphore.release()
                            executorOnError(4)
                            return@execute
                        }
                    }
                }
            }


            // [카메라 상태 초기화]
            // 이미지 리더 요청을 먼저 비우기
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                // 레코딩 중이라면 레코더 종료 후 세션 중지
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                // 세션이 실행중이라면 중지
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            // 프리뷰가 설정되어 있다면 리스너 비우기
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

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            imageReaderMbr?.close()
            cameraCaptureSessionMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            imageReaderMbr = null
            imageReaderConfigVoMbr = null
            cameraCaptureSessionMbr = null
            captureRequestBuilderMbr = null
            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()


            // [서페이스 설정 검증 및 준비]
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
                        cameraThreadVoMbr.imageReaderHandlerThreadObj.handler
                    )
                }
            }

            // (미디어 레코더 서페이스 준비)
            if (mediaRecorderConfigVo != null) {
                // 오디오 권한 검증
                // 녹음 설정을 했는데 권한이 없을 때, 에러
                if (mediaRecorderConfigVo.isAudioRecording &&
                    ActivityCompat.checkSelfPermission(
                        parentActivityMbr,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // 전 알고리즘까지 초기화
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(5)
                    return@execute
                }

                // 설정 파일 확장자 검증
                if (mediaRecorderConfigVo.mediaRecordingMp4File.extension != "mp4") {
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(14)
                    return@execute
                }

                mediaRecorderConfigVoMbr = mediaRecorderConfigVo

                // 미디어 코덱 서페이스 생성
                mediaCodecSurfaceMbr = MediaCodec.createPersistentInputSurface()

                // 미디어 레코더 생성
                mediaRecorderMbr =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(parentActivityMbr)
                    } else {
                        MediaRecorder()
                    }

                // (레코더 정보 생성)
                // 비디오 FPS
                val spf = (streamConfigurationMapMbr.getOutputMinFrameDuration(
                    MediaRecorder::class.java,
                    mediaRecorderConfigVo.cameraOrientSurfaceSize
                ) / 1_000_000_000.0)
                val mediaRecorderFps = if (spf > 0) (1.0 / spf).toInt() else 0

                // (미디어 레코더 설정)
                // 서페이스 소스 설정
                if (mediaRecorderConfigVo.isAudioRecording) {
                    mediaRecorderMbr!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                mediaRecorderMbr!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)

                // 파일 포멧 설정
                mediaRecorderMbr!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // 파일 경로 설정
                // 만일 같은 파일이 이전에 비휘발 메모리에 존재하면 새로써짐
                if (mediaRecorderConfigVo.mediaRecordingMp4File.exists()) {
                    mediaRecorderConfigVo.mediaRecordingMp4File.delete()
                }
                mediaRecorderConfigVo.mediaRecordingMp4File.createNewFile()
                mediaRecorderMbr!!.setOutputFile(mediaRecorderConfigVo.mediaRecordingMp4File.absolutePath)

                // todo : report 사용
                // todo : 최적값 찾기 videoEncodingBitrate calc
                // 데이터 저장 퀄리티 설정
                // todo : setAudioEncoding 도 적용
                if (mediaRecorderConfigVo.videoEncodingBitrate == null) {
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
                // todo 검증
                val rotateCounterClockAngle: Int =
                    when (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        parentActivityMbr.display!!.rotation
                    } else {
                        parentActivityMbr.windowManager.defaultDisplay.rotation
                    }) {
                        Surface.ROTATION_0 -> { // 카메라 기본 방향
                            // if sensorOrientationMbr = 90 -> 270
                            360 - sensorOrientationMbr
                        }
                        Surface.ROTATION_90 -> { // 카메라 기본 방향에서 역시계 방향 90도 회전 상태
                            // if sensorOrientationMbr = 90 -> 0
                            90 - sensorOrientationMbr
                        }
                        Surface.ROTATION_180 -> {
                            // if sensorOrientationMbr = 90 -> 90
                            180 - sensorOrientationMbr
                        }
                        Surface.ROTATION_270 -> {
                            // if sensorOrientationMbr = 90 -> 180
                            270 - sensorOrientationMbr
                        }
                        else -> {
                            0
                        }
                    }

                mediaRecorderMbr!!.setOrientationHint(rotateCounterClockAngle)

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
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [조건 검증]
            if (cameraDeviceMbr == null) {
                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnError(1)
                return@execute
            }

            if (previewConfigVoListMbr.isEmpty() &&
                imageReaderMbr == null &&
                mediaCodecSurfaceMbr == null
            ) { // 생성 서페이스가 하나도 존재하지 않으면,
                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnError(2)
                return@execute
            }


            // [리퀘스트 빌더 생성]
            captureRequestBuilderMbr =
                cameraDeviceMbr!!.createCaptureRequest(cameraRequestMode)


            // [타겟 서페이스 설정]
            if (onPreview) { // 프리뷰 사용 설정
                if (previewConfigVoListMbr.isEmpty()) { // 프리뷰 사용 설정인데 프리뷰 서페이스가 없을 때
                    captureRequestBuilderMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(3)
                    return@execute
                } else {
                    // 프리뷰 서페이스 타겟 추가
                    for (previewSurface in previewSurfaceListMbr) {
                        captureRequestBuilderMbr!!.addTarget(previewSurface)
                    }
                }
            }

            if (onImageReader) { // 이미지 리더 사용 설정
                if (imageReaderMbr == null) { // 이미지 리더 사용 설정인데 이미지 리더 서페이스가 없을 때
                    captureRequestBuilderMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(4)
                    return@execute
                } else {
                    // 이미지 리더 서페이스 타겟 추가
                    captureRequestBuilderMbr!!.addTarget(imageReaderMbr!!.surface)
                }
            }

            if (onMediaRecorder) { // 미디어 레코더 사용 설정
                if (mediaCodecSurfaceMbr == null) { // 미디어 레코더 사용 설정인데 미디어 레코더 서페이스가 없을 때
                    captureRequestBuilderMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(5)
                    return@execute
                } else {
                    // 미디어 레코더 서페이스 타겟 추가
                    captureRequestBuilderMbr!!.addTarget(mediaCodecSurfaceMbr!!)
                }
            }


            // [사용자에게 설정 정보를 받아오는 공간 제공]
            executorOnCameraRequestSettingTime(captureRequestBuilderMbr!!)


            // [카메라 오브젝트 내부 정보를 최종적으로 채택]
            // todo : 이 최종 영역 설정을 점차 늘리고, 위의 커스텀 설정 공간은 최종적으로 없애버릴 것
            // (오브젝트 내부 줌 정보를 설정)
            if (sensorSizeMbr != null) {
                val zoom = if (maxZoomMbr < currentCameraZoomFactorMbr) {
                    // 가용 줌 최대치에 설정을 맞추기
                    maxZoomMbr
                } else {
                    currentCameraZoomFactorMbr
                }

                // 센서 사이즈 중심점
                val centerX =
                    sensorSizeMbr.width() / 2
                val centerY =
                    sensorSizeMbr.height() / 2

                // 센서 사이즈에서 중심을 기반 크롭 박스 설정
                // zoom 은 확대 비율로, 센서 크기에서 박스의 크기가 작을수록 줌 레벨이 올라감
                val deltaX =
                    ((0.5f * sensorSizeMbr.width()) / zoom).toInt()
                val deltaY =
                    ((0.5f * sensorSizeMbr.height()) / zoom).toInt()

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

            // (카메라 떨림 보정 여부 반영)
            if (isCameraStabilizationSetMbr) { // 떨림 보정 on
                // 기계 보정이 가능하면 사용하고, 아니라면 소프트웨어 보정을 적용
                if (isOpticalStabilizationAvailableMbr) { // 기계 보정 사용 가능
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    )
                    captureRequestBuilderMbr!!.set(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    )
                } else if (isVideoStabilizationAvailableMbr) { // 소프트웨어 보정 사용 가능
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


            // [기존 세션이 실행중이라면 곧바로 설정 적용]
            if (isRepeatingMbr) {
                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraThreadVoMbr.cameraHandlerThreadObj.handler
                )
            }

            cameraThreadVoMbr.cameraSemaphore.release()
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
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (cameraCaptureSessionMbr == null) {
                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnError(1)
                return@execute
            }

            if (captureRequestBuilderMbr == null) {
                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnError(2)
                return@execute
            }

            if (isRepeating) {
                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraThreadVoMbr.cameraHandlerThreadObj.handler
                )
                isRepeatingMbr = true

                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnRequestComplete()
            } else {
                if (null == imageReaderMbr) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnError(3)
                    return@execute
                }

                cameraCaptureSessionMbr!!.capture(
                    captureRequestBuilderMbr!!.build(),
                    captureCallback,
                    cameraThreadVoMbr.cameraHandlerThreadObj.handler
                )

                isRepeatingMbr = false

                cameraThreadVoMbr.cameraSemaphore.release()
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
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (isRepeatingMbr) {
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            cameraThreadVoMbr.cameraSemaphore.release()

            executorOnCameraPause()
        }
    }

    // (카메라 세션을 멈추는 함수)
    // 카메라 디바이스를 제외한 나머지 초기화 (= 서페이스 설정하기 이전 상태로 되돌리기)
    fun stopCameraObject(executorOnCameraStop: () -> Unit) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [카메라 상태 초기화]
            // 이미지 리더 요청을 먼저 비우기
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                // 레코딩 중이라면 레코더 종료 후 세션 중지
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                // 세션이 실행중이라면 중지
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            // 프리뷰가 설정되어 있다면 리스너 비우기
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

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            imageReaderMbr?.close()
            cameraCaptureSessionMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            imageReaderMbr = null
            imageReaderConfigVoMbr = null
            cameraCaptureSessionMbr = null
            captureRequestBuilderMbr = null
            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()

            cameraThreadVoMbr.cameraSemaphore.release()

            executorOnCameraStop()
        }
    }

    // (카메라 객체를 초기화하는 함수)
    // 카메라 객체 생성자와 대비되는 함수로, 생성했으면 소멸을 시켜야 함.
    fun destroyCameraObject(executorOnCameraClear: () -> Unit) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [카메라 상태 초기화]
            // 이미지 리더 요청을 먼저 비우기
            imageReaderMbr?.setOnImageAvailableListener(null, null)

            if (isRecordingMbr) {
                // 레코딩 중이라면 레코더 종료 후 세션 중지
                mediaRecorderMbr?.stop()
                mediaRecorderMbr?.reset()
                isRecordingMbr = false

                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            } else if (isRepeatingMbr) {
                // 세션이 실행중이라면 중지
                cameraCaptureSessionMbr?.stopRepeating()
                isRepeatingMbr = false
            }

            // 프리뷰가 설정되어 있다면 리스너 비우기
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

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            imageReaderMbr?.close()
            cameraCaptureSessionMbr?.close()
            cameraDeviceMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            imageReaderMbr = null
            imageReaderConfigVoMbr = null
            cameraCaptureSessionMbr = null
            captureRequestBuilderMbr = null
            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()
            cameraDeviceMbr = null

            requestForDeleteSharedCameraIdThreadVoOnStaticMemory(cameraIdMbr)

            cameraThreadVoMbr.cameraSemaphore.release()

            executorOnCameraClear()
        }
    }

    // todo : 핀치 크기에 따라 델타 값 차등 적용
    // (뷰 핀치 동작에 따른 줌 변경 리스너 주입 함수)
    // 뷰를 주입하면 해당 뷰를 핀칭할 때에 줌을 변경할수 있도록 리스너를 주입
    // 뷰를 여러번 넣으면 각각의 뷰에 핀칭을 할 때마다 줌을 변경
    // delta : 단위 핀치 이벤트에 따른 줌 변화량 = 높을수록 민감
    var beforePinchSpacingMbr: Float? = null
    fun setCameraPinchZoomTouchListener(view: View, delta: Float = 0.05f) {
        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {}
                    MotionEvent.ACTION_UP -> {
                        // 형식 맞추기 코드
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
    fun startMediaRecording(
        onComplete: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()
            if (null == mediaCodecSurfaceMbr) { // 미디어 레코딩 서페이스 설정을 하지 않았을 때
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(1)
                return@execute
            }

            if (!isRepeatingMbr) { // 카메라 실행 중이 아닐 때
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(2)
                return@execute
            }

            if (isRecordingMbr) { // 이미 녹화 중일 때
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(0)
                return@execute
            }

            mediaRecorderMbr!!.start()

            isRecordingMbr = true

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete(0)
        }
    }

    // (미디어 레코딩 재시작)
    // 결과 코드 :
    // 0 : 정상 동작
    // 1 : 미디어 레코딩 서페이스가 없음
    // 2 : 현재 카메라가 동작중이 아님
    fun resumeMediaRecording(
        onComplete: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()
            if (null == mediaCodecSurfaceMbr) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(1)
                return@execute
            }

            if (!isRepeatingMbr) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(2)
                return@execute
            }

            if (isRecordingMbr) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(0)
                return@execute
            }

            mediaRecorderMbr!!.resume()

            isRecordingMbr = true

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete(0)
        }
    }

    // (미디어 레코딩 일시정지)
    // 결과 코드 :
    // 0 : 정상 동작
    // 1 : 미디어 레코딩 중이 아님
    fun pauseMediaRecording(
        onComplete: (Int) -> Unit
    ) {
        executorServiceMbr.execute {
            cameraThreadVoMbr.cameraSemaphore.acquire()
            if (isRecordingMbr) {
                mediaRecorderMbr?.pause()
                isRecordingMbr = false
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(0)
            } else {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete(1)
            }
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
            cameraThreadVoMbr.cameraSemaphore.acquire()
            val zoom = if (maxZoomMbr < zoomFactor) {
                // 가용 줌 최대치에 설정을 맞추기
                maxZoomMbr
            } else {
                zoomFactor
            }

            currentCameraZoomFactorMbr = zoom

            if (captureRequestBuilderMbr != null && sensorSizeMbr != null) {
                val centerX =
                    sensorSizeMbr.width() / 2
                val centerY =
                    sensorSizeMbr.height() / 2
                val deltaX =
                    ((0.5f * sensorSizeMbr.width()) / zoom).toInt()
                val deltaY =
                    ((0.5f * sensorSizeMbr.height()) / zoom).toInt()

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
                        cameraThreadVoMbr.cameraHandlerThreadObj.handler
                    )
                    isRepeatingMbr = true

                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnZoomSettingComplete(zoom)
                } else {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnZoomSettingComplete(zoom)
                }
            } else {
                cameraThreadVoMbr.cameraSemaphore.release()
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
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (stabilizationOn) {
                // 보정 기능이 하나도 제공되지 않는 경우
                if (!isVideoStabilizationAvailableMbr &&
                    !isOpticalStabilizationAvailableMbr
                ) {
                    isCameraStabilizationSetMbr = false
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }
                isCameraStabilizationSetMbr = true

                if (captureRequestBuilderMbr == null) {
                    cameraThreadVoMbr.cameraSemaphore.release()
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
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraThreadVoMbr.cameraHandlerThreadObj.handler
                )

                cameraThreadVoMbr.cameraSemaphore.release()
                executorOnCameraStabilizationSettingComplete()
            } else {
                isCameraStabilizationSetMbr = false

                if (captureRequestBuilderMbr == null) {
                    cameraThreadVoMbr.cameraSemaphore.release()
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
                    cameraThreadVoMbr.cameraSemaphore.release()
                    executorOnCameraStabilizationSettingComplete()
                    return@execute
                }

                cameraCaptureSessionMbr!!.setRepeatingRequest(
                    captureRequestBuilderMbr!!.build(),
                    null,
                    cameraThreadVoMbr.cameraHandlerThreadObj.handler
                )

                cameraThreadVoMbr.cameraSemaphore.release()
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
            cameraThreadVoMbr.cameraSemaphore.release()
            onError(6)
            return
        }

        // (카메라 디바이스 열기)
        openCameraDevice(
            onCameraDeviceReady = { // 카메라 디바이스가 준비된 시점
                for (previewConfigVo in previewConfigVoListMbr) {
                    val surfaceTexture = previewConfigVo.autoFitTextureView.surfaceTexture

                    if (surfaceTexture == null) { // 프리뷰 설정이 존재할 때 서페이스 텍스쳐가 반환되지 않은 경우
                        // 준비 서페이스 초기화
                        // 프리뷰가 설정되어 있다면 리스너 비우기
                        for (previewConfigVo1 in previewConfigVoListMbr) {
                            previewConfigVo1.autoFitTextureView.surfaceTextureListener =
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

                        // (자원 해소)
                        mediaRecorderMbr?.release()
                        mediaCodecSurfaceMbr?.release()
                        imageReaderMbr?.close()

                        // (멤버 변수 비우기)
                        mediaRecorderMbr = null
                        mediaCodecSurfaceMbr = null
                        mediaRecorderConfigVoMbr = null
                        imageReaderMbr = null
                        imageReaderConfigVoMbr = null
                        previewConfigVoListMbr.clear()
                        previewSurfaceListMbr.clear()

                        cameraThreadVoMbr.cameraSemaphore.release()
                        onError(15)
                        return@openCameraDevice
                    }

                    surfaceTexture.setDefaultBufferSize(
                        previewConfigVo.cameraOrientSurfaceSize.width,
                        previewConfigVo.cameraOrientSurfaceSize.height
                    )

                    previewSurfaceListMbr.add(Surface(surfaceTexture))
                }

                // (카메라 세션 생성)
                createCameraSessionAsync(
                    onCaptureSessionCreated = {
                        cameraThreadVoMbr.cameraSemaphore.release()
                        onSurfaceAllReady()
                    },
                    onErrorAndClearSurface = { errorCode ->
                        cameraThreadVoMbr.cameraSemaphore.release()
                        onError(errorCode)
                    }
                )
            },
            onCameraDisconnectedAndClearCamera = {
                // (카메라 상태 초기화)
                cameraThreadVoMbr.cameraSemaphore.release()
                onCameraDisconnectedMbr()
            },
            onErrorAndClearSurface = { errorCode ->
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(errorCode)
            }
        )
    }

    // 카메라 디바이스 생성
    private fun openCameraDevice(
        onCameraDeviceReady: () -> Unit,
        onCameraDisconnectedAndClearCamera: () -> Unit,
        onErrorAndClearSurface: (Int) -> Unit
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
            // 준비 서페이스 초기화
            // 프리뷰가 설정되어 있다면 리스너 비우기
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

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            imageReaderMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaCodecSurfaceMbr = null
            mediaRecorderConfigVoMbr = null
            imageReaderMbr = null
            imageReaderConfigVoMbr = null
            previewConfigVoListMbr.clear()
            previewSurfaceListMbr.clear()

            onErrorAndClearSurface(7)
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
                    // [카메라 상태 초기화]
                    // 이미지 리더 요청을 먼저 비우기
                    imageReaderMbr?.setOnImageAvailableListener(null, null)

                    if (isRecordingMbr) {
                        // 레코딩 중이라면 레코더 종료 후 세션 중지
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        isRecordingMbr = false

                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    } else if (isRepeatingMbr) {
                        // 세션이 실행중이라면 중지
                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    }

                    // 프리뷰가 설정되어 있다면 리스너 비우기
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

                    // (자원 해소)
                    mediaRecorderMbr?.release()
                    mediaCodecSurfaceMbr?.release()
                    imageReaderMbr?.close()
                    cameraCaptureSessionMbr?.close()
                    camera.close()

                    // (멤버 변수 비우기)
                    mediaRecorderMbr = null
                    mediaCodecSurfaceMbr = null
                    mediaRecorderConfigVoMbr = null
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraCaptureSessionMbr = null
                    captureRequestBuilderMbr = null
                    previewConfigVoListMbr.clear()
                    previewSurfaceListMbr.clear()
                    cameraDeviceMbr = null

                    onCameraDisconnectedAndClearCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    // [카메라 상태 초기화]
                    // 이미지 리더 요청을 먼저 비우기
                    imageReaderMbr?.setOnImageAvailableListener(null, null)

                    if (isRecordingMbr) {
                        // 레코딩 중이라면 레코더 종료 후 세션 중지
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        isRecordingMbr = false

                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    } else if (isRepeatingMbr) {
                        // 세션이 실행중이라면 중지
                        cameraCaptureSessionMbr?.stopRepeating()
                        isRepeatingMbr = false
                    }

                    // 프리뷰가 설정되어 있다면 리스너 비우기
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

                    // (자원 해소)
                    mediaRecorderMbr?.release()
                    mediaCodecSurfaceMbr?.release()
                    imageReaderMbr?.close()
                    cameraCaptureSessionMbr?.close()
                    camera.close()

                    // (멤버 변수 비우기)
                    mediaRecorderMbr = null
                    mediaCodecSurfaceMbr = null
                    mediaRecorderConfigVoMbr = null
                    imageReaderMbr = null
                    imageReaderConfigVoMbr = null
                    cameraCaptureSessionMbr = null
                    captureRequestBuilderMbr = null
                    previewConfigVoListMbr.clear()
                    previewSurfaceListMbr.clear()
                    cameraDeviceMbr = null

                    when (error) {
                        ERROR_CAMERA_DISABLED -> {
                            onErrorAndClearSurface(8)
                        }
                        ERROR_CAMERA_IN_USE -> {
                            onErrorAndClearSurface(9)
                        }
                        ERROR_MAX_CAMERAS_IN_USE -> {
                            onErrorAndClearSurface(10)
                        }
                        ERROR_CAMERA_DEVICE -> {
                            onErrorAndClearSurface(11)
                        }
                        ERROR_CAMERA_SERVICE -> {
                            onErrorAndClearSurface(12)
                        }
                    }
                }
            }, cameraThreadVoMbr.cameraHandlerThreadObj.handler
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
        onErrorAndClearSurface: (Int) -> Unit
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

            // todo 고속촬영
            cameraDeviceMbr?.createCaptureSession(SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurationList,
                HandlerExecutor(cameraThreadVoMbr.cameraHandlerThreadObj.handler!!.looper),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr = session

                        onCaptureSessionCreated()
                    }

                    // 세션 생성 실패
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // 준비 서페이스 초기화
                        // 프리뷰가 설정되어 있다면 리스너 비우기
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

                        // (자원 해소)
                        mediaRecorderMbr?.release()
                        mediaCodecSurfaceMbr?.release()
                        imageReaderMbr?.close()
                        session.close()

                        // (멤버 변수 비우기)
                        mediaRecorderMbr = null
                        mediaCodecSurfaceMbr = null
                        mediaRecorderConfigVoMbr = null
                        imageReaderMbr = null
                        imageReaderConfigVoMbr = null
                        previewConfigVoListMbr.clear()
                        previewSurfaceListMbr.clear()
                        cameraCaptureSessionMbr = null

                        onErrorAndClearSurface(13)
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
                        // 준비 서페이스 초기화
                        // 프리뷰가 설정되어 있다면 리스너 비우기
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

                        // (자원 해소)
                        mediaRecorderMbr?.release()
                        mediaCodecSurfaceMbr?.release()
                        imageReaderMbr?.close()
                        session.close()

                        // (멤버 변수 비우기)
                        mediaRecorderMbr = null
                        mediaCodecSurfaceMbr = null
                        mediaRecorderConfigVoMbr = null
                        imageReaderMbr = null
                        imageReaderConfigVoMbr = null
                        previewConfigVoListMbr.clear()
                        previewSurfaceListMbr.clear()
                        cameraCaptureSessionMbr = null

                        onErrorAndClearSurface(13)
                    }
                }, cameraThreadVoMbr.cameraHandlerThreadObj.handler
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

    data class CameraIdThreadVo(
        val cameraSemaphore: Semaphore,
        val cameraHandlerThreadObj: HandlerThreadObj,
        val imageReaderHandlerThreadObj: HandlerThreadObj,
        var publishCount: Int
    )
}