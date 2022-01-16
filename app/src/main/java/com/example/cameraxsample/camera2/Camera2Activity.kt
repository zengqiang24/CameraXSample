package com.example.cameraxsample.camera2

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.MainApplication
import com.example.cameraxsample.R
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.AVMPreviewConfig
import kotlinx.android.synthetic.main.camera_2_layout.*

class Camera2Activity : AppCompatActivity() {
    private var cameraManager: CameraManager? = null
    private lateinit var viewModel: VCameraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_2_layout)
        viewModel = VCameraViewModel((application as MainApplication).vCameraManager)
        val surfaceView = findViewById<SurfaceView>(R.id.viewFinder)
        cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        //_cameraDevice
        // Remember to call this only *after* SurfaceHolder.Callback.surfaceCreated()
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @RequiresApi(Build.VERSION_CODES.P)
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                val previewSurface = surfaceView.holder.surface
                val targets = mutableListOf<Surface>(previewSurface)
                with(
                    AVMPreviewConfig(
                        cameraId = viewModel.selectedCameraId,
                        surfaces = targets,
                    )
                ) {
                    viewModel.preview(this)
                }
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
            viewModel.capture()
        }
    }

    class CaptureHandlerThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
            Log.d(TAG, "CaptureHandlerThread running")
        }
    }
    companion object {
        private const val TAG = "Camera2Activity"
    }
}