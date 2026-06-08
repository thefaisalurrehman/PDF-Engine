package com.faisal.pdfengine.source

import android.content.Context
import android.net.Uri
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException

class UriSource(private val uri: Uri) : DocumentSource {

    override fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Cannot open file descriptor for uri: $uri")
        return core.newDocument(pfd, password)
    }
}
