package com.example.prowd_android_template.custom_view

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import kotlin.jvm.JvmOverloads
import android.graphics.*
import com.example.prowd_android_template.R
import androidx.annotation.DrawableRes
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import java.lang.Exception

// (둥근 이미지 뷰)
class CircleImageView : AppCompatImageView {
    // <멤버 변수 공간>
    var disableCircularTransformationMbr = false
    var borderColorMbr = Color.BLACK
    var circleBackgroundColorMbr = Color.TRANSPARENT
    var borderWidthMbr = 0
    var borderOverlayMbr = false

    private var objReadyMbr = false
    private var setupPendingMbr = false
    private var bitmapMbr: Bitmap? = null
    private val drawableRectMbr = RectF()
    private val borderRectMbr = RectF()
    private val shaderMatrixMbr = Matrix()
    private val bitmapPaintMbr: Paint = Paint()
    private val borderPaintMbr = Paint()
    private val circleBackgroundPaintMbr = Paint()
    private var bitmapShaderMbr: BitmapShader? = null
    private var bitmapWidthMbr = 0
    private var bitmapHeightMbr = 0
    private var drawableRadiusMbr = 0f
    private var borderRadiusMbr = 0f
    private var colorFilterMbr: ColorFilter? = null


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    constructor(context: Context?) : super(context!!) {
        init()
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    ) {
        // 유저 설정이 존재하면 이를 멤버 상태 변수에 반영
        val styleAttr =
            context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0)
        borderWidthMbr = styleAttr.getDimensionPixelSize(
            R.styleable.CircleImageView_civ_border_width,
            0
        )
        borderColorMbr =
            styleAttr.getColor(R.styleable.CircleImageView_civ_border_color, Color.BLACK)
        borderOverlayMbr =
            styleAttr.getBoolean(R.styleable.CircleImageView_civ_border_overlay, false)
        circleBackgroundColorMbr =
            styleAttr.getColor(R.styleable.CircleImageView_civ_fill_color, Color.TRANSPARENT)
        styleAttr.recycle()
        init()
    }

    private fun init() {
        super.setScaleType(ScaleType.CENTER_CROP)
        objReadyMbr = true
        outlineProvider =
            OutlineProvider()
        if (setupPendingMbr) {
            setup()
            setupPendingMbr = false
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <오버라이딩 공간>
    override fun getScaleType(): ScaleType {
        return ScaleType.CENTER_CROP
    }

    override fun setScaleType(scaleType: ScaleType) {
        // 스케일 타입이 CENTER_CROP 이 아니라면 에러
        require(scaleType == ScaleType.CENTER_CROP) {
            String.format(
                "ScaleType %s not supported.",
                scaleType
            )
        }
    }

    override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        // adjustViewBounds 가 true 라면 에러
        require(!adjustViewBounds) { "adjustViewBounds not supported." }
    }

    override fun onDraw(canvas: Canvas) {
        if (disableCircularTransformationMbr) {
            super.onDraw(canvas)
            return
        }
        if (bitmapMbr == null) {
            return
        }
        if (circleBackgroundColorMbr != Color.TRANSPARENT) {
            canvas.drawCircle(
                drawableRectMbr.centerX(),
                drawableRectMbr.centerY(),
                drawableRadiusMbr,
                circleBackgroundPaintMbr
            )
        }
        canvas.drawCircle(
            drawableRectMbr.centerX(),
            drawableRectMbr.centerY(),
            drawableRadiusMbr,
            bitmapPaintMbr
        )
        if (borderWidthMbr > 0) {
            canvas.drawCircle(
                borderRectMbr.centerX(),
                borderRectMbr.centerY(),
                borderRadiusMbr,
                borderPaintMbr
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (cf === colorFilterMbr) {
            return
        }
        colorFilterMbr = cf
        applyColorFilter()
        invalidate()
    }

    override fun getColorFilter(): ColorFilter {
        return colorFilterMbr!!
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>
    private fun applyColorFilter() {
        // This might be called from setColorFilter during ImageView construction
        // before member initialization has finished on API level <= 19.
        bitmapPaintMbr.colorFilter = colorFilterMbr
    }

    private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else try {
            val bitmap: Bitmap = if (drawable is ColorDrawable) {
                Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun initializeBitmap() {
        bitmapMbr = if (disableCircularTransformationMbr) {
            null
        } else {
            getBitmapFromDrawable(drawable)
        }
        setup()
    }

    private fun setup() {
        if (!objReadyMbr) {
            setupPendingMbr = true
            return
        }
        if (width == 0 && height == 0) {
            return
        }
        if (bitmapMbr == null) {
            invalidate()
            return
        }
        bitmapShaderMbr = BitmapShader(bitmapMbr!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        bitmapPaintMbr.isAntiAlias = true
        bitmapPaintMbr.isDither = true
        bitmapPaintMbr.isFilterBitmap = true
        bitmapPaintMbr.shader = bitmapShaderMbr
        borderPaintMbr.style = Paint.Style.STROKE
        borderPaintMbr.isAntiAlias = true
        borderPaintMbr.color = borderColorMbr
        borderPaintMbr.strokeWidth = borderWidthMbr.toFloat()
        circleBackgroundPaintMbr.style = Paint.Style.FILL
        circleBackgroundPaintMbr.isAntiAlias = true
        circleBackgroundPaintMbr.color = circleBackgroundColorMbr
        bitmapHeightMbr = bitmapMbr!!.height
        bitmapWidthMbr = bitmapMbr!!.width
        borderRectMbr.set(calculateBounds())
        borderRadiusMbr =
            ((borderRectMbr.height() - borderWidthMbr) / 2.0f).coerceAtMost((borderRectMbr.width() - borderWidthMbr) / 2.0f)
        drawableRectMbr.set(borderRectMbr)
        if (!borderOverlayMbr && borderWidthMbr > 0) {
            drawableRectMbr.inset(borderWidthMbr - 1.0f, borderWidthMbr - 1.0f)
        }
        drawableRadiusMbr = (drawableRectMbr.height() / 2.0f).coerceAtMost(drawableRectMbr.width() / 2.0f)
        applyColorFilter()
        updateShaderMatrix()
        invalidate()
    }

    private fun calculateBounds(): RectF {
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom
        val sideLength = availableWidth.coerceAtMost(availableHeight)
        val left = paddingLeft + (availableWidth - sideLength) / 2f
        val top = paddingTop + (availableHeight - sideLength) / 2f
        return RectF(left, top, left + sideLength, top + sideLength)
    }

    private fun updateShaderMatrix() {
        val scale: Float
        var dx = 0f
        var dy = 0f
        shaderMatrixMbr.set(null)
        if (bitmapWidthMbr * drawableRectMbr.height() > drawableRectMbr.width() * bitmapHeightMbr) {
            scale = drawableRectMbr.height() / bitmapHeightMbr.toFloat()
            dx = (drawableRectMbr.width() - bitmapWidthMbr * scale) * 0.5f
        } else {
            scale = drawableRectMbr.width() / bitmapWidthMbr.toFloat()
            dy = (drawableRectMbr.height() - bitmapHeightMbr * scale) * 0.5f
        }
        shaderMatrixMbr.setScale(scale, scale)
        shaderMatrixMbr.postTranslate(
            (dx + 0.5f).toInt() + drawableRectMbr.left,
            (dy + 0.5f).toInt() + drawableRectMbr.top
        )
        bitmapShaderMbr!!.setLocalMatrix(shaderMatrixMbr)
    }


    // ---------------------------------------------------------------------------------------------
    // <내부 클래스 공간>
    private inner class OutlineProvider : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (disableCircularTransformationMbr) {
                BACKGROUND.getOutline(view, outline)
            } else {
                val bounds = Rect()
                borderRectMbr.roundOut(bounds)
                outline.setRoundRect(bounds, bounds.width() / 2.0f)
            }
        }
    }
}