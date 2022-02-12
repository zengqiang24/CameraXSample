package com.example.cameraxsample.common

import android.view.Surface
import java.lang.Exception
import java.util.concurrent.Executor

data class AVMPreviewConfig(
    val surfaces: MutableList<Surface>,
    val executor: Executor = ExecutorsHelper.SINGLE_THREAD_POOL,
    var outputSize: Array<Int> ?= null
)

interface OnCaptureResultListener {
    fun onSuccess(filePath: String)
    fun onFail(e: Exception)
}