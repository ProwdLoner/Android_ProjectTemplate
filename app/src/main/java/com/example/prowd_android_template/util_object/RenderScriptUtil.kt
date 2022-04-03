package com.example.prowd_android_template.util_object

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.example.prowd_android_template.ScriptC_rotator

object RenderScriptUtil {
    fun rotateBitmap(context: Context, bitmap: Bitmap, angleCcw: Int): Bitmap? {
        if (angleCcw == 0) return bitmap
        val rs = RenderScript.create(context)
        val script = ScriptC_rotator(rs)
        script._inWidth = bitmap.width
        script._inHeight = bitmap.height
        val sourceAllocation = Allocation.createFromBitmap(
            rs, bitmap,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        bitmap.recycle()
        script._inImage = sourceAllocation
        val targetHeight: Int =
            if (angleCcw == 90 || angleCcw == 270) bitmap.width else bitmap.height
        val targetWidth: Int =
            if (angleCcw == 90 || angleCcw == 270) bitmap.height else bitmap.width
        val config = bitmap.config
        val target = Bitmap.createBitmap(targetWidth, targetHeight, config)
        val targetAllocation = Allocation.createFromBitmap(
            rs, target,
            Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        when (angleCcw) {
            -1 -> script.forEach_flip_horizontally(targetAllocation, targetAllocation)
            -2 -> script.forEach_flip_vertically(targetAllocation, targetAllocation)
            90 -> script.forEach_rotate_90_clockwise(targetAllocation, targetAllocation)
            180 -> script.forEach_flip_vertically(targetAllocation, targetAllocation)
            270 -> script.forEach_rotate_270_clockwise(targetAllocation, targetAllocation)
        }
        targetAllocation.copyTo(target)
        rs.destroy()
        return target
    }
}