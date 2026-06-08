package com.faisal.pdfengine.listener

/** Receives a callback on every scroll movement. */
fun interface OnPageScrollListener {
    /** @param page current page index, [positionOffset] offset of the current page, see [com.faisal.pdfengine.PDFView.getPositionOffset] */
    fun onPageScrolled(page: Int, positionOffset: Float)
}
