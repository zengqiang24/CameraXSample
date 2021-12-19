package com.example.cameraxsample.camera2

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
import com.example.cameraxsample.R
import kotlinx.android.synthetic.main.camera_2_layout.*

class Camera2Activity : AppCompatActivity() {
    private var _session: CameraCaptureSession? = null
    private var _workThread: CaptureHandlerThread? = null
    private var _cameraDevice: CameraDevice? = null
    private var _workHandler: Handler? = null
    private var _cameraManager : CameraManager? = null
    private val _imageReader = ImageReader.newInstance(
        1080, 750, ImageFormat.PRIVATE, 1
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_2_layout)
        val surfaceView = findViewById<SurfaceView>(R.id.viewFinder)
        val imReaderSurface = _imageReader.surface
        _cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        _cameraManager?.let {
            readCamerasFeatures(it)
        }
        //_cameraDevice
        // Remember to call this only *after* SurfaceHolder.Callback.surfaceCreated()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val previewSurface = surfaceView.holder.surface
                val targets = listOf(previewSurface, imReaderSurface)


                // Create a capture session using the predefined targets; this also involves defining the
                // session state callback to be notified of when the session is ready
                _cameraDevice?.createCaptureSession(
                    targets,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            // Do something with `session`
                            _session = session
                            _workThread = CaptureHandlerThread("capture")


                            //preview
                            _session?.let {
                                var req= it.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                req.addTarget(previewSurface)
                                _session?.setRepeatingRequest(req.build(), null, null)
                            }
                        }

                        // Omitting for brevity...
                        override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                    },
                    null
                )  // null can be repl
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
            _session?.let {
               _workHandler = _workThread?.let {
                    Handler(it.looper)
                }
                var req= it.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                req.addTarget( _imageReader.surface)
                //take picture
                it.capture(req.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                    }
                }, _workHandler)
            }
        }

    }
     //cameraIds = ["2", "3", "8", "9", "10", +2 more]
    private fun readCamerasFeatures(cameraManager: CameraManager) {
        cameraManager.let {
            val cameraIdList = it.cameraIdList
            Log.d(TAG, "readCamerasFeatures() called  cameraIdList =  ${cameraIdList.toString()}" )
        }
    }

    private class CaptureHandlerThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
        }
    }
    companion object {
        private const val TAG = "Camera2Activity"
    }
}