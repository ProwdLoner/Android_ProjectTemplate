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
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.example.prowd_android_template.custom_view.AutoFitTextureView
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.math.abs


// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용
// Output Surface 에서 프리뷰는 복수 설정이 가능, 이미지 리더와 미디어 리코더는 1개만 설정 가능

// todo : 캡쳐, 설정 변경 함수, 세션 일시정지, 재개, 프리뷰 비율, 미디어 레코더 실행 프로세스, 설정 사이즈 검증, 녹음 검증
class CameraObj private constructor(
    private val parentActivityMbr: Activity,
    val cameraIdMbr: String,
    private val cameraHandlerMbr: Handler,
    private val cameraManagerMbr: CameraManager,
    private val cameraCharacteristicsMbr: CameraCharacteristics,
    private val streamConfigurationMapMbr: StreamConfigurationMap,
    var sensorOrientationMbr: Int = 0
) {
    // (프리뷰 생성 대기시간 : 밀리초)
    // 뷰 생성시 불안정(출력 비율 일그러짐 에러)을 해소하기 위한 인위적인 대기시간
    // 기기 및 상태별로 필요 시간이 다르기에 목표 최소 디바이스를 기준으로 에러가 없는 최소 대기 시간으로 조정 필요
    private var previewStabilizationTimeMsMbr : Long = 500

    // [카메라 기본 생성 객체] : 카메라 객체 생성시 생성
    // (스레드 풀)
    var executorServiceMbr: ExecutorService? = Executors.newCachedThreadPool()

    // (카메라 부산 데이터)
    private val cameraSessionSemaphoreMbr = Semaphore(1)
    private var cameraDeviceMbr: CameraDevice? = null

    // 이미지 리더 세팅 부산물
    private var imageReaderMbr: ImageReader? = null

    // 미디어 리코더 세팅 부산물
    private var mediaRecorderMbr: MediaRecorder? = null
    var isRecordingMbr: Boolean = false
        private set

    // 프리뷰 세팅 부산물
    private val previewSurfaceListMbr: ArrayList<Surface> = ArrayList()

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
    // <생성자 공간>
    init {
        // CameraDevice 미리 열어두기
        openCameraDeviceAsync(onCameraDeviceReady = {}, onCameraDisconnected = {}, onError = {})
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>

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
    // 10 : 해당 사이즈 이미지 리더를 지원하지 않음
    // 11 : 해당 사이즈 미디어 리코더를 지원하지 않음
    // 12 : 해당 사이즈 프리뷰를 지원하지 않음
    fun startCameraSessionAsync(
        previewConfigVoList: ArrayList<PreviewConfigVo>?,
        imageReaderConfigVo: ImageReaderConfigVo?,
        mediaRecorderConfigVo: MediaRecorderConfigVo?,
        onCameraSessionStarted: () -> Unit,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            cameraSessionSemaphoreMbr.acquire()

            // (카메라 상태 초기화)
            isRecordingMbr = false
            mediaRecorderMbr?.stop()
            mediaRecorderMbr?.reset()
            mediaRecorderMbr?.release()
            mediaRecorderMbr = null

            imageReaderMbr?.setOnImageAvailableListener(null, null)
            imageReaderMbr?.close()
            imageReaderMbr = null

            previewSurfaceListMbr.clear()

            cameraCaptureSessionMbr?.stopRepeating()
            cameraCaptureSessionMbr?.close()
            cameraCaptureSessionMbr = null

            captureRequestBuilderMbr = null

            // (파라미터 검사)
            var surfaceConfigNullCount = 0

            // 서페이스 사이즈 검사
            if (imageReaderConfigVo != null) {
                val cameraSizes =
                    streamConfigurationMapMbr.getOutputSizes(ImageFormat.YUV_420_888)

                // 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes == null ||
                    cameraSizes.isEmpty() ||
                    cameraSizes.indexOfFirst {
                        it.width == imageReaderConfigVo.cameraOrientSurfaceSize.width &&
                                it.height == imageReaderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraSessionSemaphoreMbr.release()
                    parentActivityMbr.runOnUiThread {
                        onError(10)
                    }
                    return@execute
                }
            } else {
                surfaceConfigNullCount++
            }

            if (mediaRecorderConfigVo != null) {
                val cameraSizes =
                    streamConfigurationMapMbr.getOutputSizes(MediaRecorder::class.java)

                // 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes == null ||
                    cameraSizes.isEmpty() ||
                    cameraSizes.indexOfFirst {
                        it.width == mediaRecorderConfigVo.cameraOrientSurfaceSize.width &&
                                it.height == mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                    } == -1
                ) {
                    cameraSessionSemaphoreMbr.release()
                    parentActivityMbr.runOnUiThread {
                        onError(11)
                    }
                    return@execute
                }
            } else {
                surfaceConfigNullCount++
            }

            if (previewConfigVoList != null && previewConfigVoList.isNotEmpty()) {
                val cameraSizes =
                    streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

                // 지원 사이즈가 없는데 요청한 경우나, 혹은 지원 사이즈 내에 요청한 사이즈가 없는 경우 에러
                if (cameraSizes == null ||
                    cameraSizes.isEmpty()
                ) {
                    cameraSessionSemaphoreMbr.release()
                    parentActivityMbr.runOnUiThread {
                        onError(12)
                    }
                    return@execute
                } else {
                    for (previewConfig in previewConfigVoList) {
                        if (cameraSizes.indexOfFirst {
                                it.width == previewConfig.cameraOrientSurfaceSize.width &&
                                        it.height == previewConfig.cameraOrientSurfaceSize.height
                            } == -1) {
                            cameraSessionSemaphoreMbr.release()
                            parentActivityMbr.runOnUiThread {
                                onError(12)
                            }
                            return@execute
                        }
                    }
                }
            } else {
                surfaceConfigNullCount++
            }

            // 서페이스 설정 개수 검사
            if (surfaceConfigNullCount == 3) {
                cameraSessionSemaphoreMbr.release()
                parentActivityMbr.runOnUiThread {
                    onError(0)
                }
                return@execute
            }

            // (카메라 디바이스 열기)
            openCameraDeviceAsync(
                onCameraDeviceReady = {
                    // (서페이스 설정)
                    // 이미지 리더 서페이스
                    if (imageReaderConfigVo != null) {
                        val imageReader = ImageReader.newInstance(
                            imageReaderConfigVo.cameraOrientSurfaceSize.width,
                            imageReaderConfigVo.cameraOrientSurfaceSize.height,
                            ImageFormat.YUV_420_888,
                            2
                        ).apply {
                            setOnImageAvailableListener(
                                imageReaderConfigVo.imageReaderCallback,
                                imageReaderConfigVo.imageReaderHandler
                            )
                        }

                        imageReaderMbr = imageReader
                    }

                    // (미디어 리코더 서페이스 설정)
                    if (mediaRecorderConfigVo != null) {
                        mediaRecorderMbr =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(parentActivityMbr)
                            } else {
                                MediaRecorder()
                            }

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

                        // 오디오 여부
                        val isRecordAudio = mediaRecorderConfigVo.isAudioRecording &&
                                ActivityCompat.checkSelfPermission(
                                    parentActivityMbr,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                        // (레코더 설정)
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

                        if (isRecordAudio) {
                            mediaRecorderMbr!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                        }
                        mediaRecorderMbr!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                        mediaRecorderMbr!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

                        val videoFile = File(mediaRecorderConfigVo.mediaFileAbsolutePath)
                        if (videoFile.exists()) {
                            videoFile.delete()
                        }
                        videoFile.createNewFile()

                        mediaRecorderMbr!!.setOutputFile(videoFile.absolutePath)

                        val cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
                        mediaRecorderMbr!!.setVideoEncodingBitRate(
                            cpHigh.videoBitRate
                        )
                        mediaRecorderMbr!!.setVideoFrameRate(cpHigh.videoFrameRate)

                        mediaRecorderMbr!!.setVideoSize(
                            mediaRecorderConfigVo.cameraOrientSurfaceSize.width,
                            mediaRecorderConfigVo.cameraOrientSurfaceSize.height
                        )
                        mediaRecorderMbr!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                        if (isRecordAudio) {
                            mediaRecorderMbr!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        }
                        mediaRecorderMbr!!.prepare()
                    }

                    // 프리뷰 서페이스
                    setPreviewSurfaces(previewConfigVoList,
                        onPreviewSurfaceAllReady = {
                            if (previewSurfaceListMbr.isEmpty() &&
                                imageReaderMbr == null &&
                                mediaRecorderMbr == null
                            ) { // 생성 서페이스가 하나도 존재하지 않으면,
                                cameraSessionSemaphoreMbr.release()
                                parentActivityMbr.runOnUiThread {
                                    onError(8)
                                }
                                return@setPreviewSurfaces
                            }

                            // (카메라 세션 생성)
                            createCameraSessionAsync(
                                onCaptureSessionCreated = {
                                    if (mediaRecorderMbr == null) {
                                        startPreviewSessionAsync(onSessionStarted = {
                                            cameraSessionSemaphoreMbr.release()
                                            parentActivityMbr.runOnUiThread {
                                                onCameraSessionStarted()
                                            }
                                        })
                                    } else {
                                        startMediaRecorderSessionAsync(onSessionStarted = {
                                            cameraSessionSemaphoreMbr.release()
                                            parentActivityMbr.runOnUiThread {
                                                onCameraSessionStarted()
                                            }
                                        })
                                    }
                                },
                                onError = { errorCode, cameraCaptureSession ->
                                    // (카메라 상태 초기화)
                                    isRecordingMbr = false
                                    mediaRecorderMbr?.stop()
                                    mediaRecorderMbr?.reset()
                                    mediaRecorderMbr?.release()
                                    mediaRecorderMbr = null

                                    imageReaderMbr?.setOnImageAvailableListener(null, null)
                                    imageReaderMbr?.close()
                                    imageReaderMbr = null

                                    previewSurfaceListMbr.clear()

                                    cameraCaptureSession.stopRepeating()
                                    cameraCaptureSession.close()
                                    cameraCaptureSessionMbr = null

                                    captureRequestBuilderMbr = null

                                    cameraSessionSemaphoreMbr.release()
                                    parentActivityMbr.runOnUiThread {
                                        onError(errorCode)
                                    }
                                }
                            )
                        })
                },
                onCameraDisconnected = {
                    // (카메라 상태 초기화)
                    cameraSessionSemaphoreMbr.release()
                    parentActivityMbr.runOnUiThread {
                        onCameraDisconnected()
                    }
                },
                onError = { errorCode ->
                    cameraSessionSemaphoreMbr.release()
                    parentActivityMbr.runOnUiThread {
                        onError(errorCode)
                    }
                }
            )
        }
    }

    // 카메라 세션을 멈추는 함수 (카메라 디바이스를 제외한 나머지 초기화)
    fun stopCameraSession() {
        cameraSessionSemaphoreMbr.acquire()

        isRecordingMbr = false
        mediaRecorderMbr?.stop()
        mediaRecorderMbr?.reset()
        mediaRecorderMbr?.release()
        mediaRecorderMbr = null

        imageReaderMbr?.setOnImageAvailableListener(null, null)
        imageReaderMbr?.close()
        imageReaderMbr = null

        cameraCaptureSessionMbr?.stopRepeating()
        cameraCaptureSessionMbr?.close()
        cameraCaptureSessionMbr = null

        captureRequestBuilderMbr = null

        previewSurfaceListMbr.clear()

        cameraSessionSemaphoreMbr.release()
    }

    // 카메라 객체를 초기화하는 함수 (카메라 디바이스 까지 닫기)
    fun clearCameraObject() {
        cameraSessionSemaphoreMbr.acquire()

        isRecordingMbr = false
        mediaRecorderMbr?.stop()
        mediaRecorderMbr?.reset()
        mediaRecorderMbr?.release()
        mediaRecorderMbr = null

        imageReaderMbr?.setOnImageAvailableListener(null, null)
        imageReaderMbr?.close()
        imageReaderMbr = null

        cameraCaptureSessionMbr?.stopRepeating()
        cameraCaptureSessionMbr?.close()
        cameraCaptureSessionMbr = null

        captureRequestBuilderMbr = null

        previewSurfaceListMbr.clear()

        cameraDeviceMbr?.close()
        cameraDeviceMbr = null

        cameraSessionSemaphoreMbr.release()
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    private fun setPreviewSurfaces(
        previewConfigList: ArrayList<PreviewConfigVo>?,
        onPreviewSurfaceAllReady: () -> Unit
    ) {
        if (previewConfigList == null || previewConfigList.isEmpty()) {
            onPreviewSurfaceAllReady()
            return
        }

        val previewListSize = previewConfigList.size
        var checkedPreviewCount = 0
        val checkedPreviewCountSemaphore = Semaphore(1)

        for (previewIdx in 0 until previewListSize) {
            val previewObj = previewConfigList[previewIdx].autoFitTextureView
            val previewSurfaceSize = previewConfigList[previewIdx].cameraOrientSurfaceSize

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
                                previewSurfaceSize.width,
                                previewSurfaceSize.height,
                                previewObj
                            )
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                    }

                // (텍스쳐 뷰 비율 변경)
                if (parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    && (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) ||
                    parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    && (sensorOrientationMbr == 90 || sensorOrientationMbr == 270)
                ) {
                    parentActivityMbr.runOnUiThread {
                        previewObj.setAspectRatio(
                            previewSurfaceSize.height,
                            previewSurfaceSize.width
                        )
                    }
                } else {
                    parentActivityMbr.runOnUiThread {
                        previewObj.setAspectRatio(
                            previewSurfaceSize.width,
                            previewSurfaceSize.height
                        )
                    }
                }

                val surfaceTexture =
                    previewObj.surfaceTexture

                if (surfaceTexture != null) {
                    // 서페이스 버퍼 설정
                    surfaceTexture.setDefaultBufferSize(
                        previewSurfaceSize.width,
                        previewSurfaceSize.height
                    )

                    previewSurfaceListMbr.add(Surface(surfaceTexture))
                }

                checkedPreviewCountSemaphore.acquire()
                if (++checkedPreviewCount == previewListSize) {
                    // 마지막 작업일 때
                    checkedPreviewCountSemaphore.release()
                    onPreviewSurfaceAllReady()
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
                            executorServiceMbr?.execute {
                                // 뷰 생성시 불안정(출력 비율 일그러짐 에러)을 해소하기 위한 인위적인 대기시간
                                // (기기 및 상태별로 효과가 있을수도 없을 수도 있음. 목표 최소 디바이스를 기준으로 에러가 없는 최소 대기 시간으로 조정 필요)
                                Thread.sleep(previewStabilizationTimeMsMbr)

                                // (텍스쳐 뷰 비율 변경)
                                if (parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                    && (sensorOrientationMbr == 0 || sensorOrientationMbr == 180) ||
                                    parentActivityMbr.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                                    && (sensorOrientationMbr == 90 || sensorOrientationMbr == 270)
                                ) {
                                    parentActivityMbr.runOnUiThread {
                                        previewObj.setAspectRatio(
                                            previewSurfaceSize.height,
                                            previewSurfaceSize.width
                                        )
                                    }
                                } else {
                                    parentActivityMbr.runOnUiThread {
                                        previewObj.setAspectRatio(
                                            previewSurfaceSize.width,
                                            previewSurfaceSize.height
                                        )
                                    }
                                }

                                // 서페이스 버퍼 설정
                                surface.setDefaultBufferSize(
                                    previewSurfaceSize.width,
                                    previewSurfaceSize.height
                                )

                                previewSurfaceListMbr.add(Surface(surface))

                                checkedPreviewCountSemaphore.acquire()
                                if (++checkedPreviewCount == previewListSize) {
                                    // 마지막 작업일 때
                                    checkedPreviewCountSemaphore.release()
                                    onPreviewSurfaceAllReady()
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
                                previewSurfaceSize.width,
                                previewSurfaceSize.height,
                                previewObj
                            )
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                            true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                    }
            }
        }
    }

    // (프리뷰 세션을 실행하는 함수)
    private fun startPreviewSessionAsync(onSessionStarted: () -> Unit) {
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
            null,
            cameraHandlerMbr
        )

        onSessionStarted()
    }

    // (녹화 세션을 실행하는 함수)
    private fun startMediaRecorderSessionAsync(
        onSessionStarted: () -> Unit
    ) {
        // (리퀘스트 빌더 생성)
        captureRequestBuilderMbr =
            cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

        // 서페이스 주입
        imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }
        for (previewSurface in previewSurfaceListMbr) {
            captureRequestBuilderMbr!!.addTarget(previewSurface)
        }
        captureRequestBuilderMbr!!.addTarget(mediaRecorderMbr!!.surface)

        // 리퀘스트 빌더 설정
        captureRequestBuilderMbr!!.set(
            CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO
        )

