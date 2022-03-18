package com.example.prowd_android_template.native_wrapper

object NativeWrapperTest1 {
    init {
        System.loadLibrary("native_wrapper_test1")
    }

    // [래핑 함수]
    external fun stringFromJNI(): String
}