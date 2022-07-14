package com.example.prowd_android_template.custom_view.pv

import android.content.Context
import android.graphics.Matrix
import kotlin.jvm.JvmOverloads
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import java.lang.Exception
import java.lang.IllegalArgumentException

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
    // <오버라이딩 공간>
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

    interface OnPhotoTapListener {
        fun onPhotoTap(view: ImageView?, x: Float, y: Float)
    }

    interface OnScaleChangedListener {
        fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
    }

    interface OnSingleFlingListener {
        fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean
    }

    interface OnViewDragListener {
        fun onDrag(dx: Float, dy: Float)
    }

    interface OnViewTapListener {
        fun onViewTap(view: View?, x: Float, y: Float)
    }

    class CustomGestureDetector(context: Context?, listener: OnGestureListener) {
        private var mActivePointerId = INVALID_POINTER_ID
        private var mActivePointerIndex = 0
        private val mDetector: ScaleGestureDetector
        private var mVelocityTracker: VelocityTracker? = null
        var isDragging = false
            private set
        private var mLastTouchX = 0f
        private var mLastTouchY = 0f
        private val mTouchSlop: Float
        private val mMinimumVelocity: Float
        private val mListener: OnGestureListener
        private fun getActiveX(ev: MotionEvent): Float {
            return try {
                ev.getX(mActivePointerIndex)
            } catch (e: Exception) {
                ev.x
            }
        }

        private fun getActiveY(ev: MotionEvent): Float {
            return try {
                ev.getY(mActivePointerIndex)
            } catch (e: Exception) {
                ev.y
            }
        }

        val isScaling: Boolean
            get() = mDetector.isInProgress

        fun onTouchEvent(ev: MotionEvent): Boolean {
            return try {
                mDetector.onTouchEvent(ev)
                processTouchEvent(ev)
            } catch (e: IllegalArgumentException) {
                true
            }
        }

        private fun processTouchEvent(ev: MotionEvent): Boolean {
            val action = ev.action
            when (action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mActivePointerId = ev.getPointerId(0)
                    mVelocityTracker = VelocityTracker.obtain()
                    if (null != mVelocityTracker) {
                        mVelocityTracker!!.addMovement(ev)
                    }
                    mLastTouchX = getActiveX(ev)
                    mLastTouchY = getActiveY(ev)
                    isDragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = getActiveX(ev)
                    val y = getActiveY(ev)
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY
                    if (!isDragging) {
                        isDragging = Math.sqrt((dx * dx + dy * dy).toDouble()) >= mTouchSlop
                    }
                    if (isDragging) {
                        mListener.onDrag(dx, dy)
                        mLastTouchX = x
                        mLastTouchY = y
                        if (null != mVelocityTracker) {
                            mVelocityTracker!!.addMovement(ev)
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    mActivePointerId = INVALID_POINTER_ID
                    if (null != mVelocityTracker) {
                        mVelocityTracker!!.recycle()
                        mVelocityTracker = null
                    }
                }
                MotionEvent.ACTION_UP -> {
                    mActivePointerId = INVALID_POINTER_ID
                    if (isDragging) {
                        if (null != mVelocityTracker) {
                            mLastTouchX = getActiveX(ev)
                            mLastTouchY = getActiveY(ev)

                            mVelocityTracker!!.addMovement(ev)
                            mVelocityTracker!!.computeCurrentVelocity(1000)
                            val vX = mVelocityTracker!!.xVelocity
                            val vY = mVelocityTracker!!
                                .yVelocity

                            if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity) {
                                mListener.onFling(
                                    mLastTouchX, mLastTouchY, -vX,
                                    -vY
                                )
                            }
                        }
                    }

                    if (null != mVelocityTracker) {
                        mVelocityTracker!!.recycle()
                        mVelocityTracker = null
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = getPointerIndex(ev.action)
                    val pointerId = ev.getPointerId(pointerIndex)
                    if (pointerId == mActivePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mActivePointerId = ev.getPointerId(newPointerIndex)
                        mLastTouchX = ev.getX(newPointerIndex)
                        mLastTouchY = ev.getY(newPointerIndex)
                    }
                }
            }
            mActivePointerIndex = ev
                .findPointerIndex(if (mActivePointerId != INVALID_POINTER_ID) mActivePointerId else 0)
            return true
        }

        fun getPointerIndex(action: Int): Int {
            return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
        }

        companion object {
            private const val INVALID_POINTER_ID = -1
        }

        init {
            val configuration = ViewConfiguration
                .get(context)
            mMinimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
            mTouchSlop = configuration.scaledTouchSlop.toFloat()
            mListener = listener
            val mScaleListener: ScaleGestureDetector.OnScaleGestureListener = object :
                ScaleGestureDetector.OnScaleGestureListener {
                private var lastFocusX = 0f
                private var lastFocusY = 0f
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    if (java.lang.Float.isNaN(scaleFactor) || java.lang.Float.isInfinite(scaleFactor)) return false
                    if (scaleFactor >= 0) {
                        mListener.onScale(
                            scaleFactor,
                            detector.focusX,
                            detector.focusY,
                            detector.focusX - lastFocusX,
                            detector.focusY - lastFocusY
                        )
                        lastFocusX = detector.focusX
                        lastFocusY = detector.focusY
                    }
                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    lastFocusX = detector.focusX
                    lastFocusY = detector.focusY
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                }
            }
            mDetector = ScaleGestureDetector(context, mScaleListener)
        }
    }
}