package com.shockwave.pdfium.util

/** Immutable float width/height pair. */
class SizeF(val width: Float, val height: Float) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SizeF) return false
        return width == other.width && height == other.height
    }

    override fun toString(): String = "${width}x$height"

    override fun hashCode(): Int = width.toBits() xor height.toBits()

    fun toSize(): Size = Size(width.toInt(), height.toInt())
}
