package com.faisal.pdfengine

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.PointF
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller

private const val ANIMATION_DURATION_MS = 400L

/**
 * Drives every animation [PDFView] performs (panning, zooming, flinging) using [ValueAnimator]
 * for smooth pans/zooms and [OverScroller] for fling physics; each update is forwarded to the view.
 */
internal class AnimationManager(private val pdfView: PDFView) {

    private val scroller = OverScroller(pdfView.context)
    private var animation: ValueAnimator? = null

    private var flinging = false
    private var pageFlinging = false

    fun startXAnimation(xFrom: Float, xTo: Float) {
        stopAll()
        animation = ValueAnimator.ofFloat(xFrom, xTo).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                pdfView.moveTo(offset, pdfView.currentYOffset)
                pdfView.loadPageByOffset()
            }
            addListener(panAnimationListener())
            duration = ANIMATION_DURATION_MS
            start()
        }
    }

    fun startYAnimation(yFrom: Float, yTo: Float) {
        stopAll()
        animation = ValueAnimator.ofFloat(yFrom, yTo).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val offset = it.animatedValue as Float
                pdfView.moveTo(pdfView.currentXOffset, offset)
                pdfView.loadPageByOffset()
            }
            addListener(panAnimationListener())
            duration = ANIMATION_DURATION_MS
            start()
        }
    }

    fun startZoomAnimation(centerX: Float, centerY: Float, zoomFrom: Float, zoomTo: Float) {
        stopAll()
        val center = PointF(centerX, centerY)
        animation = ValueAnimator.ofFloat(zoomFrom, zoomTo).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener { pdfView.zoomCenteredTo(it.animatedValue as Float, center) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    pdfView.loadPages()
                    hideHandle()
                }

                override fun onAnimationEnd(animation: Animator) {
                    pdfView.loadPages()
                    pdfView.performPageSnap()
                    hideHandle()
                }
            })
            duration = ANIMATION_DURATION_MS
            start()
        }
    }

    fun startFlingAnimation(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
        stopAll()
        flinging = true
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    fun startPageFlingAnimation(targetOffset: Float) {
        if (pdfView.isSwipeVertical) {
            startYAnimation(pdfView.currentYOffset, targetOffset)
        } else {
            startXAnimation(pdfView.currentXOffset, targetOffset)
        }
        pageFlinging = true
    }

    fun computeFling() {
        if (scroller.computeScrollOffset()) {
            pdfView.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
            pdfView.loadPageByOffset()
        } else if (flinging) { // fling finished
            flinging = false
            pdfView.loadPages()
            hideHandle()
            pdfView.performPageSnap()
        }
    }

    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFling()
    }

    fun stopFling() {
        flinging = false
        scroller.forceFinished(true)
    }

    fun isFlinging(): Boolean = flinging || pageFlinging

    /** Shared cancel/end behavior for the X and Y pan animations. */
    private fun panAnimationListener() = object : AnimatorListenerAdapter() {
        override fun onAnimationCancel(animation: Animator) = onPanAnimationStopped()
        override fun onAnimationEnd(animation: Animator) = onPanAnimationStopped()
    }

    private fun onPanAnimationStopped() {
        pdfView.loadPages()
        pageFlinging = false
        hideHandle()
    }

    private fun hideHandle() {
        pdfView.scrollHandle?.hideDelayed()
    }
}
