package com.example.prowd_android_template.util_object.easylut.filter

import com.example.prowd_android_template.util_object.easylut.lutimage.CoordinateToColor
import com.example.prowd_android_template.util_object.easylut.lutimage.LutAlignment
import android.graphics.Bitmap
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView

abstract class LUTFilter protected constructor(
    private val strategy: BitmapStrategy,
    private val coordinateToColorType: CoordinateToColor.Type,
    private val lutAlignmentMode: LutAlignment.Mode
) : Filter {
    override fun apply(src: Bitmap): Bitmap {
        val lutBitmap = lUTBitmap
        val lutImage = LUTImage.createLutImage(lutBitmap, coordinateToColorType, lutAlignmentMode)
        return strategy.applyLut(src, lutImage)
    }

    protected abstract val lUTBitmap: Bitmap
    override fun apply(imageView: ImageView) {
        val imageDrawable = imageView.drawable as BitmapDrawable
        val source = imageDrawable.bitmap
        val bitmap = apply(source)
        imageView.setImageBitmap(bitmap)
    }

    abstract class Builder<B> {
        @JvmField
        protected var strategy: BitmapStrategy = CreatingNewBitmap()
        @JvmField
        protected var coordinateToColorType = CoordinateToColor.Type.GUESS_AXES
        @JvmField
        protected var lutAlignmentMode = LutAlignment.Mode.SQUARE
        fun withStrategy(strategy: BitmapStrategy.Type?): B {
            when (strategy) {
                BitmapStrategy.Type.APPLY_ON_ORIGINAL_BITMAP -> this.strategy = ApplyOnOriginal()
                BitmapStrategy.Type.CREATING_NEW_BITMAP -> this.strategy = CreatingNewBitmap()
            }
            return self()
        }

        fun withColorAxes(coordinateToColorType: CoordinateToColor.Type): B {
            this.coordinateToColorType = coordinateToColorType
            return self()
        }

        fun withAlignmentMode(lutAlignmentMode: LutAlignment.Mode): B {
            this.lutAlignmentMode = lutAlignmentMode
            return self()
        }

        protected abstract fun self(): B
    }
}