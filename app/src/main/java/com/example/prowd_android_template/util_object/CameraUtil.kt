package com.example.prowd_android_template.util_object

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import com.example.prowd_android_template.util_class.CameraObj

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