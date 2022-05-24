package com.example.prowd_android_template.custom_view.pv

import android.view.MotionEvent

interface OnSingleFlingListener {
    fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean
}