package com.faisal.pdfengine.listener

/** Receives a callback whenever the visible page changes (e.g. through swipe). */
fun interface OnPageChangeListener {
    /** @param page the new page index (zero-based), [pageCount] the total page count */
    fun onPageChanged(page: Int, pageCount: Int)
}
