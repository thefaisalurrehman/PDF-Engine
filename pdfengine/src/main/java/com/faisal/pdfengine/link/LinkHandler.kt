package com.faisal.pdfengine.link

import com.faisal.pdfengine.model.LinkTapEvent

/** Handles a tap on a PDF link (URI or internal page jump). */
fun interface LinkHandler {
    fun handleLinkEvent(event: LinkTapEvent)
}
