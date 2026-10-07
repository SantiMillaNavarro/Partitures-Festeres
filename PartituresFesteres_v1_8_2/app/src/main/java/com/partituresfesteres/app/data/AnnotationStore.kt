package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class AnnotationTool {
    PENCIL,
    HIGHLIGHTER,
}

enum class AnnotationTextAlign {
    LEFT,
    CENTER,
    RIGHT,
}

enum class AnnotationShapeType {
    LINE,
    ARROW,
    RECTANGLE,
    ELLIPSE,
}

data class AnnotationPoint(
    val x: Float,
    val y: Float,
)

data class AnnotationStroke(
    val tool: AnnotationTool,
    val colorArgb: Int,
    val widthFraction: Float,
    val points: List<AnnotationPoint>,
)

data class AnnotationText(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val widthFraction: Float = 0.24f,
    val heightFraction: Float = 0.09f,
    val colorArgb: Int,
    val sizeFraction: Float,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val alignment: AnnotationTextAlign = AnnotationTextAlign.LEFT,
    val backgroundEnabled: Boolean = false,
    val borderEnabled: Boolean = false,
)

data class AnnotationShape(
    val id: String,
    val type: AnnotationShapeType,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val colorArgb: Int,
    val widthFraction: Float = 0.0035f,
    val fillEnabled: Boolean = false,
)

class AnnotationStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(uri: Uri, pageIndex: Int): List<AnnotationStroke> {
        val raw = prefs.getString(strokeKey(uri, pageIndex), null) ?: return emptyList()
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (i in 0 until root.length()) {
                    val item = root.getJSONObject(i)
                    val tool = runCatching { AnnotationTool.valueOf(item.getString("tool")) }
                        .getOrDefault(AnnotationTool.PENCIL)
                    val color = item.optInt("color", 0xFFB3261E.toInt())
                    val width = item.optDouble("width", 0.003).toFloat().coerceIn(0.001f, 0.03f)
                    val pointsJson = item.getJSONArray("points")
                    val points = buildList {
                        for (p in 0 until pointsJson.length()) {
                            val pair = pointsJson.getJSONArray(p)
                            add(
                                AnnotationPoint(
                                    x = pair.getDouble(0).toFloat().coerceIn(-2f, 3f),
                                    y = pair.getDouble(1).toFloat().coerceIn(-2f, 3f),
                                )
                            )
                        }
                    }
                    if (points.isNotEmpty()) add(AnnotationStroke(tool, color, width, points))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(uri: Uri, pageIndex: Int, strokes: List<AnnotationStroke>) {
        if (strokes.isEmpty()) {
            prefs.edit().remove(strokeKey(uri, pageIndex)).apply()
            return
        }
        val root = JSONArray()
        strokes.forEach { stroke ->
            val item = JSONObject()
                .put("tool", stroke.tool.name)
                .put("color", stroke.colorArgb)
                .put("width", stroke.widthFraction.toDouble())
            val points = JSONArray()
            stroke.points.forEach { point ->
                points.put(JSONArray().put(point.x.toDouble()).put(point.y.toDouble()))
            }
            item.put("points", points)
            root.put(item)
        }
        prefs.edit().putString(strokeKey(uri, pageIndex), root.toString()).apply()
    }

    fun loadTexts(uri: Uri, pageIndex: Int): List<AnnotationText> {
        val raw = prefs.getString(textKey(uri, pageIndex), null) ?: return emptyList()
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (i in 0 until root.length()) {
                    val item = root.getJSONObject(i)
                    val value = item.optString("text").trim()
                    if (value.isBlank()) continue
                    add(
                        AnnotationText(
                            id = item.optString("id", "text_$i"),
                            text = value,
                            x = item.optDouble("x", 0.5).toFloat().coerceIn(-2f, 3f),
                            y = item.optDouble("y", 0.5).toFloat().coerceIn(-2f, 3f),
                            widthFraction = item.optDouble("boxWidth", 0.24).toFloat().coerceIn(0.02f, 3f),
                            heightFraction = item.optDouble("boxHeight", 0.09).toFloat().coerceIn(0.02f, 3f),
                            colorArgb = item.optInt("color", 0xFF8A2430.toInt()),
                            sizeFraction = item.optDouble("size", 0.026).toFloat().coerceIn(0.012f, 0.09f),
                            bold = item.optBoolean("bold", true),
                            italic = item.optBoolean("italic", false),
                            underline = item.optBoolean("underline", false),
                            alignment = runCatching {
                                AnnotationTextAlign.valueOf(item.optString("alignment", AnnotationTextAlign.LEFT.name))
                            }.getOrDefault(AnnotationTextAlign.LEFT),
                            backgroundEnabled = false,
                            borderEnabled = false,
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveTexts(uri: Uri, pageIndex: Int, texts: List<AnnotationText>) {
        if (texts.isEmpty()) {
            prefs.edit().remove(textKey(uri, pageIndex)).apply()
            return
        }
        val root = JSONArray()
        texts.forEach { annotation ->
            root.put(
                JSONObject()
                    .put("id", annotation.id)
                    .put("text", annotation.text)
                    .put("x", annotation.x.toDouble())
                    .put("y", annotation.y.toDouble())
                    .put("boxWidth", annotation.widthFraction.toDouble())
                    .put("boxHeight", annotation.heightFraction.toDouble())
                    .put("color", annotation.colorArgb)
                    .put("size", annotation.sizeFraction.toDouble())
                    .put("bold", annotation.bold)
                    .put("italic", annotation.italic)
                    .put("underline", annotation.underline)
                    .put("alignment", annotation.alignment.name)
                    .put("background", annotation.backgroundEnabled)
                    .put("border", annotation.borderEnabled)
            )
        }
        prefs.edit().putString(textKey(uri, pageIndex), root.toString()).apply()
    }

    fun loadShapes(uri: Uri, pageIndex: Int): List<AnnotationShape> {
        val raw = prefs.getString(shapeKey(uri, pageIndex), null) ?: return emptyList()
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (i in 0 until root.length()) {
                    val item = root.getJSONObject(i)
                    add(
                        AnnotationShape(
                            id = item.optString("id", "shape_$i"),
                            type = runCatching {
                                AnnotationShapeType.valueOf(item.optString("type", AnnotationShapeType.ARROW.name))
                            }.getOrDefault(AnnotationShapeType.ARROW),
                            x1 = item.optDouble("x1", 0.35).toFloat().coerceIn(-2f, 3f),
                            y1 = item.optDouble("y1", 0.35).toFloat().coerceIn(-2f, 3f),
                            x2 = item.optDouble("x2", 0.55).toFloat().coerceIn(-2f, 3f),
                            y2 = item.optDouble("y2", 0.45).toFloat().coerceIn(-2f, 3f),
                            colorArgb = item.optInt("color", 0xFFE53935.toInt()),
                            widthFraction = item.optDouble("width", 0.0035).toFloat().coerceIn(0.001f, 0.025f),
                            fillEnabled = item.optBoolean("fill", false),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveShapes(uri: Uri, pageIndex: Int, shapes: List<AnnotationShape>) {
        if (shapes.isEmpty()) {
            prefs.edit().remove(shapeKey(uri, pageIndex)).apply()
            return
        }
        val root = JSONArray()
        shapes.forEach { shape ->
            root.put(
                JSONObject()
                    .put("id", shape.id)
                    .put("type", shape.type.name)
                    .put("x1", shape.x1.toDouble())
                    .put("y1", shape.y1.toDouble())
                    .put("x2", shape.x2.toDouble())
                    .put("y2", shape.y2.toDouble())
                    .put("color", shape.colorArgb)
                    .put("width", shape.widthFraction.toDouble())
                    .put("fill", shape.fillEnabled)
            )
        }
        prefs.edit().putString(shapeKey(uri, pageIndex), root.toString()).apply()
    }

    fun clearPage(uri: Uri, pageIndex: Int) {
        prefs.edit()
            .remove(strokeKey(uri, pageIndex))
            .remove(textKey(uri, pageIndex))
            .remove(shapeKey(uri, pageIndex))
            .apply()
    }

    fun clearAll(uri: Uri) {
        val digest = digest(uri)
        val prefixes = listOf(
            "annotation_${digest}_",
            "annotation_text_${digest}_",
            "annotation_shape_${digest}_",
        )
        val editor = prefs.edit()
        prefs.all.keys.filter { key -> prefixes.any(key::startsWith) }.forEach(editor::remove)
        editor.apply()
    }

    fun migrateUri(oldUri: Uri, newUri: Uri) {
        if (oldUri == newUri) return
        val oldDigest = digest(oldUri)
        val newDigest = digest(newUri)
        val prefixes = listOf(
            "annotation_${oldDigest}_" to "annotation_${newDigest}_",
            "annotation_text_${oldDigest}_" to "annotation_text_${newDigest}_",
            "annotation_shape_${oldDigest}_" to "annotation_shape_${newDigest}_",
        )
        val editor = prefs.edit()
        prefs.all.forEach { (storedKey, value) ->
            if (value !is String) return@forEach
            prefixes.forEach { (oldPrefix, newPrefix) ->
                if (storedKey.startsWith(oldPrefix)) {
                    val suffix = storedKey.removePrefix(oldPrefix)
                    editor.putString(newPrefix + suffix, value)
                    editor.remove(storedKey)
                }
            }
        }
        editor.apply()
    }

    private fun strokeKey(uri: Uri, pageIndex: Int): String = "annotation_${digest(uri)}_$pageIndex"
    private fun textKey(uri: Uri, pageIndex: Int): String = "annotation_text_${digest(uri)}_$pageIndex"
    private fun shapeKey(uri: Uri, pageIndex: Int): String = "annotation_shape_${digest(uri)}_$pageIndex"

    private fun digest(uri: Uri): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val PREFS_NAME = "partitures_festeres_annotations"
    }
}
