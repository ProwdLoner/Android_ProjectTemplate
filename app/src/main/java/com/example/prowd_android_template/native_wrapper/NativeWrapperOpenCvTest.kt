package com.example.prowd_android_template.native_wrapper

import android.graphics.Bitmap

object NativeWrapperOpenCvTest {
    init {
        System.loadLibrary("native_wrapper_opencv_test")
    }

    // [래핑 함수]
    external fun getBitmapRGB(
        bitmap: Bitmap
    ): IntArray

    external fun getGrayBitmap(
        bitmap: Bitmap
    ): Bitmap

    external fun getCopyBitmap(
        bitmap: Bitmap
    ): Bitmap
}