//            captureRequestBuilderMbr!!.set(
//                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
//                Range(mediaRecorderFpsMbr!!, mediaRecorderFpsMbr!!)
//            )

        // (카메라 실행)
        cameraCaptureSessionMbr!!.setRepeatingRequest(
            captureRequestBuilderMbr!!.build(),
            null,
            cameraHandlerMbr
        )

        isRecordingMbr = true
        mediaRecorderMbr!!.start()

        onSessionStarted()
    }

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

    // todo sensororientation 에 따라 다른 변환
    private fun configureTransform(
        chosenWidth: Int,
        chosenHeight: Int,
        cameraPreview: AutoFitTextureView
    ) {
        val rotation = parentActivityMbr.windowManager.defaultDisplay.rotation
        val matrix = Matrix()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            val viewRect =
                RectF(0f, 0f, cameraPreview.width.toFloat(), cameraPreview.height.toFloat())
            val bufferRect = RectF(0f, 0f, chosenHeight.toFloat(), chosenWidth.toFloat())
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                cameraPreview.height.toFloat() / chosenHeight,
                cameraPreview.width.toFloat() / chosenWidth
            )
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }

        cameraPreview.setTransform(matrix)
    }

    // 카메라 디바이스 생성
    private val openCameraSemaphoreMbr = Semaphore(1)
    private fun openCameraDeviceAsync(
        onCameraDeviceReady: () -> Unit,
        onCameraDisconnected: () -> Unit,
        onError: (Int) -> Unit
    ) {
        executorServiceMbr?.execute {
            openCameraSemaphoreMbr.acquire()
            if (cameraDeviceMbr != null) {
                openCameraSemaphoreMbr.release()
                onCameraDeviceReady()
                return@execute
            }
            // 카메라 디바이스가 존재하지 않는다면 생성

            // (카메라 장치 검사)
            if (!parentActivityMbr.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                openCameraSemaphoreMbr.release()
                onError(1)
                return@execute
            }

            // (카메라 권한 검사)
            if (ActivityCompat.checkSelfPermission(
                    parentActivityMbr,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                openCameraSemaphoreMbr.release()
                onError(2)
                return@execute
            }

            cameraManagerMbr.openCamera(
                cameraIdMbr,
                object : CameraDevice.StateCallback() {
                    // 카메라 디바이스 연결
                    override fun onOpened(camera: CameraDevice) {
                        // cameraDevice 가 열리면,
                        // 객체 저장
                        cameraDeviceMbr = camera
                        openCameraSemaphoreMbr.release()
                        onCameraDeviceReady()
                    }

                    // 카메라 디바이스 연결 끊김 : 물리적 연결 종료, 혹은 권한이 높은 다른 앱에서 해당 카메라를 캐치한 경우
                    override fun onDisconnected(camera: CameraDevice) {
                        isRecordingMbr = false
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        mediaRecorderMbr?.release()
                        mediaRecorderMbr = null

                        imageReaderMbr?.setOnImageAvailableListener(null, null)
                        imageReaderMbr?.close()
                        imageReaderMbr = null

                        previewSurfaceListMbr.clear()

                        cameraCaptureSessionMbr?.stopRepeating()
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        captureRequestBuilderMbr = null

                        camera.close()
                        cameraDeviceMbr = null

                        openCameraSemaphoreMbr.release()
                        onCameraDisconnected()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        // (카메라 상태 초기화)
                        isRecordingMbr = false
                        mediaRecorderMbr?.stop()
                        mediaRecorderMbr?.reset()
                        mediaRecorderMbr?.release()
                        mediaRecorderMbr = null

                        imageReaderMbr?.setOnImageAvailableListener(null, null)
                        imageReaderMbr?.close()
                        imageReaderMbr = null

                        previewSurfaceListMbr.clear()

                        cameraCaptureSessionMbr?.stopRepeating()
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        captureRequestBuilderMbr = null

                        camera.close()
                        cameraDeviceMbr = null

                        openCameraSemaphoreMbr.release()
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
            if (null != mediaRecorderMbr) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        mediaRecorderMbr!!.surface
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
                        onError(9, session)
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
            if (null != mediaRecorderMbr) {
                val surface = mediaRecorderMbr!!.surface
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
                        onError(9, session)
                    }
                }, cameraHandlerMbr
            )
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
    data class PreviewConfigVo(
        val cameraOrientSurfaceSize: Size,
        val autoFitTextureView: AutoFitTextureView
    )

    data class ImageReaderConfigVo(
        val cameraOrientSurfaceSize: Size,
        val imageReaderHandler: Handler,
        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class MediaRecorderConfigVo(
        val cameraOrientSurfaceSize: Size,
        val mediaFileAbsolutePath: String,
        val isAudioRecording: Boolean
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