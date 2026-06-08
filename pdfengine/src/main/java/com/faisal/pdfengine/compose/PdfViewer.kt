package com.faisal.pdfengine.compose

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.faisal.pdfengine.PDFView
import com.faisal.pdfengine.scroll.DefaultScrollHandle
import com.faisal.pdfengine.util.FitPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a PDF document with pinch-to-zoom, swipe/scroll navigation and optional
 * page-by-page (horizontal) mode. This is the main entry point of the PDF Viewer SDK.
 *
 * Example:
 * ```
 * val state = rememberPdfViewerState()
 * PdfViewer(
 *     source = PdfSource.fromUri(uri),
 *     state = state,
 *     onPageChanged = { page, count -> /* persist progress yourself */ },
 * )
 * ```
 *
 * @param source where to load the document from, see [PdfSource].
 * @param state controls and observes the viewer, see [rememberPdfViewerState].
 * @param isDarkMode renders the document with inverted colors ("night mode").
 * @param isPageByPage when true, pages are laid out horizontally one at a time instead
 * of a continuous vertical scroll.
 * @param fitPolicy how pages are scaled to fit the viewport.
 * @param isBestQuality renders tiles as ARGB_8888 (sharp, more memory) instead of RGB_565
 * (banding, less memory). Defaults to true since crisp rendering matters most for a viewer.
 * @param showScrollHandle shows a draggable scroll indicator on the side of the view.
 * @param backgroundColor color shown behind/between pages while they render.
 * @param onTap called when the user taps the document (e.g. to toggle a fullscreen UI).
 * @param onPageChanged called whenever the visible page changes, with the zero-based page
 * index and the total page count.
 * @param onZoomChanged called whenever the zoom level changes.
 * @param onLoadComplete called once the document has finished loading, with its page count.
 * @param onError called if the document fails to load or render.
 */
@Composable
fun PdfViewer(
    source: PdfSource,
    modifier: Modifier = Modifier,
    state: PdfViewerState = rememberPdfViewerState(),
    isDarkMode: Boolean = false,
    isPageByPage: Boolean = false,
    fitPolicy: FitPolicy = FitPolicy.WIDTH,
    isBestQuality: Boolean = true,
    showScrollHandle: Boolean = true,
    backgroundColor: Color = Color(0xFF222222),
    onTap: () -> Unit = {},
    onPageChanged: (page: Int, pageCount: Int) -> Unit = { _, _ -> },
    onZoomChanged: (zoom: Float) -> Unit = {},
    onLoadComplete: (pageCount: Int) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val pdfView = remember(context) { PDFView(context, null) }

    DisposableEffect(pdfView) {
        state.pdfView = pdfView
        onDispose {
            state.pdfView = null
            pdfView.recycle()
        }
    }

    LaunchedEffect(source, isDarkMode, isPageByPage, fitPolicy, isBestQuality) {
        withContext(Dispatchers.Main) {
            val configurator = source.configure(pdfView)
                .enableSwipe(true)
                .swipeHorizontal(isPageByPage)
                .pageSnap(isPageByPage)
                .enableDoubletap(true)
                .defaultPage(state.consumePendingJump() ?: state.currentPage)
                .enableAnnotationRendering(false)
                .enableAntialiasing(true)
                .bestQuality(isBestQuality)
                .pageFitPolicy(fitPolicy)
                .nightMode(isDarkMode)
                .pageFling(isPageByPage)
                .spacing(if (isPageByPage) 0 else 10)
                .onLoad { pageCount ->
                    state.pageCount = pageCount
                    state.isLoaded = true
                    onLoadComplete(pageCount)
                }
                .onPageChange { page, pageCount ->
                    state.currentPage = page
                    state.pageCount = pageCount
                    onPageChanged(page, pageCount)
                }
                .setOnZoomChangedListener { zoom ->
                    state.zoom = zoom
                    onZoomChanged(zoom)
                }
                .onError { onError(it) }

            if (showScrollHandle) {
                configurator.scrollHandle(DefaultScrollHandle(context))
            }

            configurator.load()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                addView(
                    pdfView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                setOnClickListener { onTap() }
                pdfView.setOnClickListener { onTap() }
            }
        },
        update = { container ->
            container.setBackgroundColor(backgroundColor.toArgb())
            pdfView.setBackgroundColor(backgroundColor.toArgb())
        }
    )
}
