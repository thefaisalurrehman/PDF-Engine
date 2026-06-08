package com.faisal.pdfengine.compose

import android.net.Uri
import com.faisal.pdfengine.PDFView
import java.io.File
import java.io.InputStream

/**
 * Where the PDF document to render comes from. Pass one of these to [PdfViewer].
 */
sealed interface PdfSource {
    fun configure(pdfView: PDFView): PDFView.Configurator

    companion object {
        fun fromUri(uri: Uri): PdfSource = UriPdfSource(uri)
        fun fromFile(file: File): PdfSource = FilePdfSource(file)
        fun fromAsset(assetName: String): PdfSource = AssetPdfSource(assetName)
        fun fromBytes(bytes: ByteArray): PdfSource = BytesPdfSource(bytes)
        fun fromStream(stream: InputStream): PdfSource = StreamPdfSource(stream)
    }
}

private data class UriPdfSource(val uri: Uri) : PdfSource {
    override fun configure(pdfView: PDFView) = pdfView.fromUri(uri)
}

private data class FilePdfSource(val file: File) : PdfSource {
    override fun configure(pdfView: PDFView) = pdfView.fromFile(file)
}

private data class AssetPdfSource(val assetName: String) : PdfSource {
    override fun configure(pdfView: PDFView) = pdfView.fromAsset(assetName)
}

private data class BytesPdfSource(val bytes: ByteArray) : PdfSource {
    override fun configure(pdfView: PDFView) = pdfView.fromBytes(bytes)
}

private data class StreamPdfSource(val stream: InputStream) : PdfSource {
    override fun configure(pdfView: PDFView) = pdfView.fromStream(stream)
}
