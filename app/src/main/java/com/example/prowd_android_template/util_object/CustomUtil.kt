package com.example.prowd_android_template.util_object

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.util.Size
import android.view.Display
import android.view.WindowManager
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


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

    // todo : minmax 값 적용
    // (사이즈 리스트 중 원하는 비율과 크기에 가까운 사이즈를 고르는 함수)
    // preferredArea 0 은 최소, Long.MAX_VALUE 는 최대
    // preferredWHRatio 0 이하면 비율을 신경쓰지 않고 넓이만으로 비교
    fun getNearestSupportedCameraOutputSize(
        sizeArray: Array<Size>,
        preferredArea: Long,
        preferredWHRatio: Double
    ): Size {
        if (0 >= preferredWHRatio) { // whRatio 를 0 이하로 선택하면 넓이만으로 비교
            // 넓이 비슷한 것을 선정
            var smallestAreaDiff: Long = Long.MAX_VALUE
            var resultIndex = 0

            for ((index, value) in sizeArray.withIndex()) {
                val area = value.width.toLong() * value.height.toLong()
                val areaDiff = abs(area - preferredArea)
                if (areaDiff < smallestAreaDiff) {
                    smallestAreaDiff = areaDiff
                    resultIndex = index
                }
            }

            return sizeArray[resultIndex]
        } else { // 비율을 먼저 보고, 이후 넓이로 비교
            // 비율 비슷한 것을 선정
            var mostSameWhRatio = 0.0
            var smallestWhRatioDiff: Double = Double.MAX_VALUE

            for (value in sizeArray) {
                val whRatio: Double = value.width.toDouble() / value.height.toDouble()

                val whRatioDiff = abs(whRatio - preferredWHRatio)
                if (whRatioDiff < smallestWhRatioDiff) {
                    smallestWhRatioDiff = whRatioDiff
                    mostSameWhRatio = whRatio
                }
            }

            // 넓이 비슷한 것을 선정
            var resultSizeIndex = 0
            var smallestAreaDiff: Long = Long.MAX_VALUE
            // 비슷한 비율중 가장 비슷한 넓이를 선정
            for ((index, value) in sizeArray.withIndex()) {
                val whRatio: Double = value.width.toDouble() / value.height.toDouble()

                if (mostSameWhRatio == whRatio) {
                    val area = value.width.toLong() * value.height.toLong()
                    val areaDiff = abs(area - preferredArea)
                    if (areaDiff < smallestAreaDiff) {
                        smallestAreaDiff = areaDiff
                        resultSizeIndex = index
                    }
                }
            }
            return sizeArray[resultSizeIndex]
        }
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

    // (현 화면상 dp 를 px 로 변환해주는 함수)
    fun convertDPtoPX(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp.toFloat() * density).roundToInt()
    }
}