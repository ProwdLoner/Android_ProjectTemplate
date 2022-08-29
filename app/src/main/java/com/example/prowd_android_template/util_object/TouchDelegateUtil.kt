package com.example.prowd_android_template.util_object

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import kotlin.math.roundToInt

object TouchDelegateUtil {
    // (뷰 클릭 영역 확대 함수)
    //     parent : 클릭 이벤트를 담당할 대행자 뷰 (클릭 대행 의뢰 뷰보다 앞쪽에 있는게 좋음)
    //     children : parent 에게 클릭 대행을 의뢰하는 객체들 Pair. first = 뷰, second = 증폭 영역 dp 값
    //     parent 뷰에 터치 대행 설정을 붙이는 것이므로, 동일한 parent 를 다시 설정하면 마지막 설정으로 덮어써짐
    //     복수 뷰를 설정시 range 가 겹치면 동시터치가 되므로 주의
    fun expandViewHitArea(context: Context, parent: View, children: ArrayList<Pair<View, Rect>>) {
        parent.post {
            val touchDelegateComposite = TouchDelegateComposite(children[0].first)

            for (child in children) {
                val childRect = Rect()
                child.first.getHitRect(childRect)

                val addPxLeft =
                    (child.second.left.toFloat() * context.resources.displayMetrics.density).roundToInt()
                val addPxRight =
                    (child.second.right.toFloat() * context.resources.displayMetrics.density).roundToInt()
                val addPxTop =
                    (child.second.top.toFloat() * context.resources.displayMetrics.density).roundToInt()
                val addPxBottom =
                    (child.second.bottom.toFloat() * context.resources.displayMetrics.density).roundToInt()

                val addedLeft = childRect.left - addPxLeft
                val addedRight = childRect.right + addPxRight
                val addedTop = childRect.top - addPxTop
                val addedBottom = childRect.bottom + addPxBottom

                childRect.left = addedLeft
                childRect.top = addedTop
                childRect.right = addedRight
                childRect.bottom = addedBottom

                touchDelegateComposite.addDelegate(TouchDelegate(childRect, child.first))
            }


            parent.touchDelegate = touchDelegateComposite
        }
    }

    class TouchDelegateComposite(view: View) :
        TouchDelegate(emptyRect, view) {
        private val delegates: MutableList<TouchDelegate> = ArrayList()
        fun addDelegate(delegate: TouchDelegate?) {
            if (delegate != null) {
                delegates.add(delegate)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            var res = false
            val x = event.x
            val y = event.y
            for (delegate in delegates) {
                event.setLocation(x, y)
                res = delegate.onTouchEvent(event) || res
            }
            return res
        }

        companion object {
            private val emptyRect = Rect()
        }
    }
}