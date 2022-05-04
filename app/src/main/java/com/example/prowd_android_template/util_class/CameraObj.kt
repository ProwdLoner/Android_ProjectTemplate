package com.example.prowd_android_template.util_class

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission
import com.example.prowd_android_template.custom_view.AutoFitTextureView
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.lang.RuntimeException
import java.util.concurrent.Semaphore
import kotlin.math.abs
import kotlin.math.max

// <Camera 디바이스 하나에 대한 obj>
// 디바이스에 붙어있는 카메라 센서 하나에 대한 조작 객체
// 센서를 조작하는 액티비티 객체와 센서 카메라 아이디 하나를 가져와서 사용
// 외부에서는 카메라 객체를 생성 후 startCamera 를 사용하여 카메라를 실행
// 카메라를 종료할 때에는 stopCamera 를 사용

class CameraObj private constructor(
    private val parentActivityMbr: Activity
) {
    // [카메라 기본 생성 객체] : 카메라 객체 생성시 생성
    // 카메라 아이디
    lateinit var cameraIdMbr: String

    // 카메라 총괄 빌더
    private val cameraManagerMbr: CameraManager =
        parentActivityMbr.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // 카메라 정보 제공 객체
    lateinit var cameraCharacteristicsMbr: CameraCharacteristics

    // 카메라 지원 사이즈 반환 객체
    private lateinit var streamConfigurationMapMbr: StreamConfigurationMap

    // 카메라 기본 방향 정보
    var sensorOrientationMbr: Int = 0


    // ---------------------------------------------------------------------------------------------
    // <스태틱 메소드 공간>
    companion object {
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
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // (카메라 관련 함수)
    // 아래 번호는 진행 순서로, 동일 번호는 동일 시점에 병렬적으로 진행이 가능한 작업

    // 1. 카메라 디바이스 객체 생성
    // 카메라 조작용 객체를 생성하는 것으로, 이후 카메라 조작의 기본이 되는 작업.
    // 여기서 만들어진 객체를 실제 카메라 디바이스를 정보화 했다고 생각하면 됨.

    private var cameraDeviceChangingMbr: Boolean = false
    private val cameraDeviceChangingSemaphoreMbr = Semaphore(1)
    private val openCameraHandlerThreadMbr = HandlerThreadObj("openCamera")
    private var cameraDeviceMbr: CameraDevice? = null

    @RequiresPermission(Manifest.permission.CAMERA)
    fun openCameraAsync(onCameraDeviceReady: () -> Unit, onError: (Throwable) -> Unit) {
        cameraDeviceChangingSemaphoreMbr.acquire()
        if (cameraDeviceChangingMbr || null != cameraDeviceMbr) {
            // 현재 디바이스 생성/소멸 도중이거나 카메라 디바이스가 이미 만들어진 경우는 return
            cameraDeviceChangingSemaphoreMbr.release()
            return
        } else {
            cameraDeviceChangingMbr = true
            cameraDeviceChangingSemaphoreMbr.release()
        }

        if (!openCameraHandlerThreadMbr.isThreadObjAlive()) {
            // 카메라 사용 스레드가 아직 실행되지 않았을 때
            openCameraHandlerThreadMbr.startHandlerThread()
        }

        // cameraDevice open 요청
        cameraManagerMbr.openCamera(cameraIdMbr, object : CameraDevice.StateCallback() {
            // 카메라 디바이스 연결
            override fun onOpened(camera: CameraDevice) {
                // cameraDevice 가 열리면,
                // 객체 저장
                cameraDeviceMbr = camera

                cameraDeviceChangingSemaphoreMbr.acquire()
                cameraDeviceChangingMbr = false
                cameraDeviceChangingSemaphoreMbr.release()
                onCameraDeviceReady()
            }

            // 카메라 디바이스 연결 끊김
            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDeviceMbr = null

                cameraDeviceChangingSemaphoreMbr.acquire()
                cameraDeviceChangingMbr = false
                cameraDeviceChangingSemaphoreMbr.release()

                onError(RuntimeException("Camera No Longer Available"))
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDeviceMbr = null

                cameraDeviceChangingSemaphoreMbr.acquire()
                cameraDeviceChangingMbr = false
                cameraDeviceChangingSemaphoreMbr.release()

                onError(RuntimeException("Error Code : $error"))
            }
        }, openCameraHandlerThreadMbr.handler)
    }

    fun closeCamera() {
        cameraDeviceChangingSemaphoreMbr.acquire()
        if (cameraDeviceChangingMbr || null == cameraDeviceMbr) {
            // 현재 디바이스 생성/소멸 도중이거나 카메라 디바이스가 없는 경우는 return
            cameraDeviceChangingSemaphoreMbr.release()
            return
        } else {
            cameraDeviceChangingMbr = true
            cameraDeviceChangingSemaphoreMbr.release()
        }

        cameraDeviceMbr?.close()
        cameraDeviceMbr = null
        openCameraHandlerThreadMbr.stopHandlerThread()

        cameraDeviceChangingSemaphoreMbr.acquire()
        cameraDeviceChangingMbr = false
        cameraDeviceChangingSemaphoreMbr.release()
    }


    // todo : 프리뷰와 쌍으로 만들기
    // todo : 고속모드라면 서페이스 사이즈 결정에서부터 변화가 필요 사이즈 결정은 min / max 로 결정하도록 하기
    // todo : 완료 콜백으로 완료된 시점에 결정된 이미지 사이즈를 반환
    // 1. 이미지 리더 서페이스 생성
    var imageReaderMbr: ImageReader? = null
    private val imageReaderHandlerThreadMbr = HandlerThreadObj("imageReader")
    fun createImageReader(imageReaderConfigVo: ImageReaderConfigVo) {
        if (!imageReaderHandlerThreadMbr.isThreadObjAlive()) {
            // 카메라 사용 스레드가 아직 실행되지 않았을 때
            imageReaderHandlerThreadMbr.startHandlerThread()
        }

        // 카메라 디바이스에서 지원되는 이미지 사이즈 리스트
        val cameraSizes =
            streamConfigurationMapMbr.getOutputSizes(imageReaderConfigVo.imageFormat)

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
                imageReaderConfigVo.imageFormat,
                imageReaderConfigVo.maxImages
            ).apply {
                setOnImageAvailableListener(
                    imageReaderConfigVo.imageReaderCallback,
                    imageReaderHandlerThreadMbr.handler
                )
            }

            imageReaderMbr = imageReader
        } else {
            imageReaderMbr = null
        }
    }

    fun deleteImageReader() {
        imageReaderMbr?.close()
        imageReaderMbr = null
        imageReaderHandlerThreadMbr.stopHandlerThread()
    }

    // 1. 프리뷰 서페이스 생성
    var previewInfoVoListMbr: ArrayList<PreviewInfoVO>? = null
    fun createPreviewInfoAsync(
        previewList: ArrayList<AutoFitTextureView>,
        onPreviewTextureReady: () -> Unit
    ) {
        val result: ArrayList<PreviewInfoVO> = ArrayList()

        if (previewList.isNotEmpty()) {
            for (previewIdx in 0 until previewList.size) {
                val cameraPreview = previewList[previewIdx]
                var chosenPreviewSize: Size? = null

                if (cameraPreview.isAvailable) {
                    // (카메라 프리뷰 사이즈 계산)
                    // 프리뷰 사이즈는 설정한 텍스쳐 뷰 사이즈와 비율에 가깝게 설정
                    // 지원되는 카메라 사이즈 배열 가져오기
                    val cameraSizes =
                        streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

                    // 카메라 사이즈들 중 원하는 설정에 가까운 것을 선택
                    if (cameraSizes.isNotEmpty()) {
                        // 원하는 크기
                        val preferredPreviewArea: Long =
                            (cameraPreview.width.toLong() * cameraPreview.height.toLong())

                        // prefer size 는 세워진 것을 가정하고, 기본 제공 이미지는 누워진 것을 가정함
                        // (ex : preferredSize = 1080x2400, sizeListUnit = 2400x1080)
                        val preferredPreviewWHRatio: Float =
                            (cameraPreview.width.toFloat() / cameraPreview.height.toFloat())

                        chosenPreviewSize = chooseCameraSize(
                            cameraSizes,
                            preferredPreviewArea,
                            preferredPreviewWHRatio
                        )

                        val surfaceTexture = cameraPreview.surfaceTexture

                        if (null != surfaceTexture) {
                            surfaceTexture.setDefaultBufferSize(
                                chosenPreviewSize.width,
                                chosenPreviewSize.height
                            )

                            val previewInfoVO = PreviewInfoVO(
                                chosenPreviewSize,
                                Surface(surfaceTexture)
                            )

                            result.add(previewInfoVO)

                            // (텍스쳐 뷰 비율 변경)
                            if (Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) {
                                // 현 디바이스가 가로모드일 때
                                parentActivityMbr.runOnUiThread {
                                    cameraPreview.setAspectRatio(
                                        chosenPreviewSize!!.width,
                                        chosenPreviewSize!!.height
                                    )
                                }
                            } else {
                                // 현 디바이스가 세로모드일 때
                                parentActivityMbr.runOnUiThread {
                                    cameraPreview.setAspectRatio(
                                        chosenPreviewSize!!.height,
                                        chosenPreviewSize!!.width
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
                    }

                    if (previewIdx == previewList.size - 1) {
                        // 마지막 작업일 때
                        previewInfoVoListMbr = result
                        onPreviewTextureReady()
                    }
                } else {
                    cameraPreview.surfaceTextureListener =
                        object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                // (카메라 프리뷰 사이즈 계산)
                                // 프리뷰 사이즈는 설정한 텍스쳐 뷰 사이즈와 비율에 가깝게 설정
                                // 지원되는 카메라 사이즈 배열 가져오기
                                val cameraSizes =
                                    streamConfigurationMapMbr.getOutputSizes(SurfaceTexture::class.java)

                                // 카메라 사이즈들 중 원하는 설정에 가까운 것을 선택
                                if (cameraSizes.isNotEmpty()) {
                                    // 원하는 크기
                                    val preferredPreviewArea: Long =
                                        (cameraPreview.width.toLong() * cameraPreview.height.toLong())

                                    // prefer size 는 세워진 것을 가정하고, 기본 제공 이미지는 누워진 것을 가정함
                                    // (ex : preferredSize = 1080x2400, sizeListUnit = 2400x1080)
                                    val preferredPreviewWHRatio: Float =
                                        (cameraPreview.width.toFloat() / cameraPreview.height.toFloat())

                                    chosenPreviewSize = chooseCameraSize(
                                        cameraSizes,
                                        preferredPreviewArea,
                                        preferredPreviewWHRatio
                                    )

                                    val surfaceTexture = cameraPreview.surfaceTexture

                                    if (null != surfaceTexture) {
                                        surfaceTexture.setDefaultBufferSize(
                                            chosenPreviewSize!!.width,
                                            chosenPreviewSize!!.height
                                        )

                                        val previewInfoVO = PreviewInfoVO(
                                            chosenPreviewSize!!,
                                            Surface(surfaceTexture)
                                        )

                                        result.add(previewInfoVO)

                                        // (텍스쳐 뷰 비율 변경)
                                        if (Configuration.ORIENTATION_LANDSCAPE == parentActivityMbr.resources.configuration.orientation) {
                                            // 현 디바이스가 가로모드일 때
                                            parentActivityMbr.runOnUiThread {
                                                cameraPreview.setAspectRatio(
                                                    chosenPreviewSize!!.width,
                                                    chosenPreviewSize!!.height
                                                )
                                            }
                                        } else {
                                            // 현 디바이스가 세로모드일 때
                                            parentActivityMbr.runOnUiThread {
                                                cameraPreview.setAspectRatio(
                                                    chosenPreviewSize!!.height,
                                                    chosenPreviewSize!!.width
                                                )
                                            }
                                        }

                                        configureTransform(
                                            width,
                                            height,
                                            chosenPreviewSize!!.width,
                                            chosenPreviewSize!!.height,
                                            cameraPreview
                                        )
                                    }
                                }

                                if (previewIdx == previewList.size - 1) {
                                    // 마지막 작업일 때
                                    previewInfoVoListMbr = result
                                    onPreviewTextureReady()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(
                                surface: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                if (null != chosenPreviewSize) {
                                    configureTransform(
                                        width,
                                        height,
                                        chosenPreviewSize!!.width,
                                        chosenPreviewSize!!.height,
                                        cameraPreview
                                    )
                                }
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                }
            }
        } else {
            // 설정 인자 리스트가 empty 일 때
            previewInfoVoListMbr = result
            onPreviewTextureReady()
        }
    }

    fun deletePreviewInfoAsync() {
        previewInfoVoListMbr = null
    }


    // 2. 카메라 세션 생성
    // 카메라 디바이스 및 출력 서페이스 필요
    private var cameraSessionChangingMbr: Boolean = false
    private val cameraSessionChangingSemaphoreMbr = Semaphore(1)
    private val createCameraSessionHandlerThreadMbr = HandlerThreadObj("createCameraSession")
    private var cameraCaptureSessionMbr: CameraCaptureSession? = null

    fun createCameraSession(
        onCaptureSessionCreated: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cameraSessionChangingSemaphoreMbr.acquire()
        if (cameraSessionChangingMbr || null != cameraCaptureSessionMbr) {
            // 현재 세션 생성/소멸 도중이거나 세션이 이미 만들어진 경우는 return
            cameraSessionChangingSemaphoreMbr.release()
            return
        } else {
            cameraSessionChangingMbr = true
            cameraSessionChangingSemaphoreMbr.release()
        }

        // 필요 사항 준비 여부 (cameraDevice 준비 및 서페이스 준비)
        if (null == cameraDeviceMbr) {
            cameraSessionChangingSemaphoreMbr.acquire()
            cameraSessionChangingMbr = false
            cameraSessionChangingSemaphoreMbr.release()

            onError(RuntimeException("카메라 조작 준비가 필요합니다."))
            return
        } else if ((null == imageReaderMbr &&
                    (previewInfoVoListMbr == null ||
                            previewInfoVoListMbr!!.isEmpty()))
        ) {
            cameraSessionChangingSemaphoreMbr.acquire()
            cameraSessionChangingMbr = false
            cameraSessionChangingSemaphoreMbr.release()

            onError(RuntimeException("적어도 하나의 출력 설정이 필요합니다."))
            return
        }

        // 세션 실행용 스레드 생성
        if (!createCameraSessionHandlerThreadMbr.isThreadObjAlive()) {
            createCameraSessionHandlerThreadMbr.startHandlerThread()
        }

        // api 28 이상 / 미만의 요청 방식이 다름
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // api 28 이상
            // 출력 설정 객체 리스트
            val outputConfigurationList = ArrayList<OutputConfiguration>()

            // 프리뷰 서페이스 주입
            if (null != previewInfoVoListMbr) {
                for (previewInfoVO in previewInfoVoListMbr!!) {
                    val surface = previewInfoVO.surface
                    outputConfigurationList.add(OutputConfiguration(surface))
                }
            }

            // 이미지 리더 서페이스 주입
            if (null != imageReaderMbr) {
                outputConfigurationList.add(
                    OutputConfiguration(
                        imageReaderMbr!!.surface
                    )
                )
            }

            // todo SESSION_REGULAR 고속모드
            cameraDeviceMbr?.createCaptureSession(SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurationList,
                HandlerExecutor(createCameraSessionHandlerThreadMbr.handler!!.looper),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr = session

                        cameraSessionChangingSemaphoreMbr.acquire()
                        cameraSessionChangingMbr = false
                        cameraSessionChangingSemaphoreMbr.release()

                        onCaptureSessionCreated()
                    }

                    // 세션 생성 실패
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        cameraSessionChangingSemaphoreMbr.acquire()
                        cameraSessionChangingMbr = false
                        cameraSessionChangingSemaphoreMbr.release()

                        onError(RuntimeException("Create Camera Session Failed"))
                    }
                }
            ))
        } else {
            // api 28 미만
            // 출력 서페이스 리스트
            val surfaces = ArrayList<Surface>()

            // 프리뷰 서페이스 주입
            if (null != previewInfoVoListMbr) {
                for (previewInfoVO in previewInfoVoListMbr!!) {
                    val surface = previewInfoVO.surface
                    surfaces.add(surface)
                }
            }

            // 이미지 리더 서페이스 주입
            if (null != imageReaderMbr) {
                val surface = imageReaderMbr!!.surface
                surfaces.add(surface)
            }

            cameraDeviceMbr?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr = session

                        cameraSessionChangingSemaphoreMbr.acquire()
                        cameraSessionChangingMbr = false
                        cameraSessionChangingSemaphoreMbr.release()
                        onCaptureSessionCreated()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        cameraCaptureSessionMbr?.close()
                        cameraCaptureSessionMbr = null

                        cameraSessionChangingSemaphoreMbr.acquire()
                        cameraSessionChangingMbr = false
                        cameraSessionChangingSemaphoreMbr.release()

                        onError(RuntimeException("Create Camera Session Failed"))
                    }
                }, createCameraSessionHandlerThreadMbr.handler
            )
        }
    }

    fun deleteCameraSession() {
        cameraSessionChangingSemaphoreMbr.acquire()
        if (cameraSessionChangingMbr || null == cameraCaptureSessionMbr) {
            // 현재 세션 생성/소멸 도중이거나 세션이 비어있는 경우는 return
            cameraSessionChangingSemaphoreMbr.release()
            return
        } else {
            cameraSessionChangingMbr = true
            cameraSessionChangingSemaphoreMbr.release()
        }

        cameraCaptureSessionMbr?.close()
        cameraCaptureSessionMbr = null
        createCameraSessionHandlerThreadMbr.stopHandlerThread()

        cameraSessionChangingSemaphoreMbr.acquire()
        cameraSessionChangingMbr = false
        cameraSessionChangingSemaphoreMbr.release()
    }

    // 2. 카메라 세션 리퀘스트 빌더 생성
    // 카메라 디바이스 및 출력 서페이스 필요
    // todo : manual change 기능 추가
    var captureRequestBuilderMbr: CaptureRequest.Builder? = null
    fun createCameraCaptureRequest() {
        // 필요 사항 준비 여부 (cameraDevice 준비 및 서페이스 준비)
        if (null == cameraDeviceMbr) {
            cameraSessionChangingSemaphoreMbr.acquire()
            cameraSessionChangingMbr = false
            cameraSessionChangingSemaphoreMbr.release()
            return
        } else if ((null == imageReaderMbr &&
                    (previewInfoVoListMbr == null ||
                            previewInfoVoListMbr!!.isEmpty()))
        ) {
            cameraSessionChangingSemaphoreMbr.acquire()
            cameraSessionChangingMbr = false
            cameraSessionChangingSemaphoreMbr.release()
            return
        }

        // 리퀘스트 빌더 생성
        captureRequestBuilderMbr =
            cameraDeviceMbr!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

        // 서페이스 주입
        imageReaderMbr?.let { captureRequestBuilderMbr!!.addTarget(it.surface) }

        for (previewInfoVo in previewInfoVoListMbr!!) {
            captureRequestBuilderMbr!!.addTarget(previewInfoVo.surface)
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
    private val runCameraCaptureSessionSemaphoreMbr = Semaphore(1)
    private val runCameraCaptureSessionHandlerThreadMbr = HandlerThreadObj("createCameraSession")
    private var isCameraSessionRunMbr = false
    fun runCameraCaptureSession() {
        runCameraCaptureSessionSemaphoreMbr.acquire()

        if (isCameraSessionRunMbr ||
            cameraCaptureSessionMbr == null ||
            captureRequestBuilderMbr == null
        ) {
            runCameraCaptureSessionSemaphoreMbr.release()
            return
        }

        isCameraSessionRunMbr = true

        if (!runCameraCaptureSessionHandlerThreadMbr.isThreadObjAlive()) {
            // 카메라 사용 스레드가 아직 실행되지 않았을 때
            runCameraCaptureSessionHandlerThreadMbr.startHandlerThread()
        }

        cameraCaptureSessionMbr!!.setRepeatingRequest(
            captureRequestBuilderMbr!!.build(),
            object : CameraCaptureSession.CaptureCallback() {},
            runCameraCaptureSessionHandlerThreadMbr.handler
        )

        runCameraCaptureSessionSemaphoreMbr.release()
    }

    fun stopCameraCaptureSession() {
        runCameraCaptureSessionSemaphoreMbr.acquire()
        cameraCaptureSessionMbr?.stopRepeating()
        runCameraCaptureSessionHandlerThreadMbr.stopHandlerThread()
        isCameraSessionRunMbr = false
        runCameraCaptureSessionSemaphoreMbr.release()
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    // todo 개선
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

            val rotation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivityMbr.display!!.rotation
            } else {
                parentActivityMbr.windowManager.defaultDisplay.rotation
            }

            when (rotation) {
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

        val imageFormat: Int,

        // 이미지 리더 최대 사이즈
        val maxImages: Int,

        val imageReaderCallback: ImageReader.OnImageAvailableListener
    )

    data class PreviewConfigVo(
        val autoFitTextureView: AutoFitTextureView,
        val onTextureReady: () -> Unit
    )

    data class PreviewInfoVO(
        // 계산된 카메라 프리뷰 사이즈
        val chosenPreviewSize: Size,
        val surface: Surface
    )

    data class CameraConfigVO(
        // 계산된 카메라 프리뷰 사이즈
        val a: Int
    )
}