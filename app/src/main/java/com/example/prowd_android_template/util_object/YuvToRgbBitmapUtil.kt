package com.example.prowd_android_template.util_object

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.renderscript.*
import java.nio.ByteBuffer

object YuvToRgbBitmapUtil {
    fun yuv420888ToRgbBitmapUsingRenderScript(
        renderScript: RenderScript,
        scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB,
        imageWidth: Int,
        imageHeight: Int,
        pixelCount: Int,
        pixelSizeBits: Int,
        imageCrop: Rect,
        yBuffer: ByteBuffer,
        yPixelStride: Int,
        yRowStride: Int,
        uBuffer: ByteBuffer,
        uPixelStride: Int,
        uRowStride: Int,
        vBuffer: ByteBuffer,
        vPixelStride: Int,
        vRowStride: Int
    ): Bitmap {
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)

        val yuvBuffer = ByteArray(pixelCount * pixelSizeBits / 8)

        // yBuffer
        val yOutputStride = 1
        var yOutputOffset = 0
        val yPlaneWidth = imageCrop.width()
        val yPlaneHeight = imageCrop.height()
        val yRowBuffer = ByteArray(yRowStride)
        val yRowLength = if (yPixelStride == 1 && yOutputStride == 1) {
            yPlaneWidth
        } else {
            (yPlaneWidth - 1) * yPixelStride + 1
        }
        for (row in 0 until yPlaneHeight) {
            yBuffer.position((row + imageCrop.top) * yRowStride + imageCrop.left * yPixelStride)
            if (yPixelStride == 1 && yOutputStride == 1) {
                yBuffer.get(yuvBuffer, yOutputOffset, yRowLength)
                yOutputOffset += yRowLength
            } else {
                yBuffer.get(yRowBuffer, 0, yRowLength)
                for (col in 0 until yPlaneWidth) {
                    yuvBuffer[yOutputOffset] = yRowBuffer[col * yPixelStride]
                    yOutputOffset += yOutputStride
                }
            }
        }


        // uBuffer
        val uOutputStride = 2
        var uOutputOffset = pixelCount + 1
        val uPlaneCrop = imageCrop.run {
            Rect(left / 2, top / 2, right / 2, bottom / 2)
        }
        val uPlaneWidth = uPlaneCrop.width()
        val uPlaneHeight = uPlaneCrop.height()
        val uRowBuffer = ByteArray(uRowStride)
        val uRowLength = if (uPixelStride == 1 && uOutputStride == 1) {
            uPlaneWidth
        } else {
            (uPlaneWidth - 1) * uPixelStride + 1
        }
        for (row in 0 until uPlaneHeight) {
            uBuffer.position((row + uPlaneCrop.top) * uRowStride + uPlaneCrop.left * uPixelStride)
            if (uPixelStride == 1 && uOutputStride == 1) {
                uBuffer.get(yuvBuffer, uOutputOffset, uRowLength)
                uOutputOffset += uRowLength
            } else {
                uBuffer.get(uRowBuffer, 0, uRowLength)
                for (col in 0 until uPlaneWidth) {
                    yuvBuffer[uOutputOffset] = uRowBuffer[col * uPixelStride]
                    uOutputOffset += uOutputStride
                }
            }
        }


        // vBuffer
        val vOutputStride = 2
        var vOutputOffset = pixelCount
        val vPlaneCrop = imageCrop.run {
            Rect(left / 2, top / 2, right / 2, bottom / 2)
        }
        val vPlaneWidth = vPlaneCrop.width()
        val vPlaneHeight = vPlaneCrop.height()
        val vRowBuffer = ByteArray(vRowStride)
        val vRowLength = if (vPixelStride == 1 && vOutputStride == 1) {
            vPlaneWidth
        } else {
            (vPlaneWidth - 1) * vPixelStride + 1
        }
        for (row in 0 until vPlaneHeight) {
            vBuffer.position((row + vPlaneCrop.top) * vRowStride + vPlaneCrop.left * vPixelStride)
            if (vPixelStride == 1 && vOutputStride == 1) {
                vBuffer.get(yuvBuffer, vOutputOffset, vRowLength)
                vOutputOffset += vRowLength
            } else {
                vBuffer.get(vRowBuffer, 0, vRowLength)
                for (col in 0 until vPlaneWidth) {
                    yuvBuffer[vOutputOffset] = vRowBuffer[col * vPixelStride]
                    vOutputOffset += vOutputStride
                }
            }
        }

        val elemType = Type.Builder(renderScript, Element.YUV(renderScript))
            .setYuvFormat(ImageFormat.NV21)
            .create()
        val inputAllocation = Allocation.createSized(renderScript, elemType.element, yuvBuffer.size)
        val outputAllocation = Allocation.createFromBitmap(renderScript, resultBitmap)

        inputAllocation.copyFrom(yuvBuffer)
        scriptIntrinsicYuvToRGB.setInput(inputAllocation)
        scriptIntrinsicYuvToRGB.forEach(outputAllocation)
        outputAllocation.copyTo(resultBitmap)

        outputAllocation.destroy()
        inputAllocation.destroy()
        elemType.destroy()

        return resultBitmap
    }
}