package com.example.cameraxsample.camera2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import android.util.Size
import com.example.cameraxsample.camera2.CameraStateManager.frameQueue
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.ExecutorsHelper
import com.example.cameraxsample.common.getCameraName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean

class VCameraRepository(
    private val defaultDispatcher: CoroutineDispatcher
) {
    @Volatile
    var isSavingPhoto: AtomicBoolean = AtomicBoolean(false)
    private lateinit var cameraSystemManager: CameraManager
    private val threadPool = ExecutorsHelper.IO_THREAD_POOL

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
            Log.d(
                TAG,
                "saveImageData() called with: result = $result currentThread = ${Thread.currentThread().name}"
            )
        }
    }

    /**
     *  从缓冲区读取图像数据（bitmap），存入本地文件。
     */
    fun flushFrame(context: Context) {
            var id = 0
            var frame: Frame?
            synchronized(VCameraRepository::class.java) {
                if (CameraStateManager.sequenceIdToList == null || CameraStateManager.sequenceIdToList.isEmpty()) {
                    return
                } else {
                    id = CameraStateManager.sequenceIdToList.peek()
                    if (frameQueue[id]!!.bitmaps.size != 4) {
                        return
                    } else {
                        id = CameraStateManager.sequenceIdToList.poll()
                    }
                }
            }
            synchronized(VCameraRepository::class.java) {
                frame = frameQueue[id]
                if (frame != null) {
                    for ((cameraId, bitmap) in frame!!.bitmaps) {
                        Log.d(TAG, "flushFrame() called cameraId= $cameraId id = $id")
                        threadPool.execute {
                            saveBitmap(
                                context,
                                id, //Frame ID
                                bitmap, //单个摄像头图像
                                getCameraName(cameraId)// 摄像头名称
                            )
                        }
                    }
                }
            }

            Log.d(TAG, "flushFrame() called with:id = $id frame = $frame")
            synchronized(VCameraRepository::class.java) {
                frameQueue.remove(id)
                Log.d(TAG, "savePacket() called remove packet = $id")
            }
    }

    suspend fun deleteAllImages(context: Context) {
        withContext(defaultDispatcher) {
            val dir = getImageDirectory(context)
            if (dir?.exists() == true) {
                dir?.deleteRecursively()
            }
        }
    }

    companion object {
        private const val TAG = "VCameraRepository"
    }
}