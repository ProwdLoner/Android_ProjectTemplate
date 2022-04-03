package com.example.prowd_android_template.util_object

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.renderscript.*
import java.nio.ByteBuffer

object YuvToRgbBitmapUtil {
    fun yuv420888ToRgbBitmapUsingRenderScript(
        renderScript: RenderScript,
        scriptIntrinsicYuvToRGB: ScriptIntrinsicYuvToRGB,
        imageWidth: Int,
        imageHeight: Int,
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
        val pixelCount: Int = imageWidth * imageHeight

        // (yuvByteArray 생성)
        val yuvByteArray = ByteArray(pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)

        // yBuffer
        var yOutputOffset = 0
        val yRowByteArray = ByteArray(yRowStride)
        val yRowLength = if (yPixelStride == 1) {
            imageWidth
        } else {
            (imageWidth - 1) * yPixelStride + 1
        }
        for (row in 0 until imageHeight) {
            yBuffer.position(row * yRowStride)
            if (yPixelStride == 1) {
                yBuffer.get(yuvByteArray, yOutputOffset, yRowLength)
                yOutputOffset += yRowLength
            } else {
                yBuffer.get(yRowByteArray, 0, yRowLength)
                for (col in 0 until imageWidth) {
                    yuvByteArray[yOutputOffset] = yRowByteArray[col * yPixelStride]
                    yOutputOffset += 1
                }
            }
        }

        // uBuffer
        var uOutputOffset = pixelCount + 1
        val uPlaneWidth = imageWidth / 2
        val uPlaneHeight = imageHeight / 2
        val uRowByteArray = ByteArray(uRowStride)
        val uRowLength =
            (uPlaneWidth - 1) * uPixelStride + 1
        for (row in 0 until uPlaneHeight) {
            uBuffer.position((row) * uRowStride)
            uBuffer.get(uRowByteArray, 0, uRowLength)
            for (col in 0 until uPlaneWidth) {
                yuvByteArray[uOutputOffset] = uRowByteArray[col * uPixelStride]
                uOutputOffset += 2
            }
        }

        // vBuffer
        var vOutputOffset = pixelCount
        val vPlaneWidth = imageWidth / 2
        val vPlaneHeight = imageHeight / 2
        val vRowByteArray = ByteArray(vRowStride)
        val vRowLength =
            (vPlaneWidth - 1) * vPixelStride + 1
        for (row in 0 until vPlaneHeight) {
            vBuffer.position((row) * vRowStride)
            vBuffer.get(vRowByteArray, 0, vRowLength)
            for (col in 0 until vPlaneWidth) {
                yuvByteArray[vOutputOffset] = vRowByteArray[col * vPixelStride]
                vOutputOffset += 2
            }
        }

        // (RenderScriptIntrinsic 실행)
        val elemType = Type.Builder(renderScript, Element.YUV(renderScript))
            .setYuvFormat(ImageFormat.NV21)
            .create()
        val inputAllocation = Allocation.createSized(renderScript, elemType.element, yuvByteArray.size)
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
}