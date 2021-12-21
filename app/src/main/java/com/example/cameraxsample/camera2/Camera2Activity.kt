package com.example.cameraxsample.camera2

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.FULL_CAMERA_ID
import com.example.cameraxsample.R
import kotlinx.android.synthetic.main.camera_2_layout.*

class Camera2Activity : AppCompatActivity() {
    private var session: CameraCaptureSession? = null
    private var workThread: CaptureHandlerThread? = null
    private var workHandler: Handler? = null
    private var cameraManager: CameraManager? = null
    private val imageReader = ImageReader.newInstance(
        1080, 750, ImageFormat.PRIVATE, 1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_2_layout)
        val surfaceView = findViewById<SurfaceView>(R.id.viewFinder)
        val imReaderSurface = imageReader.surface
        cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        //_cameraDevice
        // Remember to call this only *after* SurfaceHolder.Callback.surfaceCreated()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                val previewSurface = surfaceView.holder.surface
                val targets = listOf(previewSurface, imReaderSurface)

                cameraManager?.openCamera(
                    FULL_CAMERA_ID,
                    ContextCompat.getMainExecutor(this@Camera2Activity),
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            Log.d(TAG, "onOpened() called with: camera = ${camera.id}")
                            camera?.createCaptureSession(
                                targets,
                                object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                                        Log.d(
                                            TAG,
                                            "onConfigured() called with: cameraCaptureSession = $cameraCaptureSession"
                                        )
                                        // Do something with `session`
                                        session = cameraCaptureSession
                                        workThread = CaptureHandlerThread("capture")
                                        workThread?.start()

                                        //preview
                                        session?.let {
                                            var req =
                                                it.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                            req.addTarget(previewSurface)
                                            session?.setRepeatingRequest(req.build(), null, null)
                                        }
                                    }

                                    // Omitting for brevity...
                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        Log.d(
                                            TAG,
                                            "onConfigureFailed() called with: session = $session"
                                        )
                                    }
                                }, null // set thread context for its callback handled
                            )
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            Log.d(TAG, "onDisconnected() called with: camera = $camera")

                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            Log.d(TAG, "onError() called with: camera = $camera, error = $error")
                        }
                    })

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })

        camera_capture_button.setOnClickListener {
            Log.d(TAG, "camera_capture_button() setOnClickListener called")
            session?.let {
                workThread?.let {
                    workHandler = Handler(it.looper)
                }
                var req = it.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                req.addTarget(imageReader.surface)
                //take picture
                it.capture(req.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        Log.d(
                            TAG,
                            "thread = ${Thread.currentThread().name}; take photo successfully!"
                        )
                    }
                }, workHandler)
            }
        }

    }


    private class CaptureHandlerThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
            Log.d(TAG, "CaptureHandlerThread running")
        }
    }

    companion object {
        private const val TAG = "Camera2Activity"
    }
}