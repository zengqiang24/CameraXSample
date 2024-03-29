package com.example.cameraxsample.camera2

import android.content.Context
import com.example.cameraxsample.common.Camera
import kotlinx.coroutines.Dispatchers

private interface CameraFactory {
    fun createCamera(context: Context, id: String, repository: VCameraRepository): CameraHandler
}

class CameraFactoryImpl : CameraFactory {
    override fun createCamera(context: Context, id: String, repository: VCameraRepository): CameraHandler {
        return CameraHandler(
            context,
            id,
            repository
        )
    }
}