package com.faisal.pdfengine.link

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.faisal.pdfengine.PDFView
import com.faisal.pdfengine.model.LinkTapEvent

/** Opens link URIs via [Intent.ACTION_VIEW] and jumps to the target page for internal links. */
class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    override fun handleLinkEvent(event: LinkTapEvent) {
        val uri = event.link.uri
        val page = event.link.destPageIdx
        if (!uri.isNullOrEmpty()) {
            handleUri(uri)
        } else if (page != null) {
            pdfView.jumpTo(page)
        }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context = pdfView.context
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Log.w(TAG, "No activity found for URI: $uri")
        }
    }

    private companion object {
        const val TAG = "DefaultLinkHandler"
    }
}
