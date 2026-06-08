package com.faisal.pdfengine.listener

/** Receives a callback once the PDF document has finished loading. */
fun interface OnLoadCompleteListener {
    /** @param pageCount the number of pages in the loaded document */
    fun loadComplete(pageCount: Int)
}
