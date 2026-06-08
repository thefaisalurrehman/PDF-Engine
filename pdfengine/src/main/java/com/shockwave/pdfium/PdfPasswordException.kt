package com.shockwave.pdfium

import java.io.IOException

/** Thrown when a document requires a password (or the supplied one is wrong). */
class PdfPasswordException : IOException {
    constructor() : super()
    constructor(detailMessage: String?) : super(detailMessage)
}
