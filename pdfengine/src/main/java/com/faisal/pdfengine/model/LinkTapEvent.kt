package com.faisal.pdfengine.model

import android.graphics.RectF
import com.shockwave.pdfium.PdfDocument

/** Carries the details of a tap that landed on a PDF link. */
data class LinkTapEvent(
    val originalX: Float,
    val originalY: Float,
    val documentX: Float,
    val documentY: Float,
    val mappedLinkRect: RectF,
    val link: PdfDocument.Link,
)
