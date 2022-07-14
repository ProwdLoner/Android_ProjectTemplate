package com.example.prowd_android_template.custom_view.pv

import android.content.Context
import android.graphics.Matrix
import kotlin.jvm.JvmOverloads
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.widget.ImageView

class PinchImageView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context, attr, defStyle
) {
    // <멤버 변수 공간>
    val attacher: PhotoViewAttacher? = PhotoViewAttacher(this)
    private var pendingScaleType: ScaleType? = null


    // ---------------------------------------------------------------------------------------------
    // <생성자 공간>
    init {
        super.setScaleType(ScaleType.MATRIX)
        if (pendingScaleType != null) {
            scaleType = pendingScaleType!!
            pendingScaleType = null
        }
    }


    // ---------------------------------------------------------------------------------------------
    // <클래스 생명주기 공간>
    override fun getScaleType(): ScaleType {
        return attacher!!.scaleType
    }

    override fun getImageMatrix(): Matrix {
        return attacher!!.imageMatrix
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        attacher!!.setOnLongClickListener(l)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        attacher!!.setOnClickListener(l)
    }

    override fun setScaleType(scaleType: ScaleType) {
        if (attacher == null) {
            pendingScaleType = scaleType
        } else {
            attacher.scaleType = scaleType
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        // setImageBitmap calls through to this method
        attacher?.update()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        attacher?.update()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        attacher?.update()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        if (changed) {
            attacher!!.update()
        }
        return changed
    }


    // ---------------------------------------------------------------------------------------------
    // <공개 메소드 공간>
    fun setRotationTo(rotationDegree: Float) {
        attacher!!.setRotationTo(rotationDegree)
    }

    fun setRotationBy(rotationDegree: Float) {
        attacher!!.setRotationBy(rotationDegree)
    }

    var isZoomable: Boolean
        get() = attacher!!.isZoomable
        set(zoomable) {
            attacher!!.isZoomable = zoomable
        }
    val displayRect: RectF
        get() = attacher!!.displayRect

    fun getDisplayMatrix(matrix: Matrix?) {
        attacher!!.getDisplayMatrix(matrix)
    }

    fun setDisplayMatrix(finalRectangle: Matrix?): Boolean {
        return attacher!!.setDisplayMatrix(finalRectangle)
    }

    fun getSuppMatrix(matrix: Matrix?) {
        attacher!!.getSuppMatrix(matrix)
    }

    fun setSuppMatrix(matrix: Matrix?): Boolean {
        return attacher!!.setDisplayMatrix(matrix)
    }

    var minimumScale: Float
        get() = attacher!!.minimumScale
        set(minimumScale) {
            attacher!!.minimumScale = minimumScale
        }
    var mediumScale: Float
        get() = attacher!!.mediumScale
        set(mediumScale) {
            attacher!!.mediumScale = mediumScale
        }
    var maximumScale: Float
        get() = attacher!!.maximumScale
        set(maximumScale) {
            attacher!!.maximumScale = maximumScale
        }
    var scale: Float
        get() = attacher!!.scale
        set(scale) {
            attacher!!.scale = scale
        }

    fun setAllowParentInterceptOnEdge(allow: Boolean) {
        attacher!!.setAllowParentInterceptOnEdge(allow)
    }

    fun setScaleLevels(minimumScale: Float, mediumScale: Float, maximumScale: Float) {
        attacher!!.setScaleLevels(minimumScale, mediumScale, maximumScale)
    }

    fun setOnMatrixChangeListener(listener: OnMatrixChangedListener?) {
        attacher!!.setOnMatrixChangeListener(listener)
    }

    fun setOnPhotoTapListener(listener: OnPhotoTapListener?) {
        attacher!!.setOnPhotoTapListener(listener)
    }

    fun setOnOutsidePhotoTapListener(listener: OnOutsidePhotoTapListener?) {
        attacher!!.setOnOutsidePhotoTapListener(listener)
    }

    fun setOnViewTapListener(listener: OnViewTapListener?) {
        attacher!!.setOnViewTapListener(listener)
    }

    fun setOnViewDragListener(listener: OnViewDragListener?) {
        attacher!!.setOnViewDragListener(listener)
    }

    fun setScale(scale: Float, animate: Boolean) {
        attacher!!.setScale(scale, animate)
    }

    fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        attacher!!.setScale(scale, focalX, focalY, animate)
    }

    fun setZoomTransitionDuration(milliseconds: Int) {
        attacher!!.setZoomTransitionDuration(milliseconds)
    }

    fun setOnDoubleTapListener(onDoubleTapListener: GestureDetector.OnDoubleTapListener?) {
        attacher!!.setOnDoubleTapListener(onDoubleTapListener)
    }

    fun setOnScaleChangeListener(onScaleChangedListener: OnScaleChangedListener?) {
        attacher!!.setOnScaleChangeListener(onScaleChangedListener)
    }

    fun setOnSingleFlingListener(onSingleFlingListener: OnSingleFlingListener?) {
        attacher!!.setOnSingleFlingListener(onSingleFlingListener)
    }


    // ---------------------------------------------------------------------------------------------
    // <비공개 메소드 공간>


    // ---------------------------------------------------------------------------------------------
    // <중첩 클래스 공간>
    interface OnGestureListener {
        fun onDrag(dx: Float, dy: Float)
        fun onFling(
            startX: Float, startY: Float, velocityX: Float,
            velocityY: Float
        )

        fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
        fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float)
    }

    interface OnMatrixChangedListener {
        fun onMatrixChanged(rect: RectF?)
    }

    interface OnOutsidePhotoTapListener {
        fun onOutsidePhotoTap(imageView: ImageView?)
    }
}