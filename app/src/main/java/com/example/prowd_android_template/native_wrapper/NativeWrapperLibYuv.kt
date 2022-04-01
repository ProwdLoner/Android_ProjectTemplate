package com.example.prowd_android_template.native_wrapper

object NativeWrapperLibYuv {
    init {
        System.loadLibrary("native_wrapper_lib_yuv")
    }

    // [래핑 함수]
    external fun yuv420888ToArgb8888()
}