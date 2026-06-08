package com.faisal.pdfengine.listener

/** Receives a one-time callback once the document has been initially rendered. */
fun interface OnRenderListener {
    fun onInitiallyRendered(pageCount: Int)
}
