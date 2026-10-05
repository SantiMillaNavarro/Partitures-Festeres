package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

/** Ajustos visuals no destructius aplicats després del recorte automàtic blanc. */
data class PdfPageAdjustment(
    val rotationQuarterTurns: Int = 0,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 0f,
    val cropBottom: Float = 0f,
    val contrast: Float = 1f,
) {
    fun normalized(): PdfPageAdjustment {
        val left = cropLeft.coerceIn(0f, 0.35f)
        val right = cropRight.coerceIn(0f, 0.35f)
        val top = cropTop.coerceIn(0f, 0.35f)
        val bottom = cropBottom.coerceIn(0f, 0.35f)
        // Deixem sempre una zona útil generosa encara que l'usuari porte dos sliders a l'extrem.
        val horizontalScale = if (left + right > 0.78f) 0.78f / (left + right) else 1f
        val verticalScale = if (top + bottom > 0.78f) 0.78f / (top + bottom) else 1f
        return copy(
            rotationQuarterTurns = ((rotationQuarterTurns % 4) + 4) % 4,
            cropLeft = left * horizontalScale,
            cropRight = right * horizontalScale,
            cropTop = top * verticalScale,
            cropBottom = bottom * verticalScale,
            contrast = contrast.coerceIn(0.65f, 1.70f),
        )
    }

    fun signature(): String {
        val value = normalized()
        return listOf(
            value.rotationQuarterTurns,
            "%.4f".format(java.util.Locale.US, value.cropLeft),
            "%.4f".format(java.util.Locale.US, value.cropTop),
            "%.4f".format(java.util.Locale.US, value.cropRight),
            "%.4f".format(java.util.Locale.US, value.cropBottom),
            "%.3f".format(java.util.Locale.US, value.contrast),
        ).joinToString(":")
    }

    fun sameGeometryAs(other: PdfPageAdjustment): Boolean {
        val a = normalized()
        val b = other.normalized()
        return a.rotationQuarterTurns == b.rotationQuarterTurns &&
            kotlin.math.abs(a.cropLeft - b.cropLeft) < 0.0001f &&
            kotlin.math.abs(a.cropTop - b.cropTop) < 0.0001f &&
            kotlin.math.abs(a.cropRight - b.cropRight) < 0.0001f &&
            kotlin.math.abs(a.cropBottom - b.cropBottom) < 0.0001f
    }

    val isDefault: Boolean
        get() = normalized().let {
            it.rotationQuarterTurns == 0 &&
                it.cropLeft == 0f && it.cropTop == 0f && it.cropRight == 0f && it.cropBottom == 0f &&
                kotlin.math.abs(it.contrast - 1f) < 0.001f
        }
}

class PdfPageAdjustmentStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(uri: Uri, pageIndex: Int): PdfPageAdjustment {
        val raw = prefs.getString(key(uri, pageIndex), null) ?: return PdfPageAdjustment()
        return runCatching {
            val p = raw.split('|')
            PdfPageAdjustment(
                rotationQuarterTurns = p.getOrNull(0)?.toIntOrNull() ?: 0,
                cropLeft = p.getOrNull(1)?.toFloatOrNull() ?: 0f,
                cropTop = p.getOrNull(2)?.toFloatOrNull() ?: 0f,
                cropRight = p.getOrNull(3)?.toFloatOrNull() ?: 0f,
                cropBottom = p.getOrNull(4)?.toFloatOrNull() ?: 0f,
                contrast = p.getOrNull(5)?.toFloatOrNull() ?: 1f,
            ).normalized()
        }.getOrDefault(PdfPageAdjustment())
    }

    fun save(uri: Uri, pageIndex: Int, adjustment: PdfPageAdjustment) {
        val value = adjustment.normalized()
        if (value.isDefault) {
            prefs.edit().remove(key(uri, pageIndex)).apply()
            return
        }
        val raw = listOf(
            value.rotationQuarterTurns,
            value.cropLeft,
            value.cropTop,
            value.cropRight,
            value.cropBottom,
            value.contrast,
        ).joinToString("|")
        prefs.edit().putString(key(uri, pageIndex), raw).apply()
    }

    fun clear(uri: Uri, pageIndex: Int) {
        prefs.edit().remove(key(uri, pageIndex)).apply()
    }

    fun clearAll(uri: Uri) {
        val prefix = "pdf_adjustment_${digest(uri)}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }

    fun migrateUri(oldUri: Uri, newUri: Uri) {
        if (oldUri == newUri) return
        val oldPrefix = "pdf_adjustment_${digest(oldUri)}_"
        val newPrefix = "pdf_adjustment_${digest(newUri)}_"
        val editor = prefs.edit()
        prefs.all.forEach { (storedKey, value) ->
            if (storedKey.startsWith(oldPrefix) && value is String) {
                val suffix = storedKey.removePrefix(oldPrefix)
                editor.putString(newPrefix + suffix, value)
                editor.remove(storedKey)
            }
        }
        editor.apply()
    }

    private fun key(uri: Uri, pageIndex: Int): String = "pdf_adjustment_${digest(uri)}_$pageIndex"

    private fun digest(uri: Uri): String = MessageDigest.getInstance("SHA-256")
        .digest(uri.toString().toByteArray())
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val PREFS_NAME = "partitures_festeres_pdf_adjustments"
    }
}

