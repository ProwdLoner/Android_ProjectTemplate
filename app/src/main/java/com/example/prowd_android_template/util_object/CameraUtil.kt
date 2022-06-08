package com.example.prowd_android_template.util_object

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import android.util.Size
import android.view.Surface
import com.example.prowd_android_template.util_class.CameraObj
import kotlin.math.abs

object CameraUtil {
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

    // (cameraSizes 들 중에 preferredArea 와 가장 유사한 것을 선택하고, 그 중에서도 preferredWHRatio 가 유사한 것을 선택)
    // preferredArea 0 은 최소, Long.MAX_VALUE 는 최대
    // preferredWHRatio 0 이하면 비율을 신경쓰지 않고 넓이만으로 비교
    // 반환 사이즈의 방향은 카메라 방향
    fun<T> getCameraSize(
        parentActivity: Activity,
        cameraId: String,
        preferredArea: Long,
        preferredWHRatio: Double,
        imageFormat: Class<T>
    ): Size? {
        val cameraCharacteristics =
            ((parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager).getCameraCharacteristics(
                cameraId
            ))

        val streamConfigurationMap: StreamConfigurationMap =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return null

        val cameraSizes =
            streamConfigurationMap.getOutputSizes(imageFormat) ?: return null

        val sensorOrientationMbr: Int =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: return null

        if (0 >= preferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
            // 넓이 비슷한 것을 선정
            var smallestAreaDiff: Long = Long.MAX_VALUE
            var resultIndex = 0

            for ((index, value) in cameraSizes.withIndex()) {
                val area = value.width.toLong() * value.height.toLong()
                val areaDiff = abs(area - preferredArea)
                if (areaDiff < smallestAreaDiff) {
                    smallestAreaDiff = areaDiff
                    resultIndex = index
                }
            }

            return cameraSizes[resultIndex]
        } else { // 비율을 먼저 보고, 이후 넓이로 비교
            // 카메라 디바이스와 휴대폰 rotation 이 서로 다른지를 확인
            var isCameraDeviceAndMobileRotationDifferent = false

            val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivity.display!!.rotation
            } else {
                parentActivity.windowManager.defaultDisplay.rotation
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

            for (value in cameraSizes) {
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
            for ((index, value) in cameraSizes.withIndex()) {
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
            return cameraSizes[resultSizeIndex]
        }
    }

    fun getCameraSize(
        parentActivity: Activity,
        cameraId: String,
        preferredArea: Long,
        preferredWHRatio: Double,
        imageFormat: Int
    ): Size? {
        val cameraCharacteristics =
            ((parentActivity.getSystemService(Context.CAMERA_SERVICE) as CameraManager).getCameraCharacteristics(
                cameraId
            ))

        val streamConfigurationMap: StreamConfigurationMap =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return null

        val cameraSizes =
            streamConfigurationMap.getOutputSizes(imageFormat) ?: return null

        val sensorOrientationMbr: Int =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: return null

        if (0 >= preferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
            // 넓이 비슷한 것을 선정
            var smallestAreaDiff: Long = Long.MAX_VALUE
            var resultIndex = 0

            for ((index, value) in cameraSizes.withIndex()) {
                val area = value.width.toLong() * value.height.toLong()
                val areaDiff = abs(area - preferredArea)
                if (areaDiff < smallestAreaDiff) {
                    smallestAreaDiff = areaDiff
                    resultIndex = index
                }
            }

            return cameraSizes[resultIndex]
        } else { // 비율을 먼저 보고, 이후 넓이로 비교
            // 카메라 디바이스와 휴대폰 rotation 이 서로 다른지를 확인
            var isCameraDeviceAndMobileRotationDifferent = false

            val deviceOrientation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                parentActivity.display!!.rotation
            } else {
                parentActivity.windowManager.defaultDisplay.rotation
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

            for (value in cameraSizes) {
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
            for ((index, value) in cameraSizes.withIndex()) {
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
            return cameraSizes[resultSizeIndex]
        }
    }

    // (가용 카메라 리스트 반환)
    fun getCameraInfoList(parentActivity: Activity): ArrayList<CameraObj.CameraInfo> {
        val cameraInfoList: ArrayList<CameraObj.CameraInfo> = ArrayList()

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

            val previewInfoList = ArrayList<CameraObj.CameraInfo.DeviceInfo>()
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
                        CameraObj.CameraInfo.DeviceInfo(
                            size, fps
                        )
                    )
                }
            }

            val imageReaderInfoList = ArrayList<CameraObj.CameraInfo.DeviceInfo>()
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
                        CameraObj.CameraInfo.DeviceInfo(
                            size, fps
                        )
                    )
                }
            }

            val mediaRecorderInfoList = ArrayList<CameraObj.CameraInfo.DeviceInfo>()
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
                        CameraObj.CameraInfo.DeviceInfo(
                            size, fps
                        )
                    )
                }
            }

            val highSpeedInfoList = ArrayList<CameraObj.CameraInfo.DeviceInfo>()
            if (capabilities.contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
                )
            ) {
                cameraConfig.highSpeedVideoSizes.forEach { size ->
                    cameraConfig.getHighSpeedVideoFpsRangesFor(size).forEach { fpsRange ->
                        val fps = fpsRange.upper
                        highSpeedInfoList.add(
                            CameraObj.CameraInfo.DeviceInfo(
                                size, fps
                            )
                        )
                    }
                }
            }

            cameraInfoList.add(
                CameraObj.CameraInfo(
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