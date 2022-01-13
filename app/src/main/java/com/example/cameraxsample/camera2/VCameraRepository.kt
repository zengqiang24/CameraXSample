package com.example.cameraxsample.camera2

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import com.example.cameraxsample.common.Camera

class VCameraRepository(private val cameraSystemManager: CameraManager) {
    fun getCameraIds(): List<String> {
        return Camera.values().map { it.ID }
    }

    fun getCaptureResolutionList(id: String): Array<Size>? {
        val cameraCharacteristics = cameraSystemManager.getCameraCharacteristics(id)
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.getOutputSizes(ImageFormat.YUV_420_888)
    }
}