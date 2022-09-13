package com.example.prowd_android_template.util_class

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.custom_view.AutoFitTextureView
import com.example.prowd_android_template.util_object.CustomUtil
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.io.File
import java.util.concurrent.Semaphore
import kotlin.math.sqrt


// todo : 예시 샘플에서 fps 조절 부분도 추가
// todo : 각 수치 조정은 setter 로 처리
// todo : auto 설정일 때 focus distance 등의 현재 수치 가져오기
// todo :( repeatingRequest / capture) on manual 제공
// todo : af, ae, awb, iso area 설정
// todo : 터치 exposure, whitebalance, focus 등 (핀치 줌을 참고)
// todo : iso 내부 멤버변수로 두고 자동, 수동 모드 변경 및 수동 수치 조작 가능하게
//    https://stackoverflow.com/questions/28293078/how-to-control-iso-manually-in-camera2-android
// todo : 디바이스 방향 관련 부분 다시 살피기
// todo : ui thread 사용 부분 개선(최소화 및 회전으로 인한 불안정 해결)
// todo : 최고 화질시 녹화 확인
// todo : 녹화 미디어 플레이어 템플릿 사용 - 플레이어 제공 사이즈와 상호검증
// todo : 캡쳐 제로 셧
//    https://developer.android.com/reference/android/hardware/camera2/CameraDevice#TEMPLATE_RECORD
// todo : 버스트 세션 사용
// todo : 종료시 aborting 사용 고려
// todo : still capture 시 stopRepeating -> abortCaptures 를 먼저 한 후  capture 를 하여 그 결과 콜백 내에서 기존 repeat 재개

// <Camera 객체>
// : 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체 (생성시 카메라 아이디를 주입) 및 카메라 관련 함수 라이브러리
//     생성시엔 생성 함수 getInstance 를 사용하고, 카메라 사용이 필요 없어지면 destroyCameraObject 를 사용할것
//     Output Surface 에서 프리뷰는 복수 설정이 가능, 이미지 리더는 2개, 미디어 리코더는 1개만 설정 가능
//     이미지 리더 중 한개는 캡쳐용, 다른 하나는 분석용으로, 분석용은 반복 리퀘스트가 가능하지만 캡쳐용은 캡쳐 시점 일순간만 실행됨
//     내부 제공 함수들은 대다수 Ui 스레드와 별도의 카메라 스레드에서 비동기 동작을 수행합니다.
//     카메라 동작 관련 함수들 모두 세마포어로 뮤텍스가 되어있으므로 이 경우 꼭 완료 콜백을 통하지 않아도 선행 후행의 싱크가 어긋나지 않습니다.
//     연산량이 많은 작업은 카메라 스레드에서 처리하되, 프리뷰와 같이 UI를 사용하는 작업은 Ui 스레드를 적절히 사용하여 내부 처리

// (카메라 지원 모드 종류) :
// 1~5 기본 모드 개별 지원여부
//     1 : preview - preview 가 존재 (SurfaceTexture 지원여부)
//     2 : capture - captureImageReader 가 존재 (ImageFormat.JPEG 지원여부)
//     3 : mediaRecord - mediaRecorder 가 존재 (MediaRecorder 지원여부)
//     4 : analysis - analysisImageReader 가 존재 (ImageFormat.YUV_420_888 지원여부)
//     5 : highSpeed - highSpeed 가 존재 (cameraConfig.highSpeedVideoSizes 지원여부)

// 6~16 각 개별모드를 같이 사용할 때 동일 서페이스 비율을 제공하는지 여부 (동일 서페이스 비율이 있으면 사이즈 등의 계산이 편함)
// highSpeed 는 이미지 리더와 프리뷰 모두 동일한 소스에서 나오므로 지원만 되면 아래 콤비네이션과 상관없음
//     6 : preview and capture - 동일 서페이스 비율 제공
//     7 : preview and mediaRecord - 동일 서페이스 비율 제공
//     8 : preview and analysis - 동일 서페이스 비율 제공
//     9 : capture and mediaRecord - 동일 서페이스 비율 제공
//     10 : capture and analysis - 동일 서페이스 비율 제공
//     11 : mediaRecord and analysis - 동일 서페이스 비율 제공
//     12 : preview and capture and mediaRecord - 동일 서페이스 비율 제공
//     13 : preview and capture and analysis - 동일 서페이스 비율 제공
//     14 : preview and mediaRecord and analysis - 동일 서페이스 비율 제공
//     15 : capture and mediaRecord and analysis - 동일 서페이스 비율 제공
//     16 : preview and capture and mediaRecord and analysis - 동일 서페이스 비율 제공

