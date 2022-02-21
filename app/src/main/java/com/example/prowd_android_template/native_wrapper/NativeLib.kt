package com.example.prowd_android_template.native_wrapper

object NativeLib {
    init {
        System.loadLibrary("native_lib")
    }

    // [래핑 함수]
    external fun stringFromJNI(): String
}