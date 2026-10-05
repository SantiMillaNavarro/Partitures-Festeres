package com.partituresfesteres.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import com.partituresfesteres.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class PdfViewerRepository(private val context: Context) {

    // Caché aproximada de 48 MB. Mantiene preparadas varias páginas sin cargar PDFs completos.
    private val pageCache = object : LruCache<String, Bitmap>(48 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun pageCount(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error(context.getString(R.string.pdf_open_error))
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            }
        }
    }

    suspend fun renderPage(
        uri: Uri,
        pageIndex: Int,
        targetWidthPx: Int,
        adjustment: PdfPageAdjustment = PdfPageAdjustment(),
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val safeWidth = targetWidthPx.coerceIn(800, 2400)
        val normalizedAdjustment = adjustment.normalized()
        val key = cacheKey(uri, pageIndex, safeWidth, normalizedAdjustment)
        pageCache.get(key)?.let { return@withContext Result.success(it) }

        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error(context.getString(R.string.pdf_open_error))
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex !in 0 until renderer.pageCount) {
                        error(context.getString(R.string.pdf_page_missing))
                    }
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val targetHeight = (safeWidth * ratio).roundToInt().coerceAtLeast(1)
                        val fullBitmap = Bitmap.createBitmap(
                            safeWidth,
                            targetHeight,
                            Bitmap.Config.ARGB_8888,
                        )
                        fullBitmap.eraseColor(Color.WHITE)
                        page.render(fullBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        // Recorte automàtic no destructiu consolidat des de v0.4.
                        var displayBitmap = cropWhiteMargins(fullBitmap)
                        if (displayBitmap !== fullBitmap) fullBitmap.recycle()

                        val manuallyCropped = applyManualCrop(displayBitmap, normalizedAdjustment)
                        if (manuallyCropped !== displayBitmap) {
                            displayBitmap.recycle()
                            displayBitmap = manuallyCropped
                        }

                        val rotated = applyRotation(displayBitmap, normalizedAdjustment.rotationQuarterTurns)
                        if (rotated !== displayBitmap) {
                            displayBitmap.recycle()
                            displayBitmap = rotated
                        }

                        val contrasted = applyContrast(displayBitmap, normalizedAdjustment.contrast)
                        if (contrasted !== displayBitmap) {
                            displayBitmap.recycle()
                            displayBitmap = contrasted
                        }

                        pageCache.put(key, displayBitmap)
                        displayBitmap
                    }
                }
            }
        }
    }

    suspend fun prefetch(
        uri: Uri,
        pageIndexes: List<Int>,
        targetWidthPx: Int,
        adjustmentForPage: (Int) -> PdfPageAdjustment = { PdfPageAdjustment() },
    ) {
        pageIndexes.distinct().filter { it >= 0 }.forEach { page ->
            renderPage(uri, page, targetWidthPx, adjustmentForPage(page))
        }
    }

    private fun applyManualCrop(source: Bitmap, adjustment: PdfPageAdjustment): Bitmap {
        val value = adjustment.normalized()
        if (value.cropLeft <= 0.0001f && value.cropTop <= 0.0001f &&
            value.cropRight <= 0.0001f && value.cropBottom <= 0.0001f
        ) return source

        val left = (source.width * value.cropLeft).roundToInt().coerceIn(0, source.width - 1)
        val top = (source.height * value.cropTop).roundToInt().coerceIn(0, source.height - 1)
        val right = (source.width * (1f - value.cropRight)).roundToInt().coerceIn(left + 1, source.width)
        val bottom = (source.height * (1f - value.cropBottom)).roundToInt().coerceIn(top + 1, source.height)
        val width = right - left
        val height = bottom - top
        if (left == 0 && top == 0 && width == source.width && height == source.height) return source
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    private fun applyRotation(source: Bitmap, quarterTurns: Int): Bitmap {
        val turns = ((quarterTurns % 4) + 4) % 4
        if (turns == 0) return source
        val matrix = Matrix().apply { postRotate(turns * 90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun applyContrast(source: Bitmap, contrast: Float): Bitmap {
        val value = contrast.coerceIn(0.65f, 1.70f)
        if (kotlin.math.abs(value - 1f) < 0.005f) return source
        val translate = (-0.5f * value + 0.5f) * 255f
        val matrix = ColorMatrix(
            floatArrayOf(
                value, 0f, 0f, 0f, translate,
                0f, value, 0f, 0f, translate,
                0f, 0f, value, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        val target = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(target)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return target
    }

    /**
     * Busca el rectángulo ocupado por contenido suficientemente oscuro y elimina solo
     * el blanco exterior. Se muestrea la imagen para mantener el coste bajo incluso
     * en tablets antiguas. Si no hay un recorte claro, devuelve el bitmap original.
     */
    private fun cropWhiteMargins(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        if (width < 200 || height < 200) return source

        val step = max(2, minOf(width, height) / 500)
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (isVisibleContent(source.getPixel(x, y))) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }

        if (maxX < minX || maxY < minY) return source

        // Un 2,5 % de marge de seguretat al voltant del contingut detectat.
        val safeX = max(18, (width * 0.025f).roundToInt())
        val safeY = max(18, (height * 0.025f).roundToInt())
        val left = (minX - safeX).coerceAtLeast(0)
        val top = (minY - safeY).coerceAtLeast(0)
        val right = (maxX + safeX + step).coerceAtMost(width)
        val bottom = (maxY + safeY + step).coerceAtMost(height)

        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth <= 0 || cropHeight <= 0) return source

        // Evita crear altre bitmap quan pràcticament no hi ha marge que guanyar.
        val widthGain = 1f - cropWidth.toFloat() / width.toFloat()
        val heightGain = 1f - cropHeight.toFloat() / height.toFloat()
        if (widthGain < 0.02f && heightGain < 0.02f) return source

        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    private fun isVisibleContent(pixel: Int): Boolean {
        if (Color.alpha(pixel) < 32) return false
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        // Una nota/pentagrama gris o negre es detecta; el blanc i crema clar s'ignoren.
        return minOf(red, green, blue) < 232
    }

    private fun cacheKey(uri: Uri, pageIndex: Int, width: Int, adjustment: PdfPageAdjustment): String =
        "${uri}|$pageIndex|$width|crop-v1|${adjustment.signature()}"
}
