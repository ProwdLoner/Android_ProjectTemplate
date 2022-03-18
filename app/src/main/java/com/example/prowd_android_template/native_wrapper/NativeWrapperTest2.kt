package com.example.prowd_android_template.native_wrapper

object NativeWrapperTest2 {
    init {
        System.loadLibrary("native_wrapper_test2")
    }

    // [래핑 함수]
    external fun stringFromJNI(): String
}