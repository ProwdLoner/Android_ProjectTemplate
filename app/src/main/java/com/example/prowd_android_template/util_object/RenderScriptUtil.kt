package com.example.prowd_android_template.util_object

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.example.prowd_android_template.ScriptC_rotator

object RenderScriptUtil {
    fun rotateBitmap(
        renderScript: RenderScript,
        scriptCRotator: ScriptC_rotator,
        bitmap: Bitmap,
        angleCcw: Int
    ): Bitmap {
        if (angleCcw == 0) return bitmap

        scriptCRotator._inWidth = bitmap.width
        scriptCRotator._inHeight = bitmap.height

        val sourceAllocation = Allocation.createFromBitmap(
            renderScript, bitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )

        bitmap.recycle()

        scriptCRotator._inImage = sourceAllocation

        val targetHeight: Int =
            if (angleCcw == 90 || angleCcw == 270) bitmap.width else bitmap.height
        val targetWidth: Int =
            if (angleCcw == 90 || angleCcw == 270) bitmap.height else bitmap.width

        val config = bitmap.config
        val target = Bitmap.createBitmap(targetWidth, targetHeight, config)
        val targetAllocation = Allocation.createFromBitmap(
            renderScript, target,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        when (angleCcw) {
            -1 -> scriptCRotator.forEach_flip_horizontally(targetAllocation, targetAllocation)
            -2 -> scriptCRotator.forEach_flip_vertically(targetAllocation, targetAllocation)
            90 -> scriptCRotator.forEach_rotate_90_clockwise(targetAllocation, targetAllocation)
            180 -> scriptCRotator.forEach_flip_vertically(targetAllocation, targetAllocation)
            270 -> scriptCRotator.forEach_rotate_270_clockwise(targetAllocation, targetAllocation)
        }
        targetAllocation.copyTo(target)

        sourceAllocation.destroy()
        targetAllocation.destroy()

        return target
    }
}