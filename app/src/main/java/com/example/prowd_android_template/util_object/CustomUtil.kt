package com.example.prowd_android_template.util_object

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.util.Size
import android.view.*
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CustomUtil {
    // 현재 실행 환경이 디버그 모드인지 파악하는 함수
    fun isDebuggable(context: Context): Boolean {
        return 0 != context.packageManager.getApplicationInfo(
            context.packageName,
            0
        ).flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    // 스테이터스 바 높이 픽셀 반환
    fun getStatusBarHeightPixel(context: Context): Int {
        var statusBarHeight = 0
        val resourceId: Int =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    // 액션 바 높이 픽셀 반환
    fun getActionBarHeightPixel(context: Context): Int {
        var actionBarHeight = 0
        val styledAttributes: TypedArray = context.theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.actionBarSize
            )
        )
        actionBarHeight = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()

        return actionBarHeight
    }

    // 소프트 네비게이션 바 높이 픽셀 반환
    fun getSoftNavigationBarHeightPixel(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.defaultDisplay
        val appUsableSize = Point()
        display.getSize(appUsableSize)

        val realScreenSize = Point()
        display.getRealSize(realScreenSize)

        return if (appUsableSize.x < realScreenSize.x) {
            realScreenSize.x - appUsableSize.x
        } else if (appUsableSize.y < realScreenSize.y) {
            realScreenSize.y - appUsableSize.y
        } else {
            0
        }
    }

    fun getBitmapFromAssets(context: Context, filePath: String): Bitmap? {
        val assetManager: AssetManager = context.getAssets()
        val istr: InputStream
        var bitmap: Bitmap? = null
        istr = assetManager.open(filePath)
        bitmap = BitmapFactory.decodeStream(istr)
        return bitmap
    }

    fun cloneByteBuffer(original: ByteBuffer): ByteBuffer {
        val clone = ByteBuffer.allocate(original.capacity())
        original.rewind()
        clone.put(original)
        original.rewind()
        clone.flip()
        return clone
    }

    // 최대공약수
    fun getGcd(a: Int, b: Int): Int {
        val maximum = max(a, b)
        val minimum = min(a, b)

        return if (minimum == 0) {
            maximum
        } else {
            getGcd(minimum, maximum % minimum)
        }
    }

    // 최소공배수
    fun getLcm(a: Int, b: Int): Int =
        (a * b) / getGcd(a, b)
}