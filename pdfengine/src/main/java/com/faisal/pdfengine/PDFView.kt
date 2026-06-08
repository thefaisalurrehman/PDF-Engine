/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.faisal.pdfengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.widget.RelativeLayout
import com.faisal.pdfengine.exception.PageRenderingException
import com.faisal.pdfengine.link.DefaultLinkHandler
import com.faisal.pdfengine.link.LinkHandler
import com.faisal.pdfengine.listener.Callbacks
import com.faisal.pdfengine.listener.OnDrawListener
import com.faisal.pdfengine.listener.OnErrorListener
import com.faisal.pdfengine.listener.OnLoadCompleteListener
import com.faisal.pdfengine.listener.OnLongPressListener
import com.faisal.pdfengine.listener.OnPageChangeListener
import com.faisal.pdfengine.listener.OnPageErrorListener
import com.faisal.pdfengine.listener.OnPageScrollListener
import com.faisal.pdfengine.listener.OnRenderListener
import com.faisal.pdfengine.listener.OnTapListener
import com.faisal.pdfengine.listener.OnZoomChangedListener
import com.faisal.pdfengine.model.PagePart
import com.faisal.pdfengine.scroll.ScrollHandle
import com.faisal.pdfengine.source.AssetSource
import com.faisal.pdfengine.source.ByteArraySource
import com.faisal.pdfengine.source.DocumentSource
import com.faisal.pdfengine.source.FileSource
import com.faisal.pdfengine.source.InputStreamSource
import com.faisal.pdfengine.source.UriSource
import com.faisal.pdfengine.util.Constants
import com.faisal.pdfengine.util.FitPolicy
import com.faisal.pdfengine.util.MathUtils
import com.faisal.pdfengine.util.SnapEdge
import com.faisal.pdfengine.util.Util
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import java.io.File
import java.io.InputStream

/**
 * The PDFView can be used as a normal layout.
 *
 * It renders a PDF document fragment by fragment, with each fragment displayed as a bitmap tile,
 * and supports pinch-zoom, swipe and animated navigation. Build it with one of the `from...`
 * factory methods and finish the chain with [Configurator.load].
 */
class PDFView(context: Context, set: AttributeSet?) : RelativeLayout(context, set) {

    var minZoom = DEFAULT_MIN_SCALE
    var midZoom = DEFAULT_MID_SCALE
    var maxZoom = DEFAULT_MAX_SCALE

    /**
     * START - scrolling in first page direction
     * END - scrolling in last page direction
     * NONE - not scrolling
     */
    private enum class ScrollDir { NONE, START, END }

    private var scrollDir = ScrollDir.NONE

    /** Rendered parts go to the cache manager. */
    internal val cacheManager = CacheManager()

    /** Animation manager manages all offset and zoom animations. */
    private val animationManager = AnimationManager(this)

    /** Drag manager manages all touch events. */
    private val dragPinchManager = DragPinchManager(this, AnimationManager(this).let { animationManager })

    internal var pdfFile: PdfFile? = null

    /** The index of the current page. */
    var currentPage = 0
        private set

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture.
     */
    var currentXOffset = 0f
        private set

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the top border of the screen in this big picture.
     */
    var currentYOffset = 0f
        private set

    /** The zoom level, always >= 1. */
    var zoom = 1f
        private set

    /** True if the PDFView has been recycled. */
    private var recycled = true

    /** Current state of the view. */
    private var state = State.DEFAULT

    /** Coroutine-backed decoder used during the loading phase to decode a PDF document. */
    private var documentDecoder: DocumentDecoder? = null

    /** The thread [renderingHandler] will run on. */
    private var renderingHandlerThread: HandlerThread? = HandlerThread("PDF renderer")

    /** Handler always waiting in the background and rendering tasks. */
    internal var renderingHandler: RenderingHandler? = null

    private val pagesLoader = PagesLoader(this)

    internal var callbacks = Callbacks()

    /** Paint object used for drawing rendered tiles. */
    private val paint = Paint()

    /** Paint object used for drawing debug overlays. */
    private val debugPaint = Paint().apply { style = Style.STROKE }

    /** Policy used for fitting pages to the screen. */
    var pageFitPolicy = FitPolicy.WIDTH
        private set

    var isFitEachPage = false
        private set

    private var defaultPage = 0

    /** True if pages should scroll vertically instead of horizontally. */
    var isSwipeVertical = true
        private set

    var isSwipeEnabled = true

    var isDoubletapEnabled = true

    private var isNightMode = false

    var isPageSnap = true

    /** Pdfium core for loading and rendering PDFs. */
    private val pdfiumCore = PdfiumCore(context)

    internal var scrollHandle: ScrollHandle? = null
        private set

