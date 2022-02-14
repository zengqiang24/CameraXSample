package com.example.cameraxsample.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.common.ExecutorsHelper
import com.example.cameraxsample.common.getCameraName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.inv


class CameraHandler(
    private val context: Context,
    private val cameraId: String,
    private val vCameraRepository: VCameraRepository
) {
    private val state: AtomicBoolean = AtomicBoolean(false)
    private var captureSession: CameraCaptureSession? = null
    private var previewConfig: AVMPreviewConfig? = null
    private var imageReader: ImageReader =
        ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 1)
    private var backgroundThread: BackgroundThread? = null
    private var backgroundHandler: Handler? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var cameraSystemManager = context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
    private var cameraDevice:CameraDevice? = null
    init {
        backgroundThread = BackgroundThread("capture-thread $cameraId")
        backgroundThread?.let {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    fun onResume() {
        if (backgroundThread == null) {
            Log.d(TAG, "imageReader background thread start() called")
            backgroundThread?.let {
                it.start()
                backgroundHandler = Handler(it.looper)
            }
        }
    }

    fun onPause() {
        Log.d(TAG, "imageReader background thread quitSafely() called")
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    init {
        imageReader.setOnImageAvailableListener(
            {
                Log.d(TAG, "in imageReader, thread name = ${Thread.currentThread().name}")
                saveFrame(it.acquireNextImage())
            }, backgroundHandler
        )
    }

    /**
     * save  frame will block cpu
     */
    private fun saveFrame(image: Image) {
        val savingPhoto = vCameraRepository.isSavingPhoto
        if(savingPhoto) {
            image.close()
            Log.d(TAG, "正在保存图片，跳过此帧...")
            return
        }
        val timestamp: Long = image.timestamp
        val yuv420888tonv21 = YUV_420_888toNV21(image)
        val bitmap = BitmapFactory.decodeByteArray(yuv420888tonv21, 0, yuv420888tonv21!!.size)
        if(bitmap == null) {
            Log.d(TAG, "saveFrame() called with: bitmap = $bitmap")
            image.close()
            return
        }
        vCameraRepository.saveFrame(context, getCameraName(cameraId), bitmap)
        image.close()
    }

    private fun YUV_420_888toNV21(image: Image): ByteArray? {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer: ByteBuffer = image.planes[0].buffer // Y
        val uBuffer: ByteBuffer = image.planes[1].buffer // U
        val vBuffer: ByteBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize)
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong() // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer.get(nv21, pos, width)
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) === vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel: Byte = vBuffer.get(1)
            try {
                vBuffer.put(1, savePixel.inv() as Byte)
                if (uBuffer.get(0) === savePixel.inv() as Byte) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer.get(nv21, ySize, 1)
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining())
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer.get(vuPos)
                nv21[pos++] = uBuffer.get(vuPos)
            }
        }
        return nv21
    }

    fun preview(previewConfig: AVMPreviewConfig) {
        previewConfig.surfaces.add(imageReader.surface)
        this.previewConfig = previewConfig
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cameraSystemManager.openCamera(
                cameraId,
                ExecutorsHelper.IO_THREAD_POOL,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val sessionConfiguration = SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR,
                            previewConfig.surfaces.map {
                                OutputConfiguration(it)
                            },
                            ExecutorsHelper.IO_THREAD_POOL,
                            object :
                                CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    val request =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    request.addTarget(previewConfig.surfaces[0])
                                    request.addTarget(previewConfig.surfaces[1])

                                    val bui = request.build()
                                    session.setRepeatingRequest(bui, null, null)
                                    Log.d(TAG, "onConfigured() called with: session = $session")
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.d(
                                        TAG,
                                        "onConfigureFailed() called with: session = $session"
                                    )
                                }

                            }
                        )
                        camera.createCaptureSession(sessionConfiguration)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.d(TAG, "onDisconnected() called with: camera = $camera")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.d(TAG, "onError() called with: camera = $camera, error = $error")
                    }

                })
        }

    }

    fun capture(onSuccess: (filePath: String) -> Unit, onFail: (e: Exception) -> Unit) {
        captureSession?.let { sesstion ->
            var request: CaptureRequest =
                sesstion.device?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).also {
                    it.addTarget(imageReader.surface)
                }.build()

            sesstion.capture(request, object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    //save to disk
                    Log.d(
                        TAG,
                        "onCaptureCompleted() called with: session = $session, request = $request, result = $result"
                    )
                    state.compareAndSet(false, true)
                    coroutineScope.launch {
                        vCameraRepository.saveImageData(result)
                        onSuccess("file path .... ${Thread.currentThread().name}")
                    }
                }

                override fun onCaptureFailed(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    failure: CaptureFailure
                ) {
                    super.onCaptureFailed(session, request, failure)
                    onFail(java.lang.Exception(failure.toString()))
                }
            }, null)
        }
    }

    fun getCameraIds(): List<String> {
        return vCameraRepository.getCameraIds()
    }

    fun getSolutionSizes(id: String): Array<Size>? {
        return vCameraRepository.getCaptureResolutionList(id)
    }

    fun release() {
        if(captureSession?.device!=null) {
            captureSession?.device?.close()
            captureSession?.device == null
        }

        if(captureSession !=null) {
            captureSession?.close()
            captureSession == null
        }

        if(imageReader !=null) {
            imageReader?.close()
            imageReader == null
        }

        Log.d(TAG, "camera release() called")
    }

    companion object {
        private const val TAG = "VCameraManager"
    }

    class BackgroundThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
        }
    }
}