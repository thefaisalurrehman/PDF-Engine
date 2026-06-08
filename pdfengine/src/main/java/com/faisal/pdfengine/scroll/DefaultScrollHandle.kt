package com.faisal.pdfengine.scroll

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.faisal.pdfengine.PDFView
import com.faisal.pdfengine.R
import com.faisal.pdfengine.util.Util

/** Default [ScrollHandle] implementation: a small draggable tab showing the current page number. */
open class DefaultScrollHandle @JvmOverloads constructor(
    context: Context,
    private val inverted: Boolean = false,
) : RelativeLayout(context), ScrollHandle {

    private var relativeHandlerMiddle = 0f
    protected val textView: TextView = TextView(context)
    private var pdfView: PDFView? = null
    private var currentPos = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val hidePageScrollerRunnable = Runnable { hide() }

    init {
        visibility = INVISIBLE
        setTextColor(Color.BLACK)
        setTextSize(DEFAULT_TEXT_SIZE)
    }

    override fun setupLayout(pdfView: PDFView) {
        val align: Int
        val width: Int
        val height: Int
        val background: android.graphics.drawable.Drawable?
        if (pdfView.isSwipeVertical) {
            width = 21
            height = 34
            align = if (inverted) ALIGN_PARENT_LEFT else ALIGN_PARENT_RIGHT
            background = ContextCompat.getDrawable(context, R.drawable.scroll_handle_vertical)
        } else {
            width = 35
            height = 22
            align = if (inverted) ALIGN_PARENT_TOP else ALIGN_PARENT_BOTTOM
            background = ContextCompat.getDrawable(context, R.drawable.scroll_handle_horizontal)
        }

        setBackground(background)

        val lp = LayoutParams(Util.getDP(context, width), Util.getDP(context, height))
        lp.setMargins(10, 10, 10, 10)
        lp.addRule(align)

        (parent as? ViewGroup)?.removeView(this)

        pdfView.addView(this, lp)
        this.pdfView = pdfView
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        if (!shown()) {
            show()
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable)
        }
        pdfView?.let { setPosition((if (it.isSwipeVertical) it.height else it.width) * position) }
    }

    private fun setPosition(rawPos: Float) {
        if (rawPos.isInfinite() || rawPos.isNaN()) return
        val view = pdfView ?: return

        val pdfViewSize = if (view.isSwipeVertical) view.height else view.width
        var pos = rawPos - relativeHandlerMiddle

        val maxPos = pdfViewSize - Util.getDP(context, 22)
        pos = pos.coerceIn(0f, maxPos.toFloat())

        if (view.isSwipeVertical) y = pos else x = pos

        calculateMiddle()
        invalidate()
    }

    private fun calculateMiddle() {
        val view = pdfView ?: return
        val pos: Float
        val viewSize: Float
        val pdfViewSize: Float
        if (view.isSwipeVertical) {
            pos = y
            viewSize = height.toFloat()
            pdfViewSize = view.height.toFloat()
        } else {
            pos = x
            viewSize = width.toFloat()
            pdfViewSize = view.width.toFloat()
        }
        relativeHandlerMiddle = (pos + relativeHandlerMiddle) / pdfViewSize * viewSize
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        val text = pageNum.toString()
        if (textView.text != text) {
            textView.text = text
        }
    }

    override fun shown(): Boolean = visibility == VISIBLE

    override fun show() {
        visibility = VISIBLE
    }

    override fun hide() {
        visibility = INVISIBLE
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    /** @param size text size in dp */
    fun setTextSize(size: Int) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
    }

    private fun isPdfViewReady(): Boolean {
        val view = pdfView ?: return false
        return view.pageCount > 0 && !view.documentFitsView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val view = pdfView
        if (view == null || !isPdfViewReady()) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                view.stopFling()
                handler.removeCallbacks(hidePageScrollerRunnable)
                currentPos = if (view.isSwipeVertical) event.rawY - y else event.rawX - x
                return updateFromTouch(view, event)
            }
            MotionEvent.ACTION_MOVE -> return updateFromTouch(view, event)
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed()
                view.performPageSnap()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(view: PDFView, event: MotionEvent): Boolean {
        if (view.isSwipeVertical) {
            setPosition(event.rawY - currentPos + relativeHandlerMiddle)
            view.setPositionOffset(relativeHandlerMiddle / height, false)
        } else {
            setPosition(event.rawX - currentPos + relativeHandlerMiddle)
            view.setPositionOffset(relativeHandlerMiddle / width, false)
        }
        return true
    }

    private companion object {
        const val DEFAULT_TEXT_SIZE = 16
    }
}
