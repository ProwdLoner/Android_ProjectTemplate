package com.example.prowd_android_template.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

// (세로 스크롤 내 가로 스크롤 사용성을 위한 래핑 뷰 클래스)
class RecyclerViewForInnerHorizontalScrollView(context: Context, attrs: AttributeSet?) :
    RecyclerView(context, attrs) {
    private val mTouchSlop: Int
    private var mPrevX = 0f

    init {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mPrevX = MotionEvent.obtain(event).x
            MotionEvent.ACTION_MOVE -> {
                val eventX = event.x
                val xDiff = abs(eventX - mPrevX)
                if (xDiff > mTouchSlop) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }
}