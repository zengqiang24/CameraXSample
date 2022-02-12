package com.example.cameraxsample.common

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExecutorsHelper {
    companion object {
         val SINGLE_THREAD_POOL: ExecutorService = Executors.newSingleThreadExecutor()
        val  IO_THREAD_POOL: ExecutorService = Executors.newCachedThreadPool()

    }
}