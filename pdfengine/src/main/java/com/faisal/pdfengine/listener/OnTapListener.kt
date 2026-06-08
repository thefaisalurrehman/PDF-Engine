package com.faisal.pdfengine.listener

import android.view.MotionEvent

/** Receives a callback when the user taps the view. */
fun interface OnTapListener {
    /** @return true if the tap was handled, false to let the scroll handle toggle visibility */
    fun onTap(event: MotionEvent): Boolean
}
