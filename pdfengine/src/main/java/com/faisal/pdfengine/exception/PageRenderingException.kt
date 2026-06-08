package com.faisal.pdfengine.exception

class PageRenderingException(val page: Int, cause: Throwable) : Exception(cause)
