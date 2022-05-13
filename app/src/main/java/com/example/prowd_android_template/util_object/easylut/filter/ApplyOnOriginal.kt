package com.example.prowd_android_template.util_object.easylut.filter

import com.example.prowd_android_template.util_object.easylut.filter.BitmapStrategy
import android.graphics.Bitmap
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage

class ApplyOnOriginal : BitmapStrategy {
    override fun applyLut(src: Bitmap, lutImage: LUTImage): Bitmap {
        for (y in 0 until src.height) {
            for (x in 0 until src.width) {
                val pixel = src.getPixel(x, y)
                val colorPixelOnLut = lutImage.getColorPixelOnLut(pixel)
                src.setPixel(x, y, colorPixelOnLut)
            }
        }
        return src
    }
}