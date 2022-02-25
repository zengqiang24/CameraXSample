package com.example.cameraxsample.camera2

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.MainApplication
import com.example.cameraxsample.R
import com.example.cameraxsample.common.Camera
import com.example.cameraxsample.common.ExecutorsHelper.Companion.SCHEDULED_THREAD_POOL
import com.example.cameraxsample.databinding.Camera2LayoutBinding
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Camera2Activity : AppCompatActivity() {
    private lateinit var frontFragment: PreviewFragment
    private lateinit var backFragment: PreviewFragment
    private lateinit var leftFragment: PreviewFragment
    private lateinit var rightFragment: PreviewFragment

    private var viewModel:MainViewModel? = null
    private var handler: Handler? = null
    private lateinit var context: Context
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        val binding: Camera2LayoutBinding = DataBindingUtil.setContentView(this, R.layout.camera_2_layout)
       frontFragment  = PreviewFragment.newInstance(
            Camera.FRONT_CAMERA.ID)
        backFragment  = PreviewFragment.newInstance(
            Camera.BACK_CAMERA.ID)
        leftFragment  = PreviewFragment.newInstance(
                Camera.LEFT_CAMERA.ID)
        rightFragment  = PreviewFragment.newInstance(
                Camera.RIGHT_CAMERA.ID)

        addFragment(R.id.fl_viewfinder1, frontFragment)
        addFragment(R.id.fl_viewfinder2, backFragment)
        addFragment(R.id.fl_viewfinder3, leftFragment)
        addFragment(R.id.fl_viewfinder4, rightFragment)

         viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        handler = Handler {
            if(CameraStateManager.getCurrentSequenceId() == 1000) {
                Log.d(TAG, "CameraStateManager.getCurrentSequenceId() == 1000")
                true
            }

            if(CameraStateManager.isUnlocked()) {
                CameraStateManager.lock()
                frontFragment.capture()
                backFragment.capture()
                leftFragment.capture()
                rightFragment.capture()
            }
            handler?.sendEmptyMessageDelayed(0, 100)
            val mainApplication = context.applicationContext as MainApplication
            mainApplication.getRespository().flushFrame(context)
            true
        }
        binding.cameraClearingButton.setOnClickListener {

        }

        binding.cameraRecordButton.setOnClickListener {
            handler?.sendEmptyMessageDelayed(0, 0)

//            CameraStateManager.startRender(context)
        }


        binding.cameraStopButton.setOnClickListener {
        }
    }

    private fun addFragment(id:Int, fragment: PreviewFragment){
        supportFragmentManager.beginTransaction().replace(id, fragment).commit()
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