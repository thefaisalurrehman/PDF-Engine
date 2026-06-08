package com.faisal.pdfengine.listener

/** Receives a callback whenever the zoom level changes. */
fun interface OnZoomChangedListener {
    fun onZoomChanged(zoom: Float)
}
