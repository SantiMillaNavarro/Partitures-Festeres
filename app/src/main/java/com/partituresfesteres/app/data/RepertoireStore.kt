package com.partituresfesteres.app.data

import android.content.Context
import com.partituresfesteres.app.model.Repertoire
import com.partituresfesteres.app.model.RepertoireEntry
import org.json.JSONArray
import org.json.JSONObject

class RepertoireStore(context: Context) {
    private val prefs = context.getSharedPreferences("partitures_festeres_repertoires", Context.MODE_PRIVATE)

    fun load(): List<Repertoire> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val objectJson = array.getJSONObject(i)
                    val entriesJson = objectJson.optJSONArray("entries") ?: JSONArray()
                    val entries = buildList {
                        for (j in 0 until entriesJson.length()) {
                            val entry = entriesJson.getJSONObject(j)
                            add(
                                RepertoireEntry(
                                    uriString = entry.optString("uriString"),
                                    fileName = entry.optString("fileName"),
                                    displayName = entry.optString("displayName"),
                                    relativePath = entry.optString("relativePath"),
                                )
                            )
                        }
                    }
                    add(
                        Repertoire(
                            id = objectJson.getString("id"),
                            name = objectJson.getString("name"),
                            entries = entries,
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(repertoires: List<Repertoire>) {
        val array = JSONArray()
        repertoires.forEach { repertoire ->
            val entries = JSONArray()
            repertoire.entries.forEach { entry ->
                entries.put(
                    JSONObject()
                        .put("uriString", entry.uriString)
                        .put("fileName", entry.fileName)
                        .put("displayName", entry.displayName)
                        .put("relativePath", entry.relativePath)
                )
            }
            array.put(
                JSONObject()
                    .put("id", repertoire.id)
                    .put("name", repertoire.name)
                    .put("entries", entries)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "repertoires_json"
    }
}
