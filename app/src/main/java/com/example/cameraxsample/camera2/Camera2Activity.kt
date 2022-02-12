package com.example.cameraxsample.camera2

import android.os.Bundle
import android.os.HandlerThread
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.cameraxsample.R
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.databinding.Camera2LayoutBinding

class Camera2Activity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: Camera2LayoutBinding = DataBindingUtil.setContentView(this, R.layout.camera_2_layout)
        supportFragmentManager.beginTransaction().replace(R.id.fl_viewfinder1, PreviewFragment.newInstance(
            Camera.FRONT_CAMERA.ID)).commit()
        supportFragmentManager.beginTransaction().replace(R.id.fl_viewfinder2, PreviewFragment.newInstance(
            Camera.BACK_CAMERA.ID)).commit()
        supportFragmentManager.beginTransaction().replace(R.id.fl_viewfinder3, PreviewFragment.newInstance(
            Camera.LEFT_CAMERA.ID)).commit()
        supportFragmentManager.beginTransaction().replace(R.id.fl_viewfinder4, PreviewFragment.newInstance(
            Camera.RIGHT_CAMERA.ID)).commit()
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