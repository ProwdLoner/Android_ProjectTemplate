package com.example.prowd_android_template.native_wrapper

object NativeWrapperFFMpegWrapper {
    init {
        System.loadLibrary("native_wrapper_ffmpeg_wrapper")
    }

    // [래핑 함수]
    external fun libTest()
}