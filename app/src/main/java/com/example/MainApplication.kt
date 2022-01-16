package com.example

import android.app.Application
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.example.cameraxsample.camera2.VCameraRepository
import com.example.cameraxsample.camera2.VCameraManager
import kotlinx.coroutines.Dispatchers

class MainApplication : Application(), CameraXConfig.Provider {
    val vCameraManager: VCameraManager by lazy {
        initVCamera()
    }

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * 初始化相机SDK
     */
    private fun initVCamera(): VCameraManager {
        val cameraSystemManager =
            this.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        return VCameraManager(
            this.applicationContext,
            cameraSystemManager,
            VCameraRepository(cameraSystemManager, Dispatchers.IO)
        )
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build()
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}