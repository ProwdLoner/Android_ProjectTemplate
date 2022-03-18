package com.example.prowd_android_template.native_wrapper

object NativeWrapperJniStringTest {
    init {
        System.loadLibrary("native_wrapper_jni_string_test")
    }

    // [래핑 함수]
    external fun getJniString(): String
}