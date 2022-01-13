package com.example.cameraxsample.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Log
import android.util.Size
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxsample.common.AVMPreviewConfig
import com.example.cameraxsample.common.OnCaptureResultListener

class VCameraManager(
    private val context: Context,
    private val cameraSystemManager: CameraManager,
    private val vCameraRepository: VCameraRepository
) {
    private var captureSession: CameraCaptureSession? = null
    private var previewConfig: AVMPreviewConfig? = null
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
                            listOf(OutputConfiguration(previewConfig.surface)),
                            previewConfig.executor,
                            object :
                                CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    val request =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    request.addTarget(previewConfig.surface)
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

    fun capture(listener: OnCaptureResultListener) {
        captureSession?.let { sesstion ->
            var request: CaptureRequest =
                sesstion.device?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).also {
                    it.addTarget(previewConfig?.surface!!)
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
}