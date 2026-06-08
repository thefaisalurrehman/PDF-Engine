package com.faisal.pdfengine.listener

import android.view.MotionEvent
import com.faisal.pdfengine.link.LinkHandler
import com.faisal.pdfengine.model.LinkTapEvent

/** Holds and dispatches every listener a [com.faisal.pdfengine.PDFView] can be configured with. */
class Callbacks {

    private var onLoadCompleteListener: OnLoadCompleteListener? = null
    private var onErrorListener: OnErrorListener? = null
    private var onPageErrorListener: OnPageErrorListener? = null
    private var onRenderListener: OnRenderListener? = null
    private var onPageChangeListener: OnPageChangeListener? = null
    private var onZoomChangedListener: OnZoomChangedListener? = null
    private var onPageScrollListener: OnPageScrollListener? = null
    private var onDrawListener: OnDrawListener? = null
    private var onDrawAllListener: OnDrawListener? = null
    private var onTapListener: OnTapListener? = null
    private var onLongPressListener: OnLongPressListener? = null
    private var linkHandler: LinkHandler? = null

    fun setOnLoadComplete(listener: OnLoadCompleteListener?) {
        onLoadCompleteListener = listener
    }

    fun callOnLoadComplete(pageCount: Int) {
        onLoadCompleteListener?.loadComplete(pageCount)
    }

    fun setOnError(listener: OnErrorListener?) {
        onErrorListener = listener
    }

    fun getOnError(): OnErrorListener? = onErrorListener

    fun setOnPageError(listener: OnPageErrorListener?) {
        onPageErrorListener = listener
    }

    fun callOnPageError(page: Int, error: Throwable): Boolean {
        val listener = onPageErrorListener ?: return false
        listener.onPageError(page, error)
        return true
    }

    fun setOnRender(listener: OnRenderListener?) {
        onRenderListener = listener
    }

    fun callOnRender(pageCount: Int) {
        onRenderListener?.onInitiallyRendered(pageCount)
    }

    fun setOnPageChange(listener: OnPageChangeListener?) {
        onPageChangeListener = listener
    }

    fun callOnPageChange(page: Int, pageCount: Int) {
        onPageChangeListener?.onPageChanged(page, pageCount)
    }

    fun setOnZoomChangedListener(listener: OnZoomChangedListener?) {
        onZoomChangedListener = listener
    }

    fun callOnZoomChanged(zoom: Float) {
        onZoomChangedListener?.onZoomChanged(zoom)
    }

    fun setOnPageScroll(listener: OnPageScrollListener?) {
        onPageScrollListener = listener
    }

    fun callOnPageScroll(page: Int, offset: Float) {
        onPageScrollListener?.onPageScrolled(page, offset)
    }

    fun setOnDraw(listener: OnDrawListener?) {
        onDrawListener = listener
    }

    fun getOnDraw(): OnDrawListener? = onDrawListener

    fun setOnDrawAll(listener: OnDrawListener?) {
        onDrawAllListener = listener
    }

    fun getOnDrawAll(): OnDrawListener? = onDrawAllListener

    fun setOnTap(listener: OnTapListener?) {
        onTapListener = listener
    }

    fun callOnTap(event: MotionEvent): Boolean = onTapListener?.onTap(event) ?: false

    fun setOnLongPress(listener: OnLongPressListener?) {
        onLongPressListener = listener
    }

    fun callOnLongPress(event: MotionEvent) {
        onLongPressListener?.onLongPress(event)
    }

    fun setLinkHandler(handler: LinkHandler?) {
        linkHandler = handler
    }

    fun callLinkHandler(event: LinkTapEvent) {
        linkHandler?.handleLinkEvent(event)
    }
}
