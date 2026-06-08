package com.faisal.pdfengine.scroll

import com.faisal.pdfengine.PDFView

/** A draggable indicator showing scroll position, attachable to a [PDFView]. */
interface ScrollHandle {

    /** Moves the handle. Called internally by [PDFView]. @param position scroll ratio between 0 and 1 */
    fun setScroll(position: Float)

    /**
     * Called by [PDFView] right after the handle is attached. Do not call manually.
     * See [DefaultScrollHandle] for a usage sample.
     */
    fun setupLayout(pdfView: PDFView)

    /** Called by [PDFView] when the handle should be removed from the layout. Do not call manually. */
    fun destroyLayout()

    /** Sets the page number displayed on the handle. */
    fun setPageNum(pageNum: Int)

    /** @return true if the handle is visible. */
    fun shown(): Boolean

    fun show()

    fun hide()

    /** Hides the handle after a delay defined by the implementation. */
    fun hideDelayed()
}
