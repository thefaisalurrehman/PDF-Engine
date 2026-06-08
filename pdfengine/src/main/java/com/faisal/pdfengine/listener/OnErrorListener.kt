package com.faisal.pdfengine.listener

/** Receives a callback when the document fails to load. */
fun interface OnErrorListener {
    fun onError(error: Throwable)
}
