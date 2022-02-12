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
import androidx.core.content.ContextCompat
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.common.ExecutorsHelper
import com.example.cameraxsample.common.getCameraName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

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

    init {
        backgroundThread = BackgroundThread("11capture-thread $cameraId")
        backgroundThread?.let {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }

    fun onResume() {
        if (backgroundThread == null) {
            backgroundThread?.let {
                it.start()
                backgroundHandler = Handler(it.looper)
            }
        }
    }

    fun onPause() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    init {
//        imageReader.setOnImageAvailableListener(
//            {
//                Log.d(TAG, "in imageReader, thread name = ${Thread.currentThread().name}")
////                val frame = it.acquireNextImage()
////                saveFrame(frame)
//            }, backgroundHandler
//        )
    }

    private fun saveFrame(image: Image) {
        val timestamp: Long = image.getTimestamp()

        // long timestampSinceBoot = SystemClock.elapsedRealtimeNanos();
        // Log.d(TAG, "timestampSinceBoot : " + timestampSinceBoot);

        // Collection of bytes of the image

        // long timestampSinceBoot = SystemClock.elapsedRealtimeNanos();
        // Log.d(TAG, "timestampSinceBoot : " + timestampSinceBoot);

        // Collection of bytes of the image
        val rez: ByteArray

        // Convert to NV21 format
        // https://github.com/bytedeco/javacv/issues/298#issuecomment-169100091

        // Convert to NV21 format
        // https://github.com/bytedeco/javacv/issues/298#issuecomment-169100091
        val buffer0: ByteBuffer = image.getPlanes().get(0).getBuffer()
        val buffer2: ByteBuffer = image.getPlanes().get(2).getBuffer()
        val buffer0_size = buffer0.remaining()
        val buffer2_size = buffer2.remaining()
        rez = ByteArray(buffer0_size + buffer2_size)

        // Load the final data var with the actual bytes

        // Load the final data var with the actual bytes
        buffer0[rez, 0, buffer0_size]
        buffer2[rez, buffer0_size, buffer2_size]

        // Byte output stream, so we can save the file

        // Byte output stream, so we can save the file
        val out = ByteArrayOutputStream()

        // Create YUV image file

        // Create YUV image file
        val yuvImage = YuvImage(rez, ImageFormat.NV21, image.getWidth(), image.getHeight(), null)
        yuvImage.compressToJpeg(Rect(0, 0, image.getWidth(), image.getHeight()), 90, out)
        val imageBytes = out.toByteArray()

        // Display for the end user
        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        vCameraRepository.saveFrame(context, getCameraName(cameraId), bmp)
        image.close()
    }

    fun preview(previewConfig: AVMPreviewConfig) {
//        previewConfig.surfaces.add(imageReader.surface)
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
                            previewConfig.executor,
                            object :
                                CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    val request =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    request.addTarget(previewConfig.surfaces[0])
//                                    request.addTarget(previewConfig.surfaces[1])

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
                sesstion.device?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).also {
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

    companion object {
        private const val TAG = "VCameraManager"
    }

    class BackgroundThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
        }
    }
}