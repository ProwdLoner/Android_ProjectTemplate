package com.example.prowd_android_template.util_object.easylut.lutimage

import android.graphics.Color
import com.example.prowd_android_template.util_object.easylut.lutimage.LUTImage
import com.example.prowd_android_template.util_object.easylut.lutimage.CoordinateToColor

class GuessCoordinateToColor(private val lutImage: LUTImage) : CoordinateToColor {
    private val isRedMappedToX: Boolean
    private val isRedMappedToY: Boolean
    private val isRedMappedToZ: Boolean
    private val isGreenMappedToX: Boolean
    private val isGreenMappedToY: Boolean
    private val isGreenMappedToZ: Boolean
    override fun isRedMappedToX(): Boolean {
        return isRedMappedToX
    }

    override fun isRedMappedToY(): Boolean {
        return isRedMappedToY
    }

    override fun isRedMappedToZ(): Boolean {
        return isRedMappedToZ
    }

    override fun isGreenMappedToX(): Boolean {
        return isGreenMappedToX
    }

    override fun isGreenMappedToY(): Boolean {
        return isGreenMappedToY
    }

    override fun isGreenMappedToZ(): Boolean {
        return isGreenMappedToZ
    }

    private fun calculateRedMappedToX(): Boolean {
        val xOnStrongest = lutImage.getPixelByIndex(lutImage.sideSize - 1)
        return redIsStrongestOnPixel(xOnStrongest)
    }

    fun calculateRedMappedToY(): Boolean {
        val yIndex = lutImage.lutWidth * (lutImage.sideSize - 1)
        val yOnStrongest = lutImage.getPixelByIndex(yIndex)
        return redIsStrongestOnPixel(yOnStrongest)
    }

    fun calculateRedMappedToZ(): Boolean {
        val columnDepth = lutImage.columnDepth
        val X = (columnDepth - 1) * lutImage.sideSize + 1
        val Y = (lutImage.rowDepth - 1) * lutImage.sideSize + 1
        val xOnStrongest = lutImage.getPixelByIndex(Y * lutImage.lutWidth + X)
        return redIsStrongestOnPixel(xOnStrongest)
    }

    fun calculateGreenMappedToX(): Boolean {
        val xOnStrongest = lutImage.getPixelByIndex(lutImage.sideSize - 1)
        return greenIsStrongestOnPixel(xOnStrongest)
    }

    fun calculateGreenMappedToY(): Boolean {
        val yIndex = lutImage.lutWidth * (lutImage.sideSize - 1)
        val yOnStrongest = lutImage.getPixelByIndex(yIndex)
        return greenIsStrongestOnPixel(yOnStrongest)
    }

    fun calculateGreenMappedToZ(): Boolean {
        val columnDepth = lutImage.columnDepth
        val X = (columnDepth - 1) * lutImage.sideSize + 1
        val Y = (lutImage.rowDepth - 1) * lutImage.sideSize + 1
        val xOnStrongest = lutImage.getPixelByIndex(Y * lutImage.lutWidth + X)
        return greenIsStrongestOnPixel(xOnStrongest)
    }

    private fun greenIsStrongestOnPixel(color: Int): Boolean {
        val green = Color.green(color)
        val red = Color.red(color)
        val blue = Color.blue(color)
        return green > red &&
                green > blue
    }

    private fun redIsStrongestOnPixel(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return red > green &&
                red > blue
    }

    init {
        isRedMappedToX = calculateRedMappedToX()
        isRedMappedToY = calculateRedMappedToY()
        isGreenMappedToX = calculateGreenMappedToX()
        isGreenMappedToY = calculateGreenMappedToY()
        isRedMappedToZ = calculateRedMappedToZ()
        isGreenMappedToZ = calculateGreenMappedToZ()
    }
}