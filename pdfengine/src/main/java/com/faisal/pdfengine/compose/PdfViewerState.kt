package com.faisal.pdfengine.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.faisal.pdfengine.PDFView

/**
 * Holds the live state of a [PdfViewer] (current page, total page count, zoom)
 * and exposes actions to control it (e.g. [jumpToPage]).
 *
 * Create one with [rememberPdfViewerState] and pass it to [PdfViewer].
 */
class PdfViewerState internal constructor(initialPage: Int = 0) {

    var currentPage: Int by mutableIntStateOf(initialPage)
        internal set

    var pageCount: Int by mutableIntStateOf(0)
        internal set

    var zoom: Float by mutableFloatStateOf(1f)
        internal set

    var isLoaded: Boolean by mutableStateOf(false)
        internal set

    private var pendingJump: Int? = initialPage.takeIf { it > 0 }
    internal var pdfView: PDFView? = null

    /** Jumps to the given zero-based [page]. Works before and after the document finishes loading. */
    fun jumpToPage(page: Int, withAnimation: Boolean = false) {
        val view = pdfView
        if (view != null && isLoaded) {
            view.jumpTo(page, withAnimation)
        } else {
            pendingJump = page
        }
    }

    /** Resets the zoom level back to the document's default fit. */
    fun resetZoom() {
        pdfView?.resetZoomWithAnimation()
    }

    internal fun consumePendingJump(): Int? {
        val page = pendingJump
        pendingJump = null
        return page
    }
}

/** Creates and remembers a [PdfViewerState], optionally starting on [initialPage] (zero-based). */
@Composable
fun rememberPdfViewerState(initialPage: Int = 0): PdfViewerState {
    return remember { PdfViewerState(initialPage) }
}
