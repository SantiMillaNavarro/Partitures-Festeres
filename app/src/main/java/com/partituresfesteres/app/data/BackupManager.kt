package com.partituresfesteres.app.data

import android.content.Context
import android.net.Uri
import com.partituresfesteres.app.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Exporta i restaura només les dades internes de l'app.
 * Els PDF continuen vivint en la carpeta arrel i no es dupliquen en la còpia.
 */
class BackupManager(private val context: Context) {
    private val preferenceFiles = listOf(
        "partitures_festeres_repertoires",
        "partitures_festeres_library_state",
        "partitures_festeres_annotations",
        "partitures_festeres_viewer",
        PdfPageAdjustmentStore.PREFS_NAME,
        SettingsStore.PREFS_NAME,
    )

    fun exportTo(uri: Uri): Result<Unit> = runCatching {
        val root = JSONObject()
            .put("format", "partitures-festeres-backup")
            .put("version", 1)
            .put("createdAt", System.currentTimeMillis())

        val preferences = JSONObject()
        preferenceFiles.forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val values = JSONObject()
            prefs.all.forEach { (key, value) ->
                values.put(key, encodeValue(value))
            }
            preferences.put(name, values)
        }
        root.put("preferences", preferences)

        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { writer ->
            writer.write(root.toString(2))
        } ?: error(context.getString(R.string.backup_file_create_error))
    }

    fun restoreFrom(uri: Uri): Result<Unit> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        } ?: error(context.getString(R.string.backup_file_read_error))

        val root = JSONObject(text)
        require(root.optString("format") == "partitures-festeres-backup") {
            context.getString(R.string.backup_invalid_file)
        }
        val preferences = root.getJSONObject("preferences")

        preferenceFiles.forEach { name ->
            if (!preferences.has(name)) return@forEach
            val values = preferences.getJSONObject(name)
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            val keys = values.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                decodeInto(editor, key, values.getJSONObject(key))
            }
            editor.commit()
        }
    }

    private fun encodeValue(value: Any?): JSONObject {
        val encoded = JSONObject()
        when (value) {
            is String -> encoded.put("type", "string").put("value", value)
            is Int -> encoded.put("type", "int").put("value", value)
            is Long -> encoded.put("type", "long").put("value", value)
            is Float -> encoded.put("type", "float").put("value", value.toDouble())
            is Boolean -> encoded.put("type", "boolean").put("value", value)
            is Set<*> -> {
                val array = JSONArray()
                value.filterIsInstance<String>().forEach { array.put(it) }
                encoded.put("type", "strings").put("value", array)
            }
            else -> encoded.put("type", "string").put("value", value?.toString().orEmpty())
        }
        return encoded
    }

    private fun decodeInto(editor: android.content.SharedPreferences.Editor, key: String, encoded: JSONObject) {
        when (encoded.getString("type")) {
            "string" -> editor.putString(key, encoded.optString("value"))
            "int" -> editor.putInt(key, encoded.optInt("value"))
            "long" -> editor.putLong(key, encoded.optLong("value"))
            "float" -> editor.putFloat(key, encoded.optDouble("value").toFloat())
            "boolean" -> editor.putBoolean(key, encoded.optBoolean("value"))
            "strings" -> {
                val array = encoded.optJSONArray("value") ?: JSONArray()
                val set = buildSet {
                    for (i in 0 until array.length()) add(array.getString(i))
                }
                editor.putStringSet(key, set)
            }
        }
    }
}
