package com.faisal.pdfengine.listener

import android.graphics.Canvas

/**
 * Lets an external class draw on top of the PDFView canvas, above the rendered pages.
 * The page origin is (0,0).
 */
fun interface OnDrawListener {
    fun onLayerDrawn(canvas: Canvas, pageWidth: Float, pageHeight: Float, displayedPage: Int)
}
