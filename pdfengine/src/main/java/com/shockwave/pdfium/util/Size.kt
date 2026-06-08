package com.shockwave.pdfium.util

/**
 * Immutable integer width/height pair.
 *
 * Instances are also constructed from native code via the `(II)V` constructor
 * (see `nativeGetPageSizeByIndex` in [com.shockwave.pdfium.PdfiumCore]), so the
 * primary constructor's signature must not change.
 */
class Size(val width: Int, val height: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Size) return false
        return width == other.width && height == other.height
    }

    override fun toString(): String = "${width}x$height"

    override fun hashCode(): Int {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return height xor ((width shl (Int.SIZE_BITS / 2)) or (width ushr (Int.SIZE_BITS / 2)))
    }
}
