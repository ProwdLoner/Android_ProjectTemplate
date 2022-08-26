package com.example.prowd_android_template.util_object

import kotlin.math.roundToInt

object ScreenDensityCalcUtil {
    // 해상도 density 에서 dp 가 몇 px 인지
    fun dpToPx(density: Density, dp: Float): Int {
        return (dp * (density.dpi.toFloat() / Density.MDPI.dpi.toFloat())).roundToInt()
    }

    // 해상도 density 에서 px 가 몇 dp 인지
    fun pxToDp(density: Density, px: Int): Float {
        return px.toFloat() / (density.dpi.toFloat() / Density.MDPI.dpi.toFloat())
    }

    enum class Density(val dpi: Int) {
        LDPI(120),
        MDPI(160), // android dp 단위 기준.
        HDPI(240),
        XDPI(320),
        XXDPI(480),
        XXXDPI(640)
    }
}