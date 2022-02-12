package com.example.cameraxsample.camera2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.util.Log
import android.util.Size
import com.example.cameraxsample.common.Camera
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VCameraRepository(
    private val defaultDispatcher: CoroutineDispatcher) {
    private lateinit var cameraSystemManager : CameraManager
    init {

    }
    fun getCameraIds(): List<String> {
        return Camera.values().map { it.ID }
    }

    fun getCaptureResolutionList(id: String): Array<Size>? {
        val cameraCharacteristics = cameraSystemManager.getCameraCharacteristics(id)
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map?.getOutputSizes(ImageFormat.YUV_420_888)
    }

    suspend fun saveImageData(result: TotalCaptureResult) {
        withContext(defaultDispatcher) {
            Log.d(TAG, "saveImageData() called with: result = $result currentThread = ${Thread.currentThread().name}" )
        }
    }

   fun saveFrame(context: Context, cameraDeviceName:String, bitmap: Bitmap) {
        saveBitmap(context, bitmap, cameraDeviceName)
    }


    companion object {
        private const val TAG = "VCameraRepository"
    }
}