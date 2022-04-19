package com.example.prowd_android_template.util_object

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.*
import com.example.prowd_android_template.ScriptC_rotator

object RenderScriptUtil {
    // input example :
    //    var renderScript: RenderScript = RenderScript.create(application)

    // (YUV420888 Image 객체를 ByteArray 로 변환해서 반환하는 함수)
    fun yuv420888ImageToByteArray(image: Image): ByteArray {
        assert(image.format == ImageFormat.YUV_420_888)

        val pixelCount = image.width * image.height
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val resultYuvByteArray = ByteArray(pixelCount * pixelSizeBits / 8)

        val imagePlanes = image.planes
        imagePlanes.forEachIndexed { planeIndex, plane ->
            val outputStride: Int

            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    outputOffset = pixelCount
                }
                else -> {
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val imageCrop = Rect(0, 0, image.width, image.height)
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            val rowBuffer = ByteArray(plane.rowStride)

            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )

                if (pixelStride == 1 && outputStride == 1) {
                    planeBuffer.get(resultYuvByteArray, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        resultYuvByteArray[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
        return resultYuvByteArray
    }

    // (YUV 420 888 Byte Array 를 ARGB 8888 비트맵으로 변환하여 반환하는 함수)
    // input example :
    //    var scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(
    //        renderScript,
    //        Element.U8_4(renderScript)
    //    )
    fun yuv420888ToRgbBitmap(
        renderScript: RenderScript,
        scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB,
        imageWidth: Int,
        imageHeight: Int,
        yuvByteArray: ByteArray
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

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

        return resultBitmap
    }

    // (비트맵을 특정 사이즈로 리사이징 하는 함수)
    // input example :
    //    var scriptIntrinsicResize: ScriptIntrinsicResize = ScriptIntrinsicResize.create(
    //        renderScript
    //    )
    fun resizeBitmap(
        renderScript: RenderScript,
        scriptIntrinsicResize: ScriptIntrinsicResize,
        bitmap: Bitmap,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(dstWidth, dstHeight, bitmap.config)

        val inAlloc = Allocation.createFromBitmap(renderScript, bitmap)
        val outAlloc = Allocation.createFromBitmap(renderScript, resultBitmap)

        scriptIntrinsicResize.setInput(inAlloc)
        scriptIntrinsicResize.forEach_bicubic(outAlloc)
        outAlloc.copyTo(resultBitmap)

        inAlloc.destroy()
        outAlloc.destroy()

        return resultBitmap
    }

    // (이미지를 특정 각도로 회전시키는 함수)
    // input example :
    //    var scriptCRotator: ScriptC_rotator = ScriptC_rotator(renderScript)
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