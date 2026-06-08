package com.shockwave.pdfium

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import com.shockwave.pdfium.util.Size
import java.io.FileDescriptor
import java.io.IOException
import java.lang.reflect.Field

/**
 * JNI bridge to the native PDFium engine.
 *
 * The `external` declarations below are bound to symbols in the prebuilt `.so` libraries
 * (e.g. `Java_com_shockwave_pdfium_PdfiumCore_nativeOpenDocument`). Their names, parameter
 * types and the enclosing class's fully-qualified name must stay byte-for-byte compatible
 * with the native side, so do not rename or reorder them.
 *
 * @param ctx context, needed to read the screen density used when rasterizing pages.
 */
class PdfiumCore(ctx: Context) {

    private val currentDpi: Int = ctx.resources.displayMetrics.densityDpi

    // region native methods
    private external fun nativeOpenDocument(fd: Int, password: String?): Long

    private external fun nativeOpenMemDocument(data: ByteArray, password: String?): Long

    private external fun nativeCloseDocument(docPtr: Long)

    private external fun nativeGetPageCount(docPtr: Long): Int

    private external fun nativeLoadPage(docPtr: Long, pageIndex: Int): Long

    private external fun nativeLoadPages(docPtr: Long, fromIndex: Int, toIndex: Int): LongArray

    private external fun nativeClosePage(pagePtr: Long)

    private external fun nativeClosePages(pagesPtr: LongArray)

    private external fun nativeGetPageWidthPixel(pagePtr: Long, dpi: Int): Int

    private external fun nativeGetPageHeightPixel(pagePtr: Long, dpi: Int): Int

    private external fun nativeGetPageWidthPoint(pagePtr: Long): Int

    private external fun nativeGetPageHeightPoint(pagePtr: Long): Int

    private external fun nativeRenderPage(
        pagePtr: Long, surface: Surface?, dpi: Int,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean,
    )

    private external fun nativeRenderPageBitmap(
        pagePtr: Long, bitmap: Bitmap?, dpi: Int,
        startX: Int, startY: Int,
        drawSizeHor: Int, drawSizeVer: Int,
        renderAnnot: Boolean,
    )

    private external fun nativeGetDocumentMetaText(docPtr: Long, tag: String): String?

    private external fun nativeGetFirstChildBookmark(docPtr: Long, bookmarkPtr: Long?): Long?

    private external fun nativeGetSiblingBookmark(docPtr: Long, bookmarkPtr: Long): Long?

    private external fun nativeGetBookmarkTitle(bookmarkPtr: Long): String?

    private external fun nativeGetBookmarkDestIndex(docPtr: Long, bookmarkPtr: Long): Long

    private external fun nativeGetPageSizeByIndex(docPtr: Long, pageIndex: Int, dpi: Int): Size

    private external fun nativeGetPageLinks(pagePtr: Long): LongArray

    private external fun nativeGetDestPageIndex(docPtr: Long, linkPtr: Long): Int?

    private external fun nativeGetLinkURI(docPtr: Long, linkPtr: Long): String?

    private external fun nativeGetLinkRect(linkPtr: Long): RectF?

    private external fun nativePageCoordsToDevice(
        pagePtr: Long, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double,
    ): Point

    private external fun nativeSplitPdf(inputPath: String, outputPath: String, pages: IntArray): Boolean?
    // endregion

    fun splitPdf(inputPath: String, outputPath: String, pages: IntArray): Boolean? =
        nativeSplitPdf(inputPath, outputPath, pages)