    private var isScrollHandleInit = false

    /**
     * True if bitmaps should use ARGB_8888 format and take more memory,
     * false if bitmaps should be compressed using RGB_565 format and take less memory.
     */
    var isBestQuality = false

    /** True if annotations should be rendered. */
    var isAnnotationRendering = false
        private set

    /**
     * True if the view should render during scaling. Cannot be forced on older API versions
     * (< [Build.VERSION_CODES.KITKAT]) as the GestureDetector does not detect scrolling while scaling.
     */
    private var renderDuringScale = false

    /** Antialiasing and bitmap filtering. */
    var isAntialiasing = true
    private val antialiasFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Spacing between pages, in px. */
    var spacingPx = 0
        private set

    /** True to add dynamic spacing so each page fits separately on the screen. */
    var isAutoSpacingEnabled = false
        private set

    /** True to fling a single page at a time. */
    var isPageFlingEnabled = true

    /** Page numbers used when calling the "draw all" listener. */
    private val onDrawPagesNums = ArrayList<Int>(10)

    /** True once the view has been added to the layout and has a width and height. */
    private var hasSize = false

    /** Holds the last used [Configurator] that should be loaded once the view has a size. */
    private var waitingDocumentConfigurator: Configurator? = null

    init {
        if (!isInEditMode) {
            setWillNotDraw(false)
        }
    }

    private fun load(docSource: DocumentSource, password: String?, userPages: IntArray? = null) {
        check(recycled) { "Don't call load on a PDF View without recycling it first." }

        recycled = false
        // Start decoding document
        documentDecoder = DocumentDecoder(docSource, password, userPages, this, pdfiumCore).apply { start() }
    }

    /** Go to the given page. */
    @JvmOverloads
    fun jumpTo(page: Int, withAnimation: Boolean = false) {
        val pdfFile = pdfFile ?: return

        val targetPage = pdfFile.determineValidPageNumberFrom(page)
        val offset = if (targetPage == 0) 0f else -pdfFile.getPageOffset(targetPage, zoom)
        if (isSwipeVertical) {
            if (withAnimation) {
                animationManager.startYAnimation(currentYOffset, offset)
            } else {
                moveTo(currentXOffset, offset)
            }
        } else {
            if (withAnimation) {
                animationManager.startXAnimation(currentXOffset, offset)
            } else {
                moveTo(offset, currentYOffset)
            }
        }
        showPage(targetPage)
    }

    internal fun showPage(pageNb: Int) {
        if (recycled) return
        val pdfFile = pdfFile ?: return

        // Check the page number and make the difference between UserPages and DocumentPages
        currentPage = pdfFile.determineValidPageNumberFrom(pageNb)

        loadPages()

        scrollHandle?.let {
            if (!documentFitsView()) it.setPageNum(currentPage + 1)
        }

        callbacks.callOnPageChange(currentPage, pdfFile.pagesCount)
    }

    /**
     * Get current position as a ratio of document length to visible area.
     * 0 means the document start is visible, 1 that the document end is visible.
     *
     * @return offset between 0 and 1
     */
    fun getPositionOffset(): Float {
        val pdfFile = pdfFile ?: return 0f
        val offset = if (isSwipeVertical) {
            -currentYOffset / (pdfFile.getDocLen(zoom) - height)
        } else {
            -currentXOffset / (pdfFile.getDocLen(zoom) - width)
        }
        return MathUtils.limit(offset, 0f, 1f)
    }

    /** @see PDFView.getPositionOffset */
    @JvmOverloads
    fun setPositionOffset(progress: Float, moveHandle: Boolean = true) {
        val pdfFile = pdfFile ?: return
        if (isSwipeVertical) {
            moveTo(currentXOffset, (-pdfFile.getDocLen(zoom) + height) * progress, moveHandle)
        } else {
            moveTo((-pdfFile.getDocLen(zoom) + width) * progress, currentYOffset, moveHandle)
        }
        loadPageByOffset()
    }

    fun stopFling() {
        animationManager.stopFling()
    }

    val pageCount: Int
        get() = pdfFile?.pagesCount ?: 0

    fun setNightMode(nightMode: Boolean) {
        isNightMode = nightMode
        paint.colorFilter = if (nightMode) {
            ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        } else {
            null
        }
    }

    internal fun onPageError(ex: PageRenderingException) {
        if (!callbacks.callOnPageError(ex.page, ex.cause ?: ex)) {
            Log.e(TAG, "Cannot open page ${ex.page}", ex.cause)
        }
    }

