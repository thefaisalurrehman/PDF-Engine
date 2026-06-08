package com.faisal.pdfengine

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.faisal.pdfengine.exception.PageRenderingException
import com.faisal.pdfengine.model.PagePart
import kotlin.math.roundToInt

/** Renders [PagePart] tiles off the main thread and posts the results back to [PDFView]. */
internal class RenderingHandler(looper: Looper, private val pdfView: PDFView) : Handler(looper) {

    private val renderBounds = RectF()
    private val roundedRenderBounds = Rect()
    private val renderMatrix = Matrix()
    private var running = false

    fun addRenderingTask(
        page: Int,
        width: Float,
        height: Float,
        bounds: RectF,
        thumbnail: Boolean,
        cacheOrder: Int,
        bestQuality: Boolean,
        annotationRendering: Boolean,
    ) {
        val task = RenderingTask(width, height, bounds, page, thumbnail, cacheOrder, bestQuality, annotationRendering)
        sendMessage(obtainMessage(MSG_RENDER_TASK, task))
    }

    override fun handleMessage(message: Message) {
        val task = message.obj as RenderingTask
        try {
            val part = render(task) ?: return
            if (running) {
                pdfView.post { pdfView.onBitmapRendered(part) }
            } else {
                part.renderedBitmap?.recycle()
            }
        } catch (ex: PageRenderingException) {
            pdfView.post { pdfView.onPageError(ex) }
        }
    }

    private fun render(task: RenderingTask): PagePart? {
        val pdfFile = pdfView.pdfFile ?: return null
        pdfFile.openPage(task.page)

        val w = task.width.roundToInt()
        val h = task.height.roundToInt()

        if (w == 0 || h == 0 || pdfFile.pageHasError(task.page)) return null

        val render: Bitmap = try {
            Bitmap.createBitmap(w, h, if (task.bestQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Cannot create bitmap", e)
            return null
        }
        calculateBounds(w, h, task.bounds)

        pdfFile.renderPageBitmap(render, task.page, roundedRenderBounds, task.annotationRendering)

        return PagePart(task.page, render, task.bounds, task.thumbnail, task.cacheOrder)
    }

    private fun calculateBounds(width: Int, height: Int, pageSliceBounds: RectF) {
        renderMatrix.reset()
        renderMatrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height)
        renderMatrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height())

        renderBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        renderMatrix.mapRect(renderBounds)
        renderBounds.round(roundedRenderBounds)
    }

    fun stop() {
        running = false
    }

    fun start() {
        running = true
    }

    private class RenderingTask(
        val width: Float,
        val height: Float,
        val bounds: RectF,
        val page: Int,
        val thumbnail: Boolean,
        val cacheOrder: Int,
        val bestQuality: Boolean,
        val annotationRendering: Boolean,
    )

    companion object {
        /** [Message.what] kind of message this handler processes. */
        const val MSG_RENDER_TASK = 1
        private const val TAG = "RenderingHandler"
    }
}
