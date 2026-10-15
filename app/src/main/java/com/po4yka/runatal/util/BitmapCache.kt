package com.po4yka.runatal.util

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU cache for rendered bitmap images.
 * Eviction releases the cache reference. Consumers may still hold the bitmap,
 * so its memory is reclaimed by Android only after all references are released.
 */
object BitmapCache {

    // 4MB cache size - enough for ~10-15 widget bitmaps
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            // Size in bytes
            return value.byteCount
        }

    }

    /**
     * Retrieves a cached bitmap if available.
     *
     * @param key Cache key
     * @return Cached bitmap or null if not found
     */
    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    /**
     * Stores a bitmap in the cache.
     *
     * @param key Cache key
     * @param bitmap Bitmap to cache
     */
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    /** Keys the bitmap by every render parameter and the resolved SP size in pixels. */
    fun generateKey(config: RenderConfig, textSizePx: Float): String {
        return "$config|$textSizePx"
    }

    /**
     * Releases all cache references without invalidating consumers' bitmaps.
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * Returns current cache size in bytes.
     */
    fun size(): Int {
        return cache.size()
    }

    /**
     * Returns maximum cache size in bytes.
     */
    fun maxSize(): Int {
        return cache.maxSize()
    }
}
