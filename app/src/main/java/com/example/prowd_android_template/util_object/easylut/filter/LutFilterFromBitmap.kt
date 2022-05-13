package com.example.prowd_android_template.util_object.easylut.filter

import android.graphics.Bitmap
import com.example.prowd_android_template.util_object.easylut.lutimage.CoordinateToColor
import com.example.prowd_android_template.util_object.easylut.lutimage.LutAlignment

class LutFilterFromBitmap private constructor(
    override val lUTBitmap: Bitmap, strategy: BitmapStrategy,
    coordinateToColorType: CoordinateToColor.Type,
    lutAlignmentMode: LutAlignment.Mode
) : LUTFilter(strategy, coordinateToColorType, lutAlignmentMode) {

    class Builder : LUTFilter.Builder<Builder?>() {
        private lateinit var bitmap: Bitmap
        fun withBitmap(bitmap: Bitmap): Builder {
            this.bitmap = bitmap
            return this
        }

        fun createFilter(): Filter {
            return LutFilterFromBitmap(
                bitmap, strategy,
                coordinateToColorType, lutAlignmentMode
            )
        }

        override fun self(): Builder {
            return this
        }
    }
}