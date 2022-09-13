package com.example.prowd_android_template.custom_view.pv

internal interface OnGestureListener {
    fun onDrag(dx: Float, dy: Float)
    fun onFling(
        startX: Float, startY: Float, velocityX: Float,
        velocityY: Float
    )

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float)
}