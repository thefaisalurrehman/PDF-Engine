package com.faisal.pdfengine

import android.graphics.RectF
import com.faisal.pdfengine.model.PagePart
import com.faisal.pdfengine.util.Constants.Cache.CACHE_SIZE
import com.faisal.pdfengine.util.Constants.Cache.THUMBNAILS_CACHE_SIZE
import java.util.PriorityQueue

/** Keeps rendered [PagePart] tiles around so scrolling back to a page doesn't require re-rendering it. */
internal class CacheManager {

    private val orderComparator = Comparator<PagePart> { a, b -> a.cacheOrder.compareTo(b.cacheOrder) }
    private val activeCache = PriorityQueue(CACHE_SIZE, orderComparator)
    private val passiveCache = PriorityQueue(CACHE_SIZE, orderComparator)
    private val thumbnails = ArrayList<PagePart>()

    private val passiveActiveLock = Any()

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {
            makeFreeSpace()
            activeCache.offer(part)
        }
    }

    fun makeNewSet() {
        synchronized(passiveActiveLock) {
            passiveCache.addAll(activeCache)
            activeCache.clear()
        }
    }

    private fun makeFreeSpace() {
        synchronized(passiveActiveLock) {
            while (activeCache.size + passiveCache.size >= CACHE_SIZE && passiveCache.isNotEmpty()) {
                passiveCache.poll()?.renderedBitmap?.recycle()
            }
            while (activeCache.size + passiveCache.size >= CACHE_SIZE && activeCache.isNotEmpty()) {
                activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {
            while (thumbnails.size >= THUMBNAILS_CACHE_SIZE) {
                thumbnails.removeAt(0).renderedBitmap?.recycle()
            }
            addWithoutDuplicates(thumbnails, part)
        }
    }

    fun upPartIfContained(page: Int, pageRelativeBounds: RectF, toOrder: Int): Boolean {
        val key = PagePart(page, null, pageRelativeBounds, isThumbnail = false, cacheOrder = 0)

        synchronized(passiveActiveLock) {
            val foundInPassive = find(passiveCache, key)
            if (foundInPassive != null) {
                passiveCache.remove(foundInPassive)
                foundInPassive.cacheOrder = toOrder
                activeCache.offer(foundInPassive)
                return true
            }
            return find(activeCache, key) != null
        }
    }

    /** @return true if a [PagePart] thumbnail matching [page]/[pageRelativeBounds] is already cached. */
    fun containsThumbnail(page: Int, pageRelativeBounds: RectF): Boolean {
        val key = PagePart(page, null, pageRelativeBounds, isThumbnail = true, cacheOrder = 0)
        synchronized(thumbnails) {
            return thumbnails.any { it == key }
        }
    }

    /** Adds [newPart] unless an equal part already exists, in which case its bitmap is recycled. */
    private fun addWithoutDuplicates(collection: MutableCollection<PagePart>, newPart: PagePart) {
        if (collection.any { it == newPart }) {
            newPart.renderedBitmap?.recycle()
            return
        }
        collection.add(newPart)
    }

    private fun find(queue: PriorityQueue<PagePart>, key: PagePart): PagePart? = queue.firstOrNull { it == key }

    fun getPageParts(): List<PagePart> = synchronized(passiveActiveLock) {
        passiveCache.toMutableList().apply { addAll(activeCache) }
    }

    fun getThumbnails(): List<PagePart> = synchronized(thumbnails) { thumbnails.toList() }

    fun recycle() {
        synchronized(passiveActiveLock) {
            passiveCache.forEach { it.renderedBitmap?.recycle() }
            passiveCache.clear()
            activeCache.forEach { it.renderedBitmap?.recycle() }
            activeCache.clear()
        }
        synchronized(thumbnails) {
            thumbnails.forEach { it.renderedBitmap?.recycle() }
            thumbnails.clear()
        }
    }
}
