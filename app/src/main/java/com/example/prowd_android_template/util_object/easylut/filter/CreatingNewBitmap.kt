package com.example.prowd_android_template.util_object.easylut.filter

import android.graphics.Bitmap
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage

class CreatingNewBitmap : BitmapStrategy {
    override fun applyLut(src: Bitmap, lutImage: LUTImage): Bitmap {
        val mWidth = src.width
        val mHeight = src.height
        val pix = IntArray(mWidth * mHeight)
        src.getPixels(pix, 0, mWidth, 0, 0, mWidth, mHeight)
        for (y in 0 until mHeight) {
            for (x in 0 until mWidth) {
                val index = y * mWidth + x
                val pixel = pix[index]
                pix[index] = lutImage.getColorPixelOnLut(pixel)
            }
        }
        val bm = Bitmap.createBitmap(mWidth, mHeight, src.config)
        bm.setPixels(pix, 0, mWidth, 0, 0, mWidth, mHeight)
        return bm
    }
}