package com.faisal.pdfengine.util

private const val BIG_ENOUGH_INT = 16 * 1024
private const val BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT.toDouble()
private const val BIG_ENOUGH_CEIL = 16384.999999999996

object MathUtils {

    /** Clamps [number] between [between] and [and]. */
    fun limit(number: Int, between: Int, and: Int): Int = number.coerceIn(between, and)

    /** Clamps [number] between [between] and [and]. */
    fun limit(number: Float, between: Float, and: Float): Float = number.coerceIn(between, and)

    fun max(number: Float, max: Float): Float = if (number > max) max else number

    fun min(number: Float, min: Float): Float = if (number < min) min else number

    fun max(number: Int, max: Int): Int = if (number > max) max else number

    fun min(number: Int, min: Int): Int = if (number < min) min else number

    /**
     * From libGDX (https://github.com/libgdx/libgdx).
     * Largest integer <= [value]. Only correct for floats from -(2^14) to (Float.MAX_VALUE - 2^14).
     */
    fun floor(value: Float): Int = (value + BIG_ENOUGH_FLOOR).toInt() - BIG_ENOUGH_INT

    /**
     * From libGDX (https://github.com/libgdx/libgdx).
     * Smallest integer >= [value]. Only correct for floats from -(2^14) to (Float.MAX_VALUE - 2^14).
     */
    fun ceil(value: Float): Int = (value + BIG_ENOUGH_CEIL).toInt() - BIG_ENOUGH_INT
}
