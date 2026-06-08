package com.faisal.pdfengine.model

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * A rendered tile of a page, positioned within the page via [pageRelativeBounds].
 *
 * [renderedBitmap] is nullable to allow constructing lightweight "key-only" instances used
 * purely for cache lookups by [equals] (see [com.faisal.pdfengine.CacheManager]).
 */
class PagePart(
    val page: Int,
    val renderedBitmap: Bitmap?,
    val pageRelativeBounds: RectF,
    val isThumbnail: Boolean,
    var cacheOrder: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is PagePart) return false
        return other.page == page &&
            other.pageRelativeBounds.left == pageRelativeBounds.left &&
            other.pageRelativeBounds.right == pageRelativeBounds.right &&
            other.pageRelativeBounds.top == pageRelativeBounds.top &&
            other.pageRelativeBounds.bottom == pageRelativeBounds.bottom
    }

    override fun hashCode(): Int = page * 31 + pageRelativeBounds.hashCode()
}
