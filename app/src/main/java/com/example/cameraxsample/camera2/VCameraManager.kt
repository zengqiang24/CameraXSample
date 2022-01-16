package com.example.cameraxsample.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxsample.common.AVMPreviewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class VCameraManager(
    private val context: Context,
    private val cameraSystemManager: CameraManager,
    private val vCameraRepository: VCameraRepository
) {
    private val state: AtomicBoolean = AtomicBoolean(false)
    private var captureSession: CameraCaptureSession? = null
    private var previewConfig: AVMPreviewConfig? = null
    private var imageReader: ImageReader =
        ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 1)
    private var backgroundThread: BackgroundThread? = null
    private var backgroundHandler: Handler? = null
    init {
        backgroundThread = BackgroundThread("capture-thread")
        backgroundThread?.let {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }
    fun onResume() {
        if(backgroundThread == null) {
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
        imageReader.setOnImageAvailableListener(
            {
                val image: Image = it.acquireNextImage()
                image.planes[0].buffer.run {
                    this
                }
            }, backgroundHandler
        )
    }

    fun preview(previewConfig: AVMPreviewConfig) {
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
                previewConfig.cameraId,
                ContextCompat.getMainExecutor(context),
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
                    CoroutineScope(Dispatchers.Main).launch {
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