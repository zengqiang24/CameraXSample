package com.example.cameraxsample

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.open_app_widget_layout.*

class TestAppWidgetActivity : AppCompatActivity() {
    private var sensorService: SensorService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.open_app_widget_layout)
        btBindSensorService.setOnClickListener {
            val intent = Intent(this, SensorService::class.java)
            bindService(intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Toast.makeText(
                        this@TestAppWidgetActivity,
                        "service connected!",
                        Toast.LENGTH_SHORT
                    ).show()
                    val myBinder = service as SensorService.MyBinder
                    sensorService = myBinder.service
                    Log.d(TAG, "onServiceConnected()")
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.d(TAG, "onServiceDisconnected() called with: name = $name")
                }
            }, BIND_AUTO_CREATE)
        }

        setImage()
        btGetImage.setOnClickListener {
            setImage()
        }
        btShowAppWidget.setOnClickListener { showAppWidget() }
    }

    private fun setImage() {
        sensorService?.getLatestImage()?.let {
            preview.setImageBitmap(it)
        }
    }

    private fun showAppWidget() {
     }

    companion object {
        private const val TAG = "TestAppWidgetActivity"
    }
}