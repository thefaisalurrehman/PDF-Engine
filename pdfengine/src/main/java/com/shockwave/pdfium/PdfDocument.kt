package com.shockwave.pdfium

import android.graphics.RectF
import android.os.ParcelFileDescriptor
import androidx.collection.ArrayMap

/**
 * Handle to an open PDF document. Wraps the native document pointer plus the pointers of any
 * pages opened so far. Constructed only by [PdfiumCore]; consumers treat it as an opaque token.
 */
class PdfDocument internal constructor() {

    internal var mNativeDocPtr: Long = 0
    internal var parcelFileDescriptor: ParcelFileDescriptor? = null
    internal val mNativePagesPtr: MutableMap<Int, Long> = ArrayMap()

    fun hasPage(index: Int): Boolean = mNativePagesPtr.containsKey(index)

    /** Document metadata. Populated by [PdfiumCore.getDocumentMeta]. */
    class Meta {
        var title: String? = null
            internal set
        var author: String? = null
            internal set
        var subject: String? = null
            internal set
        var keywords: String? = null
            internal set
        var creator: String? = null
            internal set
        var producer: String? = null
            internal set
        var creationDate: String? = null
            internal set
        var modDate: String? = null
            internal set
    }

    /** A single entry in the document's table of contents. May have nested [children]. */
    class Bookmark {
        val children: MutableList<Bookmark> = ArrayList()
        var title: String? = null
            internal set
        var pageIdx: Long = 0
            internal set
        internal var mNativePtr: Long = 0

        fun hasChildren(): Boolean = children.isNotEmpty()
    }

    /** A hyperlink on a page, either to another page ([destPageIdx]) or an external [uri]. */
    class Link(val bounds: RectF, val destPageIdx: Int?, val uri: String?)
}
