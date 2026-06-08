package com.faisal.pdfengine.util

object Constants {

    var DEBUG_MODE = false

    /** Between 0 and 1, the thumbnail quality (default 0.3). Increasing this may hurt performance. */
    const val THUMBNAIL_RATIO = 0.3f

    /**
     * The size of the rendered tiles (default 256).
     * Smaller: slower to render a full page but more reactive.
     * Bigger: longer wait for the first visible result.
     */
    const val PART_SIZE = 256f

    /** Portion of the document above/below the screen that should be preloaded, in dp. */
    const val PRELOAD_OFFSET = 20

    object Cache {
        /** The number of bitmaps kept in the cache. */
        const val CACHE_SIZE = 120
        const val THUMBNAILS_CACHE_SIZE = 8
    }

    object Pinch {
        const val MAXIMUM_ZOOM = 10f
        const val MINIMUM_ZOOM = 1f
    }
}
