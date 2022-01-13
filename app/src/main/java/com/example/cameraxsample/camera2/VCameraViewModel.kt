package com.example.cameraxsample.camera2

import android.util.Log
import android.util.Size
import androidx.lifecycle.ViewModel
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.common.OnCaptureResultListener
import java.lang.Exception

/**
 * 职责：
 * 1. 为UI层提供相机配置类的数据
 * 2. 处理来自UI层的事件； 拍照，预览
 */
class VCameraViewModel(private val vCameraManager: VCameraManager) : ViewModel() {


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
        vCameraManager.capture(object : OnCaptureResultListener {
            override fun onSuccess(filePath: String) {
                Log.d(TAG, "onSuccess() called with: filePath = $filePath")

            }

            override fun onFail(e: Exception) {
                Log.d(TAG, "onFail() called with: e = $e")
            }
        })
    }


    /**
     * 预览相机
     */

    fun preview(AVMPreviewParam: AVMPreviewConfig) {
        vCameraManager.preview(AVMPreviewParam)
    }

    companion object {
        private const val TAG = "VCameraViewModel"
    }
}