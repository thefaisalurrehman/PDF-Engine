package com.faisal.pdfengine.source

import android.content.Context
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

class ByteArraySource(private val data: ByteArray) : DocumentSource {

    override fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument =
        core.newDocument(data, password)
}
