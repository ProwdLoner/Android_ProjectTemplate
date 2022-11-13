package com.example.prowd_android_template.util_object

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Point
import android.media.MediaPlayer
import android.util.Size
import android.view.*
import java.io.*
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CustomUtil {
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

    fun playAssetsAudioFile(
        context: Context,
        filePath: String,
        isLooping: Boolean,
        onCompletePlaying: (MediaPlayer) -> Unit
    ): MediaPlayer {
        val mediaPlayer = MediaPlayer()
        val descriptor: AssetFileDescriptor = context.assets.openFd(filePath)
        mediaPlayer.setDataSource(
            descriptor.fileDescriptor,
            descriptor.startOffset,
            descriptor.length
        )
        descriptor.close()
        mediaPlayer.setOnCompletionListener {
            onCompletePlaying(it)
        }
        mediaPlayer.prepare()
        mediaPlayer.setVolume(1f, 1f)
        mediaPlayer.isLooping = isLooping
        mediaPlayer.start()

        return mediaPlayer
    }

    fun fileCopy(src: File?, dst: File?) {
        val inputStream1: InputStream = FileInputStream(src)
        inputStream1.use { inputStream ->
            val out: OutputStream = FileOutputStream(dst)
            out.use { outputStream ->
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    outputStream.write(buf, 0, len)
                }
            }
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun getBitmapFromView(view: View, defaultColor: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.drawColor(defaultColor)
        view.draw(canvas)
        return bitmap
    }
}