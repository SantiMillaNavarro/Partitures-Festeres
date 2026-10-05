package com.partituresfesteres.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class PdfThumbnailRepository(private val context: Context) {

    // Caché en memoria suficiente para varias decenas de miniaturas sin cargar PDFs completos.
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun firstPage(uri: Uri, targetWidthPx: Int = 360): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(uri.toString())?.let { return@withContext it }

        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching null
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return@use null
                    renderer.openPage(0).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val targetHeightPx = (targetWidthPx * ratio).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(
                            targetWidthPx.coerceAtLeast(1),
                            targetHeightPx,
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        cache.put(uri.toString(), bitmap)
                        bitmap
                    }
                }
            }
        }.getOrNull()
    }
}
