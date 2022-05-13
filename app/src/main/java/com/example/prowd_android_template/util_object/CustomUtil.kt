package com.example.prowd_android_template.util_object

import android.R
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.TypedArray


object CustomUtil {
    // 현재 실행 환경이 디버그 모드인지 파악하는 함수
    fun isDebuggable(context: Context): Boolean {
        return 0 != context.packageManager.getApplicationInfo(
            context.packageName,
            0
        ).flags and ApplicationInfo.FLAG_DEBUGGABLE
    }

    fun getStatusBarHeightPixel(context: Context): Int {
        var statusBarHeight = 0
        val resourceId: Int =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    fun getActionBarHeightPixel(context: Context): Int {
        var actionBarHeight = 0
        val styledAttributes: TypedArray = context.theme.obtainStyledAttributes(
            intArrayOf(
                R.attr.actionBarSize
            )
        )
        actionBarHeight = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()

        return actionBarHeight
    }

    fun getNavigationBarHeightPixel(context: Context): Int {
        var navigationBarHeight = 0
        val resourceId: Int =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

}