class Camera2Obj private constructor(
    private val parentActivityMbr: Activity,
    private val cameraManagerMbr: CameraManager,
    private val onCameraDisconnectedMbr: (() -> Unit)
) {
    // <스태틱 공간>
    companion object {
        // <공개 메소드 공간>
        // (가용한 카메라 아이디 리스트 반환)
        // : 단순히 Camera2 API 반환 리스트뿐 아니라 동작에 필요한 필수 정보 제공여부도 파악
        fun getAllAvailableCameraIdList(parentActivity: Activity): ArrayList<String> {
            val resultList = ArrayList<String>()

            if (!parentActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                // 카메라 장치가 없을 때
                return resultList
            }

            val cameraManager: CameraManager =
                parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraIdList = cameraManager.cameraIdList

            for (cameraId in cameraIdList) {
                // 카메라 정보 가져오기
                val cameraCharacteristics =
                    cameraManager.getCameraCharacteristics(cameraId)

                // 필수 정보 확인
                val facing =
                    cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

                val streamConfigurationMap: StreamConfigurationMap? =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                val sensorOrientation: Int? =
                    cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

                val sensorSize =
                    cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

                val capabilities = cameraCharacteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )

                if (null == facing ||
                    null == streamConfigurationMap ||
                    null == sensorOrientation ||
                    null == sensorSize ||
                    null == capabilities
                ) {
                    // 필수 정보가 하나라도 없으면 비가용
                    continue
                }

                // (지원 사이즈 검증)
                val previewInfoList = ArrayList<SizeSpecInfoVo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)
                        .forEach { size ->
                            val secondsPerFrame =
                                streamConfigurationMap.getOutputMinFrameDuration(
                                    SurfaceTexture::class.java,
                                    size
                                ) / 1_000_000_000.0
                            val fps =
                                if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                            previewInfoList.add(
                                SizeSpecInfoVo(
                                    size, fps
                                )
                            )
                        }
                }

                val captureImageReaderInfoList = ArrayList<SizeSpecInfoVo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).forEach { size ->
                        val secondsPerFrame =
                            streamConfigurationMap.getOutputMinFrameDuration(
                                ImageFormat.YUV_420_888,
                                size
                            ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        captureImageReaderInfoList.add(
                            SizeSpecInfoVo(
                                size, fps
                            )
                        )
                    }
                }

                val mediaRecorderInfoList = ArrayList<SizeSpecInfoVo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    streamConfigurationMap.getOutputSizes(MediaRecorder::class.java)
                        .forEach { size ->
                            val secondsPerFrame =
                                streamConfigurationMap.getOutputMinFrameDuration(
                                    MediaRecorder::class.java,
                                    size
                                ) / 1_000_000_000.0
                            val fps =
                                if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                            mediaRecorderInfoList.add(
                                SizeSpecInfoVo(
                                    size, fps
                                )
                            )
                        }
                }

                val analysisImageReaderInfoList = ArrayList<SizeSpecInfoVo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    )
                ) {
                    streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888).forEach { size ->
                        val secondsPerFrame =
                            streamConfigurationMap.getOutputMinFrameDuration(
                                ImageFormat.YUV_420_888,
                                size
                            ) / 1_000_000_000.0
                        val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                        analysisImageReaderInfoList.add(
                            SizeSpecInfoVo(
                                size, fps
                            )
                        )
                    }
                }

                val highSpeedInfoList = ArrayList<SizeSpecInfoVo>()
                if (capabilities.contains(
                        CameraCharacteristics
                            .REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
                    )
                ) {
                    streamConfigurationMap.highSpeedVideoSizes.forEach { size ->
                        streamConfigurationMap.getHighSpeedVideoFpsRangesFor(size)
                            .forEach { fpsRange ->
                                val fps = fpsRange.upper
                                if (highSpeedInfoList.indexOfFirst {
                                        it.size == size && it.fps == fps
                                    } == -1) {
                                    highSpeedInfoList.add(
                                        SizeSpecInfoVo(
                                            size, fps
                                        )
                                    )
                                }
                            }
                    }
                }

                // 출력 지원 사이즈가 하나도 없다면 비가용
                if (previewInfoList.isEmpty() &&
                    captureImageReaderInfoList.isEmpty() &&
                    mediaRecorderInfoList.isEmpty() &&
                    analysisImageReaderInfoList.isEmpty() &&
                    highSpeedInfoList.isEmpty()
                ) {
                    continue
                }

                resultList.add(cameraId)
            }

            return resultList
        }

        // (객체 생성 함수 = 조건에 맞지 않으면 null 반환)
        // 조작하길 원하는 카메라 ID 를 설정하여 해당 카메라 정보를 생성
        fun getInstance(
            parentActivity: Activity,
            cameraId: String,
            onCameraDisconnectedAndClearCamera: (() -> Unit)
        ): Camera2Obj? {
            // [카메라 디바이스 유무 검증]
            if (!parentActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                // 카메라 장치가 없다면 null 반환
                return null
            }

            // [카메라 필수 데이터 검증]
            // 카메라 총괄 빌더
            val cameraManager: CameraManager =
                parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraIdList = cameraManager.cameraIdList
            if (!cameraIdList.contains(cameraId)) { // 입력한 CameraId 가 지원되지 않을 때
                return null
            }

            // 카메라 Id에 해당하는 카메라 정보 가져오기
            val cameraCharacteristics =
                cameraManager.getCameraCharacteristics(cameraId)

            // 필수 정보 확인
            val facing =
                cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

            val streamConfigurationMap: StreamConfigurationMap? =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val sensorOrientation: Int? =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            val sensorSize =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

            val capabilities = cameraCharacteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )

            if (null == facing ||
                null == streamConfigurationMap ||
                null == sensorOrientation ||
                null == sensorSize ||
                null == capabilities
            ) {
                // 필수 정보가 하나라도 없으면 null 반환
                return null
            }

            // (지원 사이즈 검증)
            val previewInfoList = ArrayList<SizeSpecInfoVo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
            ) {
                streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java).forEach { size ->
                    val secondsPerFrame =
                        streamConfigurationMap.getOutputMinFrameDuration(
                            SurfaceTexture::class.java,
                            size
                        ) / 1_000_000_000.0
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    previewInfoList.add(
                        SizeSpecInfoVo(
                            size, fps
                        )
                    )
                }
            }

            val captureImageReaderInfoList = ArrayList<SizeSpecInfoVo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
            ) {
                streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).forEach { size ->
                    val secondsPerFrame =
                        streamConfigurationMap.getOutputMinFrameDuration(
                            ImageFormat.YUV_420_888,
                            size
                        ) / 1_000_000_000.0
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    captureImageReaderInfoList.add(
                        SizeSpecInfoVo(
                            size, fps
                        )
                    )
                }
            }

            val mediaRecorderInfoList = ArrayList<SizeSpecInfoVo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
            ) {
                streamConfigurationMap.getOutputSizes(MediaRecorder::class.java).forEach { size ->
                    val secondsPerFrame =
                        streamConfigurationMap.getOutputMinFrameDuration(
                            MediaRecorder::class.java,
                            size
                        ) / 1_000_000_000.0
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    mediaRecorderInfoList.add(
                        SizeSpecInfoVo(
                            size, fps
                        )
                    )
                }
            }

            val analysisImageReaderInfoList = ArrayList<SizeSpecInfoVo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
            ) {
                streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888).forEach { size ->
                    val secondsPerFrame =
                        streamConfigurationMap.getOutputMinFrameDuration(
                            ImageFormat.YUV_420_888,
                            size
                        ) / 1_000_000_000.0
                    val fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                    analysisImageReaderInfoList.add(
                        SizeSpecInfoVo(
                            size, fps
                        )
                    )
                }
            }

            val highSpeedInfoList = ArrayList<SizeSpecInfoVo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
                )
            ) {
                streamConfigurationMap.highSpeedVideoSizes.forEach { size ->
                    streamConfigurationMap.getHighSpeedVideoFpsRangesFor(size).forEach { fpsRange ->
                        val fps = fpsRange.upper
                        if (highSpeedInfoList.indexOfFirst {
                                it.size == size && it.fps == fps
                            } == -1) {
                            highSpeedInfoList.add(
                                SizeSpecInfoVo(
                                    size, fps
                                )
                            )
                        }
                    }
                }
            }

            // 출력 지원 사이즈가 하나도 없다면 null 반환
            if (previewInfoList.isEmpty() &&
                captureImageReaderInfoList.isEmpty() &&
                mediaRecorderInfoList.isEmpty() &&
                analysisImageReaderInfoList.isEmpty() &&
                highSpeedInfoList.isEmpty()
            ) {
                return null
            }

            // [카메라 객체 생성]
            val resultCameraObject = Camera2Obj(
                parentActivity,
                cameraManager,
                onCameraDisconnectedAndClearCamera
            )

            // [카메라 객체 내부 멤버변수 생성]
            // 플래시 사용 가능여부
            val flashSupported =
                cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // AF 지원 가능 여부
            val afAvailableModes: IntArray? =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)

            val fastAutoFocusSupported: Boolean
            val naturalAutoFocusSupported: Boolean
            if (afAvailableModes == null || afAvailableModes.isEmpty() || (afAvailableModes.size == 1
                        && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)
            ) {
                fastAutoFocusSupported = false
                naturalAutoFocusSupported = false
            } else {
                fastAutoFocusSupported =
                    afAvailableModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                naturalAutoFocusSupported =
                    afAvailableModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }

            // AF 초기 설정 결정
            if (fastAutoFocusSupported) {
                resultCameraObject.focusDistanceMbr = -2f
            } else if (naturalAutoFocusSupported) {
                resultCameraObject.focusDistanceMbr = -1f
            } else {
                resultCameraObject.focusDistanceMbr = 0f
            }

            // area af 가능 여부
            val maxRegionAf =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
            val autoFocusMeteringAreaSupported =
                null != maxRegionAf && maxRegionAf >= 1

            // 가장 가까운 초점 거리
            val supportedMinimumFocusDistance =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                    ?: 0f

            // AE 지원 가능 여부
            val aeAvailableModes: IntArray? =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            val autoExposureSupported =
                !(aeAvailableModes == null || aeAvailableModes.isEmpty() || (aeAvailableModes.size == 1
                        && aeAvailableModes[0] == CameraMetadata.CONTROL_AE_MODE_OFF))

            // AE Area 지원 가능 여부
            val aeState: Int? =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)
            val autoExposureMeteringAreaSupported =
                aeState != null && aeState >= 1

            // AWB 지원 가능 여부
            val awbAvailableModes: IntArray? =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            val autoWhiteBalanceSupported =
                !(awbAvailableModes == null || awbAvailableModes.isEmpty() || (awbAvailableModes.size == 1
                        && awbAvailableModes[0] == CameraMetadata.CONTROL_AWB_MODE_OFF))

            val maxRegionAwb: Int? =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)

            val autoWhiteBalanceMeteringAreaSupported =
                maxRegionAwb != null && maxRegionAwb >= 1

            // max zoom 정보
            var maxZoom =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            maxZoom =
                if (maxZoom == null) {
                    1.0f
                } else {
                    if (maxZoom < 1.0f) {
                        1.0f
                    } else {
                        maxZoom
                    }
                }

            // 기계적 떨림 보정 여부
            val availableOpticalStabilization =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            val isOpticalStabilizationAvailable: Boolean =
                availableOpticalStabilization?.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
                    ?: false

            // 소프트웨어 떨림 보정 여부
            val availableVideoStabilization =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            val isVideoStabilizationAvailable: Boolean =
                availableVideoStabilization?.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)
                    ?: false

            // [멤버변수 주입]
            // 카메라 정보
            resultCameraObject.cameraInfoVoMbr = CameraInfoVo(
                cameraId,
                facing,
                sensorOrientation,
                previewInfoList,
                captureImageReaderInfoList,
                mediaRecorderInfoList,
                analysisImageReaderInfoList,
                highSpeedInfoList,
                flashSupported,
                fastAutoFocusSupported,
                naturalAutoFocusSupported,
                autoFocusMeteringAreaSupported,
                autoExposureSupported,
                autoExposureMeteringAreaSupported,
                autoWhiteBalanceSupported,
                autoWhiteBalanceMeteringAreaSupported,
                supportedMinimumFocusDistance,
                isOpticalStabilizationAvailable,
                isVideoStabilizationAvailable,
                sensorSize,
                maxZoom
            )

            // 카메라 사용 스레드
            resultCameraObject.cameraThreadVoMbr =
                publishSharedCameraIdThreadVoOnStaticMemory(cameraId)

            return resultCameraObject
        }


        // ---------------------------------------------------------------------------------------------
        // <비공개 메소드 공간>
        // (전역에서 해당 카메라 아이디에 공유되는 스레드 객체를 반환)
        // 카메라 관련 함수 실행시 사용될 스레드와 세마포어
        // 카메라라는 자원은 하나이므로 하나의 카메라를 조작할 때에는 하나의 스레드에서 싱크를 맞춰야 하기에 이를 스태틱 공간에 생성하여 사용
        // 한 카메라에 대해 하나의 스레드와 세마포어를 생성후 사용
        private val cameraIdThreadVoList: ArrayList<CameraIdThreadVo> = ArrayList()
        private val cameraIdThreadVoListSemaphore: Semaphore = Semaphore(1)
        private fun publishSharedCameraIdThreadVoOnStaticMemory(cameraId: String): CameraIdThreadVo {
            cameraIdThreadVoListSemaphore.acquire()

            // 해당 아이디에 대해 기존 발행된 스레드가 존재하는지 찾기
            val listIdx = cameraIdThreadVoList.indexOfFirst {
                it.cameraId == cameraId
            }

            if (-1 == listIdx) { // 기존에 발행된 스레드 객체가 없다면,
                // 새로운 스레드 객체를 발행
                val cameraThreadVo = CameraIdThreadVo(
                    cameraId,
                    Semaphore(1),
                    HandlerThreadObj(cameraId),
                    HandlerThreadObj(cameraId),
                    HandlerThreadObj(cameraId)
                )

                // 새로운 핸들러 스레드 실행
                cameraThreadVo.cameraHandlerThreadObj.startHandlerThread()
                cameraThreadVo.captureImageReaderHandlerThreadObj.startHandlerThread()
                cameraThreadVo.analysisImageReaderHandlerThreadObj.startHandlerThread()

                cameraIdThreadVoList.add(
                    cameraThreadVo
                )

                cameraIdThreadVoListSemaphore.release()
                return cameraThreadVo
            } else { // 기존에 발행된 스레드 객체가 있다면,
                // 기존 스레드 객체를 가져오고 publishCount +1
                val cameraThreadVo = cameraIdThreadVoList[listIdx]

                cameraIdThreadVoListSemaphore.release()
                return cameraThreadVo
            }
        }


        // ---------------------------------------------------------------------------------------------
        // <중첩 클래스 공간>
        // (카메라 실행 스레드 리스트와 발행 세마포어)
        // 카메라 관련 함수 실행시 사용될 스레드와 세마포어
        // 카메라라는 자원은 하나이므로 하나의 카메라를 조작할 때에는 하나의 스레드에서 싱크를 맞춰야
        // 하기에 이를 스태틱 공간에 생성하여 사용
        data class CameraIdThreadVo(
            val cameraId: String,
            val cameraSemaphore: Semaphore,
            val cameraHandlerThreadObj: HandlerThreadObj,
            val captureImageReaderHandlerThreadObj: HandlerThreadObj,
            val analysisImageReaderHandlerThreadObj: HandlerThreadObj
        )
    }

    // ---------------------------------------------------------------------------------------------
    // <멤버 변수 공간>
    // (스레드 풀)
    // 카메라 API 사용 스레드 세트
    private lateinit var cameraThreadVoMbr: CameraIdThreadVo


    // [카메라 지원 정보]
    // 현 카메라 정보
    lateinit var cameraInfoVoMbr: CameraInfoVo
        private set


    // [카메라 설정 정보]
    // (3A 설정)
    // 포커스 거리 :
    //     거리는 0부터 시작해서 minimumFocusDistanceMbr 까지의 수치
    //     0은 가장 먼 곳, 수치가 커질수록 가까운 곳의 포커스
    //     -1 일 때는 CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO (자연스런 포커싱)
    //     -2 일 때는 CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE (빠른 포커싱)
    //     기본 값은 객체 생성시 결정되어, 카메라 지원 정보에 따라 -2 or -1 or 0 의 순서대로 적용 가능한 설정을 결정
    var focusDistanceMbr: Float = -1f
        private set

    // 센서 노출 시간 :
    //     나노초 == 초/1000000000
    //     한 프레임에 대한 노출 시간
    //     null 이라면 Auto Exposure ON
    //     ex : 1000000000 / 80 // 나눠주는 값이 작을수록 밝아짐
    var exposureTimeNsMbr: Long? = null
        private set

    // 화이트 밸런스 색온도 :
    //     null 이라면 Auto WhiteBalance ON
    //     화이트 밸런스는 0 ~ 100 사이의 정수값
    //     0이면 파란색으로 차가운 색, 100이면 노란색으로 따뜻한 색
    //     템플릿 적용 파라미터
    //         -1 : CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
    //         -2 : CONTROL_AWB_MODE_DAYLIGHT
    //         -3 : CONTROL_AWB_MODE_FLUORESCENT
    //         -4 : CONTROL_AWB_MODE_INCANDESCENT
    //         -5 : CONTROL_AWB_MODE_SHADE
    //         -6 : CONTROL_AWB_MODE_TWILIGHT
    //         -7 : CONTROL_AWB_MODE_WARM_FLUORESCENT
    var whiteBalanceColorTemperatureMbr: Int? = null

    // (수동 조작 설정)
    // 떨림 보정 기능 적용 여부
    //     0 : 적용 안함
    //     1 : 기계적 떨림 보정
    //     2 : 소프트웨어 떨림 보정
    //     기계적 떨림 보정 적용을 해보고, 안되면 소프트웨어 떨림 보정 적용
    var cameraStabilizationSetMbr: Int = 0
        private set

    // 카메라 현재 줌 배수
    //     1f 부터 cameraInfoVoMbr.maxZoom 까지
    var zoomFactorMbr: Float = 1.0f
        private set


    // [카메라 상태 정보]
    // (카메라 현 상태 코드)
    // : 각 상태 코드별 절차 순서를 나타냄
    //     큰 숫자의 코드는 작은 숫자 프로세싱이 완료되어야 전환이 가능
    //     -1 : 카메라 객체 파괴
    //     0 : 카메라 객체 초기 상태
    //     1 : surface output 세팅 완료
    //     2 : request repeating
    var cameraStatusCodeMbr: Int = 0

    // 미디어 레코더 현 상태 코드
    //     0 : media recorder surface set 이전
    //     1 : media recorder surface set (cameraStatusCodeMbr == 1 and mediaRecorderConfig)
    //     2 : media recorder ready (cameraStatusCodeMbr == 2 and requestForMediaRecorder)
    //     3 : media recording (cameraStatusCodeMbr == 2 and startRecording)
    //     4 : media recorder pause (cameraStatusCodeMbr == 2 and pauseRecording)
    var mediaRecorderStatusCodeMbr: Int = 0


    // [카메라 API 생산 데이터]
    private var cameraDeviceMbr: CameraDevice? = null

    // 이미지 리더 세팅 부산물
    var captureImageReaderConfigVoMbr: ImageReaderConfigVo? = null
    private var captureImageReaderMbr: ImageReader? = null

    var analysisImageReaderConfigVoMbr: ImageReaderConfigVo? = null
    private var analysisImageReaderMbr: ImageReader? = null

    // 미디어 리코더 세팅 부산물
    var mediaRecorderConfigVoMbr: MediaRecorderConfigVo? = null
    private var mediaRecorderMbr: MediaRecorder? = null
    private var mediaCodecSurfaceMbr: Surface? = null

    // 프리뷰 세팅 부산물
    var previewConfigVoListMbr: ArrayList<PreviewConfigVo>? = null
    private var previewSurfaceListMbr: ArrayList<Surface>? = ArrayList()

    // 카메라 세션 객체
    private var cameraCaptureSessionMbr: CameraCaptureSession? = null

    // 반복 리퀘스트 타겟 객체
    private var repeatRequestTargetVoMbr: RepeatRequestTargetVo? = null


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>

    // [카메라 정보 반환 함수]

    // (현 카메라의 지원 모드 리스트 반환)
    // : 카메라 지원 모드 종류는 클래스 선언 위의 설명 참조
    fun getAllSupportedCameraModeSet(): HashSet<Int> {
        val resultSet = HashSet<Int>()

        // (지원 사이즈 리스트 가져오기)
        val previewSizeList = cameraInfoVoMbr.previewSizeInfoList

        val captureImageReaderSizeList = cameraInfoVoMbr.captureImageReaderSizeInfoList

        val mediaRecorderSizeList = cameraInfoVoMbr.mediaRecorderSizeInfoList

        val analysisImageReaderSizeList = cameraInfoVoMbr.analysisImageReaderSizeInfoList

        val highSpeedSizeList = cameraInfoVoMbr.highSpeedSizeInfoList

        // (지원 모드 비교)
        if (previewSizeList.isNotEmpty()) {
            resultSet.add(1)
        }

        if (captureImageReaderSizeList.isNotEmpty()) {
            resultSet.add(2)
        }

        if (mediaRecorderSizeList.isNotEmpty()) {
            resultSet.add(3)
        }

        if (analysisImageReaderSizeList.isNotEmpty()) {
            resultSet.add(4)
        }

        if (highSpeedSizeList.isNotEmpty()) {
            resultSet.add(5)
        }

        if (sizeListSameWhRatioExists(previewSizeList, captureImageReaderSizeList)) {
            resultSet.add(6)
        }

        if (sizeListSameWhRatioExists(previewSizeList, mediaRecorderSizeList)) {
            resultSet.add(7)
        }

        if (sizeListSameWhRatioExists(previewSizeList, analysisImageReaderSizeList)) {
            resultSet.add(8)
        }

        if (sizeListSameWhRatioExists(captureImageReaderSizeList, mediaRecorderSizeList)) {
            resultSet.add(9)
        }

        if (sizeListSameWhRatioExists(
                captureImageReaderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(10)
        }

        if (sizeListSameWhRatioExists(
                mediaRecorderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(11)
        }

        if (sizeListSameWhRatioExists(
                previewSizeList,
                captureImageReaderSizeList,
                mediaRecorderSizeList
            )
        ) {
            resultSet.add(12)
        }

        if (sizeListSameWhRatioExists(
                previewSizeList,
                captureImageReaderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(13)
        }

        if (sizeListSameWhRatioExists(
                previewSizeList,
                mediaRecorderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(14)
        }

        if (sizeListSameWhRatioExists(
                captureImageReaderSizeList,
                mediaRecorderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(15)
        }

        if (sizeListSameWhRatioExists(
                previewSizeList,
                captureImageReaderSizeList,
                mediaRecorderSizeList,
                analysisImageReaderSizeList
            )
        ) {
            resultSet.add(16)
        }

        return resultSet
    }

    // (특정 모드가 가능한 가용 카메라 아이디 리스트 반환)
    // : 카메라 지원 모드 종류는 클래스 선언 위의 설명 참조
    fun isThisCameraSupportedForMode(
        mode: Int
    ): Boolean {
        // (카메라 제공 사이즈)
        val previewSizeList = cameraInfoVoMbr.previewSizeInfoList

        val captureImageReaderSizeList = cameraInfoVoMbr.captureImageReaderSizeInfoList

        val mediaRecorderSizeList = cameraInfoVoMbr.mediaRecorderSizeInfoList

        val analysisImageReaderSizeList = cameraInfoVoMbr.analysisImageReaderSizeInfoList

        val highSpeedSizeList = cameraInfoVoMbr.highSpeedSizeInfoList

        // (지원 모드 비교)
        val mode1 = previewSizeList.isNotEmpty()
        val mode2 = captureImageReaderSizeList.isNotEmpty()
        val mode3 = mediaRecorderSizeList.isNotEmpty()
        val mode4 = analysisImageReaderSizeList.isNotEmpty()
        val mode5 = highSpeedSizeList.isNotEmpty()
        val mode6 =
            sizeListSameWhRatioExists(previewSizeList, captureImageReaderSizeList)
        val mode7 = sizeListSameWhRatioExists(previewSizeList, mediaRecorderSizeList)
        val mode8 =
            sizeListSameWhRatioExists(previewSizeList, analysisImageReaderSizeList)
        val mode9 =
            sizeListSameWhRatioExists(
                captureImageReaderSizeList,
                mediaRecorderSizeList
            )
        val mode10 = sizeListSameWhRatioExists(
            captureImageReaderSizeList,
            analysisImageReaderSizeList
        )
        val mode11 =
            sizeListSameWhRatioExists(
                mediaRecorderSizeList,
                analysisImageReaderSizeList
            )
        val mode12 = sizeListSameWhRatioExists(
            previewSizeList,
            captureImageReaderSizeList,
            mediaRecorderSizeList
        )
        val mode13 = sizeListSameWhRatioExists(
            previewSizeList,
            captureImageReaderSizeList,
            analysisImageReaderSizeList
        )
        val mode14 = sizeListSameWhRatioExists(
            previewSizeList,
            mediaRecorderSizeList,
            analysisImageReaderSizeList
        )
        val mode15 = sizeListSameWhRatioExists(
            captureImageReaderSizeList,
            mediaRecorderSizeList,
            analysisImageReaderSizeList
        )
        val mode16 = sizeListSameWhRatioExists(
            previewSizeList,
            captureImageReaderSizeList,
            mediaRecorderSizeList,
            analysisImageReaderSizeList
        )

        // (가용 모드 확인)
        if (mode == 1) { // preview mode 가용
            if (!mode1) {
                return false
            }
        } else if (mode == 2) { // captureImageReader mode 가용
            if (!mode2) {
                return false
            }
        } else if (mode == 3) { // mediaRecorder mode 가용
            if (!mode3) {
                return false
            }
        } else if (mode == 4) { // analysisImageReader mode 가용
            if (!mode4) {
                return false
            }
        } else if (mode == 5) { // high speed mode 가용
            if (!mode5) {
                return false
            }
        } else if (mode == 6) { // preview and capture mode 가용
            if (!mode6) {
                return false
            }
        } else if (mode == 7) { // preview and mediaRecord mode 가용
            if (!mode7) {
                return false
            }
        } else if (mode == 8) { // preview and analysis mode 가용
            if (!mode8) {
                return false
            }
        } else if (mode == 9) { // capture and mediaRecord mode 가용
            if (!mode9) {
                return false
            }
        } else if (mode == 10) { // capture and analysis mode 가용
            if (!mode10) {
                return false
            }
        } else if (mode == 11) { // mediaRecord and analysis mode 가용
            if (!mode11) {
                return false
            }
        } else if (mode == 12) { // preview and capture and mediaRecord mode 가용
            if (!mode12) {
                return false
            }
        } else if (mode == 13) { // preview and capture and analysis mode 가용
            if (!mode13) {
                return false
            }
        } else if (mode == 14) { // preview and mediaRecord and analysis mode 가용
            if (!mode14) {
                return false
            }
        } else if (mode == 15) { // capture and mediaRecord and analysi mode 가용
            if (!mode15) {
                return false
            }
        } else if (mode == 16) { // preview and capture and mediaRecord and analysis mode 가용
            if (!mode16) {
                return false
            }
        } else { // 그외 모드
            // 파라미터 에러로, 그냥 전부 스킵
            return false
        }

        return true
    }

    // (특정 카메라의 특정 모드 지원에 대한 중복되지 않는 단위 사이즈 반환)
    // : 단위 사이즈란, 정수에서 더이상 나누어 떨어지지 않는 사이즈로, 사이즈 width, height 최대공약수로 나눈 값
    //     카메라 방향 기준으로, 3:2, 1:1 등의 비율을 나타냄
    //     카메라 지원 모드 종류는 클래스 선언 위의 설명 참조
    fun getSupportedCameraOrientSurfaceUnitSize(
        mode: Int
    ): HashSet<Size> {
        // (카메라 제공 사이즈)
        val previewSizeList = cameraInfoVoMbr.previewSizeInfoList

        val captureImageReaderSizeList = cameraInfoVoMbr.captureImageReaderSizeInfoList

        val mediaRecorderSizeList = cameraInfoVoMbr.mediaRecorderSizeInfoList

        val analysisImageReaderSizeList = cameraInfoVoMbr.analysisImageReaderSizeInfoList

        val highSpeedSizeList = cameraInfoVoMbr.highSpeedSizeInfoList

        val resultSet = HashSet<Size>()
        when (mode) {
            1 -> { // preview 모드
                for (sizeInfo in previewSizeList) {
                    val gcd = CustomUtil.getGcd(sizeInfo.size.width, sizeInfo.size.height)
                    resultSet.add(
                        Size(
                            sizeInfo.size.width / gcd,
                            sizeInfo.size.height / gcd
                        )
                    )
                }
            }
            2 -> { // capture 모드
                for (sizeInfo in captureImageReaderSizeList) {
                    val gcd = CustomUtil.getGcd(sizeInfo.size.width, sizeInfo.size.height)
                    resultSet.add(
                        Size(
                            sizeInfo.size.width / gcd,
                            sizeInfo.size.height / gcd
                        )
                    )
                }
            }
            3 -> { // record 모드
                for (sizeInfo in mediaRecorderSizeList) {
                    val gcd = CustomUtil.getGcd(sizeInfo.size.width, sizeInfo.size.height)
                    resultSet.add(
                        Size(
                            sizeInfo.size.width / gcd,
                            sizeInfo.size.height / gcd
                        )
                    )
                }
            }
            4 -> { // analysis 모드
                for (sizeInfo in analysisImageReaderSizeList) {
                    val gcd = CustomUtil.getGcd(sizeInfo.size.width, sizeInfo.size.height)
                    resultSet.add(
                        Size(
                            sizeInfo.size.width / gcd,
                            sizeInfo.size.height / gcd
                        )
                    )
                }
            }
            5 -> { // high speed 모드
                for (sizeInfo in highSpeedSizeList) {
                    val gcd = CustomUtil.getGcd(sizeInfo.size.width, sizeInfo.size.height)
                    resultSet.add(
                        Size(
                            sizeInfo.size.width / gcd,
                            sizeInfo.size.height / gcd
                        )
                    )
                }
            }
            6 -> { // preview and capture 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (captureImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            7 -> { // preview and mediaRecord 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            8 -> { // preview and analysis 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            9 -> { // capture and mediaRecord 모드
                for (sizeInfo in captureImageReaderSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            10 -> { // capture and analysis 모드
                for (sizeInfo in captureImageReaderSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            11 -> { // mediaRecord and analysis 모드
                for (sizeInfo in mediaRecorderSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            12 -> { // preview and capture and mediaRecord 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (captureImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            13 -> { // preview and capture and analysis 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (captureImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            14 -> { // preview and mediaRecord and analysis 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            15 -> { // capture and mediaRecord and analysis 모드
                for (sizeInfo in captureImageReaderSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
            16 -> { // preview and capture and mediaRecord and analysis 모드
                for (sizeInfo in previewSizeList) {
                    val gcd =
                        CustomUtil.getGcd(
                            sizeInfo.size.width,
                            sizeInfo.size.height
                        )
                    val unitSize = Size(
                        sizeInfo.size.width / gcd,
                        sizeInfo.size.height / gcd
                    )

                    if (captureImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() ==
                                    unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        mediaRecorderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1 &&
                        analysisImageReaderSizeList.indexOfFirst {
                            it.size.width.toDouble() / it.size.height.toDouble() == unitSize.width.toDouble() / unitSize.height.toDouble()
                        } != -1) {
                        resultSet.add(unitSize)
                    }
                }
            }
        }
        return resultSet
    }

    // (원하는 Area 와 Ratio 에 가장 유사한 사이즈 반환 함수)
    // preferredArea : 원하는 넓이. 0 이하로 선택하면 넓이만으로 비교
    // cameraOrientPreferredWHRatio : 원하는 width * height 비율
    // srcSizeList : 추출할 사이즈 리스트
    fun getNearestSize(
        srcSizeList: List<Size>,
        preferredArea: Long,
        cameraOrientPreferredWHRatio: Double
    ): Size {
        if (0 >= cameraOrientPreferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
            // 넓이 비슷한 것을 선정
            var smallestAreaDiff: Long = Long.MAX_VALUE
            var resultIndex = 0

            for ((index, value) in srcSizeList.withIndex()) {
                val area = value.width.toLong() * value.height.toLong()
                val areaDiff = kotlin.math.abs(area - preferredArea)
                if (areaDiff < smallestAreaDiff) {
                    smallestAreaDiff = areaDiff
                    resultIndex = index
                }
            }

            return srcSizeList[resultIndex]
        } else { // 비율을 먼저 보고, 이후 넓이로 비교
            // 비율 비슷한 것을 선정
            var mostSameWhRatio = 0.0
            var smallestWhRatioDiff: Double = Double.MAX_VALUE

            for (value in srcSizeList) {
                val whRatio: Double = value.width.toDouble() / value.height.toDouble()

                val whRatioDiff = kotlin.math.abs(whRatio - cameraOrientPreferredWHRatio)
                if (whRatioDiff < smallestWhRatioDiff) {
                    smallestWhRatioDiff = whRatioDiff
                    mostSameWhRatio = whRatio
                }
            }

            // 넓이 비슷한 것을 선정
            var resultSizeIndex = 0
            var smallestAreaDiff: Long = Long.MAX_VALUE
            // 비슷한 비율중 가장 비슷한 넓이를 선정
            for ((index, value) in srcSizeList.withIndex()) {
                val whRatio: Double = value.width.toDouble() / value.height.toDouble()

                if (mostSameWhRatio == whRatio) {
                    val area = value.width.toLong() * value.height.toLong()
                    val areaDiff = kotlin.math.abs(area - preferredArea)
                    if (areaDiff < smallestAreaDiff) {
                        smallestAreaDiff = areaDiff
                        resultSizeIndex = index
                    }
                }
            }
            return srcSizeList[resultSizeIndex]
        }
    }

    // (카메라와 현 디바이스의 Width, height 개념이 동일한지)
    // 카메라와 디바이스 방향이 90, 270 차이가 난다면 둘의 width, height 개념이 상반됨
    fun cameraSensorOrientationAndDeviceAreSameWh(): Boolean {
        // 디바이스 물리적 기본 방향을 0으로 뒀을 때, 현 디바이스가 반시계 방향으로 몇도가 돌아갔는가
        // 0 = 0, 반시계 90 = 1, 180 = 2, 반시계 270 = 3
        val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parentActivityMbr.display!!.rotation
        } else {
            parentActivityMbr.windowManager.defaultDisplay.rotation
        }

        return when (deviceOrientation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                // 디바이스 현 방향이 90도 단위로 기울어졌을 때
                when (cameraInfoVoMbr.sensorOrientation) {
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
                when (cameraInfoVoMbr.sensorOrientation) {
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

    // 해당 비율의 사이즈 리스트들 반환
    // whRatio : 카메라 방향의 서페이스 width / height 값
    // 반환값 : 카메라 방향의 whRatio 와 동일한 카메라 방향의 제공되는 사이즈
    fun getSizeListsForWhRatio(whRatio: Double): SurfacesSizeList {
        val previewSizeList = ArrayList<Size>()
        for (previewSizeInfo in cameraInfoVoMbr.previewSizeInfoList) {
            if ((previewSizeInfo.size.width.toDouble() / previewSizeInfo.size.height.toDouble()) == whRatio) {
                previewSizeList.add(previewSizeInfo.size)
            }
        }

        val captureImageReaderSizeList = ArrayList<Size>()
        for (captureImageReaderSizeInfo in cameraInfoVoMbr.captureImageReaderSizeInfoList) {
            if ((captureImageReaderSizeInfo.size.width.toDouble() / captureImageReaderSizeInfo.size.height.toDouble()) == whRatio) {
                captureImageReaderSizeList.add(captureImageReaderSizeInfo.size)
            }
        }

        val mediaRecorderSizeList = ArrayList<Size>()
        for (mediaRecorderSizeInfo in cameraInfoVoMbr.mediaRecorderSizeInfoList) {
            if ((mediaRecorderSizeInfo.size.width.toDouble() / mediaRecorderSizeInfo.size.height.toDouble()) == whRatio) {
                mediaRecorderSizeList.add(mediaRecorderSizeInfo.size)
            }
        }

        val analysisImageReaderSizeList = ArrayList<Size>()
        for (analysisImageReaderSizeInfo in cameraInfoVoMbr.analysisImageReaderSizeInfoList) {
            if ((analysisImageReaderSizeInfo.size.width.toDouble() / analysisImageReaderSizeInfo.size.height.toDouble()) == whRatio) {
                analysisImageReaderSizeList.add(analysisImageReaderSizeInfo.size)
            }
        }

        val highSpeedSizeList = ArrayList<Size>()
        for (highSpeedSizeInfo in cameraInfoVoMbr.highSpeedSizeInfoList) {
            if ((highSpeedSizeInfo.size.width.toDouble() / highSpeedSizeInfo.size.height.toDouble()) == whRatio) {
                highSpeedSizeList.add(highSpeedSizeInfo.size)
            }
        }

        return SurfacesSizeList(
            previewSizeList,
            captureImageReaderSizeList,
            mediaRecorderSizeList,
            analysisImageReaderSizeList,
            highSpeedSizeList
        )
    }

    // todo : 핀치 크기에 따라 델타 값 차등 적용
    // todo : AE, AF 적용 (하나 클릭 감지, 빨리 클릭시 af로 돌아오기, 오래 클릭시 해당 값으로 고정 - 이 상태에서 다른데 클릭시 고정 풀기)
    // todo : 할수있다면 더블 클릭 줌인
    // (뷰 핀치 동작에 따른 줌 변경 리스너 주입 함수)
    // 뷰를 주입하면 해당 뷰를 핀칭할 때에 줌을 변경할수 있도록 리스너를 주입
    // 뷰를 여러번 넣으면 각각의 뷰에 핀칭을 할 때마다 줌을 변경
    // delta : 단위 핀치 이벤트에 따른 줌 변화량 = 높을수록 민감
    var beforePinchSpacingMbr: Float? = null
    var pinchBeforeMbr: Boolean = false
    var clickStartTimeMsMbr: Long? = null
    var longClickedBeforeMbr = false
    val longClickTimeMsMbr = 500
    fun setCameraPinchZoomTouchListener(
        view: View,
        delta: Float = 0.05f
    ) {
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

                when (event.pointerCount) {
                    2 -> { // 핀치를 위한 더블 터치일 경우
                        // 두손가락을 대고있는 매 순간 실행

                        // 현재 핀치 넓이 구하기
                        val currentFingerSpacing: Float
                        val x = event.getX(0) - event.getX(1)
                        val y = event.getY(0) - event.getY(1)
                        currentFingerSpacing = sqrt((x * x + y * y).toDouble()).toFloat()

                        if (beforePinchSpacingMbr != null) {
                            if (currentFingerSpacing > beforePinchSpacingMbr!!) { // 손가락을 벌린 경우
                                val zoom =
                                    if ((zoomFactorMbr + delta) > cameraInfoVoMbr.maxZoom) {
                                        cameraInfoVoMbr.maxZoom
                                    } else {
                                        zoomFactorMbr + delta
                                    }
                                setZoomFactor(
                                    zoom,
                                    onComplete = {})
                            } else if (currentFingerSpacing < beforePinchSpacingMbr!!) { // 손가락을 좁힌 경우
                                val zoom =
                                    if ((zoomFactorMbr - delta) < 1.0f) {
                                        1.0f
                                    } else {
                                        zoomFactorMbr - delta
                                    }
                                setZoomFactor(
                                    zoom,
                                    onComplete = {})
                            }
                        }

                        // 핀치 너비를 갱신
                        beforePinchSpacingMbr = currentFingerSpacing

                        pinchBeforeMbr = true
                        clickStartTimeMsMbr = null
                        longClickedBeforeMbr = false

                        return true
                    }
                    1 -> { // 한손가락을 대고있는 매 순간 실행

                        // long click 탐지
                        if (!pinchBeforeMbr) {
                            if (clickStartTimeMsMbr == null) {
                                clickStartTimeMsMbr = SystemClock.elapsedRealtime()
                                longClickedBeforeMbr = false
                            } else {
                                longClickedBeforeMbr =
                                    if (SystemClock.elapsedRealtime() - clickStartTimeMsMbr!! >= longClickTimeMsMbr) {
                                        if (!longClickedBeforeMbr) {
                                            // longClick 으로 전환되는 순간

                                        }
                                        true
                                    } else {
                                        false
                                    }
                            }
                        }

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                pinchBeforeMbr = false
                            }
                            MotionEvent.ACTION_UP -> {
                                if (!pinchBeforeMbr && !longClickedBeforeMbr) {
                                    // 핀치도, 롱 클릭도 아닌 단순 클릭

                                }

                                pinchBeforeMbr = false
                                longClickedBeforeMbr = false
                                clickStartTimeMsMbr = null
                            }

                            MotionEvent.ACTION_CANCEL -> {
                                pinchBeforeMbr = false
                                longClickedBeforeMbr = false
                                clickStartTimeMsMbr = null
                            }
                            else -> {}
                        }

                        return true
                    }
                    else -> {
                        return true
                    }
                }
            }
        })
    }


    // [카메라 조작 함수]
    // : 아래로부터는 Camera 현 status 에 따라 처리해야 할 부분을 자동으로 처리하고,
    //     중요한 조작 관련 인터페이스를 제공해주는 함수들

    // (카메라 객체 소멸)
    // camera status : n -> -1
    // media status : n -> 0
    // 카메라 객체 생성용 getInstance() 함수와 대비되는 메소드로, 생성했으면 소멸을 시켜야 함.
    fun destroyCameraObject(
        onComplete: () -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (cameraStatusCodeMbr == -1) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [카메라 상태 초기화]
            // 이미지 리더 요청을 먼저 비우기
            analysisImageReaderMbr?.setOnImageAvailableListener(
                { it.acquireLatestImage()?.close() },
                cameraThreadVoMbr.analysisImageReaderHandlerThreadObj.handler
            )

            captureImageReaderMbr?.setOnImageAvailableListener(
                { it.acquireLatestImage()?.close() },
                cameraThreadVoMbr.captureImageReaderHandlerThreadObj.handler
            )

            // 프리뷰가 설정되어 있다면 리스너 비우기
            if (previewConfigVoListMbr != null) {
                for (previewConfigVo in previewConfigVoListMbr!!) {
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
            }

            if (mediaRecorderStatusCodeMbr != 0) {
                mediaRecorderMbr?.reset()
                mediaRecorderStatusCodeMbr = 0

                cameraCaptureSessionMbr?.stopRepeating()
                cameraStatusCodeMbr = 1
            } else if (cameraStatusCodeMbr == 2) {
                // 세션이 실행중이라면 중지
                cameraCaptureSessionMbr?.stopRepeating()
                cameraStatusCodeMbr = 1
            }

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            analysisImageReaderMbr?.close()
            captureImageReaderMbr?.close()
            cameraCaptureSessionMbr?.close()
            cameraDeviceMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaRecorderConfigVoMbr = null
            analysisImageReaderMbr = null
            captureImageReaderMbr = null
            analysisImageReaderConfigVoMbr = null
            captureImageReaderConfigVoMbr = null
            cameraCaptureSessionMbr = null
            previewConfigVoListMbr = null
            previewSurfaceListMbr = null
            cameraDeviceMbr = null
            repeatRequestTargetVoMbr = null

            cameraStatusCodeMbr = -1

            cameraThreadVoMbr.cameraSemaphore.release()

            onComplete()
        }
    }

    // todo : 검사 및 리펙토링
    // (출력 서페이스 설정 함수)
    // camera status : 0 -> 1
    // media status : 0 -> 1, 0 -> 0
    // 서페이스 설정 검증 및 생성, 이후 CameraDevice, CameraCaptureSession 생성까지를 수행
    // 사용할 서페이스 사이즈 및 종류를 결정하는 것이 주요 기능
    // 실행시 카메라 초기화를 실행 (이미 생성된 CameraDevice 만 놔두고 모든것을 초기화) 후 설정
    // 주의 : 디바이스에 따라 서페이스 갯수에 따라 에러가 발생할 가능성이 있음.
    // onError 에러 코드 :
    // 아래 에러코드가 실행된다면 카메라가 초기화 된 상태에서 멈추며 executorOnError 가 실행됨
    // 0 : cameraStatusCodeMbr 가 0 이 아님
    // 1 : 출력 서페이스가 하나도 입력되어 있지 않음
    // 2 : 해당 사이즈 프리뷰 서페이스를 지원하지 않음
    // 3 : 해당 사이즈 캡쳐 이미지 리더 서페이스를 지원하지 않음
    // 4 : 해당 사이즈 미디어 리코더 서페이스를 지원하지 않음
    // 5 : 해당 사이즈 분석 이미지 리더 서페이스를 지원하지 않음
    // 6 : 미디어 레코더 오디오 녹음 설정 권한 비충족
    // 7 : 녹화 파일 확장자가 mp4 가 아님
    // 8 : 카메라 권한이 없음
    // 9 : CameraDevice.StateCallback.ERROR_CAMERA_DISABLED (권한 등으로 인해 사용이 불가능)
    // 10 : CameraDevice.StateCallback.ERROR_CAMERA_IN_USE (해당 카메라가 이미 사용중)
    // 11 : CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE (시스템에서 허용한 카메라 동시 사용을 초과)
    // 12 : CameraDevice.StateCallback.ERROR_CAMERA_DEVICE (카메라 디바이스 자체적인 문제)
    // 13 : CameraDevice.StateCallback.ERROR_CAMERA_SERVICE (안드로이드 시스템 문제)
    // 14 : 서페이스 텍스쳐 생성 불가
    // 15 : 생성된 서페이스가 존재하지 않음
    // 16 : 카메라 세션 생성 실패
    fun setCameraOutputSurfaces(
        previewConfigVoList: ArrayList<PreviewConfigVo>?,
        captureImageReaderConfigVo: ImageReaderConfigVo?,
        mediaRecorderConfigVo: MediaRecorderConfigVo?,
        analysisImageReaderConfigVo: ImageReaderConfigVo?,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [조건 검증]
            // (camera 상태 검증)
            if (cameraStatusCodeMbr != 0) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(0)
                return@run
            }

            // (서페이스 설정 파라미터 개수 검사)
            if (((previewConfigVoList == null ||
                        previewConfigVoList.isEmpty())) &&
                captureImageReaderConfigVo == null &&
                mediaRecorderConfigVo == null &&
                analysisImageReaderConfigVo == null
            ) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return@run
            }

            // (서페이스 사이즈 검사)
            // 프리뷰 리스트 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
            if (previewConfigVoList != null && previewConfigVoList.isNotEmpty()) {
                val cameraSizes = cameraInfoVoMbr.previewSizeInfoList

                // 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes.isEmpty()
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(2)
                    return@run
                } else {
                    for (previewConfig in previewConfigVoList) {
                        if (cameraSizes.indexOfFirst {
                                it.size.width == previewConfig.cameraOrientSurfaceSize.width &&
                                        it.size.height == previewConfig.cameraOrientSurfaceSize.height
                            } == -1) {
                            cameraThreadVoMbr.cameraSemaphore.release()
                            onError(2)
                            return@run
                        }
                    }
                }
            }

            if (captureImageReaderConfigVo != null) {
                val cameraSizes = cameraInfoVoMbr.captureImageReaderSizeInfoList

                // 이미지 리더 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes.isEmpty() || cameraSizes.indexOfFirst {
                        it.size.width == captureImageReaderConfigVo.cameraOrientSurfaceSize.width &&
                                it.size.height == captureImageReaderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(3)
                    return@run
                }
            }

            if (mediaRecorderConfigVo != null) {
                val cameraSizes = cameraInfoVoMbr.mediaRecorderSizeInfoList

                // 이미지 리더 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes.isEmpty() || cameraSizes.indexOfFirst {
                        it.size.width == mediaRecorderConfigVo.cameraOrientSurfaceSize.width &&
                                it.size.height == mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(4)
                    return@run
                }
            }

            if (analysisImageReaderConfigVo != null) {
                val cameraSizes = cameraInfoVoMbr.analysisImageReaderSizeInfoList

                // 이미지 리더 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes.isEmpty() || cameraSizes.indexOfFirst {
                        it.size.width == analysisImageReaderConfigVo.cameraOrientSurfaceSize.width &&
                                it.size.height == analysisImageReaderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(5)
                    return@run
                }
            }

            // (미디어 레코더 설정 검증)
            if (mediaRecorderConfigVo != null) {
                // 미디어 레코더, 오디오 녹화 설정을 했는데 권한이 없을 때
                if (mediaRecorderConfigVo.audioRecordingBitrate != null &&
                    ActivityCompat.checkSelfPermission(
                        parentActivityMbr,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(6)
                    return@run
                }

                // 설정 파일 확장자 검증
                if (mediaRecorderConfigVo.mediaRecordingMp4File.extension != "mp4") {
                    analysisImageReaderMbr = null
                    captureImageReaderMbr = null
                    analysisImageReaderConfigVoMbr = null
                    captureImageReaderConfigVoMbr = null
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(7)
                    return@run
                }
            }


            // [서페이스 준비]
            // (이미지 리더 서페이스 준비)
            val analysisImageReader = if (analysisImageReaderConfigVo != null) {
                ImageReader.newInstance(
                    analysisImageReaderConfigVo.cameraOrientSurfaceSize.width,
                    analysisImageReaderConfigVo.cameraOrientSurfaceSize.height,
                    ImageFormat.YUV_420_888,
                    2
                ).apply {
                    setOnImageAvailableListener(
                        analysisImageReaderConfigVo.imageReaderCallback,
                        cameraThreadVoMbr.analysisImageReaderHandlerThreadObj.handler
                    )
                }
            } else {
                null
            }

            val captureImageReader = if (captureImageReaderConfigVo != null) {
                ImageReader.newInstance(
                    captureImageReaderConfigVo.cameraOrientSurfaceSize.width,
                    captureImageReaderConfigVo.cameraOrientSurfaceSize.height,
                    ImageFormat.JPEG,
                    2
                ).apply {
                    setOnImageAvailableListener(
                        captureImageReaderConfigVo.imageReaderCallback,
                        cameraThreadVoMbr.captureImageReaderHandlerThreadObj.handler
                    )
                }
            } else {
                null
            }

            // (미디어 레코더 서페이스 준비)
            var mediaRecorder: MediaRecorder? = null
            var mediaCodecSurface: Surface? = null
            if (mediaRecorderConfigVo != null) {
                // 미디어 레코더 생성
                mediaRecorder =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(parentActivityMbr)
                    } else {
                        MediaRecorder()
                    }

                // (미디어 레코더 설정)
                // 서페이스 소스 설정
                if (mediaRecorderConfigVo.audioRecordingBitrate != null) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)

                // 파일 포멧 설정
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                // 파일 경로 설정
                // 만일 같은 파일이 이전에 비휘발 메모리에 존재하면 새로써짐
                if (mediaRecorderConfigVo.mediaRecordingMp4File.exists()) {
                    mediaRecorderConfigVo.mediaRecordingMp4File.delete()
                }
                mediaRecorderConfigVo.mediaRecordingMp4File.createNewFile()
                mediaRecorder.setOutputFile(mediaRecorderConfigVo.mediaRecordingMp4File.absolutePath)

                // 데이터 저장 프레임 설정
                val maxMediaRecorderFps =
                    cameraInfoVoMbr.mediaRecorderSizeInfoList[cameraInfoVoMbr.mediaRecorderSizeInfoList.indexOfFirst {
                        it.size.width == mediaRecorderConfigVo.cameraOrientSurfaceSize.width &&
                                it.size.height == mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                    }].fps

                if (mediaRecorderConfigVo.videoRecordingFps > maxMediaRecorderFps) {
                    mediaRecorderConfigVo.videoRecordingFps = maxMediaRecorderFps
                }

                mediaRecorder.setVideoFrameRate(mediaRecorderConfigVo.videoRecordingFps)

                // 영상 데이터 저장 퀄리티 설정
                // todo 최소 타겟 디바이스에서 에러 발생 안하는 최대 값
                val maxVideoBitrate = (Int.MAX_VALUE * 0.85).toInt()

                // 커스텀 설정 값이 있을 때
                if (mediaRecorderConfigVo.videoRecordingBitrate > maxVideoBitrate) {
                    mediaRecorderConfigVo.videoRecordingBitrate = maxVideoBitrate
                }

                mediaRecorder.setVideoEncodingBitRate(mediaRecorderConfigVo.videoRecordingBitrate)

                // 음성 데이터 저장 퀄리티 설정
                if (mediaRecorderConfigVo.audioRecordingBitrate != null) {

                    // todo
                    val maxAudioBitrate = 2048000 // 256 kb

                    // 커스텀 설정 값이 있을 때
                    if (mediaRecorderConfigVo.audioRecordingBitrate!! > maxAudioBitrate) {
                        mediaRecorderConfigVo.audioRecordingBitrate = maxAudioBitrate
                    }

                    mediaRecorder.setAudioEncodingBitRate(mediaRecorderConfigVo.audioRecordingBitrate!!)
                }

                // 서페이스 사이즈 설정
                mediaRecorder.setVideoSize(
                    mediaRecorderConfigVo.cameraOrientSurfaceSize.width,
                    mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                )

                // 인코딩 타입 설정
                if (mediaRecorderConfigVo.useH265Codec) {
                    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
                } else {
                    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                }
                if (mediaRecorderConfigVo.audioRecordingBitrate != null) {
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }

                // 서페이스 방향 설정
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

                val deviceOrientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    parentActivityMbr.display!!.rotation
                } else {
                    parentActivityMbr.windowManager.defaultDisplay.rotation
                }

                when (cameraInfoVoMbr.sensorOrientation) {
                    90 ->
                        mediaRecorder.setOrientationHint(
                            defaultOrientation.get(deviceOrientation)
                        )
                    270 ->
                        mediaRecorder.setOrientationHint(
                            inverseOrientation.get(deviceOrientation)
                        )
                }

                // 미디어 레코더 서페이스 생성
                mediaCodecSurface = MediaCodec.createPersistentInputSurface()
                mediaRecorder.setInputSurface(mediaCodecSurface)
                mediaRecorder.prepare()
            }

            // (프리뷰 서페이스 준비)
            if (previewConfigVoList != null &&
                previewConfigVoList.isNotEmpty()
            ) {
                val previewListSize = previewConfigVoList.size
                var checkedPreviewCount = 0
                val checkedPreviewCountSemaphore = Semaphore(1)

                for (previewIdx in 0 until previewListSize) {
                    checkedPreviewCountSemaphore.acquire()

                    val previewObj = previewConfigVoList[previewIdx].autoFitTextureView
                    val surfaceSize = previewConfigVoList[previewIdx].cameraOrientSurfaceSize

                    if (parentActivityMbr.isDestroyed || parentActivityMbr.isFinishing) {
                        cameraThreadVoMbr.cameraSemaphore.release()
                        return@run
                    }

                    parentActivityMbr.runOnUiThread {
                        if (previewObj.isAvailable) {
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

                            // (텍스쳐 뷰 비율 변경)
                            if (cameraSensorOrientationAndDeviceAreSameWh()) {
                                previewObj.setAspectRatio(
                                    surfaceSize.width,
                                    surfaceSize.height
                                )
                            } else {
                                previewObj.setAspectRatio(
                                    surfaceSize.height,
                                    surfaceSize.width
                                )
                            }

                            configureTransform(
                                previewObj,
                                surfaceSize.width,
                                surfaceSize.height,
                                previewObj.width,
                                previewObj.height
                            )

                            if (++checkedPreviewCount == previewListSize) {
                                // 마지막 작업일 때
                                checkedPreviewCountSemaphore.release()

                                cameraThreadVoMbr.cameraHandlerThreadObj.runInHandlerThread {
                                    onSurfacesAllChecked(
                                        analysisImageReaderConfigVo,
                                        analysisImageReader,
                                        captureImageReaderConfigVo,
                                        captureImageReader,
                                        mediaRecorderConfigVo,
                                        mediaRecorder,
                                        mediaCodecSurface,
                                        previewConfigVoList,
                                        onComplete,
                                        onError
                                    )
                                }
                            } else {
                                checkedPreviewCountSemaphore.release()
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
                                        if (cameraSensorOrientationAndDeviceAreSameWh()) {
                                            previewObj.setAspectRatio(
                                                surfaceSize.width,
                                                surfaceSize.height
                                            )
                                        } else {
                                            previewObj.setAspectRatio(
                                                surfaceSize.height,
                                                surfaceSize.width
                                            )
                                        }

                                        configureTransform(
                                            previewObj,
                                            surfaceSize.width,
                                            surfaceSize.height,
                                            width,
                                            height
                                        )

                                        if (++checkedPreviewCount == previewListSize) {
                                            // 마지막 작업일 때
                                            checkedPreviewCountSemaphore.release()

                                            cameraThreadVoMbr.cameraHandlerThreadObj.runInHandlerThread {
                                                onSurfacesAllChecked(
                                                    analysisImageReaderConfigVo,
                                                    analysisImageReader,
                                                    captureImageReaderConfigVo,
                                                    captureImageReader,
                                                    mediaRecorderConfigVo,
                                                    mediaRecorder,
                                                    mediaCodecSurface,
                                                    previewConfigVoList,
                                                    onComplete,
                                                    onError
                                                )
                                            }
                                        } else {
                                            checkedPreviewCountSemaphore.release()
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
                    analysisImageReaderConfigVo,
                    analysisImageReader,
                    captureImageReaderConfigVo,
                    captureImageReader,
                    mediaRecorderConfigVo,
                    mediaRecorder,
                    mediaCodecSurface,
                    previewConfigVoList,
                    onComplete,
                    onError,
                )
            }
        }
    }

    // (카메라 서페이스 까지 초기화)
    // camera status : -1 -> -1, 0 -> 0, n -> 0
    // media status : n -> 0
    // 카메라 디바이스를 제외한 나머지 초기화 (= 서페이스 설정하기 이전 상태로 되돌리기)
    // 미디어 레코딩 결과물 파일을 얻기 위해선 먼저 이를 실행해야함
    fun unsetCameraOutputSurfaces(
        onComplete: () -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (cameraStatusCodeMbr == -1 || cameraStatusCodeMbr == 0) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [카메라 상태 초기화]
            // 이미지 리더 요청을 먼저 비우기
            analysisImageReaderMbr?.setOnImageAvailableListener(
                { it.acquireLatestImage()?.close() },
                cameraThreadVoMbr.analysisImageReaderHandlerThreadObj.handler
            )
            captureImageReaderMbr?.setOnImageAvailableListener(
                { it.acquireLatestImage()?.close() },
                cameraThreadVoMbr.captureImageReaderHandlerThreadObj.handler
            )

            // 프리뷰가 설정되어 있다면 리스너 비우기
            if (previewConfigVoListMbr != null) {
                for (previewConfigVo in previewConfigVoListMbr!!) {
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
            }

            if (mediaRecorderStatusCodeMbr != 0) {
                // 레코딩 중이라면 레코더 종료 후 세션 중지
                mediaRecorderMbr?.reset()
                mediaRecorderStatusCodeMbr = 0

                cameraCaptureSessionMbr?.stopRepeating()
                cameraStatusCodeMbr = 1
            } else if (cameraStatusCodeMbr == 2) {
                // 세션이 실행중이라면 중지
                cameraCaptureSessionMbr?.stopRepeating()
                cameraStatusCodeMbr = 1
            }

            // (자원 해소)
            mediaRecorderMbr?.release()
            mediaCodecSurfaceMbr?.release()
            analysisImageReaderMbr?.close()
            captureImageReaderMbr?.close()
            cameraCaptureSessionMbr?.close()

            // (멤버 변수 비우기)
            mediaRecorderMbr = null
            mediaRecorderConfigVoMbr = null
            analysisImageReaderMbr = null
            captureImageReaderMbr = null
            analysisImageReaderConfigVoMbr = null
            captureImageReaderConfigVoMbr = null
            cameraCaptureSessionMbr = null
            previewConfigVoListMbr = null
            previewSurfaceListMbr = null
            repeatRequestTargetVoMbr = null

            cameraStatusCodeMbr = 0

            cameraThreadVoMbr.cameraSemaphore.release()

            onComplete()
        }
    }

    // (반복 리퀘스트 실행)
    // camera status : 1 -> 2, 2 -> 2
    // media status : 1 -> 2, n(2, 3, 4) -> n
    // forPreview, forAnalysisImageReader, forMediaRecorder -> 어느 서페이스를 사용할지를 결정
    // 타겟 서페이스 종류에 따라 TEMPLATE_RECORD(forMediaRecorder) 혹은 TEMPLATE_PREVIEW(not forMediaRecorder) 세션 사용
    // onError 에러 코드 :
    // 1 : cameraStatusCodeMbr 가 1 도 아니고, 2도 아닐 때
    // 2 : preview 설정이지만 preview 서페이스가 없을 때
    // 3 : analysisImageReader 설정이지만 analysisImageReader 서페이스가 없을 때
    // 4 : mediaRecorder 설정이지만 mediaRecorder 서페이스가 없을 때
    fun repeatingRequestOnTemplate(
        forPreview: Boolean,
        forMediaRecorder: Boolean,
        forAnalysisImageReader: Boolean,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            // [조건 검증]
            if (cameraStatusCodeMbr != 1 &&
                cameraStatusCodeMbr != 2
            ) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return@run
            }

            if (forPreview) {
                if (previewSurfaceListMbr == null ||
                    previewSurfaceListMbr!!.isEmpty()
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(2)
                    return@run
                }
            }

            if (forAnalysisImageReader) {
                if (analysisImageReaderMbr == null) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(3)
                    return@run
                }
            }

            if (forMediaRecorder) {
                if (mediaCodecSurfaceMbr == null) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(4)
                    return@run
                }
            }

            // [모드 설정]
            val requestTemplate = if (forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                forPreview,
                false,
                forMediaRecorder,
                forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            repeatRequestTargetVoMbr = RepeatRequestTargetVo(
                forPreview,
                forAnalysisImageReader,
                forMediaRecorder
            )

            if (forMediaRecorder && mediaRecorderStatusCodeMbr == 1) {
                mediaRecorderStatusCodeMbr = 2
            }
            cameraStatusCodeMbr = 2

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (반복 리퀘스트 중단 함수)
    // camera status : 2 -> 1
    // media status : 3 -> 4, n -> n
    // 현재 세션이 repeating 이라면 이를 중단함.
    // 현재 미디어 레코딩 중이라면 이를 일시중지함.

    // onError 에러 코드 :
    // 1 : 카메라가 현재 실행 상태가 아님
    fun stopRepeatingRequest(
        onCameraPause: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()
            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return
            }

            if (mediaRecorderStatusCodeMbr == 3) {
                mediaRecorderMbr?.pause()
                mediaRecorderStatusCodeMbr = 4
            }

            cameraCaptureSessionMbr?.stopRepeating()
            repeatRequestTargetVoMbr = null
            cameraStatusCodeMbr = 1

            cameraThreadVoMbr.cameraSemaphore.release()

            onCameraPause()
        }
    }

    // (캡쳐 리퀘스트 요청 함수)
    // camera status : 1, 2
    // 카메라 현 설정으로 캡쳐
    // TEMPLATE_STILL_CAPTURE 으로 실행되며, 만약 녹화중이라면 TEMPLATE_VIDEO_SNAPSHOT 으로 실행
    // 캡쳐 결과는 captureImageReader 로 반환됨

    // onError 에러 코드 :
    // 1 : 캡쳐 가능 상태가 아님
    // 2 : 캡쳐 이미지 리더가 설정되지 않음
    fun captureRequest(
        captureCallback: CameraCaptureSession.CaptureCallback?,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()
            if (cameraStatusCodeMbr != 1 && cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return
            }

            if (captureImageReaderMbr == null) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(2)
                return
            }

            // [모드 설정]
            val requestTemplate = if (mediaRecorderStatusCodeMbr != 3) { // 미디어 레코딩 중이 아닐시
                CameraDevice.TEMPLATE_STILL_CAPTURE
            } else { // 미디어 레코딩 중일시
                CameraDevice.TEMPLATE_VIDEO_SNAPSHOT
            }

            // [리퀘스트 빌더 생성]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                forPreview = false,
                forCaptureImageReader = true,
                forMediaRecorder = false,
                forAnalysisImageReader = false
            )

            cameraCaptureSessionMbr!!.capture(
                request,
                captureCallback,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
        }
    }


    // [카메라 설정 조작 함수]
    // 카메라 내부 멤버변수 설정 조작 함수
    // 설정이 조작되면 다음 리퀘스트에 반영이 되며, 만약 리퀘스트 반복 실행중이라면 변경사항 즉시 적용

    // (카메라 줌 비율을 변경하는 함수)
    // 카메라가 실행중 상태라면 즉시 반영
    // onZoomSettingComplete : 반환값 = 적용된 현재 줌 펙터 값
    fun setZoomFactor(
        zoomFactor: Float,
        onComplete: () -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            val zoom = if (cameraInfoVoMbr.maxZoom < zoomFactor) {
                // 가용 줌 최대치에 설정을 맞추기
                cameraInfoVoMbr.maxZoom
            } else if (zoomFactor < 1f) {
                1f
            } else {
                zoomFactor
            }

            zoomFactorMbr = zoom

            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [모드 설정]
            val requestTemplate = if (repeatRequestTargetVoMbr!!.forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                repeatRequestTargetVoMbr!!.forPreview,
                false,
                repeatRequestTargetVoMbr!!.forMediaRecorder,
                repeatRequestTargetVoMbr!!.forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (손떨림 방지 설정)
    // onError 에러 코드 :
    // 1 : Stabilization 지원 불가
    fun setCameraStabilization(
        stabilizationOn: Boolean,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            val stabilization = if (stabilizationOn) {
                if (cameraInfoVoMbr.isOpticalStabilizationAvailable) { // 기계 보정 사용 가능
                    1
                } else if (cameraInfoVoMbr.isVideoStabilizationAvailable) {// 소프트 웨어 보정 사용 가능
                    2
                } else {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(1)
                    return@run
                }
            } else {
                0
            }

            cameraStabilizationSetMbr = stabilization

            // 세션이 현재 실행중이 아니라면 여기서 멈추기
            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return@run
            }

            // [모드 설정]
            val requestTemplate = if (repeatRequestTargetVoMbr!!.forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                repeatRequestTargetVoMbr!!.forPreview,
                false,
                repeatRequestTargetVoMbr!!.forMediaRecorder,
                repeatRequestTargetVoMbr!!.forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (포커스 거리 설정)
    // 거리는 0부터 시작해서 minimumFocusDistanceMbr 까지의 수치
    // 0은 가장 먼 곳, 수치가 커질수록 가까운 곳의 포커스
    // -1 은 자연스런 오토 포커스, -2 는 빠른 오토 포커스
    // onError 에러 코드 :
    // 1 : focusDistance 파라미터 에러
    // 2 : naturalAutoFocusSupported 지원 불가
    // 3 : fastAutoFocusSupported 지원 불가
    fun setFocusDistance(
        focusDistance: Float,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            focusDistanceMbr = if (focusDistance >= 0f) {
                if (focusDistance <= cameraInfoVoMbr.supportedMinimumFocusDistance) {
                    focusDistance
                } else {
                    cameraInfoVoMbr.supportedMinimumFocusDistance
                }
            } else {
                if (focusDistance == -1f) {
                    if (cameraInfoVoMbr.naturalAutoFocusSupported) {
                        focusDistance
                    } else {
                        cameraThreadVoMbr.cameraSemaphore.release()
                        onError(2)
                        return@run
                    }
                } else if (focusDistance == -2f) {
                    if (cameraInfoVoMbr.fastAutoFocusSupported) {
                        focusDistance
                    } else {
                        cameraThreadVoMbr.cameraSemaphore.release()
                        onError(3)
                        return@run
                    }
                } else {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(1)
                    return@run
                }
            }

            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [모드 설정]
            val requestTemplate = if (repeatRequestTargetVoMbr!!.forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                repeatRequestTargetVoMbr!!.forPreview,
                false,
                repeatRequestTargetVoMbr!!.forMediaRecorder,
                repeatRequestTargetVoMbr!!.forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (Exposure 설정)
    // exposureNanoSec : 나노초 == 초/1000000000
    //     음수라면 Auto Exposure ON
    // ex : 1000000000 / 80

    // onError 에러 코드 :
    // 1 : 오토 Exposure 지원이 불가
    // 1 : exposureNanoSec 파라미터 에러
    fun setExposureTime(
        exposureNanoSec: Long?,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            exposureTimeNsMbr = if (exposureNanoSec == null) {
                if (!cameraInfoVoMbr.autoExposureSupported) {
                    // 오토 Exposure 지원이 안되면
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(1)
                    return@run
                } else {
                    null
                }
            } else {
                if (exposureNanoSec < 0) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(2)
                    return@run
                } else {
                    exposureNanoSec
                }
            }

            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [모드 설정]
            val requestTemplate = if (repeatRequestTargetVoMbr!!.forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                repeatRequestTargetVoMbr!!.forPreview,
                false,
                repeatRequestTargetVoMbr!!.forMediaRecorder,
                repeatRequestTargetVoMbr!!.forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (WhiteBalance Color Temperature 설정)
    // null 이라면 AWB, 0~100 범위의 값 설정
    // 0이면 파란색으로 차가운 색, 100이면 노란색으로 따뜻한 색
    // 템플릿 적용 파라미터
    // -1 : CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
    // -2 : CONTROL_AWB_MODE_DAYLIGHT
    // -3 : CONTROL_AWB_MODE_FLUORESCENT
    // -4 : CONTROL_AWB_MODE_INCANDESCENT
    // -5 : CONTROL_AWB_MODE_SHADE
    // -6 : CONTROL_AWB_MODE_TWILIGHT
    // -7 : CONTROL_AWB_MODE_WARM_FLUORESCENT
    // onError 에러 코드 :
    // 1 : 오토 WhiteBalance 지원이 불가
    // 2 : colorTemperature 파라미터 에러
    fun setWhiteBalanceColorTemperature(
        colorTemperature: Int?,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            whiteBalanceColorTemperatureMbr = if (colorTemperature == null) {
                if (!cameraInfoVoMbr.autoWhiteBalanceSupported) {
                    // 오토 WhiteBalance 지원이 안되면
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(1)
                    return@run
                } else {
                    null
                }
            } else {
                if (colorTemperature < -7 || colorTemperature > 100) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    onError(2)
                    return@run
                } else {
                    colorTemperature
                }
            }

            if (cameraStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onComplete()
                return
            }

            // [모드 설정]
            val requestTemplate = if (repeatRequestTargetVoMbr!!.forMediaRecorder) { // 레코딩 설정시
                CameraDevice.TEMPLATE_RECORD
            } else { // 레코딩 설정이 아닐시
                CameraDevice.TEMPLATE_PREVIEW
            }

            // [카메라 실행]
            val request = getCameraMemberConfigRequest(
                requestTemplate,
                repeatRequestTargetVoMbr!!.forPreview,
                false,
                repeatRequestTargetVoMbr!!.forMediaRecorder,
                repeatRequestTargetVoMbr!!.forAnalysisImageReader
            )

            cameraCaptureSessionMbr!!.setRepeatingRequest(
                request,
                null,
                cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }


    // [미디어 레코더 조작 함수]
    // prepare 는 서페이스 설정시 자동 실행
    // stop 은 cameraObject 를 stop 시킬 때, 혹은 setSurface 의 초기화 로직에서 자동으로 실행됨
    // 이외에 start, pause, resume 은 내부적으로 전혀 손대지 않기에 아래 함수로 사용자가 적절히 사용할 것.
    // camera pause 시 아직 mediaRecorder 가 녹화중이라면 화면은 움직이지 않고 계속 시간에 따라 녹화가 진행됨.
    // 고로 단순히 pause 후 연속 녹화를 하려면 외부에서 camera pause 이전에 mediaRecorder pause 를 먼저 해주는 것을 추천

    // (미디어 레코딩 시작)
    // 에러 코드 :
    // 1 : 레코딩 준비 상태가 아님
    // 2 : 현재 카메라가 동작중이 아님
    fun startMediaRecording(
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (mediaRecorderStatusCodeMbr != 2) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return@run
            }

            mediaRecorderMbr!!.start()

            mediaRecorderStatusCodeMbr = 3

            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (미디어 레코딩 일시정지)
    // 결과 코드 :
    // 1 : 미디어 레코딩 중이 아님
    fun pauseMediaRecording(
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (mediaRecorderStatusCodeMbr != 3) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return
            }

            mediaRecorderMbr?.pause()
            mediaRecorderStatusCodeMbr = 4
            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }

    // (미디어 레코딩 재시작)
    // 결과 코드 :
    // 1 : 현제 미디어 레코딩 일시중지 중이 아님
    fun resumeMediaRecording(
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        cameraThreadVoMbr.cameraHandlerThreadObj.run {
            cameraThreadVoMbr.cameraSemaphore.acquire()

            if (mediaRecorderStatusCodeMbr != 4) {
                cameraThreadVoMbr.cameraSemaphore.release()
                onError(1)
                return
            }

            mediaRecorderMbr?.resume()
            mediaRecorderStatusCodeMbr = 3
            cameraThreadVoMbr.cameraSemaphore.release()
            onComplete()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // (startCamera 함수 서페이스 준비가 끝난 시점의 처리 함수)
    // uiThread 에서 실행된다고 가정
    private fun onSurfacesAllChecked(
        analysisImageReaderConfigVo: ImageReaderConfigVo?,
        analysisImageReader: ImageReader?,
        captureImageReaderConfigVo: ImageReaderConfigVo?,
        captureImageReader: ImageReader?,
        mediaRecorderConfigVo: MediaRecorderConfigVo?,
        mediaRecorder: MediaRecorder?,
        mediaCodecSurface: Surface?,
        previewConfigVoList: ArrayList<PreviewConfigVo>?,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        // (카메라 디바이스 열기)
        openCameraDevice(
            onCameraDeviceReady = { // 카메라 디바이스가 준비된 시점

                // UI 스레드 생존 검증
                if (parentActivityMbr.isDestroyed ||
                    parentActivityMbr.isFinishing
                ) {
                    cameraThreadVoMbr.cameraSemaphore.release()
                    return@openCameraDevice
                }

                parentActivityMbr.runOnUiThread {
                    var previewSurfaceList: ArrayList<Surface>? = null
                    if (previewConfigVoList != null) {
                        previewSurfaceList = ArrayList()
                        for (previewConfigVo in previewConfigVoList) {
                            val surfaceTexture =
                                previewConfigVo.autoFitTextureView.surfaceTexture

                            if (surfaceTexture == null) { // 프리뷰 설정이 존재할 때 서페이스 텍스쳐가 반환되지 않은 경우
                                for (previewConfigVo1 in previewConfigVoList) {
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
                                mediaRecorder?.reset()
                                mediaRecorder?.release()
                                mediaCodecSurface?.release()
                                analysisImageReader?.close()
                                captureImageReader?.close()

                                cameraThreadVoMbr.cameraSemaphore.release()
                                onError(14)
                                return@runOnUiThread
                            }

                            surfaceTexture.setDefaultBufferSize(
                                previewConfigVo.cameraOrientSurfaceSize.width,
                                previewConfigVo.cameraOrientSurfaceSize.height
                            )

                            previewSurfaceList.add(Surface(surfaceTexture))
                        }
                    }

                    cameraThreadVoMbr.cameraHandlerThreadObj.runInHandlerThread {
                        if (analysisImageReaderConfigVo == null &&
                            analysisImageReader == null &&
                            captureImageReaderConfigVo == null &&
                            captureImageReader == null &&
                            mediaRecorderConfigVo == null &&
                            mediaRecorder == null &&
                            mediaCodecSurface == null &&
                            previewConfigVoList == null &&
                            (previewSurfaceList == null ||
                                    previewSurfaceList.isEmpty())
                        ) { // 생성 서페이스가 하나도 존재하지 않으면,
                            cameraThreadVoMbr.cameraSemaphore.release()
                            onError(15)
                            return@runInHandlerThread
                        }

                        // (카메라 세션 생성)
                        createCameraSessionAsync(
                            previewSurfaceList,
                            analysisImageReader,
                            captureImageReader,
                            mediaCodecSurface,
                            onComplete = {
                                analysisImageReaderConfigVoMbr = analysisImageReaderConfigVo
                                analysisImageReaderMbr = analysisImageReader
                                captureImageReaderConfigVoMbr = captureImageReaderConfigVo
                                captureImageReaderMbr = captureImageReader
                                mediaRecorderConfigVoMbr = mediaRecorderConfigVo
                                mediaRecorderMbr = mediaRecorder
                                mediaCodecSurfaceMbr = mediaCodecSurface
                                previewConfigVoListMbr = previewConfigVoList
                                previewSurfaceListMbr = previewSurfaceList

                                if (mediaRecorderConfigVoMbr != null) {
                                    mediaRecorderStatusCodeMbr = 1
                                }

                                cameraStatusCodeMbr = 1

                                cameraThreadVoMbr.cameraSemaphore.release()
                                onComplete()
                            },
                            onError = { errorCode ->
                                if (parentActivityMbr.isDestroyed || parentActivityMbr.isFinishing) {
                                    cameraThreadVoMbr.cameraSemaphore.release()
                                    return@createCameraSessionAsync
                                }

                                if (null != previewConfigVoList) {
                                    for (previewConfigVo1 in previewConfigVoList) {
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
                                }

                                // (자원 해소)
                                mediaRecorder?.reset()
                                mediaRecorder?.release()
                                mediaCodecSurface?.release()
                                analysisImageReader?.close()
                                captureImageReader?.close()

                                cameraThreadVoMbr.cameraSemaphore.release()
                                onError(errorCode)
                            }
                        )

                    }
                }
            },
            onCameraDisconnected = {
                // todo
                onCameraDisconnectedMbr()
            },
            onError = { errorCode ->
                if (null != previewConfigVoList) {
                    for (previewConfigVo1 in previewConfigVoList) {
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
                }

                // (자원 해소)
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaCodecSurface?.release()
                analysisImageReader?.close()
                captureImageReader?.close()

                cameraThreadVoMbr.cameraSemaphore.release()
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
            onError(8)
            return
        }

        cameraManagerMbr.openCamera(
            cameraInfoVoMbr.cameraId,
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
                    cameraDeviceMbr = camera
                    onCameraDisconnected()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    when (error) {
                        ERROR_CAMERA_DISABLED -> {
                            onError(9)
                        }
                        ERROR_CAMERA_IN_USE -> {
                            onError(10)
                        }
                        ERROR_MAX_CAMERAS_IN_USE -> {
                            onError(11)
                        }
                        ERROR_CAMERA_DEVICE -> {
                            onError(12)
                        }
                        ERROR_CAMERA_SERVICE -> {
                            onError(13)
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
        previewSurfaceList: ArrayList<Surface>?,
        analysisImageReader: ImageReader?,
        captureImageReader: ImageReader?,
        mediaCodecSurface: Surface?,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        // api 28 이상 / 미만의 요청 방식이 다름
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // api 28 이상
            // 출력 설정 객체 리스트
            val outputConfigurationList = ArrayList<OutputConfiguration>()

            // 프리뷰 서페이스 주입
            if (null != previewSurfaceList) {
                for (previewSurface in previewSurfaceList) {
                    outputConfigurationList.add(OutputConfiguration(previewSurface))
                }
            }

            // 이미지 리더 서페이스 주입
            if (null != analysisImageReader) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        analysisImageReader.surface
                    )
                )
            }
            if (null != captureImageReader) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        captureImageReader.surface
                    )
                )
            }

            // 미디어 리코더 서페이스 주입
            if (null != mediaCodecSurface) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        mediaCodecSurface
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

                        onComplete()
                    }

                    // 세션 생성 실패
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError(16)
                    }
                }
            ))
        } else {
            // api 28 미만
            // 출력 서페이스 리스트
            val surfaces = ArrayList<Surface>()

            // 프리뷰 서페이스 주입
            if (null != previewSurfaceList) {
                surfaces.addAll(previewSurfaceList)
            }

            // 이미지 리더 서페이스 주입
            if (null != analysisImageReader) {
                surfaces.add(analysisImageReader.surface)
            }
            if (null != captureImageReader) {
                surfaces.add(captureImageReader.surface)
            }

            // 비디오 리코더 서페이스 주입
            if (null != mediaCodecSurface) {
                surfaces.add(mediaCodecSurface)
            }

            // todo 고속촬영
            cameraDeviceMbr?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr = session

                        onComplete()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError(16)
                    }
                }, cameraThreadVoMbr.cameraHandlerThreadObj.handler
            )
        }
    }

    // (카메라 상태 멤버변수에 따라 캡쳐 리퀘스트를 생성하는 함수)
    private fun getCameraMemberConfigRequest(
        requestTemplate: Int,
        forPreview: Boolean,
        forCaptureImageReader: Boolean,
        forMediaRecorder: Boolean,
        forAnalysisImageReader: Boolean,
    ): CaptureRequest {
        // [리퀘스트 빌더 생성]
        val captureRequestBuilder: CaptureRequest.Builder =
            cameraDeviceMbr!!.createCaptureRequest(requestTemplate)

        // [타겟 서페이스 설정]
        if (forPreview) { // 프리뷰 사용 설정
            // 프리뷰 서페이스 타겟 추가
            for (previewSurface in previewSurfaceListMbr!!) {
                captureRequestBuilder.addTarget(previewSurface)
            }
        }

        if (forCaptureImageReader) { // 이미지 리더 사용 설정
            // 이미지 리더 서페이스 타겟 추가
            captureRequestBuilder.addTarget(captureImageReaderMbr!!.surface)

            // todo : front 시에 캡쳐 방향 틀림
            val orientation = SparseIntArray().apply {
                append(
                    Surface.ROTATION_0,
                    90
                )
                append(
                    Surface.ROTATION_90,
                    0
                )
                append(
                    Surface.ROTATION_180,
                    270
                )
                append(
                    Surface.ROTATION_270,
                    180
                )
            }

            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivityMbr.display!!.rotation
            } else {
                parentActivityMbr.windowManager.defaultDisplay.rotation
            }

            captureRequestBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                (orientation.get(rotation) + cameraInfoVoMbr.sensorOrientation + 270) % 360
            )
        }

        if (forMediaRecorder) { // 미디어 레코더 사용 설정
            // 미디어 레코더 서페이스 타겟 추가
            captureRequestBuilder.addTarget(mediaCodecSurfaceMbr!!)
        }

        if (forAnalysisImageReader) { // 이미지 리더 사용 설정
            // 이미지 리더 서페이스 타겟 추가
            captureRequestBuilder.addTarget(analysisImageReaderMbr!!.surface)
        }

        // [리퀘스트 설정]
        // (3A 설정)
        // (포커스 거리 설정)
        if (focusDistanceMbr == -1f) {
            // 자연스런 오토 포커스 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        } else if (focusDistanceMbr == -2f) {
            // 빠른 오토 포커스 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        } else if (focusDistanceMbr >= 0f) {
            // 포커스 수동 거리 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            captureRequestBuilder.set(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                focusDistanceMbr
            )
        }

        // (Exposure 설정)
        if (exposureTimeNsMbr == null) {
            // AE 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )
        } else {
            // 수동 노출 시간 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )

            captureRequestBuilder.set(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                exposureTimeNsMbr
            )
        }

        // (WhiteBalance 설정)
        if (whiteBalanceColorTemperatureMbr == null) {
            // AWH 설정
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        } else {
            when (whiteBalanceColorTemperatureMbr) {
                -1 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    )
                }
                -2 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                    )
                }
                -3 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                    )
                }
                -4 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                    )
                }
                -5 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_SHADE
                    )
                }
                -6 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_TWILIGHT
                    )
                }
                -7 -> {
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT
                    )
                }
                else -> {
                    // 색온도 설정
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_OFF
                    )
                    captureRequestBuilder.set(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                    )
                    captureRequestBuilder.set(
                        CaptureRequest.COLOR_CORRECTION_GAINS,
                        getTemperatureVector(whiteBalanceColorTemperatureMbr!!)
                    )
                }
            }
        }

        // (수동 설정)
        // 센서 사이즈 중심점
        val centerX =
            cameraInfoVoMbr.sensorSize.width() / 2
        val centerY =
            cameraInfoVoMbr.sensorSize.height() / 2

        // 센서 사이즈에서 중심을 기반 크롭 박스 설정
        // zoom 은 확대 비율로, 센서 크기에서 박스의 크기가 작을수록 줌 레벨이 올라감
        val deltaX =
            ((0.5f * cameraInfoVoMbr.sensorSize.width()) / zoomFactorMbr).toInt()
        val deltaY =
            ((0.5f * cameraInfoVoMbr.sensorSize.height()) / zoomFactorMbr).toInt()

        val mCropRegion = Rect().apply {
            set(
                centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY
            )
        }

        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mCropRegion)

        // (카메라 떨림 보정 여부 반영)
        when (cameraStabilizationSetMbr) {
            0 -> { // 떨림 보정 off
                captureRequestBuilder.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
            }
            1 -> { // 기계적 떨림 보정
                captureRequestBuilder.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
            }
            2 -> { // 소프트웨어 떨림 보정
                captureRequestBuilder.set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
                captureRequestBuilder.set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
            }
        }

        return captureRequestBuilder.build()
    }

    // (WhiteBalance ColorTemperature 세팅 벡터 반환 함수)
    // factor 는 0 ~ 100 범위에서 선택
    // 0은 파란계열 차가운 느낌, 100은 노랑계열 따뜻한 느낌
    private fun getTemperatureVector(factor: Int): RggbChannelVector {
        return RggbChannelVector(
            0.635f + 0.0208333f * factor,
            1.0f,
            1.0f,
            3.7420394f + -0.0287829f * factor
        )
    }

    // todo : capture jpeg orientation front facing 에 참고
