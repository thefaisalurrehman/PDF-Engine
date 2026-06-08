package com.faisal.pdfengine.source

import android.content.Context
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

/** Knows how to open a [PdfDocument] from a particular kind of input. */
fun interface DocumentSource {
    @Throws(IOException::class)
    fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument
}
