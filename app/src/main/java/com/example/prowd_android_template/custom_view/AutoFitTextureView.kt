package com.example.prowd_android_template.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

// <카메라 프리뷰로 사용할 텍스쳐 뷰>
// setAspectRatio 으로 설정한 width / height 와 화면 전체 크기에 따라 재조정 되는 텍스쳐 뷰
class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {
    // <멤버 변수 공간>
    private var ratioWidth = 0
    private var ratioHeight = 0


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < ((height * ratioWidth) / ratioHeight)) {
                setMeasuredDimension(width, (width * ratioHeight) / ratioWidth)
            } else {
                setMeasuredDimension((height * ratioWidth) / ratioHeight, height)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

}