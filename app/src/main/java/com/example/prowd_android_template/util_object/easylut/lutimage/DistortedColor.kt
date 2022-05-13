package com.example.prowd_android_template.util_object.easylut.lutimage

import android.graphics.Color

object DistortedColor {
    @JvmStatic
    fun getColorOnXCoordinate(lutImage: LUTImage, pixelColor: Int): Int {
        return if (lutImage.coordinateToColor.isRedMappedToX) {
            getRed(lutImage, pixelColor)
        } else if (lutImage.coordinateToColor.isGreenMappedToX) {
            getGreen(lutImage, pixelColor)
        } else {
            getBlue(lutImage, pixelColor)
        }
    }

    @JvmStatic
    fun getColorOnYCoordinate(lutImage: LUTImage, pixelColor: Int): Int {
        return if (lutImage.coordinateToColor.isRedMappedToY) {
            getRed(lutImage, pixelColor)
        } else if (lutImage.coordinateToColor.isGreenMappedToY) {
            getGreen(lutImage, pixelColor)
        } else {
            getBlue(lutImage, pixelColor)
        }
    }

    @JvmStatic
    fun getColorOnZCoordinate(lutImage: LUTImage, pixelColor: Int): Int {
        return if (lutImage.coordinateToColor.isRedMappedToZ) {
            getRed(lutImage, pixelColor)
        } else if (lutImage.coordinateToColor.isGreenMappedToZ) {
            getGreen(lutImage, pixelColor)
        } else {
            getBlue(lutImage, pixelColor)
        }
    }

    private fun getBlue(lutImage: LUTImage, pixelColor: Int): Int {
        return Color.blue(pixelColor) / lutImage.rgbDistortion
    }

    private fun getGreen(lutImage: LUTImage, pixelColor: Int): Int {
        return Color.green(pixelColor) / lutImage.rgbDistortion
    }

    private fun getRed(lutImage: LUTImage, pixelColor: Int): Int {
        return Color.red(pixelColor) / lutImage.rgbDistortion
    }
}