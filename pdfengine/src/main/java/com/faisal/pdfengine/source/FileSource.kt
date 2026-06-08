package com.faisal.pdfengine.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File

class FileSource(private val file: File) : DocumentSource {

    override fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return core.newDocument(pfd, password)
    }
}