/**
 * Les anotacions històriques estan guardades en coordenades normalitzades de la vista
 * que existia en el moment de crear-les. Quan canviem rotació/recorte, les remapejem
 * perquè continuen damunt del mateix punt musical.
 */
fun remapAnnotationPoint(
    point: AnnotationPoint,
    from: PdfPageAdjustment,
    to: PdfPageAdjustment,
): AnnotationPoint {
    val base = displayedToBase(point, from.normalized())
    return baseToDisplayed(base, to.normalized())
}

fun remapAnnotationStroke(
    stroke: AnnotationStroke,
    from: PdfPageAdjustment,
    to: PdfPageAdjustment,
): AnnotationStroke = stroke.copy(points = stroke.points.map { remapAnnotationPoint(it, from, to) })

fun remapAnnotationShape(
    shape: AnnotationShape,
    from: PdfPageAdjustment,
    to: PdfPageAdjustment,
): AnnotationShape {
    val p1 = remapAnnotationPoint(AnnotationPoint(shape.x1, shape.y1), from, to)
    val p2 = remapAnnotationPoint(AnnotationPoint(shape.x2, shape.y2), from, to)
    return shape.copy(x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y)
}

fun remapAnnotationText(
    text: AnnotationText,
    from: PdfPageAdjustment,
    to: PdfPageAdjustment,
): AnnotationText {
    val corners = listOf(
        AnnotationPoint(text.x, text.y),
        AnnotationPoint(text.x + text.widthFraction, text.y),
        AnnotationPoint(text.x, text.y + text.heightFraction),
        AnnotationPoint(text.x + text.widthFraction, text.y + text.heightFraction),
    ).map { remapAnnotationPoint(it, from, to) }
    val left = corners.minOf { it.x }
    val top = corners.minOf { it.y }
    val right = corners.maxOf { it.x }
    val bottom = corners.maxOf { it.y }
    return text.copy(
        x = left,
        y = top,
        widthFraction = max(0.02f, right - left),
        heightFraction = max(0.02f, bottom - top),
    )
}

private fun displayedToBase(point: AnnotationPoint, adjustment: PdfPageAdjustment): AnnotationPoint {
    val unrotated = inverseRotate(point, adjustment.rotationQuarterTurns)
    val width = (1f - adjustment.cropLeft - adjustment.cropRight).coerceAtLeast(0.02f)
    val height = (1f - adjustment.cropTop - adjustment.cropBottom).coerceAtLeast(0.02f)
    return AnnotationPoint(
        adjustment.cropLeft + unrotated.x * width,
        adjustment.cropTop + unrotated.y * height,
    )
}

private fun baseToDisplayed(point: AnnotationPoint, adjustment: PdfPageAdjustment): AnnotationPoint {
    val width = (1f - adjustment.cropLeft - adjustment.cropRight).coerceAtLeast(0.02f)
    val height = (1f - adjustment.cropTop - adjustment.cropBottom).coerceAtLeast(0.02f)
    val cropped = AnnotationPoint(
        (point.x - adjustment.cropLeft) / width,
        (point.y - adjustment.cropTop) / height,
    )
    return rotate(cropped, adjustment.rotationQuarterTurns)
}

private fun rotate(point: AnnotationPoint, quarterTurns: Int): AnnotationPoint = when (((quarterTurns % 4) + 4) % 4) {
    1 -> AnnotationPoint(1f - point.y, point.x)
    2 -> AnnotationPoint(1f - point.x, 1f - point.y)
    3 -> AnnotationPoint(point.y, 1f - point.x)
    else -> point
}

private fun inverseRotate(point: AnnotationPoint, quarterTurns: Int): AnnotationPoint = when (((quarterTurns % 4) + 4) % 4) {
    1 -> AnnotationPoint(point.y, 1f - point.x)
    2 -> AnnotationPoint(1f - point.x, 1f - point.y)
    3 -> AnnotationPoint(1f - point.y, point.x)
    else -> point
}
