package com.example.cameraxsample.camera2

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.example.MainApplication
import com.example.cameraxsample.common.ExecutorsHelper
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashMap

object CameraStateManager {
    private const val TAG = "CameraStateManager"

    private var backgroundThread: BackgroundThread? = null
    private var backgroundHandler: Handler? = null


    val sequenceIdToList = LinkedList<Int>()
    var frameQueue: LinkedHashMap<Int, Frame> = LinkedHashMap()


    /***
     * 帧序列号，标识唯一的一组图片
     */
    private val sequenceId: AtomicInteger = AtomicInteger(-1)

    /***
     * 新增帧序列号
     */
    fun generateSeq(): Int{
        return sequenceId.incrementAndGet()
    }

    /***
     * 获取当前帧序列号
     */
    fun getCurrentSequenceId() : Int{
        return sequenceId.get()
    }

    /**
     * 计算器锁
     */
    val countLocker: AtomicInteger = AtomicInteger(0)

    fun init() {
        backgroundThread = BackgroundThread("background-thread")
        backgroundThread?.let {
            it.start()
            backgroundHandler = Handler(it.looper)
        }
    }


    fun onResume() {
        if (backgroundThread == null || backgroundHandler == null) {
            init()
        }
    }

    fun onPause() {
        Log.d(TAG, "backgroundThread background thread quitSafely() called")
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    fun getBackgroundHandler(): Handler? {
        return backgroundHandler
    }

    fun isUnlocked(): Boolean {
        Log.d(TAG, "countLocker = ${countLocker.get()}")
         return countLocker.get() == 0
    }

    fun lock() {
        countLocker.set(4)
        generateSeq()
    }

    fun lockCountDown() {
        countLocker.decrementAndGet()
    }

    fun startRender(context: Context) {
        val mainApplication = context.applicationContext as MainApplication

        Log.d(TAG, "startRender() called")
        ExecutorsHelper.SCHEDULED_THREAD_POOL.scheduleAtFixedRate({
            mainApplication.getRespository().flushFrame(context)
        }, 100,100, TimeUnit.MILLISECONDS)
    }

    class BackgroundThread(name: String) : HandlerThread(name) {
        override fun run() {
            super.run()
        }
    }
}