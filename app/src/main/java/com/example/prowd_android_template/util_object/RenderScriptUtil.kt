package com.example.prowd_android_template.util_object

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.renderscript.*
import com.example.prowd_android_template.ScriptC_rotator
import com.xxx.yyy.ScriptC_crop

// [랜더스크립트 이미지 처리 유틸]
// 비동기 데이터 처리중 랜더스크립트 객체는 onDestroy 에서 해제되는 경우가 있으므로
// 해당 객체 사용시 try catch 를 사용하여 크랙 방지 및 안정성 재고
object RenderScriptUtil {
    // (YUV 420 888 Byte Array 를 ARGB 8888 비트맵으로 변환하여 반환하는 함수)
    // input example :
    //    var scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(
    //        renderScript,
    //        Element.U8_4(renderScript)
    //    )
    fun yuv420888ToARgb8888BitmapIntrinsic(
        renderScript: RenderScript,
        scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB,
        imageWidth: Int,
        imageHeight: Int,
        yuvByteArray: ByteArray
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        try {
            val elemType = Type.Builder(renderScript, Element.YUV(renderScript))
                .setYuvFormat(ImageFormat.NV21)
                .create()
            val inputAllocation =
                Allocation.createSized(renderScript, elemType.element, yuvByteArray.size)
            val outputAllocation = Allocation.createFromBitmap(renderScript, resultBitmap)

            inputAllocation.copyFrom(yuvByteArray)
            scriptIntrinsicYuvToRGB.setInput(inputAllocation)
            scriptIntrinsicYuvToRGB.forEach(outputAllocation)
            outputAllocation.copyTo(resultBitmap)

            outputAllocation.destroy()
            inputAllocation.destroy()
            elemType.destroy()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultBitmap
    }

    // (이미지를 특정 각도로 회전시키는 함수)
    // input example :
    //    var scriptCRotator: ScriptC_rotator = ScriptC_rotator(renderScript)
    fun rotateBitmapCounterClock(
        renderScript: RenderScript,
        scriptCRotator: ScriptC_rotator,
        bitmap: Bitmap,
        angleCcw: Int
    ): Bitmap {
        if (angleCcw == 0) return bitmap
        val resultBitmap = Bitmap.createBitmap(
            if (angleCcw == 90 || angleCcw == 270) bitmap.height else bitmap.width,
            if (angleCcw == 90 || angleCcw == 270) bitmap.width else bitmap.height,
            bitmap.config
        )

        try {
            scriptCRotator._inWidth = bitmap.width
            scriptCRotator._inHeight = bitmap.height

            val sourceAllocation = Allocation.createFromBitmap(
                renderScript, bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )

            scriptCRotator._inImage = sourceAllocation

            val targetAllocation = Allocation.createFromBitmap(
                renderScript, resultBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            when (angleCcw) {
                -1 -> scriptCRotator.forEach_flip_horizontally(targetAllocation, targetAllocation)
                -2 -> scriptCRotator.forEach_flip_vertically(targetAllocation, targetAllocation)
                90 -> scriptCRotator.forEach_rotate_90_clockwise(targetAllocation, targetAllocation)
                180 -> scriptCRotator.forEach_rotate_180_clockwise(
                    targetAllocation,
                    targetAllocation
                )
                270 -> scriptCRotator.forEach_rotate_270_clockwise(
                    targetAllocation,
                    targetAllocation
                )
            }
            targetAllocation.copyTo(resultBitmap)

            sourceAllocation.destroy()
            targetAllocation.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultBitmap
    }

    // (비트맵을 특정 사이즈로 리사이징 하는 함수)
    // input example :
    //    var scriptIntrinsicResize: ScriptIntrinsicResize = ScriptIntrinsicResize.create(
    //        renderScript
    //    )
    fun resizeBitmapIntrinsic(
        renderScript: RenderScript,
        scriptIntrinsicResize: ScriptIntrinsicResize,
        bitmap: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(dstWidth, dstHeight, bitmap.config)

        try {
            val inAlloc = Allocation.createFromBitmap(renderScript, bitmap)
            val outAlloc = Allocation.createFromBitmap(renderScript, resultBitmap)

            scriptIntrinsicResize.setInput(inAlloc)
            scriptIntrinsicResize.forEach_bicubic(outAlloc)
            outAlloc.copyTo(resultBitmap)

            inAlloc.destroy()
            outAlloc.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultBitmap
    }

    // (이미지를 특정 좌표에서 자르는 함수)
    fun cropBitmap(
        renderScript: RenderScript,
        scriptCCrop: ScriptC_crop,
        sourceBitmap: Bitmap,
        roiRect: Rect
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            roiRect.right - roiRect.left,
            roiRect.bottom - roiRect.top,
            sourceBitmap.config
        )

        val width = sourceBitmap.width
        val height = sourceBitmap.height

        try {
            val inputType =
                Type.createXY(renderScript, Element.RGBA_8888(renderScript), width, height)
            val inputAllocation =
                Allocation.createTyped(renderScript, inputType, Allocation.USAGE_SCRIPT)
            inputAllocation.copyFrom(sourceBitmap)

            val outputType =
                Type.createXY(
                    renderScript,
                    Element.RGBA_8888(renderScript),
                    roiRect.right - roiRect.left,
                    roiRect.bottom - roiRect.top
                )
            val outputAllocation =
                Allocation.createTyped(renderScript, outputType, Allocation.USAGE_SCRIPT)

            scriptCCrop._croppedImg = outputAllocation
            scriptCCrop._width = width
            scriptCCrop._height = height
            scriptCCrop._xStart = roiRect.left.toLong()
            scriptCCrop._yStart = roiRect.top.toLong()

            val launchOptions: Script.LaunchOptions = Script.LaunchOptions()
            launchOptions.setX(roiRect.left, roiRect.right)
            launchOptions.setY(roiRect.top, roiRect.bottom)

            scriptCCrop.forEach_doCrop(inputAllocation, launchOptions)

            outputAllocation.copyTo(resultBitmap)

            inputType.destroy()
            inputAllocation.destroy()
            outputType.destroy()
            outputAllocation.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return resultBitmap
    }
}