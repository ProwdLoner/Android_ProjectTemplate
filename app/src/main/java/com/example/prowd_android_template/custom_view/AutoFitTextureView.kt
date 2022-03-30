package com.example.prowd_android_template.custom_view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.TextureView

// <카메라 프리뷰로 사용할 텍스쳐 뷰>
// setAspectRatio 으로 설정한 width / height 와 화면 전체 크기에 따라 재조정 되는 텍스쳐 뷰
class AutoFitTextureView : TextureView {
    // <멤버 변수 공간>
    // (뷰 크기 정보)
    private var ratioWidthMbr = 0
    private var ratioHeightMbr = 0


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )


    // ---------------------------------------------------------------------------------------------
    // <뷰 생명주기 공간>

    // (크기가 정해지고 다시 뷰를 그리는 시점)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (0 == ratioWidthMbr || 0 == ratioHeightMbr) { // 외부에서 전해준 값이 없을 경우
            // 기본 설정된 최대 사이즈를 사용
            // 화면 그리기 = onDraw 실행
            setMeasuredDimension(width, height)
        } else {
            // 외부에서 전해준 사이즈 값을 비율에 맞게 변경
            if (width < height * ratioWidthMbr / ratioHeightMbr) {
                // 외부 설정 사이즈 비율 width 값이 기본 width 보다 큰 경우(새 비율의 width 가 정해진 최대 크기를 넘어설 경우)
                // width 는 최대값으로 고정하고, height 는 비율에 맞게 수정
                // 화면 그리기
                setMeasuredDimension(width, width * ratioHeightMbr / ratioWidthMbr)
            } else {
                // 외부 설정 사이즈 비율 width 값이 기본 width 보다 작은 경우(새 비율의 height 가 정해진 최대 크기를 넘어설 경우)
                // height 는 최대값으로 고정하고, width 는 비율에 맞게 수정
                // 화면 그리기
                setMeasuredDimension(height * ratioWidthMbr / ratioHeightMbr, height)
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    // [다시 그리기]
    // 인자로 전해준 width, height 값의 "비율에 맞게" 화면을 다시 그리기
    // ratioWidth, ratioHeight 가 설정되고 onMeasure 가 다시 실행됨
    fun setAspectRatio(width: Int, height: Int) {
        if (width >= 0 && height >= 0) { // size 값이 음수가 아닐 때만
            ratioWidthMbr = width
            ratioHeightMbr = height

            // 화면 갱신 = onMeasure 실행
            requestLayout()
        }
    }
}