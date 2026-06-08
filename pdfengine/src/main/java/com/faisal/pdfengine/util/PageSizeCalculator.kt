package com.faisal.pdfengine.util

import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import kotlin.math.floor

/** Computes the on-screen size of pages for a given [fitPolicy], scaling everything from the largest page. */
class PageSizeCalculator(
    private val fitPolicy: FitPolicy,
    private val originalMaxWidthPageSize: Size,
    private val originalMaxHeightPageSize: Size,
    private val viewSize: Size,
    private val fitEachPage: Boolean,
) {
    var optimalMaxWidthPageSize: SizeF = SizeF(0f, 0f)
        private set
    var optimalMaxHeightPageSize: SizeF = SizeF(0f, 0f)
        private set
    private var widthRatio = 0f
    private var heightRatio = 0f

    init {
        calculateMaxPages()
    }

    fun calculate(pageSize: Size): SizeF {
        if (pageSize.width <= 0 || pageSize.height <= 0) return SizeF(0f, 0f)

        val maxWidth = if (fitEachPage) viewSize.width.toFloat() else pageSize.width * widthRatio
        val maxHeight = if (fitEachPage) viewSize.height.toFloat() else pageSize.height * heightRatio
        return when (fitPolicy) {
            FitPolicy.HEIGHT -> fitHeight(pageSize, maxHeight)
            FitPolicy.BOTH -> fitBoth(pageSize, maxWidth, maxHeight)
            else -> fitWidth(pageSize, maxWidth)
        }
    }

    private fun calculateMaxPages() {
        when (fitPolicy) {
            FitPolicy.HEIGHT -> {
                optimalMaxHeightPageSize = fitHeight(originalMaxHeightPageSize, viewSize.height.toFloat())
                heightRatio = optimalMaxHeightPageSize.height / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitHeight(originalMaxWidthPageSize, originalMaxWidthPageSize.height * heightRatio)
            }
            FitPolicy.BOTH -> {
                val localOptimalMaxWidth = fitBoth(originalMaxWidthPageSize, viewSize.width.toFloat(), viewSize.height.toFloat())
                val localWidthRatio = localOptimalMaxWidth.width / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize = fitBoth(
                    originalMaxHeightPageSize,
                    originalMaxHeightPageSize.width * localWidthRatio,
                    viewSize.height.toFloat(),
                )
                heightRatio = optimalMaxHeightPageSize.height / originalMaxHeightPageSize.height
                optimalMaxWidthPageSize = fitBoth(originalMaxWidthPageSize, viewSize.width.toFloat(), originalMaxWidthPageSize.height * heightRatio)
                widthRatio = optimalMaxWidthPageSize.width / originalMaxWidthPageSize.width
            }
            else -> {
                optimalMaxWidthPageSize = fitWidth(originalMaxWidthPageSize, viewSize.width.toFloat())
                widthRatio = optimalMaxWidthPageSize.width / originalMaxWidthPageSize.width
                optimalMaxHeightPageSize = fitWidth(originalMaxHeightPageSize, originalMaxHeightPageSize.width * widthRatio)
            }
        }
    }

    private fun fitWidth(pageSize: Size, maxWidth: Float): SizeF {
        val ratio = pageSize.width.toFloat() / pageSize.height
        val w = maxWidth
        val h = floor(maxWidth / ratio)
        return SizeF(w, h)
    }

    private fun fitHeight(pageSize: Size, maxHeight: Float): SizeF {
        val ratio = pageSize.height.toFloat() / pageSize.width
        val h = maxHeight
        val w = floor(maxHeight / ratio)
        return SizeF(w, h)
    }

    private fun fitBoth(pageSize: Size, maxWidth: Float, maxHeight: Float): SizeF {
        val ratio = pageSize.width.toFloat() / pageSize.height
        var w = maxWidth
        var h = floor(maxWidth / ratio)
        if (h > maxHeight) {
            h = maxHeight
            w = floor(maxHeight * ratio)
        }
        return SizeF(w, h)
    }
}
