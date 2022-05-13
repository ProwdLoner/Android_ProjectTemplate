package com.example.prowd_android_template.util_object.easylut

import com.example.prowd_android_template.util_object.easylut.filter.LutFilterFromBitmap

// lut 적용 라이브러리 (Not GPU)
object EasyLUT {
    fun fromBitmap(): LutFilterFromBitmap.Builder {
        return LutFilterFromBitmap.Builder()
    }
}