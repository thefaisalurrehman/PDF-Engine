package com.faisal.pdfengine.util

import android.content.Context
import android.util.TypedValue
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val DEFAULT_BUFFER_SIZE = 1024 * 4

object Util {

    fun getDP(context: Context, dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

    fun toByteArray(inputStream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
