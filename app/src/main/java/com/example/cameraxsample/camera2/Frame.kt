package com.example.cameraxsample.camera2

import android.graphics.Bitmap

/**
 * 代表一帧图像（由四个摄像头组成）
 */
data class Frame(val sequenceId: Int, val bitmaps: HashMap<String, Bitmap>)