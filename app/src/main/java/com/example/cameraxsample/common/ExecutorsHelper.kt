package com.example.cameraxsample.common

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory

class ExecutorsHelper {
    companion object {
        val SINGLE_THREAD_POOL: ExecutorService = Executors.newSingleThreadExecutor()
        val IO_THREAD_POOL: ExecutorService = Executors.newFixedThreadPool(5)
        val SCHEDULED_THREAD_POOL: ScheduledExecutorService = Executors.newScheduledThreadPool(
            5
        ) { Thread("Image-process-thread-${System.currentTimeMillis()}") }

    }
}