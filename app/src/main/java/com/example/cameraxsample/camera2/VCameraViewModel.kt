package com.example.cameraxsample.camera2

import android.util.Log
import android.util.Size
import androidx.lifecycle.ViewModel
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.AVMPreviewConfig

/**
 * 职责：
 * 1. 为UI层提供相机配置类的数据
 * 2. 处理来自UI层的事件； 拍照，预览
 */
class VCameraViewModel(private val cameraHandler: CameraHandler) : ViewModel() {


    val defaultOutputSize: Array<Int> = arrayOf(1920, 1080)

    /**
     * 相机摄像头所有逻辑id
     */
    var cameraIds: ArrayList<String>? = null

    /**
     * 选中的摄像头
     */
    var selectedCameraId: String = Camera.FRONT_CAMERA.ID

    /**
     * 拍照支持的分辨率集合
     */
    var captureResolutionList: ArrayList<Size>? = null


    /**
     * 选中的拍照分辨率
     */
    var selectedSize: Size? = null

    /**
     * 拍照
     */
    fun capture() {
        cameraHandler.capture(
            onSuccess = {
                Log.d(TAG, "capture() called with: filePath = $it")
            },
            onFail = {
                Log.e(TAG, "capture failed: ", it)
            })
    }


    /**
     * 预览相机
     */

    fun onResume() {
        cameraHandler.onResume()
    }

    fun onPause() {
        cameraHandler.onPause()
    }
    fun preview(AVMPreviewParam: AVMPreviewConfig) {
        cameraHandler.preview(AVMPreviewParam)
    }

    fun releaseCameras(){
        cameraHandler.release()
    }
    companion object {
        private const val TAG = "VCameraViewModel"
    }
}