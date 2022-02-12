package com.example

import android.app.Application
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import com.example.cameraxsample.camera2.VCameraRepository
import com.example.cameraxsample.camera2.CameraHandler
import com.example.cameraxsample.common.Camera
import kotlinx.coroutines.Dispatchers

class MainApplication : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build()
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}