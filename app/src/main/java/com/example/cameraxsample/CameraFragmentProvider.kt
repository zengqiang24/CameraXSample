package com.example.cameraxsample

import com.example.FRONT_CAMERA_ID

object CameraFragmentProvider {
    fun createFragment(cameraId: String): BasePreviewFragment{
       return when(cameraId) {
            FRONT_CAMERA_ID -> { BasePreviewFragment() }
            else ->  BasePreviewFragment()
        }
    }
}