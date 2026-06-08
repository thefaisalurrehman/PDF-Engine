package com.faisal.pdfengine.listener

/** Receives a callback when a single page fails to render. */
fun interface OnPageErrorListener {
    fun onPageError(page: Int, error: Throwable)
}
