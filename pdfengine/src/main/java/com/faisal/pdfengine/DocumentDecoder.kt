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

import com.faisal.pdfengine.source.DocumentSource
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * Decodes a [DocumentSource] into a [PdfFile] on a background dispatcher and delivers the
 * result back to [PDFView] on the main thread — a coroutine-based replacement for the old
 * `AsyncTask` based decoder. Holds the view only through a [WeakReference] so an in-flight
 * decode never leaks it, and [cancel] tears the whole job down (matching the previous
 * `AsyncTask.cancel(true)` contract).
 */
internal class DocumentDecoder(
    private val docSource: DocumentSource,
    private val password: String?,
    private val userPages: IntArray?,
    pdfView: PDFView,
    private val pdfiumCore: PdfiumCore,
) {

    private val pdfViewRef = WeakReference(pdfView)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var cancelled = false

    fun start() {
        scope.launch {
            val pdfView = pdfViewRef.get() ?: return@launch
            try {
                val pdfFile = withContext(Dispatchers.IO) { decode(pdfView) }
                if (!cancelled) pdfView.loadComplete(pdfFile)
            } catch (t: Throwable) {
                pdfView.loadError(t)
            }
        }
    }

    fun cancel() {
        cancelled = true
        scope.cancel()
    }

    private fun decode(pdfView: PDFView): PdfFile {
        val pdfDocument = docSource.createDocument(pdfView.context, pdfiumCore, password)
        return PdfFile(
            pdfiumCore,
            pdfDocument,
            pdfView.pageFitPolicy,
            Size(pdfView.width, pdfView.height),
            userPages,
            pdfView.isSwipeVertical,
            pdfView.spacingPx,
            pdfView.isAutoSpacingEnabled,
            pdfView.isFitEachPage,
        )
    }
}
