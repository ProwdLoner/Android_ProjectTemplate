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
import android.media.MediaRecorder
import android.os.Build
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
import kotlin.math.abs
import kotlin.math.max


// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용


// todo : lv1 clear, lv2 clear 등 레벨 단위 초기화 기능 추가로 더 편한 스위칭 기능
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


    // ---------------------------------------------------------------------------------------------
    // <스태틱 메소드 공간>
    companion object {
        // (카메라 리스트 반환)
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
    // (카메라 관련 함수)
    // 아래 번호는 진행 순서로, 동일 번호는 동일 시점에 병렬적으로 진행이 가능한 작업

    // 1. 카메라 디바이스 객체 생성
    // 카메라 조작용 객체를 생성하는 것으로, 이후 카메라 조작의 기본이 되는 작업.
    // 여기서 만들어진 객체를 실제 카메라 디바이스를 정보화 했다고 생각하면 됨.
    private var cameraDeviceMbr: CameraDevice? = null

    // todo : LOCK
    fun openCamera(
        onCameraDeviceReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (null != cameraDeviceMbr) {
            onCameraDeviceReady()
            return
        }

        // cameraDevice open 요청
        if (ActivityCompat.checkSelfPermission(
                parentActivityMbr,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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

                    // 카메라 디바이스 연결 끊김
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDeviceMbr = null

                        onError(RuntimeException("Camera No Longer Available"))
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDeviceMbr = null

                        onError(RuntimeException("Error Code : $error"))
                    }
                }, cameraHandlerThreadMbr.handler
            )
        } else {
            onError(RuntimeException("Camera Permission Denied!"))
        }
    }

    fun closeCamera() {
        cameraDeviceMbr?.close()
        cameraDeviceMbr = null
    }


    // 1. 이미지 리더 서페이스 생성
    // 이미지 리더 스트림은 1개만 생성 가능
    // 완료시 이미지 리더 생성 정보 반환, 에러시 null 반환
    private var imageReaderMbr: ImageReader? = null
    private val imageReaderHandlerThreadMbr = HandlerThreadObj("imageReader").apply {
        this.startHandlerThread()
    }

    fun setImageReaderSurface(imageReaderConfigVo: ImageReaderConfigVo): ImageReaderInfoVo? {
        // 카메라 디바이스에서 지원되는 이미지 사이즈 리스트
        val cameraSizes =
            streamConfigurationMapMbr.getOutputSizes(ImageFormat.YUV_420_888)

        if (cameraSizes.isNotEmpty()) {
            // 원하는 사이즈에 유사한 사이즈를 선정
            val chosenImageReaderSize = chooseCameraSize(
                cameraSizes,
                imageReaderConfigVo.preferredImageReaderArea,
                imageReaderConfigVo.preferredImageReaderWHRatio
            )

            val imageReader = ImageReader.newInstance(
                chosenImageReaderSize.width,
                chosenImageReaderSize.height,
                ImageFormat.YUV_420_888,
                1
            ).apply {
                setOnImageAvailableListener(
                    imageReaderConfigVo.imageReaderCallback,
                    imageReaderHandlerThreadMbr.handler
                )
            }

            imageReaderMbr = imageReader
            return ImageReaderInfoVo(chosenImageReaderSize)
        } else {
            imageReaderMbr = null
            return null
        }
    }

    fun unSetImageReaderSurface() {
        imageReaderMbr?.close()
        imageReaderMbr = null
    }

    // 1. 프리뷰 서페이스 생성
    // 프리뷰 서페이스는 복수개 존재 가능
    // 반환값 : 설정된 프리뷰 정보 리스트. 에러시 null
    val previewSurfaceListMbr: ArrayList<Surface> = ArrayList()
    fun setPreviewSurfaceList(
        previewConfigList: ArrayList<PreviewConfigVo>,
        onPreviewSurfaceReady: (ArrayList<PreviewInfoVO>?) -> Unit
    ) {
        previewSurfaceListMbr.clear()

        if (previewConfigList.isEmpty()) {
            onPreviewSurfaceReady(null)
            return
        }

        // 지원되는 카메라 사이즈 배열 가져오기
        val cameraSizes =
            streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

        if (cameraSizes.isEmpty()) {
            onPreviewSurfaceReady(null)
            return
        }

        val result = ArrayList<PreviewInfoVO>()

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

            val preferredPreviewWHRatio: Float =
                if (previewConfigVo.preferredImageReaderWHRatio == -1f) {
                    (parentActivityMbr.resources.displayMetrics.widthPixels.toFloat() /
                            parentActivityMbr.resources.displayMetrics.heightPixels.toFloat())
                } else {
                    previewConfigVo.preferredImageReaderWHRatio
                }

            chosenPreviewSize = chooseCameraSize(
                cameraSizes,
                preferredPreviewArea,
                preferredPreviewWHRatio
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

                    result.add(previewInfoVO)
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
                    if (result.isEmpty()) {
                        onPreviewSurfaceReady(null)
                    } else {
                        onPreviewSurfaceReady(result)
                    }
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

                                result.add(previewInfoVO)
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
                                if (result.isEmpty()) {
                                    onPreviewSurfaceReady(null)
                                } else {
                                    onPreviewSurfaceReady(result)
                                }
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

    fun unSetPreviewSurfaceList() {
        previewSurfaceListMbr.clear()
    }

    // 1. 영상 저장 서페이스 생성
    // 영상 저장 스트림은 한개만 생성 가능
    // todo: 파라미터 확인
    private var videoRecordMediaRecorderMbr: MediaRecorder? = null
    fun setVideoRecordingSurface(videoRecorderConfigVo: VideoRecorderConfigVo): VideoRecorderInfoVO {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(parentActivityMbr)
        } else {
            MediaRecorder()
        }

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(videoRecorderConfigVo.saveFile.absolutePath)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setVideoEncodingBitRate(10000000)

        // 지원되는 카메라 사이즈 배열 가져오기
        val cameraSizes =
            streamConfigurationMapMbr.getOutputSizes(MediaRecorder::class.java)

        // 원하는 사이즈에 유사한 사이즈를 선정
        val chosenVideoSize = chooseCameraSize(
            cameraSizes,
            videoRecorderConfigVo.preferredImageReaderArea,
            videoRecorderConfigVo.preferredImageReaderWHRatio
        )
        mediaRecorder.setVideoSize(chosenVideoSize.width, chosenVideoSize.height)

        if (videoRecorderConfigVo.isRecordAudio) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }

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

        mediaRecorder.prepare()

        videoRecordMediaRecorderMbr = mediaRecorder

        return VideoRecorderInfoVO(mediaRecorder, chosenVideoSize)
    }

    fun unSetVideoRecordingSurface() {
        videoRecordMediaRecorderMbr?.pause()
        videoRecordMediaRecorderMbr?.stop()
        videoRecordMediaRecorderMbr?.reset()
        videoRecordMediaRecorderMbr?.release()
        videoRecordMediaRecorderMbr = null
    }


    // 2. 카메라 세션 생성
    // 카메라 디바이스 및 출력 서페이스 필요
    private var cameraCaptureSessionMbr: CameraCaptureSession? = null

    fun createCameraSession(
        onCaptureSessionCreated: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (null != cameraCaptureSessionMbr) {
            onCaptureSessionCreated()
            return
        }

        // 필요 사항 준비 여부
        if (null == cameraDeviceMbr) {
            onError(RuntimeException("need to create cameraDevice before"))
            return
        } else if ((null == imageReaderMbr &&
                    previewSurfaceListMbr.isEmpty() &&
                    videoRecordMediaRecorderMbr == null)
        ) {
            onError(RuntimeException("need output Setup at least one"))
            return
        }

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

            if (null != videoRecordMediaRecorderMbr) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        videoRecordMediaRecorderMbr!!.surface
                    )
                )
            }

            // todo SESSION_REGULAR 고속모드
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
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        onError(RuntimeException("Create Camera Session Failed"))
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
            if (null != videoRecordMediaRecorderMbr) {
                val surface = videoRecordMediaRecorderMbr!!.surface
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
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        onError(RuntimeException("Create Camera Session Failed"))
                    }
                }, cameraHandlerThreadMbr.handler
            )
        }
    }

    fun deleteCameraSession() {
        cameraCaptureSessionMbr?.close()
        cameraCaptureSessionMbr = null
    }

    // 2. 카메라 세션 리퀘스트 빌더 생성
    // 카메라 디바이스 및 출력 서페이스 필요
    // todo : manual change 기능 추가
    var captureRequestBuilderMbr: CaptureRequest.Builder? = null
    fun createCameraCaptureRequest() {
        // 필요 사항 준비 여부 (cameraDevice 준비 및 서페이스 준비)
        if (null == cameraDeviceMbr) {
            // 빌더 생성용 카메라 객체
            return
        } else if ((null == imageReaderMbr &&
                    previewSurfaceListMbr.isEmpty() &&
                    videoRecordMediaRecorderMbr == null)
        ) {
            // 출력용 서페이스가 하나도 없을 때
            return
        }

        // 리퀘스트 빌더 생성
        // todo 비디오 서페이스 있으면 CameraDevice.TEMPLATE_RECORD
        captureRequestBuilderMbr =
            cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        // 서페이스 주입
        imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }
        videoRecordMediaRecorderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }

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

    fun deleteCameraCaptureRequest() {
        captureRequestBuilderMbr = null
    }

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
    private fun chooseCameraSize(
        offeredCameraSizes: Array<Size>,
        preferredArea: Long,
        preferredWHRatio: Float
    ): Size {
        if (0f >= preferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
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
            var mostSameWhRatio = 0f
            var smallestWhRatioDiff: Float = Float.MAX_VALUE

            for (value in offeredCameraSizes) {
                val whRatio: Float = if (isCameraDeviceAndMobileRotationDifferent) {
                    value.height.toFloat() / value.width.toFloat()
                } else {
                    value.width.toFloat() / value.height.toFloat()
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
                val whRatio: Float = if (isCameraDeviceAndMobileRotationDifferent) {
                    value.height.toFloat() / value.width.toFloat()
                } else {
                    value.width.toFloat() / value.height.toFloat()
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

        // 원하는 이미지 비율
        // width / height
        // 0 이하 값이 있으면 비율은 생각치 않음
        val preferredImageReaderWHRatio: Float,

        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class ImageReaderInfoVo(
        val chosenSize: Size
    )

    data class PreviewConfigVo(
        // 원하는 이미지 비율
        // width / height
        // 0 이라면 비율은 생각치 않음, -1 이라면 디바이스 스크린 크기와 동일
        val preferredImageReaderWHRatio: Float,

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

        // 원하는 이미지 비율
        // width / height
        // 0 이하 값이 있으면 비율은 생각치 않음
        val preferredImageReaderWHRatio: Float,

        val isRecordAudio: Boolean,

        // ex : CamcorderProfile.QUALITY_480P
        val videoResolution: Int,

        val saveFile: File
    )

    data class VideoRecorderInfoVO(
        val mediaRecorder: MediaRecorder,
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