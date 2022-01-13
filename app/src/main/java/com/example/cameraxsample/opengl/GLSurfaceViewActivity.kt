package com.example.cameraxsample.opengl

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GLSurfaceViewActivity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glSurfaceView = GLView(this)
        setContentView(glSurfaceView)
        glSurfaceView.start()
    }
}