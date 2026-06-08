package com.faisal.pdfengine.source

import android.content.Context
import com.faisal.pdfengine.util.Util
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.InputStream

class InputStreamSource(private val inputStream: InputStream) : DocumentSource {

    override fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument =
        core.newDocument(Util.toByteArray(inputStream), password)
}