    /** Create new document from file. */
    @JvmOverloads
    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor, password: String? = null): PdfDocument {
        val document = PdfDocument()
        document.parcelFileDescriptor = fd
        synchronized(lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password)
        }
        return document
    }

    /** Create new document from a byte array. */
    @JvmOverloads
    @Throws(IOException::class)
    fun newDocument(data: ByteArray, password: String? = null): PdfDocument {
        val document = PdfDocument()
        synchronized(lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password)
        }
        return document
    }

    /** Get total number of pages in document. */
    fun getPageCount(doc: PdfDocument): Int = synchronized(lock) {
        nativeGetPageCount(doc.mNativeDocPtr)
    }

    /** Open page and store native pointer in [PdfDocument]. */
    fun openPage(doc: PdfDocument, pageIndex: Int): Long = synchronized(lock) {
        val pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex)
        doc.mNativePagesPtr[pageIndex] = pagePtr
        pagePtr
    }

    /** Open range of pages and store native pointers in [PdfDocument]. */
    fun openPage(doc: PdfDocument, fromIndex: Int, toIndex: Int): LongArray = synchronized(lock) {
        val pagesPtr = nativeLoadPages(doc.mNativeDocPtr, fromIndex, toIndex)
        var pageIndex = fromIndex
        for (page in pagesPtr) {
            if (pageIndex > toIndex) break
            doc.mNativePagesPtr[pageIndex] = page
            pageIndex++
        }
        pagesPtr
    }

    /**
     * Get page width in pixels.
     * This method requires page to be opened.
     */
    fun getPageWidth(doc: PdfDocument, index: Int): Int = synchronized(lock) {
        val pagePtr = doc.mNativePagesPtr[index]
        if (pagePtr != null) nativeGetPageWidthPixel(pagePtr, currentDpi) else 0
    }

    /**
     * Get page height in pixels.
     * This method requires page to be opened.
     */
    fun getPageHeight(doc: PdfDocument, index: Int): Int = synchronized(lock) {
        val pagePtr = doc.mNativePagesPtr[index]
        if (pagePtr != null) nativeGetPageHeightPixel(pagePtr, currentDpi) else 0
    }

    /**
     * Get page width in PostScript points (1/72th of an inch).
     * This method requires page to be opened.
     */
    fun getPageWidthPoint(doc: PdfDocument, index: Int): Int = synchronized(lock) {
        val pagePtr = doc.mNativePagesPtr[index]
        if (pagePtr != null) nativeGetPageWidthPoint(pagePtr) else 0
    }

    /**
     * Get page height in PostScript points (1/72th of an inch).
     * This method requires page to be opened.
     */
    fun getPageHeightPoint(doc: PdfDocument, index: Int): Int = synchronized(lock) {
        val pagePtr = doc.mNativePagesPtr[index]
        if (pagePtr != null) nativeGetPageHeightPoint(pagePtr) else 0
    }

    /**
     * Get size of page in pixels.
     * This method does not require given page to be opened.
     */
    fun getPageSize(doc: PdfDocument, index: Int): Size = synchronized(lock) {
        nativeGetPageSizeByIndex(doc.mNativeDocPtr, index, currentDpi)
    }

    /**
     * Render page fragment on [Surface]. This method allows to render annotations.
     * Page must be opened before rendering.
     */
    @JvmOverloads
    fun renderPage(
        doc: PdfDocument, surface: Surface, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false,
    ) {
        synchronized(lock) {
            try {
                nativeRenderPage(
                    doc.mNativePagesPtr[pageIndex]!!, surface, currentDpi,
                    startX, startY, drawSizeX, drawSizeY, renderAnnot,
                )
            } catch (e: NullPointerException) {
                Log.e(TAG, "mContext may be null")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Exception throw from native")
                e.printStackTrace()
            }
        }
    }

    /**
     * Render page fragment on [Bitmap]. This method allows to render annotations.
     * Page must be opened before rendering.
     *
     * Supported bitmap configurations:
     * - ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
     * - RGB_565 - little worse quality, twice less memory usage
     */
    @JvmOverloads
    fun renderPageBitmap(
        doc: PdfDocument, bitmap: Bitmap, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int,
        renderAnnot: Boolean = false,
    ) {
        synchronized(lock) {
            try {
                nativeRenderPageBitmap(
                    doc.mNativePagesPtr[pageIndex]!!, bitmap, currentDpi,
                    startX, startY, drawSizeX, drawSizeY, renderAnnot,
                )
            } catch (e: NullPointerException) {
                Log.e(TAG, "mContext may be null")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Exception throw from native")
                e.printStackTrace()
            }
        }
    }

    /** Release native resources and the opened file. */
    fun closeDocument(doc: PdfDocument) {
        synchronized(lock) {
            for (pagePtr in doc.mNativePagesPtr.values) {
                nativeClosePage(pagePtr)
            }
            doc.mNativePagesPtr.clear()

            nativeCloseDocument(doc.mNativeDocPtr)

            doc.parcelFileDescriptor?.let { // if document was loaded from file
                try {
                    it.close()
                } catch (e: IOException) {
                    /* ignore */
                }
                doc.parcelFileDescriptor = null
            }
        }
    }

    /** Get metadata for given document. */
    fun getDocumentMeta(doc: PdfDocument): PdfDocument.Meta = synchronized(lock) {
        PdfDocument.Meta().apply {
            title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title")
            author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author")
            subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject")
            keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords")
            creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator")
            producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer")
            creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate")
            modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate")
        }
    }

    /** Get table of contents (bookmarks) for given document. */
    fun getTableOfContents(doc: PdfDocument): List<PdfDocument.Bookmark> = synchronized(lock) {
        val topLevel = ArrayList<PdfDocument.Bookmark>()
        val first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null)
        if (first != null) {
            recursiveGetBookmark(topLevel, doc, first)
        }
        topLevel
    }

    private fun recursiveGetBookmark(tree: MutableList<PdfDocument.Bookmark>, doc: PdfDocument, bookmarkPtr: Long) {
        val bookmark = PdfDocument.Bookmark()
        bookmark.mNativePtr = bookmarkPtr
        bookmark.title = nativeGetBookmarkTitle(bookmarkPtr)
        bookmark.pageIdx = nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr)
        tree.add(bookmark)

        val child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, bookmarkPtr)
        if (child != null) {
            recursiveGetBookmark(bookmark.children, doc, child)
        }

        val sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr)
        if (sibling != null) {
            recursiveGetBookmark(tree, doc, sibling)
        }
    }

    /** Get all links from given page. */
    fun getPageLinks(doc: PdfDocument, pageIndex: Int): List<PdfDocument.Link> = synchronized(lock) {
        val links = ArrayList<PdfDocument.Link>()
        val nativePagePtr = doc.mNativePagesPtr[pageIndex] ?: return@synchronized links
        for (linkPtr in nativeGetPageLinks(nativePagePtr)) {
            val index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr)
            val uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr)
            val rect = nativeGetLinkRect(linkPtr)
            if (rect != null && (index != null || uri != null)) {
                links.add(PdfDocument.Link(rect, index, uri))
            }
        }
        links
    }

    /**
     * Map page coordinates to device screen coordinates.
     *
     * @param doc       pdf document
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     *                  2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param pageX     X value in page coordinates
     * @param pageY     Y value in page coordinate
     * @return mapped coordinates
     */
    fun mapPageCoordsToDevice(
        doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, pageX: Double, pageY: Double,
    ): Point {
        val pagePtr = doc.mNativePagesPtr[pageIndex]!!
        return nativePageCoordsToDevice(pagePtr, startX, startY, sizeX, sizeY, rotate, pageX, pageY)
    }

    /**
     * @return mapped coordinates
     * @see mapPageCoordsToDevice
     */
    fun mapRectToDevice(
        doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int,
        sizeY: Int, rotate: Int, coords: RectF,
    ): RectF {
        val leftTop = mapPageCoordsToDevice(
            doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.left.toDouble(), coords.top.toDouble(),
        )
        val rightBottom = mapPageCoordsToDevice(
            doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
            coords.right.toDouble(), coords.bottom.toDouble(),
        )
        return RectF(
            leftTop.x.toFloat(), leftTop.y.toFloat(),
            rightBottom.x.toFloat(), rightBottom.y.toFloat(),
        )
    }

    companion object {
        private val TAG: String = PdfiumCore::class.java.name
        private val FD_CLASS: Class<*> = FileDescriptor::class.java
        private const val FD_FIELD_NAME = "descriptor"

        /** Synchronizes all native calls. */
        private val lock = Any()
        private var fdField: Field? = null

        init {
            try {
                System.loadLibrary("c++_shared")
                System.loadLibrary("modpng")
                System.loadLibrary("modft2")
                System.loadLibrary("modpdfium")
                System.loadLibrary("jniPdfium")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native libraries failed to load - $e")
            }
        }

        fun getNumFd(fdObj: ParcelFileDescriptor): Int = try {
            val field = fdField ?: FD_CLASS.getDeclaredField(FD_FIELD_NAME).apply {
                isAccessible = true
            }.also { fdField = it }
            field.getInt(fdObj.fileDescriptor)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            -1
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            -1
        }
    }
}
