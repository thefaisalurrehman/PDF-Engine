package com.faisal.pdfengine.listener

import android.view.MotionEvent

/** Receives a callback when the user long-presses the view. */
fun interface OnLongPressListener {
    fun onLongPress(event: MotionEvent)
}