    fun recycle() {
        waitingDocumentConfigurator = null

        animationManager.stopAll()
        dragPinchManager.disable()

        // Stop tasks
        renderingHandler?.let {
            it.stop()
            it.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        }
        documentDecoder?.cancel()

        // Clear caches
        cacheManager.recycle()

        if (isScrollHandleInit) {
            scrollHandle?.destroyLayout()
        }

        pdfFile?.dispose()
        pdfFile = null

        renderingHandler = null
        scrollHandle = null
        isScrollHandleInit = false
        currentXOffset = 0f
        currentYOffset = 0f
        zoom = 1f
        recycled = true
        callbacks = Callbacks()
        state = State.DEFAULT
    }

    val isRecycled: Boolean
        get() = recycled

    /** Handle fling animation. */
    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) return
        animationManager.computeFling()
    }

    override fun onDetachedFromWindow() {
        recycle()
        renderingHandlerThread?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                it.quitSafely()
            } else {
                it.quit()
            }
            renderingHandlerThread = null
        }
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        waitingDocumentConfigurator?.load()
        if (isInEditMode || state != State.SHOWN) return

        val pdfFile = pdfFile ?: return

        // calculates the position of the point which is in the center of the view relative to the big strip
        val centerPointInStripXOffset = -currentXOffset + oldw * 0.5f
        val centerPointInStripYOffset = -currentYOffset + oldh * 0.5f

        val relativeCenterPointInStripXOffset: Float
        val relativeCenterPointInStripYOffset: Float

        if (isSwipeVertical) {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile.maxPageWidth
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile.getDocLen(zoom)
        } else {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile.getDocLen(zoom)
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile.maxPageHeight
        }

        animationManager.stopAll()
        pdfFile.recalculatePageSizes(Size(w, h))

        if (isSwipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.maxPageWidth + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.getDocLen(zoom) + h * 0.5f
        } else {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.getDocLen(zoom) + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.maxPageHeight + h * 0.5f
        }
        moveTo(currentXOffset, currentYOffset)
        loadPageByOffset()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val pdfFile = pdfFile ?: return true

        if (isSwipeVertical) {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + toCurrentScale(pdfFile.maxPageWidth) > width) {
                return true
            }
        } else {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + pdfFile.getDocLen(zoom) > width) {
                return true
            }
        }
        return false
    }

    override fun canScrollVertically(direction: Int): Boolean {
        val pdfFile = pdfFile ?: return true

        if (isSwipeVertical) {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + pdfFile.getDocLen(zoom) > height) {
                return true
            }
        } else {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + toCurrentScale(pdfFile.maxPageHeight) > height) {
                return true
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) return
        // As said in this class's KDoc, we can think of this canvas as a huge strip on which we
        // draw all the images. We actually only draw the rendered parts, of course, but we render
        // them in the place they belong on this huge strip.
        //
        // That's where Canvas.translate(x, y) becomes very helpful. This is the situation:
        //  _______________________________________________
        // |             |                                 |
        // | the actual  |                  The big strip  |
        // |   canvas    |                                 |
        // |_____________|                                 |
        // |_______________________________________________|
        //
        // If the rendered part is in the bottom right corner of the strip we can draw it,
        // but we won't see it because the canvas is not big enough.
        //
        // But if we call translate(-X, -Y) on the canvas just before drawing the object:
        //  _______________________________________________
        // |                                  _____________|
        // |   The big strip                 |             |
        // |                                 |  the actual |
        // |                                 |    canvas   |
        // |_________________________________|_____________|
        //
        // The object will be on the canvas. This technique is massively used in this method,
        // and allows abstraction of the screen position when rendering the parts.

        // Draws background
        if (isAntialiasing) {
            canvas.drawFilter = antialiasFilter
        }

        val bg = background
        if (bg == null) {
            canvas.drawColor(if (isNightMode) Color.BLACK else Color.WHITE)
        } else {
            bg.draw(canvas)
        }

        if (recycled) return
        if (state != State.SHOWN) return

        // Moves the canvas before drawing any element
        val currentXOffset = currentXOffset
        val currentYOffset = currentYOffset
        canvas.translate(currentXOffset, currentYOffset)

        // Draws thumbnails
        for (part in cacheManager.getThumbnails()) {
            drawPart(canvas, part)
        }

        // Draws parts
        for (part in cacheManager.getPageParts()) {
            drawPart(canvas, part)
            if (callbacks.getOnDrawAll() != null && part.page !in onDrawPagesNums) {
                onDrawPagesNums.add(part.page)
            }
        }

        for (page in onDrawPagesNums) {
            drawWithListener(canvas, page, callbacks.getOnDrawAll())
        }
        onDrawPagesNums.clear()

        drawWithListener(canvas, currentPage, callbacks.getOnDraw())

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset)
    }

    private fun drawWithListener(canvas: Canvas, page: Int, listener: OnDrawListener?) {
        val pdfFile = pdfFile ?: return
        if (listener == null) return

        val translateX: Float
        val translateY: Float
        if (isSwipeVertical) {
            translateX = 0f
            translateY = pdfFile.getPageOffset(page, zoom)
        } else {
            translateY = 0f
            translateX = pdfFile.getPageOffset(page, zoom)
        }

        canvas.translate(translateX, translateY)
        val size = pdfFile.getPageSize(page)
        listener.onLayerDrawn(canvas, toCurrentScale(size.width), toCurrentScale(size.height), page)

        canvas.translate(-translateX, -translateY)
    }

    /** Draw a given [PagePart] on the canvas. */
    private fun drawPart(canvas: Canvas, part: PagePart) {
        val pdfFile = pdfFile ?: return

        // Can seem strange, but avoids a lot of calls
        val pageRelativeBounds = part.pageRelativeBounds
        val renderedBitmap = part.renderedBitmap ?: return

        if (renderedBitmap.isRecycled) return

        // Move to the target page
        val localTranslationX: Float
        val localTranslationY: Float
        val size = pdfFile.getPageSize(part.page)

        if (isSwipeVertical) {
            localTranslationY = pdfFile.getPageOffset(part.page, zoom)
            val maxWidth = pdfFile.maxPageWidth
            localTranslationX = toCurrentScale(maxWidth - size.width) / 2
        } else {
            localTranslationX = pdfFile.getPageOffset(part.page, zoom)
            val maxHeight = pdfFile.maxPageHeight
            localTranslationY = toCurrentScale(maxHeight - size.height) / 2
        }
        canvas.translate(localTranslationX, localTranslationY)

        val srcRect = Rect(0, 0, renderedBitmap.width, renderedBitmap.height)

        val offsetX = toCurrentScale(pageRelativeBounds.left * size.width)
        val offsetY = toCurrentScale(pageRelativeBounds.top * size.height)
        val width = toCurrentScale(pageRelativeBounds.width() * size.width)
        val height = toCurrentScale(pageRelativeBounds.height() * size.height)

        // If we use float values for this rectangle, there will be a possible gap between page
        // parts, especially when the zoom level is high.
        val dstRect = RectF(
            offsetX.toInt().toFloat(),
            offsetY.toInt().toFloat(),
            (offsetX + width).toInt().toFloat(),
            (offsetY + height).toInt().toFloat(),
        )

        // Check if the bitmap is on the screen
        val translationX = currentXOffset + localTranslationX
        val translationY = currentYOffset + localTranslationY
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0 ||
            translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0
        ) {
            canvas.translate(-localTranslationX, -localTranslationY)
            return
        }

        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint)

        if (Constants.DEBUG_MODE) {
            debugPaint.color = if (part.page % 2 == 0) Color.RED else Color.BLUE
            canvas.drawRect(dstRect, debugPaint)
        }

        // Restore the canvas position
        canvas.translate(-localTranslationX, -localTranslationY)
    }

    /**
     * Load all the parts around the center of the screen, taking into account X and Y offsets,
     * the zoom level, and the page currently displayed.
     */
    fun loadPages() {
        if (pdfFile == null || renderingHandler == null) return

        // Cancel all current tasks
        renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        cacheManager.makeNewSet()

        pagesLoader.loadPages()
        redraw()
    }

    /** Called when the PDF is loaded. */
    internal fun loadComplete(pdfFile: PdfFile) {
        state = State.LOADED

        this.pdfFile = pdfFile

        val renderingHandlerThread = renderingHandlerThread ?: return
        if (!renderingHandlerThread.isAlive) {
            renderingHandlerThread.start()
        }
        renderingHandler = RenderingHandler(renderingHandlerThread.looper, this).also { it.start() }

        scrollHandle?.let {
            it.setupLayout(this)
            isScrollHandleInit = true
        }

        dragPinchManager.enable()

        callbacks.callOnLoadComplete(pdfFile.pagesCount)

        jumpTo(defaultPage, false)
    }

    internal fun loadError(t: Throwable) {
        state = State.ERROR
        // store reference, because callbacks will be cleared in recycle()
        val onErrorListener = callbacks.getOnError()
        recycle()
        invalidate()
        if (onErrorListener != null) {
            onErrorListener.onError(t)
        } else {
            Log.e("PDFView", "load pdf error", t)
        }
    }

    internal fun redraw() {
        invalidate()
    }

    /**
     * Called when a rendering task is over and a [PagePart] has been freshly created.
     *
     * @param part the created PagePart
     */
    internal fun onBitmapRendered(part: PagePart) {
        val pdfFile = pdfFile ?: return

        // when it is the first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN
            callbacks.callOnRender(pdfFile.pagesCount)
        }

        if (part.isThumbnail) {
            cacheManager.cacheThumbnail(part)
        } else {
            cacheManager.cachePart(part)
        }
        redraw()
    }

    /**
     * Move to the given X and Y offsets, but check them ahead of time to be sure not to go
     * outside the big strip.
     *
     * @param offsetX    the big strip X offset to use as the left border of the screen
     * @param offsetY    the big strip Y offset to use as the top border of the screen
     * @param moveHandle whether to move the scroll handle or not
     */
    @JvmOverloads
    fun moveTo(offsetX: Float, offsetY: Float, moveHandle: Boolean = true) {
        val pdfFile = pdfFile ?: return
        var offsetX = offsetX
        var offsetY = offsetY

        if (isSwipeVertical) {
            // Check X offset
            val scaledPageWidth = toCurrentScale(pdfFile.maxPageWidth)
            offsetX = if (scaledPageWidth < width) {
                width / 2 - scaledPageWidth / 2
            } else {
                if (offsetX > 0) {
                    0f
                } else if (offsetX + scaledPageWidth < width) {
                    width - scaledPageWidth
                } else {
                    offsetX
                }
            }

            // Check Y offset
            val contentHeight = pdfFile.getDocLen(zoom)
            offsetY = if (contentHeight < height) { // whole document height visible on screen
                (height - contentHeight) / 2
            } else {
                if (offsetY > 0) { // top visible
                    0f
                } else if (offsetY + contentHeight < height) { // bottom visible
                    -contentHeight + height
                } else {
                    offsetY
                }
            }

            scrollDir = when {
                offsetY < currentYOffset -> ScrollDir.END
                offsetY > currentYOffset -> ScrollDir.START
                else -> ScrollDir.NONE
            }
        } else {
            // Check Y offset
            val scaledPageHeight = toCurrentScale(pdfFile.maxPageHeight)
            offsetY = if (scaledPageHeight < height) {
                height / 2 - scaledPageHeight / 2
            } else {
                if (offsetY > 0) {
                    0f
                } else if (offsetY + scaledPageHeight < height) {
                    height - scaledPageHeight
                } else {
                    offsetY
                }
            }

            // Check X offset
            val contentWidth = pdfFile.getDocLen(zoom)
            offsetX = if (contentWidth < width) { // whole document width visible on screen
                (width - contentWidth) / 2
            } else {
                if (offsetX > 0) { // left visible
                    0f
                } else if (offsetX + contentWidth < width) { // right visible
                    -contentWidth + width
                } else {
                    offsetX
                }
            }

            scrollDir = when {
                offsetX < currentXOffset -> ScrollDir.END
                offsetX > currentXOffset -> ScrollDir.START
                else -> ScrollDir.NONE
            }
        }

        currentXOffset = offsetX
        currentYOffset = offsetY
        val positionOffset = getPositionOffset()

        if (moveHandle && !documentFitsView()) {
            scrollHandle?.setScroll(positionOffset)
        }

        callbacks.callOnPageScroll(currentPage, positionOffset)

        redraw()
    }

    internal fun loadPageByOffset() {
        val pdfFile = pdfFile ?: return
        if (pdfFile.pagesCount == 0) return

        val offset: Float
        val screenCenter: Float
        if (isSwipeVertical) {
            offset = currentYOffset
            screenCenter = height / 2f
        } else {
            offset = currentXOffset
            screenCenter = width / 2f
        }

        val page = pdfFile.getPageAtOffset(-(offset - screenCenter), zoom)

        if (page in 0..(pdfFile.pagesCount - 1) && page != currentPage) {
            showPage(page)
        } else {
            loadPages()
        }
    }

    /** Animate to the nearest snapping position for the current snap policy. */
    fun performPageSnap() {
        val pdfFile = pdfFile ?: return
        if (!isPageSnap || pdfFile.pagesCount == 0) return

        val centerPage = findFocusPage(currentXOffset, currentYOffset)
        val edge = findSnapEdge(centerPage)
        if (edge == SnapEdge.NONE) return

        val offset = snapOffsetForPage(centerPage, edge)
        if (isSwipeVertical) {
            animationManager.startYAnimation(currentYOffset, -offset)
        } else {
            animationManager.startXAnimation(currentXOffset, -offset)
        }
    }

    /** Find the edge to snap to when showing the specified page. */
    internal fun findSnapEdge(page: Int): SnapEdge {
        val pdfFile = pdfFile ?: return SnapEdge.NONE
        if (!isPageSnap || page < 0) return SnapEdge.NONE

        val currentOffset = if (isSwipeVertical) currentYOffset else currentXOffset
        val offset = -pdfFile.getPageOffset(page, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile.getPageLength(page, zoom)

        return when {
            length >= pageLength -> SnapEdge.CENTER
            currentOffset >= offset -> SnapEdge.START
            offset - pageLength > currentOffset - length -> SnapEdge.END
            else -> SnapEdge.NONE
        }
    }

    /** Get the offset to move to in order to snap to the page. */
    internal fun snapOffsetForPage(pageIndex: Int, edge: SnapEdge): Float {
        val pdfFile = pdfFile ?: return 0f
        var offset = pdfFile.getPageOffset(pageIndex, zoom)

        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile.getPageLength(pageIndex, zoom)

        if (edge == SnapEdge.CENTER) {
            offset = offset - length / 2f + pageLength / 2f
        } else if (edge == SnapEdge.END) {
            offset = offset - length + pageLength
        }
        return offset
    }

    internal fun findFocusPage(xOffset: Float, yOffset: Float): Int {
        val pdfFile = pdfFile ?: return 0
        val currOffset = if (isSwipeVertical) yOffset else xOffset
        val length = if (isSwipeVertical) height else width
        // make sure first and last page can be found
        return when {
            currOffset > -1 -> 0
            currOffset < -pdfFile.getDocLen(zoom) + length + 1 -> pdfFile.pagesCount - 1
            else -> {
                // else find page in center
                val center = currOffset - length / 2f
                pdfFile.getPageAtOffset(-center, zoom)
            }
        }
    }

    /** @return true if a single page fills the entire screen in the scrolling direction */
    fun pageFillsScreen(): Boolean {
        val pdfFile = pdfFile ?: return false
        val start = -pdfFile.getPageOffset(currentPage, zoom)
        val end = start - pdfFile.getPageLength(currentPage, zoom)
        return if (isSwipeVertical) {
            start > currentYOffset && end < currentYOffset - height
        } else {
            start > currentXOffset && end < currentXOffset - width
        }
    }

    /**
     * Move relatively to the current position.
     *
     * @param dx the X difference to apply
     * @param dy the Y difference to apply
     * @see PDFView.moveTo
     */
    fun moveRelativeTo(dx: Float, dy: Float) {
        moveTo(currentXOffset + dx, currentYOffset + dy)
    }

    /** Change the zoom level. */
    fun zoomTo(zoom: Float) {
        this.zoom = zoom
    }

    /**
     * Change the zoom level, relative to a pivot point. Calls [moveTo] to make sure the given
     * point stays in the middle of the screen.
     *
     * @param zoom  the zoom level
     * @param pivot the point on the screen that should stay in place
     */
    fun zoomCenteredTo(zoom: Float, pivot: PointF) {
        val dzoom = zoom / this.zoom
        zoomTo(zoom)
        var baseX = currentXOffset * dzoom
        var baseY = currentYOffset * dzoom
        baseX += pivot.x - pivot.x * dzoom
        baseY += pivot.y - pivot.y * dzoom
        moveTo(baseX, baseY)
        callbacks.callOnZoomChanged(zoom)
        Log.d(TAG, "zoomCenteredTo: $zoom")
    }

    /** @see PDFView.zoomCenteredTo */
    fun zoomCenteredRelativeTo(dzoom: Float, pivot: PointF) {
        zoomCenteredTo(zoom * dzoom, pivot)
    }

    /**
     * Checks if the whole document can be displayed on the screen, not accounting for zoom.
     *
     * @return true if the whole document can be displayed at once, false otherwise
     */
    fun documentFitsView(): Boolean {
        val pdfFile = pdfFile ?: return false
        val len = pdfFile.getDocLen(1f)
        return if (isSwipeVertical) len < height else len < width
    }

    fun fitToWidth(page: Int) {
        val pdfFile = pdfFile ?: return
        if (state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet")
            return
        }
        zoomTo(width / pdfFile.getPageSize(page).width)
        jumpTo(page)
    }

    fun getPageSize(pageIndex: Int): SizeF = pdfFile?.getPageSize(pageIndex) ?: SizeF(0f, 0f)

    fun toRealScale(size: Float): Float = size / zoom

    fun toCurrentScale(size: Float): Float = size * zoom

    val isZooming: Boolean
        get() = zoom != minZoom

    private fun setDefaultPage(defaultPage: Int) {
        this.defaultPage = defaultPage
    }

    fun resetZoom() {
        zoomTo(minZoom)
    }

    fun resetZoomWithAnimation() {
        zoomWithAnimation(minZoom)
    }

    fun zoomWithAnimation(centerX: Float, centerY: Float, scale: Float) {
        animationManager.startZoomAnimation(centerX, centerY, zoom, scale)
    }

    fun zoomWithAnimation(scale: Float) {
        animationManager.startZoomAnimation(width / 2f, height / 2f, zoom, scale)
    }

    /**
     * Get the page number at the given offset.
     *
     * @param positionOffset scroll offset between 0 and 1
     * @return page number at the given offset, starting from 0
     */
    fun getPageAtPositionOffset(positionOffset: Float): Int {
        val pdfFile = pdfFile ?: return 0
        return pdfFile.getPageAtOffset(pdfFile.getDocLen(zoom) * positionOffset, zoom)
    }

    fun useBestQuality(bestQuality: Boolean) {
        isBestQuality = bestQuality
    }

    private fun setSwipeVertical(swipeVertical: Boolean) {
        isSwipeVertical = swipeVertical
    }

    fun enableAnnotationRendering(annotationRendering: Boolean) {
        isAnnotationRendering = annotationRendering
    }

    fun enableRenderDuringScale(renderDuringScale: Boolean) {
        this.renderDuringScale = renderDuringScale
    }

    internal fun doRenderDuringScale(): Boolean = renderDuringScale

    private fun setSpacing(spacingDp: Int) {
        spacingPx = Util.getDP(context, spacingDp)
    }

    private fun setAutoSpacing(autoSpacing: Boolean) {
        isAutoSpacingEnabled = autoSpacing
    }

    private fun setPageFitPolicy(pageFitPolicy: FitPolicy) {
        this.pageFitPolicy = pageFitPolicy
    }

    private fun setFitEachPage(fitEachPage: Boolean) {
        isFitEachPage = fitEachPage
    }

    /** Returns null if the document is not loaded. */
    fun getDocumentMeta(): PdfDocument.Meta? = pdfFile?.getMetaData()

    /** Empty until the document is loaded. */
    fun getTableOfContents(): List<PdfDocument.Bookmark> = pdfFile?.getBookmarks() ?: emptyList()

    /** Empty until the document is loaded. */
    fun getLinks(page: Int): List<PdfDocument.Link> = pdfFile?.getPageLinks(page) ?: emptyList()

    /** Use an asset file as the PDF source. */
    fun fromAsset(assetName: String): Configurator = Configurator(AssetSource(assetName))

    /** Use a file as the PDF source. */
    fun fromFile(file: File): Configurator = Configurator(FileSource(file))

    /** Use a URI as the PDF source, for use with content providers. */
    fun fromUri(uri: Uri): Configurator = Configurator(UriSource(uri))

    /** Use a byte array as the PDF source — the document is not saved. */
    fun fromBytes(bytes: ByteArray): Configurator = Configurator(ByteArraySource(bytes))

    /** Use a stream as the PDF source. The stream is copied to a byte array since native code does not support Java streams. */
    fun fromStream(stream: InputStream): Configurator = Configurator(InputStreamSource(stream))

    /** Use a custom source as the PDF source. */
    fun fromSource(docSource: DocumentSource): Configurator = Configurator(docSource)

    private enum class State { DEFAULT, LOADED, SHOWN, ERROR }

    /** Fluent builder used to configure and load a document into the [PDFView]. */
    inner class Configurator internal constructor(private val documentSource: DocumentSource) {

        private var pageNumbers: IntArray? = null
        private var enableSwipe = true
        private var enableDoubletap = true
        private var onDrawListener: OnDrawListener? = null
        private var onDrawAllListener: OnDrawListener? = null
        private var onLoadCompleteListener: OnLoadCompleteListener? = null
        private var onErrorListener: OnErrorListener? = null
        private var onPageChangeListener: OnPageChangeListener? = null
        private var zoomChangedListener: OnZoomChangedListener? = null
        private var onPageScrollListener: OnPageScrollListener? = null
        private var onRenderListener: OnRenderListener? = null
        private var onTapListener: OnTapListener? = null
        private var onLongPressListener: OnLongPressListener? = null
        private var onPageErrorListener: OnPageErrorListener? = null
        private var linkHandler: LinkHandler = DefaultLinkHandler(this@PDFView)
        private var defaultPage = 0
        private var swipeHorizontal = false
        private var annotationRendering = false
        private var password: String? = null
        private var scrollHandle: ScrollHandle? = null
        private var antialiasing = true
        private var spacing = 0
        private var autoSpacing = false
        private var pageFitPolicy = FitPolicy.WIDTH
        private var fitEachPage = false
        private var pageFling = false
        private var pageSnap = false
        private var nightMode = false
        private var bestQuality = false

        fun pages(vararg pageNumbers: Int) = apply { this.pageNumbers = pageNumbers }

        fun enableSwipe(enableSwipe: Boolean) = apply { this.enableSwipe = enableSwipe }

        fun enableDoubletap(enableDoubletap: Boolean) = apply { this.enableDoubletap = enableDoubletap }

        fun enableAnnotationRendering(annotationRendering: Boolean) = apply { this.annotationRendering = annotationRendering }

        fun onDraw(onDrawListener: OnDrawListener) = apply { this.onDrawListener = onDrawListener }

        fun onDrawAll(onDrawAllListener: OnDrawListener) = apply { this.onDrawAllListener = onDrawAllListener }

        fun onLoad(onLoadCompleteListener: OnLoadCompleteListener) = apply { this.onLoadCompleteListener = onLoadCompleteListener }

        fun onPageScroll(onPageScrollListener: OnPageScrollListener) = apply { this.onPageScrollListener = onPageScrollListener }

        fun onError(onErrorListener: OnErrorListener) = apply { this.onErrorListener = onErrorListener }

        fun onPageError(onPageErrorListener: OnPageErrorListener) = apply { this.onPageErrorListener = onPageErrorListener }

        fun onPageChange(onPageChangeListener: OnPageChangeListener) = apply { this.onPageChangeListener = onPageChangeListener }

        fun setOnZoomChangedListener(listener: OnZoomChangedListener) = apply { this.zoomChangedListener = listener }

        fun onRender(onRenderListener: OnRenderListener) = apply { this.onRenderListener = onRenderListener }

        fun onTap(onTapListener: OnTapListener) = apply { this.onTapListener = onTapListener }

        fun onLongPress(onLongPressListener: OnLongPressListener) = apply { this.onLongPressListener = onLongPressListener }

        fun linkHandler(linkHandler: LinkHandler) = apply { this.linkHandler = linkHandler }

        fun defaultPage(defaultPage: Int) = apply { this.defaultPage = defaultPage }

        fun swipeHorizontal(swipeHorizontal: Boolean) = apply { this.swipeHorizontal = swipeHorizontal }

        fun password(password: String?) = apply { this.password = password }

        fun scrollHandle(scrollHandle: ScrollHandle?) = apply { this.scrollHandle = scrollHandle }

        fun enableAntialiasing(antialiasing: Boolean) = apply { this.antialiasing = antialiasing }

        fun spacing(spacing: Int) = apply { this.spacing = spacing }

        fun autoSpacing(autoSpacing: Boolean) = apply { this.autoSpacing = autoSpacing }

        fun pageFitPolicy(pageFitPolicy: FitPolicy) = apply { this.pageFitPolicy = pageFitPolicy }

        fun fitEachPage(fitEachPage: Boolean) = apply { this.fitEachPage = fitEachPage }

        fun pageSnap(pageSnap: Boolean) = apply { this.pageSnap = pageSnap }

        fun pageFling(pageFling: Boolean) = apply { this.pageFling = pageFling }

        fun nightMode(nightMode: Boolean) = apply { this.nightMode = nightMode }

        /** Renders tiles as ARGB_8888 (sharper, more memory) instead of RGB_565 (banding, less memory). */
        fun bestQuality(bestQuality: Boolean) = apply { this.bestQuality = bestQuality }

        fun disableLongpress() = apply { dragPinchManager.disableLongpress() }

        fun load() {
            if (!hasSize) {
                waitingDocumentConfigurator = this
                return
            }

            recycle()
            callbacks = callbacks.also {
                it.setOnLoadComplete(onLoadCompleteListener)
                it.setOnError(onErrorListener)
                it.setOnDraw(onDrawListener)
                it.setOnDrawAll(onDrawAllListener)
                it.setOnPageChange(onPageChangeListener)
                it.setOnZoomChangedListener(zoomChangedListener)
                it.setOnPageScroll(onPageScrollListener)
                it.setOnRender(onRenderListener)
                it.setOnTap(onTapListener)
                it.setOnLongPress(onLongPressListener)
                it.setOnPageError(onPageErrorListener)
                it.setLinkHandler(linkHandler)
            }
            isSwipeEnabled = enableSwipe
            setNightMode(nightMode)
            isDoubletapEnabled = enableDoubletap
            setDefaultPage(defaultPage)
            setSwipeVertical(!swipeHorizontal)
            enableAnnotationRendering(annotationRendering)
            this@PDFView.scrollHandle = scrollHandle
            isAntialiasing = antialiasing
            setSpacing(spacing)
            setAutoSpacing(autoSpacing)
            setPageFitPolicy(pageFitPolicy)
            setFitEachPage(fitEachPage)
            isPageSnap = pageSnap
            isPageFlingEnabled = pageFling
            isBestQuality = bestQuality

            val pages = pageNumbers
            if (pages != null) {
                this@PDFView.load(documentSource, password, pages)
            } else {
                this@PDFView.load(documentSource, password)
            }
        }
    }

    companion object {
        private val TAG: String = PDFView::class.java.simpleName

        const val DEFAULT_MAX_SCALE = 3.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 1.0f
    }
}
