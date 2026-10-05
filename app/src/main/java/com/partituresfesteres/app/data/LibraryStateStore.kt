package com.partituresfesteres.app.data

import android.content.Context
import com.partituresfesteres.app.model.LibraryPdf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistix favorits i recents sense tocar els PDF originals.
 *
 * Guardem una còpia mínima de les metadades perquè les pantalles continuen
 * sent útils encara que l'índex global encara s'estiga reconstruint.
 */
class LibraryStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("partitures_festeres_library_state", Context.MODE_PRIVATE)

    fun loadFavorites(): List<LibraryPdf> = loadList(KEY_FAVORITES)

    fun saveFavorites(items: List<LibraryPdf>) {
        saveList(KEY_FAVORITES, items.distinctBy { it.uri.toString() })
    }

    fun loadRecents(): List<LibraryPdf> = loadList(KEY_RECENTS)

    fun saveRecents(items: List<LibraryPdf>) {
        saveList(KEY_RECENTS, items.distinctBy { it.uri.toString() }.take(50))
    }

    fun recordRecent(pdf: LibraryPdf, maxItems: Int = 50): List<LibraryPdf> {
        val updated = buildList {
            add(pdf)
            loadRecents().forEach { existing ->
                if (existing.uri != pdf.uri) add(existing)
            }
        }.take(maxItems)
        saveList(KEY_RECENTS, updated)
        return updated
    }

    private fun loadList(key: String): List<LibraryPdf> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        LibraryPdf(
                            fileName = obj.optString("fileName"),
                            displayName = obj.optString("displayName"),
                            uri = android.net.Uri.parse(obj.optString("uriString")),
                            relativePath = obj.optString("relativePath"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveList(key: String, items: List<LibraryPdf>) {
        val array = JSONArray()
        items.forEach { pdf ->
            array.put(
                JSONObject()
                    .put("uriString", pdf.uri.toString())
                    .put("fileName", pdf.fileName)
                    .put("displayName", pdf.displayName)
                    .put("relativePath", pdf.relativePath)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favorites_json"
        private const val KEY_RECENTS = "recents_json"
    }
}
