package com.faisal.pdfengine.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.faisal.pdfengine.util.FileUtils
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

class AssetSource(private val assetName: String) : DocumentSource {

    override fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument {
        val file = FileUtils.fileFromAsset(context, assetName)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return core.newDocument(pfd, password)
    }
}
