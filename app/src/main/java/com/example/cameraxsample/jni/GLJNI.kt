package com.example.cameraxsample.jni

class GLJNI {
    //约定好与c++代码互动的接口
    companion object {
        init {
            System.loadLibrary("cameraxsample")
        }

        external fun helloJNI(): String
    }
}