//    private fun getJpegOrientation(
//        c: CameraCharacteristics,
//        deviceOrientation: Int
//    ): Int {
//        var deviceOrientation = deviceOrientation
//        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
//        val sensorOrientation =
//            c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
//
//        // Round device orientation to a multiple of 90
//        deviceOrientation = (deviceOrientation + 45) / 90 * 90
//
//        // Reverse device orientation for front-facing cameras
//        val facingFront =
//            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
//        if (facingFront) deviceOrientation = -deviceOrientation
//
//        // Calculate desired JPEG orientation relative to camera orientation to make
//        // the image upright relative to the device orientation
//        return (sensorOrientation + deviceOrientation + 360) % 360
//    }

    // (size 리스트들에서 공통 비율 사이즈가 존재하는지 여부)
    // 가변 리스트 인자를 넣어주고, 동일 width / height 비율의 사이즈가 존재하는지를 판별,
    // 하나라도 동일 비율 사이즈가 없는 리스트가 존재하면 false, 모두 어떠한 동일 비율 사이즈를 공유하면 true
    private fun sizeListSameWhRatioExists(vararg sizeLists: ArrayList<SizeSpecInfoVo>): Boolean {
        val whRatioSet = HashSet<Double>()

        for (sizeListIdx in 0..sizeLists.lastIndex) {
            val sizeList = sizeLists[sizeListIdx]

            if (sizeList.isEmpty()) {
                // 리스트 하나라도 비어있다면 동일성 존재 여부가 깨진 것으로 간주
                return false
            }

            if (sizeListIdx == 0) {
                // 첫번째 리스트이므로 비교할 것도 없이 비교용 비율을 넣어두기
                for (size in sizeList) {
                    whRatioSet.add(size.size.width.toDouble() / size.size.height.toDouble())
                }
            } else {
                // 사이즈를 순회하며 기존 비율 셋에 해당 사이즈 비율이 존재하는지 확인
                // containNum 이 1 이상이라면 통과, 0이라면 그 시점으로 false 를 return
                var containNum = 0
                val subWhRatioSet = HashSet<Double>()

                for (size in sizeList) {
                    val whRatio = size.size.width.toDouble() / size.size.height.toDouble()
                    subWhRatioSet.add(whRatio)

                    if (whRatioSet.contains(whRatio)) {
                        containNum += 1
                    }
                }

                if (containNum == 0) {
                    // 모든 리스트에 대한 비율 공유의 조건이 깨졌으므로 false
                    return false
                }

                whRatioSet.addAll(subWhRatioSet)
            }
        }

        return true
    }


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    // (각 사이즈 리스트 묶음)
    data class SurfacesSizeList(
        val previewSizeList: ArrayList<Size>,
        val captureImageReaderSizeList: ArrayList<Size>,
        val mediaRecorderSizeList: ArrayList<Size>,
        val analysisImageReaderSizeList: ArrayList<Size>,
        val highSpeedSizeList: ArrayList<Size>
    )

    // (서페이스 설정 객체)
    data class PreviewConfigVo(
        val cameraOrientSurfaceSize: Size,
        val autoFitTextureView: AutoFitTextureView
    )

    data class ImageReaderConfigVo(
        val cameraOrientSurfaceSize: Size,
        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    // mp4, H264, AAC 고정
    // fps, bitrate 수치값이 가용 최대값을 넘어가면 가용 최대값으로 변경
    data class MediaRecorderConfigVo(
        val cameraOrientSurfaceSize: Size,
        val mediaRecordingMp4File: File,
        val useH265Codec: Boolean,
        var videoRecordingFps: Int,
        var videoRecordingBitrate: Int,
        var audioRecordingBitrate: Int?
    )

    // (카메라 정보 객체)
    data class SizeSpecInfoVo(
        val size: Size,
        val fps: Int
    )

    // 카메라 정보
    // capture image reader format : JPEG 을 사용
    // analysis image reader format : YUV 420 888 을 사용
    data class CameraInfoVo(
        // 카메라 아이디
        val cameraId: String,
        // facing
        // CameraCharacteristics.LENS_FACING_FRONT: 전면 카메라. value : 0
        // CameraCharacteristics.LENS_FACING_BACK: 후면 카메라. value : 1
        // CameraCharacteristics.LENS_FACING_EXTERNAL: 기타 카메라. value : 2,
        val facing: Int,
        // 카메라 방향이 시계방향으로 얼마나 돌려야 디바이스 방향과 일치하는지에 대한 각도
        val sensorOrientation: Int,
        val previewSizeInfoList: ArrayList<SizeSpecInfoVo>,
        val captureImageReaderSizeInfoList: ArrayList<SizeSpecInfoVo>,
        val mediaRecorderSizeInfoList: ArrayList<SizeSpecInfoVo>,
        val analysisImageReaderSizeInfoList: ArrayList<SizeSpecInfoVo>,
        val highSpeedSizeInfoList: ArrayList<SizeSpecInfoVo>,
        val flashSupported: Boolean,
        // CONTROL_AF_MODE_CONTINUOUS_PICTURE auto focus 지원 여부
        val fastAutoFocusSupported: Boolean,
        // CONTROL_AF_MODE_CONTINUOUS_VIDEO auto focus 지원 여부
        val naturalAutoFocusSupported: Boolean,
        // AutoFocusArea 설정 가능 여부
        val autoFocusMeteringAreaSupported: Boolean,
        // Auto Exposure 기능을 지원해주는지
        val autoExposureSupported: Boolean,
        // AutoExposureArea 설정 가능 여부
        val autoExposureMeteringAreaSupported: Boolean,
        // Auto WhiteBalance 기능을 지원해주는지
        val autoWhiteBalanceSupported: Boolean,
        // AutoWhiteBalanceArea 설정 가능 여부
        val autoWhiteBalanceMeteringAreaSupported: Boolean,
        // LENS_FOCUS_DISTANCE 최소 초점 거리
        // 0f 는 가장 먼 초점 거리, 가장 가깝게 초점을 맞출 수 있는 nf 값
        // 이것이 0f 라는 것은 초점이 고정되어 있다는 뜻
        var supportedMinimumFocusDistance: Float,
        // 떨림 보정 기능 가능 여부 (기계적) : stabilization 설정을 할 때 우선 적용
        var isOpticalStabilizationAvailable: Boolean,
        // 떨림 보정 기능 가능 여부 (소프트웨어적) : stabilization 설정을 할 때 차선 적용
        var isVideoStabilizationAvailable: Boolean,
        // 카메라 센서 사이즈
        val sensorSize: Rect,
        // 카메라 최대 줌 배수
        // maxZoom 이 1.0 이라는 것은 줌이 불가능하다는 의미
        var maxZoom: Float
    )

    // (카메라 상태 정보 객체)
    // 현재 리퀘스트 반복의 타겟
    data class RepeatRequestTargetVo(
        val forPreview: Boolean,
        val forAnalysisImageReader: Boolean,
        val forMediaRecorder: Boolean
    )
}